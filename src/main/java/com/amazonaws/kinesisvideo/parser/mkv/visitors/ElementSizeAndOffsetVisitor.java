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
package com.amazonaws.kinesisvideo.parser.mkv.visitors;

import com.amazonaws.kinesisvideo.parser.ebml.MkvTypeInfos;
import com.amazonaws.kinesisvideo.parser.mkv.Frame;
import com.amazonaws.kinesisvideo.parser.mkv.MkvDataElement;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElement;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitException;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitor;
import com.amazonaws.kinesisvideo.parser.mkv.MkvEndMasterElement;
import com.amazonaws.kinesisvideo.parser.mkv.MkvStartMasterElement;
import com.amazonaws.kinesisvideo.parser.mkv.MkvValue;
import lombok.RequiredArgsConstructor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigInteger;

/**
 * It logs the offsets and element sizes to a writer.
 * It is useful for looking into mkv streams, where mkvinfo fails.
 */
@RequiredArgsConstructor
public class ElementSizeAndOffsetVisitor extends MkvElementVisitor {
    private final BufferedWriter writer;
    private long offsetCount = 0;


    @Override
    public void visit(MkvStartMasterElement startMasterElement) throws MkvElementVisitException {
        StringBuilder builder = new StringBuilder();
        appendOffset(startMasterElement, builder);
        appendCommonParts(startMasterElement, builder);
        builder.append(" element header size ")
                .append(startMasterElement.getIdAndSizeRawBytesLength())
                .append(" element data size ");
        if (startMasterElement.isUnknownLength()) {
            builder.append("unknown");
        } else {
            builder.append(startMasterElement.getDataSize());
        }

        offsetCount += startMasterElement.getIdAndSizeRawBytesLength();

        buildAndWrite(builder);
    }

    @Override
    public void visit(MkvEndMasterElement endMasterElement) {

    }

    @Override
    public void visit(MkvDataElement dataElement) throws MkvElementVisitException {
        StringBuilder builder = createStringBuilderWithOffset(dataElement);
        appendCommonParts(dataElement, builder);
        builder.append(" element header size ")
                .append(dataElement.getIdAndSizeRawBytesLength())
                .append(" element data size ")
                .append(dataElement.getDataSize());

        offsetCount += dataElement.getIdAndSizeRawBytesLength();
        offsetCount += dataElement.getDataSize();

        buildAndWrite(builder);
        if (MkvTypeInfos.SIMPLEBLOCK.equals(dataElement.getElementMetaData().getTypeInfo())) {
            //Print out the frame information.
            MkvValue<Frame> frameValue = dataElement.getValueCopy();
            Frame frame = frameValue.getVal();
            buildAndWrite(createStringBuilderWithOffset(dataElement).append("Frame data (size): ")
                    .append(frame.getFrameData().limit())
                    .append(" ")
                    .append(frame.toString()));
        } else if (MkvTypeInfos.TAGNAME.equals(dataElement.getElementMetaData().getTypeInfo())) {
            MkvValue<String> tagName= dataElement.getValueCopy();
            buildAndWrite(createStringBuilderWithOffset(dataElement).append("Tag Name :").append(tagName.getVal()));
        } else if (MkvTypeInfos.TIMECODE.equals(dataElement.getElementMetaData().getTypeInfo())) {
            MkvValue<BigInteger> timeCode = dataElement.getValueCopy();
            buildAndWrite(createStringBuilderWithOffset(dataElement).append("TimeCode :")
                    .append(timeCode.getVal().toString()));
        }
    }

    private StringBuilder createStringBuilderWithOffset(MkvDataElement dataElement) {
        StringBuilder frameStringBuilder = new StringBuilder();
        appendOffset(dataElement, frameStringBuilder);
        return frameStringBuilder;
    }

    private void appendCommonParts(MkvElement mkvElement, StringBuilder builder) {
        builder.append("Element ")
                .append(mkvElement.getElementMetaData().getTypeInfo().getName())
                .append(" elementNumber ")
                .append(mkvElement.getElementMetaData().getElementNumber())
                .append(" offset ")
                .append(offsetCount);
    }

    private void appendOffset(MkvElement element, StringBuilder builder) {
        int level = Math.max(0, element.getElementMetaData().getTypeInfo().getLevel());
        for (int i = 0; i < level; i++) {
            builder.append("    ");
        }
    }

    private void buildAndWrite(StringBuilder builder) throws MkvElementVisitException {
        try {
            String s = builder.toString();
            writer.write(s, 0, s.length());
            writer.newLine();
        } catch (IOException e) {
            throw new MkvElementVisitException("Failure in ElementSizeAndOffsetVisitor ", e);
        }
    }

}
