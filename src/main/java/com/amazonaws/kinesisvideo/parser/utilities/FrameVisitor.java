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

import com.amazonaws.kinesisvideo.parser.ebml.MkvTypeInfos;
import com.amazonaws.kinesisvideo.parser.mkv.Frame;
import com.amazonaws.kinesisvideo.parser.mkv.FrameProcessException;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitor;
import com.amazonaws.kinesisvideo.parser.mkv.MkvValue;
import com.amazonaws.kinesisvideo.parser.mkv.visitors.CompositeMkvElementVisitor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;

import java.math.BigInteger;
import java.util.Optional;

@Slf4j
public class FrameVisitor extends CompositeMkvElementVisitor {
    private final FragmentMetadataVisitor fragmentMetadataVisitor;
    private final FrameVisitorInternal frameVisitorInternal;
    private final FrameProcessor frameProcessor;
    private final Optional<Long> trackNumber;
    private final Optional<FragmentMetadataVisitor.MkvTagProcessor> tagProcessor;
    private Optional<BigInteger> timescale;
    private Optional<BigInteger> fragmentTimecode;

    private FrameVisitor(final FragmentMetadataVisitor fragmentMetadataVisitor,
                         final Optional<FragmentMetadataVisitor.MkvTagProcessor> tagProcessor,
                         final FrameProcessor frameProcessor, final Optional<Long> trackNumber) {
        super(fragmentMetadataVisitor);
        this.fragmentMetadataVisitor = fragmentMetadataVisitor;
        this.frameVisitorInternal = new FrameVisitorInternal();
        this.childVisitors.add(this.frameVisitorInternal);
        this.frameProcessor = frameProcessor;
        this.tagProcessor = tagProcessor;
        this.trackNumber = trackNumber;
        this.timescale = Optional.empty();
        this.fragmentTimecode = Optional.empty();
    }

    public static FrameVisitor create(final FrameProcessor frameProcessor) {
        return new FrameVisitor(FragmentMetadataVisitor.create(), Optional.empty(), frameProcessor, Optional.empty());
    }

    public static FrameVisitor create(final FrameProcessor frameProcessor,
                                      final Optional<FragmentMetadataVisitor.MkvTagProcessor> tagProcessor) {
        return new FrameVisitor(FragmentMetadataVisitor.create(tagProcessor),
                tagProcessor, frameProcessor, Optional.empty());
    }

    public static FrameVisitor create(final FrameProcessor frameProcessor,
                                      final Optional<FragmentMetadataVisitor.MkvTagProcessor> tagProcessor,
                                      final Optional<Long> trackNumber) {
        return new FrameVisitor(FragmentMetadataVisitor.create(tagProcessor),
                tagProcessor, frameProcessor, trackNumber);
    }

    public void close() {
        frameProcessor.close();
    }

    public interface FrameProcessor extends AutoCloseable {
        default void process(final Frame frame, final MkvTrackMetadata trackMetadata,
                             final Optional<FragmentMetadata> fragmentMetadata) throws FrameProcessException {
            throw new NotImplementedException("Default FrameVisitor.FrameProcessor");
        }

        default void process(final Frame frame, final MkvTrackMetadata trackMetadata,
                             final Optional<FragmentMetadata> fragmentMetadata,
                             final Optional<FragmentMetadataVisitor.MkvTagProcessor> tagProcessor)
                             throws FrameProcessException {
            if (tagProcessor.isPresent()) {
                throw new NotImplementedException("Default FrameVisitor.FrameProcessor");
            } else {
                process(frame, trackMetadata, fragmentMetadata);
            }
        }

        default void process(final Frame frame, final MkvTrackMetadata trackMetadata,
                final Optional<FragmentMetadata> fragmentMetadata,
                final Optional<FragmentMetadataVisitor.MkvTagProcessor> tagProcessor,
                final Optional<BigInteger> timescale, final Optional<BigInteger> fragmentTimecode)
                throws FrameProcessException {
            process(frame, trackMetadata, fragmentMetadata, tagProcessor);
        }

        @Override
        default void close() {
            //No op close. Derived classes should implement this method to meaningfully handle cleanup of the
            // resources.
        }
    }

    private class FrameVisitorInternal extends MkvElementVisitor {
        @Override
        public void visit(final com.amazonaws.kinesisvideo.parser.mkv.MkvStartMasterElement startMasterElement)
                throws com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitException {
        }

        @Override
        public void visit(final com.amazonaws.kinesisvideo.parser.mkv.MkvEndMasterElement endMasterElement)
                throws com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitException {
            if (tagProcessor.isPresent()
                    && MkvTypeInfos.CLUSTER.equals(endMasterElement.getElementMetaData().getTypeInfo())) {
                tagProcessor.get().clear();
            }
        }

        @Override
        public void visit(final com.amazonaws.kinesisvideo.parser.mkv.MkvDataElement dataElement)
                throws com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitException {

            if (MkvTypeInfos.TIMECODESCALE.equals(dataElement.getElementMetaData().getTypeInfo())) {
                timescale = Optional.of((BigInteger) dataElement.getValueCopy().getVal());
            }

            if (MkvTypeInfos.TIMECODE.equals(dataElement.getElementMetaData().getTypeInfo())) {
                fragmentTimecode = Optional.of((BigInteger) dataElement.getValueCopy().getVal());
            }

            if (MkvTypeInfos.SIMPLEBLOCK.equals(dataElement.getElementMetaData().getTypeInfo())) {
                final MkvValue<Frame> frame = dataElement.getValueCopy();
                Validate.notNull(frame);
                final long frameTrackNo = frame.getVal().getTrackNumber();
                final MkvTrackMetadata trackMetadata =
                        fragmentMetadataVisitor.getMkvTrackMetadata(frameTrackNo);

                if (trackNumber.orElse(frameTrackNo) == frameTrackNo) {
                    frameProcessor.process(frame.getVal(), trackMetadata,
                            fragmentMetadataVisitor.getCurrentFragmentMetadata(),
                            tagProcessor, timescale, fragmentTimecode);
                }
            }
        }
    }
}