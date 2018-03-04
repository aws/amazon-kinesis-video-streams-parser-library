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
package com.amazonaws.kinesisvideo.parser.mkv;

import com.amazonaws.kinesisvideo.parser.ebml.EBMLParser;
import com.amazonaws.kinesisvideo.parser.ebml.EBMLTypeInfo;
import com.amazonaws.kinesisvideo.parser.ebml.ParserByteSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;

/**
 * This class is used to read mkv elements from an mkv stream in a non-blocking way.
 * This is a streaming mkv reader that provides mkv elements as they become completely available.
 *
 * mightHaveNext() returns true, when the reader might have more data. If it returns false, we know that
 * there can be no more data and we can stop reading.
 *
 * nextIfAvailable returns the next MkvElement if one is available, otherwise it returns Optional.absent()
 * There are three possible types of MkvElements:
 * 1. {@link MkvStartMasterElement} which represents the start of an Mkv master element.
 * 2. {@link MkvEndMasterElement} which represents the end of an Mkv master element.
 * 3. {@link MkvDataElement} which represents a non-master MKV element that contains actual data of a particular type.
 *
 * When nextIfAvailable returns a MkvDataElement, its data buffer containing the raw bytes of the element's content
 * can only be accessed before nextIfAvailable is called again. To retain the value of the MkvDataElement for later use
 * call getValueCopy() on it. It copies the raw bytes and interprets it based on the type of the MkvDataElement.
 *
 */
@Slf4j
public class StreamingMkvReader {
    private final boolean requirePath;
    private final Set<EBMLTypeInfo> typeInfosToRead;
    private final ParserByteSource byteSource;
    private final EBMLParser parser;
    private final MkvStreamReaderCallback mkvStreamReaderCallback;
    private Optional<MkvDataElement> previousDataElement;


    StreamingMkvReader(boolean requirePath,
            Collection<EBMLTypeInfo> typeInfosToRead,
            ParserByteSource byteSource) {
        this(requirePath, typeInfosToRead, byteSource, OptionalInt.empty());
    }

    StreamingMkvReader(boolean requirePath,
            Collection<EBMLTypeInfo> typeInfosToRead,
            ParserByteSource byteSource,
            OptionalInt maxContentBytesAtOnce) {
        this.requirePath = requirePath;
        typeInfosToRead.stream().forEach(t -> Validate.isTrue(t.getType() != EBMLTypeInfo.TYPE.MASTER));
        this.typeInfosToRead = new HashSet(typeInfosToRead);

        this.byteSource = byteSource;
        this.mkvStreamReaderCallback = new MkvStreamReaderCallback(this.requirePath, elementFilter());
        this.previousDataElement = Optional.empty();
        MkvTypeInfoProvider typeInfoProvider = new MkvTypeInfoProvider();
        try {
            typeInfoProvider.load();
        } catch (IllegalAccessException e) {
            //TODO: fix this
            throw new RuntimeException("Could not load mkv info", e);
        }
        if (maxContentBytesAtOnce.isPresent()) {
            this.parser = new EBMLParser(typeInfoProvider, mkvStreamReaderCallback, maxContentBytesAtOnce.getAsInt());
        } else {
            this.parser = new EBMLParser(typeInfoProvider, mkvStreamReaderCallback);
        }
    }

    public static StreamingMkvReader createDefault(ParserByteSource byteSource) {
        return new StreamingMkvReader(true, new ArrayList<>(), byteSource, OptionalInt.empty());
    }

    public static StreamingMkvReader createWithMaxContentSize(ParserByteSource byteSource, int maxContentBytesAtOnce) {
        return new StreamingMkvReader(true, new ArrayList<>(), byteSource, OptionalInt.of(maxContentBytesAtOnce));
    }

    public boolean mightHaveNext() {
        if (mkvStreamReaderCallback.hasElementsToReturn()) {
            log.debug("ReaderCallback has elements to return ");
            return true;
        }
        if( !byteSource.eof() && !parser.isClosed()) {
            return true;
        } else if (byteSource.eof()) {
            log.info("byteSource has reached eof");
            if(!parser.isClosed()) {
                log.info("byteSource has reached eof and calling close on parser");
                parser.closeParser();
                return true;
            }
        }
        log.info("No more elements to process byteSource.eof {} parser.isClosed {} ",
                byteSource.eof(),
                parser.isClosed());
        return false;
    }

    public Optional<MkvElement> nextIfAvailable() {
        if (mkvStreamReaderCallback.hasElementsToReturn()) {
            if (log.isDebugEnabled()) {
                log.debug("ReaderCallback has elements to return. Return element from it.");
            }
            return getMkvElementToReturn();
        }
        parser.parse(byteSource);
        return getMkvElementToReturn();
    }

    /**
     * Method to apply a visitor in a loop to all the elements returns by a StreamingMkvReader.
     * This method polls for the next available element in a tight loop.
     * It might not be suitable in cases where the user wants to interleave some other activity between polling.
     *
     * @param visitor The visitor to apply.
     * @throws MkvElementVisitException If the visitor fails.
     */
    public void apply(MkvElementVisitor visitor) throws MkvElementVisitException {
        while (this.mightHaveNext()) {
            Optional<MkvElement> mkvElementOptional = this.nextIfAvailable();
            if (mkvElementOptional.isPresent()) {
                mkvElementOptional.get().accept(visitor);
            }
        }
    }


    private Optional<MkvElement> getMkvElementToReturn() {
        Optional<MkvElement> currentElement = mkvStreamReaderCallback.getMkvElementIfAvailable();

        //Null out the data buffer of the previous data element before returning the next element.
        //We do this because the same data buffer gets reused for consecutive data elements and we
        //do not want users to mistakenly reuse data buffers on cached data elements.
        //They should use the getValueCopy to retain the data.
        if (currentElement.isPresent()) {
            if (previousDataElement.isPresent()) {
                previousDataElement.get().clearDataBuffer();
                previousDataElement = Optional.empty();
            }
            if (!currentElement.get().isMaster()) {
                previousDataElement = Optional.of((MkvDataElement) currentElement.get());
            }
        }
        return currentElement;
    }

    private Predicate<EBMLTypeInfo> elementFilter() {
        if (typeInfosToRead.size() == 0) {
            return (t) -> t.getType() != EBMLTypeInfo.TYPE.MASTER;
        } else {
            return typeInfosToRead::contains;
        }
    }

}
