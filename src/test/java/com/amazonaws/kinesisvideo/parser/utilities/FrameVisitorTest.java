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
import lombok.Getter;
import lombok.NonNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Optional;

public class FrameVisitorTest {
    private static final int VIDEO_FRAMES_COUNT = 909;
    private static final int AUDIO_FRAMES_COUNT = 1425;
    private static final int SIMPLE_BLOCKS_COUNT_MKV = VIDEO_FRAMES_COUNT + AUDIO_FRAMES_COUNT;
    private static final long TIMESCALE = 1000000;
    private static final long LAST_FRAGMENT_TIMECODE = 28821;

    @Getter
    public static final class TestFrameProcessor implements FrameVisitor.FrameProcessor {
        private long framesCount = 0L;
        private long timescale = 0L;
        private long fragmentTimecode = 0L;

        @Override
        public void process(@NonNull final Frame frame, @NonNull final MkvTrackMetadata trackMetadata,
                final @NonNull Optional<FragmentMetadata> fragmentMetadata,
                final @NonNull Optional<FragmentMetadataVisitor.MkvTagProcessor> tagProcessor,
                final @NonNull Optional<BigInteger> timescale, final @NonNull Optional<BigInteger> fragmentTimecode) {
            this.timescale = timescale.get().longValue();
            this.fragmentTimecode = fragmentTimecode.get().longValue();
            framesCount++;
        }


    }
    private FrameVisitor frameVisitor;
    private TestFrameProcessor frameProcessor;
    private StreamingMkvReader streamingMkvReader;

    @Before
    public void setUp() throws Exception {
        frameProcessor = new TestFrameProcessor();
        frameVisitor = FrameVisitor.create(frameProcessor);
    }

    @Test
    public void testWithMkvVideo() throws Exception {
        streamingMkvReader = StreamingMkvReader.createDefault(getClustersByteSource("vogels_480.mkv"));
        streamingMkvReader.apply(frameVisitor);
        Assert.assertEquals(SIMPLE_BLOCKS_COUNT_MKV, frameProcessor.getFramesCount());
        Assert.assertEquals(TIMESCALE, frameProcessor.getTimescale());
        Assert.assertEquals(LAST_FRAGMENT_TIMECODE, frameProcessor.getFragmentTimecode());

    }

    @Test
    public void testForVideoFrames() throws Exception {
        frameVisitor = FrameVisitor.create(frameProcessor, Optional.empty(), Optional.of(1L));
        streamingMkvReader = StreamingMkvReader.createDefault(getClustersByteSource("vogels_480.mkv"));
        streamingMkvReader.apply(frameVisitor);
        Assert.assertEquals(VIDEO_FRAMES_COUNT, frameProcessor.getFramesCount());
        Assert.assertEquals(TIMESCALE, frameProcessor.getTimescale());
        Assert.assertEquals(LAST_FRAGMENT_TIMECODE, frameProcessor.getFragmentTimecode());
    }

    @Test
    public void testForAudioFrames() throws Exception {
        frameVisitor = FrameVisitor.create(frameProcessor, Optional.empty(), Optional.of(2L));
        streamingMkvReader = StreamingMkvReader.createDefault(getClustersByteSource("vogels_480.mkv"));
        streamingMkvReader.apply(frameVisitor);
        Assert.assertEquals(AUDIO_FRAMES_COUNT, frameProcessor.getFramesCount());
        Assert.assertEquals(TIMESCALE, frameProcessor.getTimescale());
        Assert.assertEquals(LAST_FRAGMENT_TIMECODE, frameProcessor.getFragmentTimecode());
    }

    private InputStreamParserByteSource getClustersByteSource(final String name) throws IOException {
        return getInputStreamParserByteSource(name);
    }

    private InputStreamParserByteSource getInputStreamParserByteSource(final String fileName) throws IOException {
        final InputStream in = TestResourceUtil.getTestInputStream(fileName);
        return new InputStreamParserByteSource(in);
    }

}
