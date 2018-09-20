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
import com.amazonaws.kinesisvideo.parser.ebml.EBMLElementMetaData;
import com.amazonaws.kinesisvideo.parser.ebml.InputStreamParserByteSource;
import com.amazonaws.kinesisvideo.parser.ebml.MkvTypeInfos;
import com.amazonaws.kinesisvideo.parser.mkv.Frame;
import com.amazonaws.kinesisvideo.parser.mkv.MkvDataElement;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitException;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitor;
import com.amazonaws.kinesisvideo.parser.mkv.StreamingMkvReader;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.io.IOException;
import java.io.InputStream;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SimpleFrameVisitor.class})
public class SimpleFrameVisitorTest {
    private int frameProcessCount;
    private int nullFrameCount;
    private static int SIMPLE_BLOCKS_COUNT_WEBM = 2308;
    private static int SIMPLE_BLOCKS_COUNT_MKV = 444;

    private final class TestFrameProcessor implements SimpleFrameVisitor.FrameProcessor {
        @Override
        public void process(final Frame frame, long clusterTimeCode, long timeCodeScale) {
            frameProcessCount++;
            if(frame == null) { nullFrameCount++; }
        }
    }

    @Mock
    private MkvDataElement mockDataElement;
    @Mock
    private EBMLElementMetaData mockElementMetaData;
    private SimpleFrameVisitor frameVisitor;
    private StreamingMkvReader streamingMkvReader;

    @Before
    public void setUp() throws Exception {
        frameVisitor = SimpleFrameVisitor
                .create(new TestFrameProcessor());
    }

    @Test
    public void testWithWebmVideo() throws Exception {
        streamingMkvReader = StreamingMkvReader
                .createDefault(getClustersByteSource("big-buck-bunny_trailer.webm"));
        streamingMkvReader.apply(frameVisitor);

        Assert.assertEquals(SIMPLE_BLOCKS_COUNT_WEBM, frameProcessCount);
        Assert.assertEquals(nullFrameCount, 0);
        MkvElementVisitor internal =  Whitebox.getInternalState(frameVisitor, "frameVisitorInternal");
        long timeCode = Whitebox.getInternalState(internal, "clusterTimeCode");
        long timeCodeScale = Whitebox.getInternalState(internal, "timeCodeScale");
        Assert.assertNotEquals(-1, timeCode);
        Assert.assertNotEquals(-1, timeCodeScale);

    }

    @Test
    public void testWithMkvVideo() throws Exception {
        streamingMkvReader = StreamingMkvReader.createDefault(getClustersByteSource("clusters.mkv"));
        streamingMkvReader.apply(frameVisitor);

        Assert.assertEquals(SIMPLE_BLOCKS_COUNT_MKV, frameProcessCount);
        Assert.assertEquals(nullFrameCount, 0);
        MkvElementVisitor internal =  Whitebox.getInternalState(frameVisitor, "frameVisitorInternal");
        long timeCode = Whitebox.getInternalState(internal, "clusterTimeCode");
        long timeCodeScale = Whitebox.getInternalState(internal, "timeCodeScale");
        Assert.assertNotEquals(-1, timeCode);
        Assert.assertNotEquals(-1, timeCodeScale);
    }

    @Test(expected = MkvElementVisitException.class)
    public void testWhenNoTimeCode() throws Exception {
        MkvElementVisitor internal =  Whitebox.getInternalState(frameVisitor, "frameVisitorInternal");
        Whitebox.setInternalState(internal, "timeCodeScale", 1);
        PowerMockito.when(mockDataElement.getElementMetaData()).thenReturn(mockElementMetaData);
        PowerMockito.when(mockElementMetaData.getTypeInfo()).thenReturn(MkvTypeInfos.SIMPLEBLOCK);
        internal.visit(mockDataElement);

    }


    @Test(expected = MkvElementVisitException.class)
    public void testWhenNoTimeCodeScale() throws Exception {
        MkvElementVisitor internal =  Whitebox.getInternalState(frameVisitor, "frameVisitorInternal");
        Whitebox.setInternalState(internal, "clusterTimeCode", 1);
        PowerMockito.when(mockDataElement.getElementMetaData()).thenReturn(mockElementMetaData);
        PowerMockito.when(mockElementMetaData.getTypeInfo()).thenReturn(MkvTypeInfos.SIMPLEBLOCK);
        internal.visit(mockDataElement);

    }

    private InputStreamParserByteSource getClustersByteSource(String name) throws IOException {
        return getInputStreamParserByteSource(name);
    }

    private InputStreamParserByteSource getInputStreamParserByteSource(String fileName) throws IOException {
        final InputStream in = TestResourceUtil.getTestInputStream(fileName);
        return new InputStreamParserByteSource(in);
    }

}
