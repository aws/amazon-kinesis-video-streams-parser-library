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
import com.amazonaws.kinesisvideo.parser.ebml.InputStreamParserByteSource;
import com.amazonaws.kinesisvideo.parser.ebml.MkvTypeInfos;
import com.amazonaws.kinesisvideo.parser.examples.KinesisVideoFrameViewer;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitException;
import com.amazonaws.kinesisvideo.parser.mkv.StreamingMkvReader;
import com.amazonaws.kinesisvideo.parser.mkv.visitors.CompositeMkvElementVisitor;
import com.amazonaws.kinesisvideo.parser.mkv.visitors.CountVisitor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;


@RunWith(PowerMockRunner.class)
public class H264FrameRendererTest {

    @Test
    public void frameRenderCountTest() throws IOException, MkvElementVisitException {
        final InputStream in = TestResourceUtil.getTestInputStream("kinesis_video_renderer_example_output.mkv");

        final KinesisVideoFrameViewer kinesisVideoFrameViewer = mock(KinesisVideoFrameViewer.class);
        doNothing().when(kinesisVideoFrameViewer).update(any(BufferedImage.class));
        H264FrameRenderer frameRenderer = H264FrameRenderer.create(kinesisVideoFrameViewer);
        StreamingMkvReader mkvStreamReader =
                StreamingMkvReader.createDefault(new InputStreamParserByteSource(in));

        CountVisitor countVisitor =
                CountVisitor.create(MkvTypeInfos.CLUSTER, MkvTypeInfos.SEGMENT, MkvTypeInfos.SIMPLEBLOCK);

        mkvStreamReader.apply(new CompositeMkvElementVisitor(countVisitor, FrameVisitor.create(frameRenderer)));

        Assert.assertEquals(444, frameRenderer.getFrameCount());

        verify(kinesisVideoFrameViewer, times(444)).update(any(BufferedImage.class));
    }
}
