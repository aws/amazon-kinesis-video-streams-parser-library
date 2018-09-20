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
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitException;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitor;
import com.amazonaws.kinesisvideo.parser.mkv.MkvValue;
import com.amazonaws.kinesisvideo.parser.mkv.visitors.CompositeMkvElementVisitor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;

import java.math.BigInteger;

/**
 * Fragment metdata tags will not be present as there is no tag when the data (mkv/webm) is not
 * retrieved from Kinesis video.
 * As a result, user cannot get the timestamp from fragment metadata tags.
 * This class is used to provide a FrameVisitor to process frame as well as time elements for timestamp.
 **/

@Slf4j
public class SimpleFrameVisitor extends CompositeMkvElementVisitor {
    private final FrameVisitorInternal frameVisitorInternal;
    private final FrameProcessor frameProcessor;

    private SimpleFrameVisitor(FrameProcessor frameProcessor) {
        this.frameProcessor = frameProcessor;
        frameVisitorInternal = new FrameVisitorInternal();
        childVisitors.add(frameVisitorInternal);

    }
    public static SimpleFrameVisitor create(FrameProcessor frameProcessor) {
        return new SimpleFrameVisitor(frameProcessor);
    }
    public interface FrameProcessor {
        default void process(Frame frame, long clusterTimeCode, long timeCodeScale) {
            throw new NotImplementedException("Default FrameVisitor with No Fragement MetaData");
        }
    }

    private class FrameVisitorInternal extends MkvElementVisitor {
        private long clusterTimeCode = -1;
        private long timeCodeScale = -1;
        @Override
        public void visit(com.amazonaws.kinesisvideo.parser.mkv.MkvStartMasterElement startMasterElement)
                throws com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitException {
        }

        @Override
        public void visit(com.amazonaws.kinesisvideo.parser.mkv.MkvEndMasterElement endMasterElement)
                throws com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitException {
        }

        @Override
        public void visit(com.amazonaws.kinesisvideo.parser.mkv.MkvDataElement dataElement)
                throws com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitException {

            if (MkvTypeInfos.TIMECODE.equals(dataElement.getElementMetaData().getTypeInfo())) {
                clusterTimeCode = ((BigInteger) dataElement.getValueCopy().getVal()).longValue();
            }

            if (MkvTypeInfos.TIMECODESCALE.equals(dataElement.getElementMetaData().getTypeInfo())) {
                timeCodeScale = ((BigInteger) dataElement.getValueCopy().getVal()).longValue();
            }

            if (MkvTypeInfos.SIMPLEBLOCK.equals(dataElement.getElementMetaData().getTypeInfo())) {
                if (clusterTimeCode == -1 || timeCodeScale == -1) {
                    throw new MkvElementVisitException("No timeCodeScale or timeCode found", new RuntimeException());
                }
                final MkvValue<Frame> frame = dataElement.getValueCopy();
                Validate.notNull(frame);
                frameProcessor.process(frame.getVal(), clusterTimeCode, timeCodeScale);
            }
        }

    }
}
