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
package com.amazonaws.kinesisvideo.parser.utilities;


import com.amazonaws.kinesisvideo.parser.ebml.EBMLTypeInfo;
import com.amazonaws.kinesisvideo.parser.ebml.MkvTypeInfos;
import com.amazonaws.kinesisvideo.parser.mkv.MkvValue;
import com.amazonaws.kinesisvideo.parser.mkv.visitors.CompositeMkvElementVisitor;
import com.amazonaws.kinesisvideo.parser.mkv.visitors.CountVisitor;
import com.amazonaws.kinesisvideo.parser.mkv.MkvDataElement;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElement;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitException;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitor;
import com.amazonaws.kinesisvideo.parser.mkv.MkvEndMasterElement;
import com.amazonaws.kinesisvideo.parser.mkv.MkvStartMasterElement;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.amazonaws.kinesisvideo.parser.utilities.OutputSegmentMerger.MergeState.BUFFERING_CLUSTER_START;
import static com.amazonaws.kinesisvideo.parser.utilities.OutputSegmentMerger.MergeState.EMITTING;

/**
 * Merge the individual consecutive mkv streams output by a GetMedia call into one or more mkv streams.
 * Each mkv stream has one segment.
 * This class merges consecutive mkv streams as long as they share the same track and EBML information.
 * It merges based on the elements that are the child elements of the track and EBML master elements.
 * It collects all the child elements for each master element for each mkv stream.
 * It compares the collected child elements of each master element in one mkv stream
 * with the collected child elements of the same master element in the previous mkv stream.
 * If the test passes for both thr track and EBML master elements in an mkv stream,
 * its headers up to its first cluster are not emitted to the output stream, otherwise they are emitted.
 * All data within or after cluster is emitted.
 *
 * The Merger can also be configured to stop emitting to the output stream once it finds the first non matching
 * mkv stream or segment. This is useful when the user wants the different merged mkv streams to go to different
 * destinations such as files.
 *
 */
@Slf4j
public class OutputSegmentMerger extends CompositeMkvElementVisitor {
    private final OutputStream outputStream;
    private final List<EBMLTypeInfo> parentElementsToCompare;
    private final List<CollectorState> collectorStates;

    enum MergeState { NEW, BUFFERING_SEGMENT, BUFFERING_CLUSTER_START, EMITTING, DONE }
    private MergeState state = MergeState.NEW;

    private final MergeVisitor mergeVisitor = new MergeVisitor();

    private final ByteArrayOutputStream bufferingSegmentStream = new ByteArrayOutputStream();
    private WritableByteChannel bufferingSegmentChannel;

    private final ByteArrayOutputStream bufferingClusterStream = new ByteArrayOutputStream();
    private WritableByteChannel bufferingClusterChannel;


    private final CountVisitor countVisitor;

    private final WritableByteChannel outputChannel;
    private final boolean stopAtFirstNonMatchingSegment;
    private long emittedSegments = 0;

    private Optional<BigInteger> lastClusterTimecode = Optional.empty();


    private static final ByteBuffer SEGMENT_ELEMENT_WITH_UNKNOWN_LENGTH =
            ByteBuffer.wrap(new byte[] { (byte) 0x18, (byte) 0x53, (byte) 0x80, (byte) 0x67,
                    (byte) 0x01, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                    (byte) 0xFF, (byte) 0xFF });

    private static final ByteBuffer VOID_ELEMENT_WITH_SIZE_ONE =
            ByteBuffer.wrap(new byte [] { (byte) 0xEC, (byte) 0x81, (byte) 0x42 });


    OutputSegmentMerger(OutputStream outputStream,
            List<EBMLTypeInfo> masterElementsToMergeOn,
            CountVisitor countVisitor,
            boolean stopAtFirstNonMatchingSegment) {
        super(countVisitor);
        childVisitors.add(mergeVisitor);
        this.countVisitor = countVisitor;

        this.outputStream = outputStream;
        this.outputChannel = Channels.newChannel(this.outputStream);
        this.bufferingSegmentChannel = Channels.newChannel(bufferingSegmentStream);
        this.bufferingClusterChannel = Channels.newChannel(bufferingClusterStream);
        this.parentElementsToCompare = masterElementsToMergeOn;
        this.collectorStates =
                parentElementsToCompare.stream().map(pt -> new CollectorState(pt)).collect(Collectors.toList());
        this.stopAtFirstNonMatchingSegment = stopAtFirstNonMatchingSegment;
    }

    /**
     * Create an OutputSegmentMerger.
     *
     * @param outputStream The output stream to write the merged segments to.
     * @return an OutputSegmentMerger that can be used to merge the segments from Kinesis Video that share a common header.
     */
    public static OutputSegmentMerger createDefault(OutputStream outputStream) {
        return new OutputSegmentMerger(outputStream, getTypesToMergeOn(), getCountVisitor(), false);
    }

    /**
     * Create an OutputSegmentMerger that stops emitting after detecting the first non matching segment.
     *
     * @param outputStream The output stream to write the merged segments to.
     * @return an OutputSegmentMerger that can be used to merge the segments from Kinesis Video that share a common header.
     */
    public static OutputSegmentMerger createToStopAtFirstNonMatchingSegment(OutputStream outputStream) {
        return new OutputSegmentMerger(outputStream, getTypesToMergeOn(), getCountVisitor(), true);
    }

    private static List<EBMLTypeInfo> getTypesToMergeOn() {
        List<EBMLTypeInfo> typeInfosToMergeOn = new ArrayList<>();
        typeInfosToMergeOn.add(MkvTypeInfos.TRACKS);
        typeInfosToMergeOn.add(MkvTypeInfos.EBML);
        return typeInfosToMergeOn;
    }

    private static CountVisitor getCountVisitor() {
        List<EBMLTypeInfo> typesToCountList = new ArrayList<>();
        typesToCountList.add(MkvTypeInfos.CLUSTER);
        typesToCountList.add(MkvTypeInfos.SEGMENT);
        typesToCountList.add(MkvTypeInfos.SIMPLEBLOCK);
        return new CountVisitor(typesToCountList);
    }


    public int getClustersCount() {
        return countVisitor.getCount(MkvTypeInfos.CLUSTER);
    }

    public int getSegmentsCount() {
        return countVisitor.getCount(MkvTypeInfos.SEGMENT);
    }

    public int getSimpleBlocksCount() {
        return countVisitor.getCount(MkvTypeInfos.SIMPLEBLOCK);
    }

    public boolean isDone() {
        return MergeState.DONE == state;
    }

    private class MergeVisitor extends MkvElementVisitor {

        @Override
        public void visit(MkvStartMasterElement startMasterElement) throws MkvElementVisitException {
            try {
                switch (state) {
                    case NEW:
                        //Only the ebml header is expected in the new state
                        Validate.isTrue(MkvTypeInfos.EBML.equals(startMasterElement.getElementMetaData().getTypeInfo()),
                                "EBML should be the only expected element type when a new MKV stream is expected");
                        log.info("Detected start of EBML element, transitioning from {} to BUFFERING", state);
                        //Change state to buffering and bufferAndCollect this element.
                        state = MergeState.BUFFERING_SEGMENT;
                        bufferAndCollect(startMasterElement);
                        break;
                    case BUFFERING_SEGMENT:
                        //if it is the cluster start element check if the buffered elements should be emitted and
                        // then change state to emitting, emit this the element as well.
                        EBMLTypeInfo startElementTypeInfo = startMasterElement.getElementMetaData().getTypeInfo();
                        if (MkvTypeInfos.CLUSTER.equals(startElementTypeInfo) || MkvTypeInfos.TAGS.equals(
                                startElementTypeInfo)) {
                            boolean shouldEmitSegment = shouldEmitBufferedSegmentData();

                            if (shouldEmitSegment) {
                                if (stopAtFirstNonMatchingSegment && emittedSegments >= 1) {
                                    log.info("Detected start of element {} transitioning from {} to DONE",
                                            startElementTypeInfo,
                                            state);
                                    state = MergeState.DONE;
                                } else {
                                    emitBufferedSegmentData(shouldEmitSegment);
                                    resetChannels();
                                    log.info("Detected start of element {} transitioning from {} to EMITTING",
                                            startElementTypeInfo,
                                            state);
                                    state = EMITTING;
                                    emit(startMasterElement);
                                }
                            } else {
                                log.info("Detected start of element {} transitioning from {} to BUFFERING_CLUSTER_START",
                                        startElementTypeInfo,
                                        state);
                                state = BUFFERING_CLUSTER_START;
                                bufferAndCollect(startMasterElement);
                            }
                        } else {
                            bufferAndCollect(startMasterElement);
                        }
                        break;
                    case BUFFERING_CLUSTER_START:
                        bufferAndCollect(startMasterElement);
                        break;
                    case EMITTING:
                        emit(startMasterElement);
                        break;
                    case DONE:
                        log.warn("OutputSegmentMerger is already done. It will not process any more elements.");
                        break;


                }
            } catch(IOException ie) {
                throw new MkvElementVisitException("IOException in merge visitor", ie);
            }

        }

        @Override
        public void visit(MkvEndMasterElement endMasterElement) throws MkvElementVisitException {
                switch (state) {
                    case NEW:
                        Validate.isTrue(false,
                                "Should not start with an EndMasterElement " + endMasterElement.toString());
                        break;
                    case BUFFERING_SEGMENT:
                    case BUFFERING_CLUSTER_START:
                        collect(endMasterElement);
                        break;
                    case EMITTING:
                        if (MkvTypeInfos.SEGMENT.equals(endMasterElement.getElementMetaData().getTypeInfo())) {
                            log.info("Detected end of segment element, transitioning from {} to NEW", state);
                            state = MergeState.NEW;
                            resetCollectors();
                        }
                        break;
                    case DONE:
                        log.warn("OutputSegmentMerger is already done. It will not process any more elements.");
                        break;
                }
        }

        @Override
        public void visit(MkvDataElement dataElement) throws MkvElementVisitException {
            try {
                switch (state) {
                    case NEW:
                        Validate.isTrue(false, "Should not start with a data element " + dataElement.toString());
                        break;
                    case BUFFERING_SEGMENT:
                        bufferAndCollect(dataElement);
                        break;
                    case BUFFERING_CLUSTER_START:
                        if (MkvTypeInfos.TIMECODE.equals(dataElement.getElementMetaData().getTypeInfo())) {
                            MkvValue<BigInteger> currentTimeCode = dataElement.getValueCopy();
                            if (lastClusterTimecode.isPresent()
                                    && currentTimeCode.getVal().compareTo(lastClusterTimecode.get()) <= 0) {
                                if (stopAtFirstNonMatchingSegment && emittedSegments >= 1) {
                                    log.info("Detected time code going back from {} to {}, state from {} to DONE",
                                            lastClusterTimecode,
                                            currentTimeCode.getVal(),
                                            state);
                                    state = MergeState.DONE;
                                } else {
                                    //emit buffered segment start
                                    emitBufferedSegmentData(true);
                                }
                            }
                            if (!isDone()) {
                                emitClusterStart();
                                resetChannels();
                                state = EMITTING;
                                emit(dataElement);
                                lastClusterTimecode = Optional.of(currentTimeCode.getVal());
                            }
                        } else {
                            bufferAndCollect(dataElement);
                        }
                        break;
                    case EMITTING:
                        if (MkvTypeInfos.TIMECODE.equals(dataElement.getElementMetaData().getTypeInfo())) {
                            MkvValue<BigInteger> currentTimeCode = dataElement.getValueCopy();
                            lastClusterTimecode = Optional.of(currentTimeCode.getVal());
                        }
                        emit(dataElement);
                        break;
                    case DONE:
                        log.warn("OutputSegmentMerger is already done. It will not process any more elements.");
                        break;
                }

            } catch (IOException ie) {
                throw new MkvElementVisitException("IOException in merge visitor", ie);
            }
        }
    }

    private void emitClusterStart() throws IOException {
        bufferingClusterChannel.close();
        int numBytes = outputChannel.write(ByteBuffer.wrap(bufferingClusterStream.toByteArray()));
        log.debug("Wrote buffered cluster start data to output stream {} bytes", numBytes);
    }

    private void bufferAndCollect(MkvStartMasterElement startMasterElement)
            throws IOException, MkvElementVisitException {
        Validate.isTrue(state == MergeState.BUFFERING_SEGMENT || state == MergeState.BUFFERING_CLUSTER_START,
                "Trying to buffer in wrong state " + state);
        //Buffer and collect
        if (MergeState.BUFFERING_SEGMENT == state) {
            if (!collectorStates.isEmpty() && MkvTypeInfos.SEGMENT.equals(startMasterElement.getElementMetaData()
                    .getTypeInfo()) && !startMasterElement.isUnknownLength()) {
                //if the start master element belongs to a segment that has a defined length,
                //change it to one with an unknown length since we will be changing the length of the segment
                //element.
                SEGMENT_ELEMENT_WITH_UNKNOWN_LENGTH.rewind();
                bufferingSegmentChannel.write(SEGMENT_ELEMENT_WITH_UNKNOWN_LENGTH);
            } else {
                startMasterElement.writeToChannel(bufferingSegmentChannel);
            }
        } else {
            startMasterElement.writeToChannel(bufferingClusterChannel);
        }
        this.sendElementToAllCollectors(startMasterElement);
    }

    private void bufferAndCollect(MkvDataElement dataElement) throws MkvElementVisitException {
        Validate.isTrue(state == MergeState.BUFFERING_SEGMENT || state == MergeState.BUFFERING_CLUSTER_START,
                "Trying to buffer in wrong state " + state);
        if (MergeState.BUFFERING_SEGMENT == state) {
            writeToChannel(bufferingSegmentChannel, dataElement);
        } else {
            writeToChannel(bufferingClusterChannel, dataElement);
        }
        this.sendElementToAllCollectors(dataElement);
    }

    private static void writeToChannel(WritableByteChannel byteChannel, MkvDataElement dataElement) throws MkvElementVisitException {
        dataElement.writeToChannel(byteChannel);
    }

    private void emit(MkvStartMasterElement startMasterElement) throws MkvElementVisitException {
        Validate.isTrue(state == EMITTING, "emitting in wrong state "+state);
        startMasterElement.writeToChannel(outputChannel);
    }

    private void emit(MkvDataElement dataElement) throws MkvElementVisitException {
        Validate.isTrue(state == EMITTING, "emitting in wrong state "+state);
        dataElement.writeToChannel(outputChannel);
    }

    private void collect(MkvEndMasterElement endMasterElement) throws MkvElementVisitException {
        //only trigger collectors since endelements do not have any data to buffer.
        this.sendElementToAllCollectors(endMasterElement);
    }

    private void sendElementToAllCollectors(MkvElement dataElement) throws MkvElementVisitException {
        for (CollectorState cs : collectorStates) {
            dataElement.accept(cs.getCollector());
        }
    }

    private void emitBufferedSegmentData(boolean shouldEmitSegmentData) throws IOException {
        bufferingSegmentChannel.close();

        if (shouldEmitSegmentData) {
            int numBytes = outputChannel.write(ByteBuffer.wrap(bufferingSegmentStream.toByteArray()));
            log.debug("Wrote buffered header data to output stream {} bytes",numBytes);
            emittedSegments++;
        } else {
            //We can merge the segments, so we dont need to write the buffered headers
            // However, we still need to introduce a dummy void element to prevent consumers
            //getting confused by two consecutive elements of the same type.
            VOID_ELEMENT_WITH_SIZE_ONE.rewind();
            outputChannel.write(VOID_ELEMENT_WITH_SIZE_ONE);
        }
    }

    private void resetChannels() {
        bufferingSegmentStream.reset();
        bufferingSegmentChannel = Channels.newChannel(bufferingSegmentStream);

        bufferingClusterStream.reset();
        bufferingClusterChannel = Channels.newChannel(bufferingClusterStream);
    }

    private boolean shouldEmitBufferedSegmentData() {
        boolean doAllCollectorsMatchPreviousResults = false;
        if (!collectorStates.isEmpty()) {
            doAllCollectorsMatchPreviousResults =
                    collectorStates.stream().allMatch(CollectorState::doCurrentAndOldResultsMatch);
        }
        log.info("Number of collectors {}. Did all collectors match previous results: {} ",
                collectorStates.size(),
                doAllCollectorsMatchPreviousResults);
        return !doAllCollectorsMatchPreviousResults;
    }

    private void resetCollectors() {
        collectorStates.stream().forEach(CollectorState::reset);
    }

    private static class CollectorState {
        @Getter
        private final EBMLTypeInfo parentTypeInfo;
        @Getter
        private final MkvChildElementCollector collector;
        private List<MkvElement> previousResult = new ArrayList<>();

        public CollectorState(EBMLTypeInfo parentTypeInfo) {
            this.parentTypeInfo = parentTypeInfo;
            this.collector = new MkvChildElementCollector(parentTypeInfo);
        }

        public void reset() {
            previousResult = collector.copyOfCollection();
            collector.clearCollection();
        }

        boolean doCurrentAndOldResultsMatch() {
            return collector.equivalent(previousResult);
        }
    }
}
