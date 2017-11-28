/*
Copyright 2017-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License"). 
You may not use this file except in compliance with the License. 
A copy of the License is located at

   http://aws.amazon.com/apache2.0/

or in the "license" file accompanying this file. 
This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and limitations under the License.
*/
package com.amazonaws.kinesisvideo.parser.ebml;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * This class is used to parse a stream of EBML.
 * It is based on the ebml specification published by the Matroska Org
 * (https://github.com/Matroska-Org/ebml-specification/blob/master/specification.markdown).
 *
 * A new instance of this object is created for a new stream of EBML (the response stream for a GetMedia call).
 * A new instance is configured with a {@link EBMLTypeInfoProvider} that provides the semantics for the EBML document
 * being parsed and a {@link EBMLParserCallbacks} that receives callbacks as the parser detects different EBML elements.
 * Once an instance of the EBML parser is created, the parse method is invoked repeatedly.
 * A stream of EBML is encapsulated by a {@link ParserByteSource} and is an argument to the parse method.
 * The parse method is non-blocking and consumes all the data passed to it in each invocation.
 * As the parser detects EBML elements it invokes methods on the {@link EBMLParserCallbacks}.
 * Once all the data in an EBML stream has being sent to the parser, the method closeParser is called to shutdown
 * the parser.
 *
 * TODO: add implementation details.
 *
 */

@Slf4j
public class EBMLParser {

    private static final int BYTE_MASK = 0xFF;
    //TODO: have it be an argument, either constructor or method
    private static final int DEFAULT_MAX_CONTENT_BYTES_IN_ONE_PASS = 8192;

    private final EBMLTypeInfoProvider typeInfoProvider;
    private final Stack<EBMLParserInternalElement> masterElements;
    private final EBMLParserCallbacks callbacks;
    private final int maxContentBytesInOnePass;
    private final ByteBuffer skipBuffer;

    private long elementCount = 0;
    private long totalBytesRead = 0;

    @Getter(AccessLevel.PACKAGE)
    private boolean endOfStream;

    @Getter(AccessLevel.PUBLIC)
    private boolean closed;
    private EBMLParserInternalElement currentElement;
    private ReplayIdAndSizeBuffer replayIdAndSizeBuffer;


    public EBMLParser(EBMLTypeInfoProvider typeInfoProvider, EBMLParserCallbacks callbacks) {
        this(typeInfoProvider, callbacks, DEFAULT_MAX_CONTENT_BYTES_IN_ONE_PASS);
    }

    public EBMLParser(EBMLTypeInfoProvider typeInfoProvider,
            EBMLParserCallbacks callbacks,
            int maxContentBytesInOnePass) {
        this.typeInfoProvider = typeInfoProvider;
        this.callbacks = callbacks;
        this.replayIdAndSizeBuffer =
                new ReplayIdAndSizeBuffer(EBMLUtils.EBML_ID_MAX_BYTES + EBMLUtils.EBML_SIZE_MAX_BYTES);
        createNewCurrentElementInfo();
        this.masterElements = new Stack<>();
        this.maxContentBytesInOnePass = maxContentBytesInOnePass;
        this.skipBuffer = ByteBuffer.allocate(maxContentBytesInOnePass);
        log.info("Creating EBMLParser with maxContentBytesInOnePass {}", this.maxContentBytesInOnePass);
    }

    public void parse(ParserByteSource byteSource) {
        try (CallState callState = new CallState(byteSource)) {
            while (callState.shouldContinueParsing()) {
                if (log.isDebugEnabled()) {
                    log.debug("Current element read state {}", currentElement.currentElementReadState);
                }
                switch (currentElement.currentElementReadState) {
                    case NEW:
                        //check if any master elements are done because their end offset has been reached.
                        removeMasterElementsBasedOnSizeEnd();

                        currentElement.readId(callState);
                        break;
                    case ID_DONE:
                        currentElement.readSize(callState);
                        break;
                    case SIZE_DONE:
                        currentElement.updateTypeInfo(typeInfoProvider);
                        //check if any master elements are done because an equal or higher level
                        //element is reached.
                        removeMasterElementsBasedOnLevel();


                        //Call onstartForElement();
                        if (currentElement.isKnownType()) {
                            log.debug("Invoking onStartElement for current element {}", currentElement);
                            callbacks.onStartElement(currentElement.getMetadata(),
                                    currentElement.getDataSize(),
                                    replayIdAndSizeBuffer.getByteBuffer(),
                                    this::currentElementPath);
                        }

                        startReadingContentBasedOnType();
                        break;
                    case CONTENT_READING:
                        Validate.isTrue(currentElement.isKnownType(),
                                "We should read only from elements with known types");
                        currentElement.readContent(callState, callState, callbacks, maxContentBytesInOnePass);
                        break;
                    case CONTENT_SKIPPING:
                        Validate.isTrue(!currentElement.isKnownType(), "We should skip data for unknown elements only");
                        skipBuffer.rewind();
                        currentElement.skipContent(callState, callState, skipBuffer);
                        break;
                    case FINISHED:
                        invokeOnEndElementCallback(currentElement);
                        //check if any master elements are done because their end offset has been reached.
                        removeMasterElementsBasedOnSizeEnd();


                        createNewCurrentElementInfo();
                        break;
                    default:
                        throw new IllegalArgumentException("Unexpected ElementReadState");
                }
            }
            log.debug("Stopping parsing");
            if (endOfStream) {
                closeParser();
            }

        }
    }


    public void closeParser() {
        if (!closed) {
            log.info("Closing EBMLParser");
            //close current element
            if (currentElement != null && currentElement.isKnownType()) {
                log.info("Closing with currentElement {} still set, invoking end element callback on it",
                        currentElement);
                invokeOnEndElementCallback(currentElement);
                currentElement = null;
            }

            log.info("Closing with {} master elements on stack, invoking end element callback on them",
                    masterElements.size());
            while (!masterElements.isEmpty()) {
                EBMLParserInternalElement top = masterElements.pop();
                //TODO: see if we need to add a flag to indicate unclean close
                invokeOnEndElementCallback(top);
            }
        }
        closed = true;
    }

    private void startReadingContentBasedOnType() {
        if (!currentElement.isKnownType()) {
            Validate.isTrue(!currentElement.isUnknownLength(), "Cannot skip element of unknown length");
            currentElement.startSkippingContent();
            log.warn("Will skip content for element number {} with unknown id {} datasize {}",
                    currentElement.getElementCount(),
                    currentElement.getId(),
                    currentElement.getDataSize());
        } else {
            if (currentElement.getTypeInfo().getType() == EBMLTypeInfo.TYPE.MASTER) {
                //Mark the master element as started although it will consist of
                //child elements. So, push it into the stack of master elements whose
                //contents are currently being read.
                currentElement.startReadingContent();
                masterElements.push(currentElement);
                createNewCurrentElementInfo();
            } else {
                //A non-master element should not have unknown or infinite length
                //as that prevents the parser finding the end of the element.
                Validate.isTrue(!currentElement.isUnknownLength(),
                        "A non-master element should not have unknown length");

                //start reading contents.
                currentElement.startReadingContent();
            }
        }
    }

    private void removeMasterElementsBasedOnLevel() {
        if (!currentElement.isKnownType()) {
            return;
        }
        if (!currentElement.getTypeInfo().isGlobal()) {
            while (!masterElements.isEmpty()) {
                EBMLParserInternalElement top = masterElements.peek();
                //For handling master elements with the wrong size (such as segments)
                //We should finish master elements of known size is another element of the same or
                //lower level is found.

                Validate.isTrue(currentElement.getElementCount() != top.getElementCount());
                if (currentElement.getTypeInfo().getLevel() <= top.getTypeInfo().getLevel()) {
                    log.debug("Removing master element {} based on level of current element {}", top, currentElement);
                    masterElements.pop();
                    invokeOnEndElementCallback(top);
                } else {
                    break;
                }

            }
        }
    }

    private void removeMasterElementsBasedOnSizeEnd() {
        if (!currentElement.isKnownType()) {
            return;
        }
        while (!masterElements.isEmpty()) {
            EBMLParserInternalElement top = masterElements.peek();
            if (!top.isUnknownLength()) {
                if (top.endOffSet() <= totalBytesRead) {
                    log.debug("Removing master element {} based on size end {}", top, totalBytesRead);
                    masterElements.pop();
                    invokeOnEndElementCallback(top);
                } else {
                    break;
                }
            } else {
                break;
            }
        }
    }

    private List<EBMLElementMetaData> currentElementPath() {
        return masterElements.stream().map(EBMLParserInternalElement::getMetadata).collect(Collectors.toList());
    }

    private void invokeOnEndElementCallback(EBMLParserInternalElement finishedElement) {
        if (finishedElement.isKnownType()) {
            log.debug("Invoking onStartElement for current element {}", finishedElement);
            callbacks.onEndElement(finishedElement.getMetadata(), this::currentElementPath);
        }
    }


    private void createNewCurrentElementInfo() {
        currentElement = new EBMLParserInternalElement(totalBytesRead, elementCount);
        elementCount++;
        replayIdAndSizeBuffer.init(totalBytesRead);
    }

    /**
     * This internal class maintains state for each parse call.
     */
    @RequiredArgsConstructor
    private class CallState implements Closeable, TrackingReplayableIdAndSizeByteSource, ParserBulkByteSource {
        private boolean parseMore = true;
        private final ParserByteSource byteSource;
        @Setter
        private long readOffsetForReplayBuffer;

        @Override
        public long getTotalBytesRead() {
            return totalBytesRead;
        }

        @Override
        public void close() {
        }

        boolean shouldContinueParsing() {
            return !endOfStream && parseMore && callbacks.continueParsing();
        }

        @Override
        public boolean checkAndReadIntoReplayBuffer(int len) {
            if (parseMore) {
                int availableInReplayBuffer = replayIdAndSizeBuffer.availableAfter(readOffsetForReplayBuffer);
                Validate.isTrue(availableInReplayBuffer >= 0);
                if (availableInReplayBuffer >= len) {
                    return true;
                } else {
                    int numBytesToRead = len - availableInReplayBuffer;
                    parseMore = byteSource.available() >= numBytesToRead;
                    numBytesToRead = Math.min(numBytesToRead, byteSource.available());
                    for (int i = 0; i < numBytesToRead; i++) {
                        readFromByteSourceIntoReplayBuffer();
                    }

                }
            }
            return parseMore;
        }

        @Override
        public int readByte() {
            if (replayIdAndSizeBuffer.inReplayBuffer(readOffsetForReplayBuffer)) {
                byte result = replayIdAndSizeBuffer.getByteFromOffset(readOffsetForReplayBuffer);
                readOffsetForReplayBuffer++;
                return result & BYTE_MASK;
            } else {
                int result = readFromByteSourceIntoReplayBuffer();
                readOffsetForReplayBuffer++;
                return result;
            }
        }

        private int readFromByteSourceIntoReplayBuffer() {
            int result = byteSource.readByte();
            if (result == -1) {
                markAsEndofStream();
                return -1;
            }
            Validate.inclusiveBetween(0, BYTE_MASK, result);
            replayIdAndSizeBuffer.addByte((byte) result);
            totalBytesRead++;
            return result;
        }

        @Override
        public int availableForContent() {
            if (parseMore) {
                int availableBytes = byteSource.available();
                if (availableBytes == 0) {
                    parseMore = false;
                }
                return availableBytes;
            }
            return 0;
        }

        @Override
        public int readBytes(ByteBuffer dest, int numBytes) {
            int readBytes = byteSource.readBytes(dest, numBytes);
            if (readBytes == -1) {
                markAsEndofStream();
                return readBytes;
            }
            Validate.isTrue(readBytes >= 0);
            totalBytesRead += readBytes;
            return readBytes;
        }

        private void markAsEndofStream() {
            endOfStream = true;
            parseMore = false;
        }

    }
}
