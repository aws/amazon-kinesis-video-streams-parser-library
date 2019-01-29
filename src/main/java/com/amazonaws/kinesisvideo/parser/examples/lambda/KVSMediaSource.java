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
package com.amazonaws.kinesisvideo.parser.examples.lambda;

import com.amazonaws.kinesisvideo.client.mediasource.CameraMediaSourceConfiguration;
import com.amazonaws.kinesisvideo.client.mediasource.MediaSourceState;
import com.amazonaws.kinesisvideo.common.exception.KinesisVideoException;
import com.amazonaws.kinesisvideo.internal.client.mediasource.MediaSource;
import com.amazonaws.kinesisvideo.internal.client.mediasource.MediaSourceConfiguration;
import com.amazonaws.kinesisvideo.internal.client.mediasource.MediaSourceSink;
import com.amazonaws.kinesisvideo.parser.utilities.ProducerStreamUtil;
import com.amazonaws.kinesisvideo.producer.KinesisVideoFrame;
import com.amazonaws.kinesisvideo.producer.StreamCallbacks;
import com.amazonaws.kinesisvideo.producer.StreamInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

@Slf4j
@RequiredArgsConstructor
public class KVSMediaSource implements MediaSource {

    private static final int FRAME_FLAG_KEY_FRAME = 1;
    private static final int FRAME_FLAG_NONE = 0;
    private static final long HUNDREDS_OF_NANOS_IN_MS = 10 * 1000;
    private static final long FRAME_DURATION_20_MS = 20L;
    
    private CameraMediaSourceConfiguration cameraMediaSourceConfiguration;
    private MediaSourceState mediaSourceState;
    private MediaSourceSink mediaSourceSink;
    private int frameIndex;
    private final StreamInfo streamInfo;

    private void putFrame(final KinesisVideoFrame kinesisVideoFrame) {
        try {
            mediaSourceSink.onFrame(kinesisVideoFrame);
        } catch (final KinesisVideoException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public MediaSourceState getMediaSourceState() {
        return mediaSourceState;
    }

    @Override
    public MediaSourceConfiguration getConfiguration() {
        return cameraMediaSourceConfiguration;
    }

    @Override
    public StreamInfo getStreamInfo() throws KinesisVideoException {
        return streamInfo;
    }

    @Override
    public void initialize(final MediaSourceSink mediaSourceSink) {
        this.mediaSourceSink = mediaSourceSink;
    }

    @Override
    public void configure(final MediaSourceConfiguration configuration) {

        if (!(configuration instanceof CameraMediaSourceConfiguration)) {
            throw new IllegalStateException("Configuration must be an instance of CameraMediaSourceConfiguration");
        }
        this.cameraMediaSourceConfiguration = (CameraMediaSourceConfiguration) configuration;
        this.frameIndex = 0;
        
    }

    @Override
    public void start() {
        mediaSourceState = MediaSourceState.RUNNING;
    }

    public void putFrameData(final EncodedFrame encodedFrame) {
        final int flags = encodedFrame.isKeyFrame() ? FRAME_FLAG_KEY_FRAME : FRAME_FLAG_NONE;
        if (encodedFrame.getByteBuffer() != null) {
            final KinesisVideoFrame frame = new KinesisVideoFrame(
                    frameIndex++,
                    flags,
                    encodedFrame.getTimeCode() * HUNDREDS_OF_NANOS_IN_MS,
                    encodedFrame.getTimeCode() * HUNDREDS_OF_NANOS_IN_MS,
                    FRAME_DURATION_20_MS * HUNDREDS_OF_NANOS_IN_MS,
                    encodedFrame.getByteBuffer());
            if (frame.getSize() == 0) {
                return;
            }
            putFrame(frame);
        } else {
            log.info("Frame Data is null !");
        }
    }

    @Override
    public void stop() {
        mediaSourceState = MediaSourceState.STOPPED;
        
    }

    @Override
    public boolean isStopped() {
        return mediaSourceState == MediaSourceState.STOPPED;
    }

    @Override
    public void free() {

    }

    @Override
    public MediaSourceSink getMediaSourceSink() {
        return mediaSourceSink;
    }

    @Nullable
    @Override
    public StreamCallbacks getStreamCallbacks() {
        return null;
    }
}
