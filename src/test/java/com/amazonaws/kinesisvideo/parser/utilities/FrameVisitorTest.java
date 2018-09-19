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

import com.amazonaws.kinesisvideo.parser.mkv.Frame;
import com.amazonaws.kinesisvideo.parser.mkv.StreamingMkvReader;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class FrameVisitorTest {
    private int frameProcessCount;
    private int nullFrameCount;
    private static int SIMPLE_BLOCKS_COUNT_MKV = 444;

    private final class TestFrameProcessor implements FrameVisitor.FrameProcessor {
        @Override
        public void process(final Frame frame, final MkvTrackMetadata trackMetadata,
                            final Optional<FragmentMetadata> fragmentMetadata) {
            frameProcessCount++;
            if(frame == null) {
                nullFrameCount++;
            }
        }

    }
    private FrameVisitor frameVisitor;
    private StreamingMkvReader streamingMkvReader;

    @Before
    public void setUp() throws Exception {
        frameVisitor = FrameVisitor.create(new TestFrameProcessor());
    }

    @Test
    public void testWithMkvVideo() throws Exception {
        streamingMkvReader = StreamingMkvReader.createDefault(getClustersByteSource("clusters.mkv"));
        streamingMkvReader.apply(frameVisitor);
        Assert.assertEquals(SIMPLE_BLOCKS_COUNT_MKV, frameProcessCount);
        Assert.assertEquals(0, nullFrameCount);
    }

    private InputStreamParserByteSource getClustersByteSource(String name) throws IOException {
        return getInputStreamParserByteSource(name);
    }

    private InputStreamParserByteSource getInputStreamParserByteSource(String fileName) throws IOException {
        final InputStream in = TestResourceUtil.getTestInputStream(fileName);
        return new InputStreamParserByteSource(in);
    }

}
