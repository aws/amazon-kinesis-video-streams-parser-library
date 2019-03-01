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

import com.amazonaws.kinesisvideo.parser.TestResourceUtil;
import com.amazonaws.kinesisvideo.parser.ebml.EBMLTypeInfo;
import com.amazonaws.kinesisvideo.parser.ebml.InputStreamParserByteSource;
import com.amazonaws.kinesisvideo.parser.ebml.MkvTypeInfos;
import com.amazonaws.kinesisvideo.parser.examples.KinesisVideoFrameViewer;
import com.amazonaws.kinesisvideo.parser.mkv.*;
import com.amazonaws.kinesisvideo.parser.mkv.visitors.CompositeMkvElementVisitor;
import com.amazonaws.kinesisvideo.parser.mkv.visitors.CountVisitor;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class FrameRendererTest {
    // long running test

    @Test
    public void frameCountTest() throws IOException, MkvElementVisitException {
        final InputStream in = TestResourceUtil.getTestInputStream("kinesis_video_renderer_example_output.mkv");
        byte[] codecPrivateData = new byte[]{
                0x01, 0x64, 0x00, 0x28, (byte) 0xff, (byte) 0xe1, 0x00,
                0x0e, 0x27, 0x64, 0x00, 0x28, (byte) 0xac, 0x2b, 0x40,
                0x50, 0x1e, (byte) 0xd0, 0x0f, 0x12, 0x26, (byte) 0xa0,
                0x01, 0x00, 0x04, 0x28, (byte) 0xee, 0x1f, 0x2c
        };

        H264FrameRenderer frameRenderer = H264FrameRenderer.create(new KinesisVideoFrameViewer(0, 0));
        StreamingMkvReader mkvStreamReader =
                StreamingMkvReader.createDefault(new InputStreamParserByteSource(in));

        CountVisitor countVisitor = CountVisitor.create(
                MkvTypeInfos.CLUSTER, MkvTypeInfos.SEGMENT, MkvTypeInfos.SIMPLEBLOCK, MkvTypeInfos.TRACKS);

        mkvStreamReader.apply(new CompositeMkvElementVisitor(countVisitor, FrameVisitor.create(frameRenderer)));

        Assert.assertEquals(8, countVisitor.getCount(MkvTypeInfos.TRACKS));
        Assert.assertEquals(8, countVisitor.getCount(MkvTypeInfos.CLUSTER));
        Assert.assertEquals(444, countVisitor.getCount(MkvTypeInfos.SIMPLEBLOCK));

        Assert.assertEquals(444, frameRenderer.getFrameCount());
        ByteBuffer codecPrivateDataFromFrame = frameRenderer.getCodecPrivateData();
        Assert.assertEquals(ByteBuffer.wrap(codecPrivateData), codecPrivateDataFromFrame);
    }

    /**
     * Test jcodec decoding a frame from an mkv that has a pixel width/height that isn't a multiple of 16
     */
    @Test
    public void frameCountTest2() throws IOException, MkvElementVisitException {
        final InputStream in = TestResourceUtil.getTestInputStream("output_get_media.mkv");
        byte[] codecPrivateData = new byte[]{
                0x01, 0x4d, 0x40, 0x1e, 0x03, 0x01, 0x00, 0x27, 0x27, 0x4d, 0x40, 0x1e,
                (byte) 0xb9, 0x10, 0x14, 0x05, (byte) 0xff, 0x2e, 0x02, (byte) 0xd4, 0x04, 0x04, 0x07, (byte) 0xc0,
                0x00, 0x00, 0x03, 0x00, 0x40, 0x00, 0x00, 0x0f, 0x38, (byte) 0xa0, 0x00, 0x3d,
                0x09, 0x00, 0x07, (byte) 0xa1, 0x3b, (byte) 0xde, (byte) 0xe0, 0x3e, 0x11, 0x08, (byte) 0xd4, 0x01,
                0x00, 0x04, 0x28, (byte) 0xfe, 0x3c, (byte) 0x80
        };

        H264FrameRenderer frameRenderer = H264FrameRenderer.create(new KinesisVideoFrameViewer(0, 0));
        StreamingMkvReader mkvStreamReader =
                StreamingMkvReader.createDefault(new InputStreamParserByteSource(in));

        CountVisitor countVisitor = CountVisitor.create(
                MkvTypeInfos.CLUSTER, MkvTypeInfos.SEGMENT, MkvTypeInfos.SIMPLEBLOCK, MkvTypeInfos.TRACKS);

        mkvStreamReader.apply(new CompositeMkvElementVisitor(countVisitor, FrameVisitor.create(frameRenderer)));

        Assert.assertEquals(5, countVisitor.getCount(MkvTypeInfos.TRACKS));
        Assert.assertEquals(5, countVisitor.getCount(MkvTypeInfos.CLUSTER));
        Assert.assertEquals(300, countVisitor.getCount(MkvTypeInfos.SIMPLEBLOCK));

        Assert.assertEquals(300, frameRenderer.getFrameCount());
        ByteBuffer codecPrivateDataFromFrame = frameRenderer.getCodecPrivateData();
        Assert.assertEquals(ByteBuffer.wrap(codecPrivateData), codecPrivateDataFromFrame);
    }

}
