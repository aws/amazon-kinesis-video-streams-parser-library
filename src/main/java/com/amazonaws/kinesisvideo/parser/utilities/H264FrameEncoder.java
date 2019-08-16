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

import com.amazonaws.kinesisvideo.parser.examples.lambda.EncodedFrame;
import lombok.extern.slf4j.Slf4j;
import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.encode.H264FixedRateControl;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.VideoEncoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;
import org.jcodec.scale.AWTUtil;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;

import static java.util.Arrays.asList;

/**
 * H264 Frame Encoder class which uses JCodec encoder to encode frames.
 */
@Slf4j
public class H264FrameEncoder {

    private Picture toEncode;
    private H264Encoder encoder;
    private SeqParameterSet sps;
    private PictureParameterSet pps;
    private ByteBuffer out;
    private byte[] cpd;
    private int frameNumber;

    public H264FrameEncoder(final int width, final int height, final int bitRate) {
        this.encoder = new H264Encoder(new H264FixedRateControl(bitRate));
        this.out = ByteBuffer.allocate(width * height * 6);
        this.frameNumber = 0;

        final Size size = new Size(width, height);
        sps = this.encoder.initSPS(size);
        pps = this.encoder.initPPS();

        final ByteBuffer spsBuffer = ByteBuffer.allocate(512);
        this.sps.write(spsBuffer);
        spsBuffer.flip();

        final ByteBuffer serialSps = ByteBuffer.allocate(512);
        this.sps.write(serialSps);
        serialSps.flip();
        H264Utils.escapeNALinplace(serialSps);

        final ByteBuffer serialPps = ByteBuffer.allocate(512);
        this.pps.write(serialPps);
        serialPps.flip();
        H264Utils.escapeNALinplace(serialPps);

        final ByteBuffer serialAvcc = ByteBuffer.allocate(512);
        final AvcCBox avcC = AvcCBox.createAvcCBox(this.sps.profileIdc, 0, this.sps.levelIdc, 4,
                asList(serialSps), asList(serialPps));
        avcC.doWrite(serialAvcc);
        serialAvcc.flip();
        cpd = new byte[serialAvcc.remaining()];
        serialAvcc.get(cpd);
    }

    public EncodedFrame encodeFrame(final BufferedImage bi) {

        // Perform conversion from buffered image to pic
        out.clear();
        toEncode = AWTUtil.fromBufferedImage(bi, ColorSpace.YUV420J);

        // First frame is treated as I Frame (IDR Frame)
        final SliceType sliceType = this.frameNumber == 0 ? SliceType.I : SliceType.P;
        log.debug("Encoding frame no: {}, frame type : {}", frameNumber, sliceType);

        final boolean idr = this.frameNumber == 0;

        // Encode image into H.264 frame, the result is stored in 'out' buffer
        final ByteBuffer data = encoder.doEncodeFrame(toEncode, out, idr, this.frameNumber++, sliceType);
        return EncodedFrame.builder()
                .byteBuffer(data)
                .isKeyFrame(idr)
                .cpd(ByteBuffer.wrap(cpd))
                .build();
    }

    public void setFrameNumber(final int frameNumber) {
        this.frameNumber = frameNumber;
    }

    public SeqParameterSet getSps() {
        return sps;
    }

    public PictureParameterSet getPps() {
        return pps;
    }

    public byte[] getCodecPrivateData() { return cpd.clone(); }

    public int getKeyInterval() {
        return encoder.getKeyInterval();
    }
}
