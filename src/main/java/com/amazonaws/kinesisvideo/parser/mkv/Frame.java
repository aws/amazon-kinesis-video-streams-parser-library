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

import com.amazonaws.kinesisvideo.parser.ebml.EBMLUtils;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.Validate;

import java.nio.ByteBuffer;



/**
 * Class that captures the meta-data and data for a frame in a Kinesis Video Stream.
 * This is based on the content of a SimpleBlock in Mkv.
 */
@Getter
@AllArgsConstructor(access=AccessLevel.PRIVATE)
@Builder
@ToString(exclude = {"frameData"})
public class Frame {
    private final long trackNumber;
    private final int timeCode;
    private final boolean keyFrame;
    private final boolean invisible;
    private final boolean discardable;
    private final Lacing lacing;
    private final ByteBuffer frameData;

    public enum Lacing { NO, XIPH, EBML, FIXED_SIZE}

    /**
     * Create a frame object for the provided data buffer.
     * Do not create a copy of the data buffer while creating the frame object.
     * @param simpleBlockDataBuffer The data buffer.
     * @return A frame containing the data buffer.
     */
    public static Frame withoutCopy(ByteBuffer simpleBlockDataBuffer) {
        FrameBuilder builder = getBuilderWithCommonParams(simpleBlockDataBuffer);

        ByteBuffer frameData = simpleBlockDataBuffer.slice();
        return builder.frameData(frameData).build();
    }

    /**
     * Create a frame object for the provided data buffer.
     * Create a copy of the data buffer while creating the frame object.
     * @param simpleBlockDataBuffer The data buffer.
     * @return A frame containing a copy of the data buffer.
     */
    public static Frame withCopy(ByteBuffer simpleBlockDataBuffer) {
        FrameBuilder builder = getBuilderWithCommonParams(simpleBlockDataBuffer);
        ByteBuffer frameData = ByteBuffer.allocate(simpleBlockDataBuffer.remaining());
        frameData.put(simpleBlockDataBuffer);
        frameData.flip();
        return builder.frameData(frameData).build();
    }

    /**
     * Create a FrameBuilder
     * @param simpleBlockDataBuffer
     * @return
     */
    private static FrameBuilder getBuilderWithCommonParams(ByteBuffer simpleBlockDataBuffer) {
        FrameBuilder builder = Frame.builder()
                .trackNumber(EBMLUtils.readEbmlInt(simpleBlockDataBuffer))
                .timeCode((int) EBMLUtils.readDataSignedInteger(simpleBlockDataBuffer, 2));

        final long flag = EBMLUtils.readUnsignedIntegerSevenBytesOrLess(simpleBlockDataBuffer, 1);
        builder.keyFrame((flag & (0x1 << 7)) > 0)
                .invisible((flag & (0x1 << 3)) > 0)
                .discardable((flag & 0x1) > 0);

        final int laceValue = (int) (flag & 0x3 << 1) >> 1;
        final Lacing lacing = getLacing(laceValue);
        builder.lacing(lacing);
        return builder;
    }

    private static Lacing getLacing(int laceValue) {
        switch(laceValue) {
            case 0:
                return Lacing.NO;
            case 1:
                return Lacing.XIPH;
            case 2:
                return Lacing.EBML;
            case 3:
                return Lacing.FIXED_SIZE;
            default:
                Validate.isTrue(false, "Invalid value of lacing "+laceValue);
        }
        throw new IllegalArgumentException("Invalid value of lacing "+laceValue);
    }
}
