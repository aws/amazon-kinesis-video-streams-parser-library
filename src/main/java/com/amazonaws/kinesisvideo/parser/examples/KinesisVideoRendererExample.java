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
package com.amazonaws.kinesisvideo.parser.examples;

import com.amazonaws.auth.AWSCredentialsProvider;

import com.amazonaws.kinesisvideo.parser.utilities.FrameVisitor;
import com.amazonaws.kinesisvideo.parser.utilities.H264FrameRenderer;
import com.amazonaws.regions.Regions;

import com.amazonaws.services.kinesisvideo.model.StartSelector;
import com.amazonaws.services.kinesisvideo.model.StartSelectorType;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/*
 * Example for integrating with Kinesis Video.
 * This example does:
 * 1. Create a stream, deleting and recreating if the stream of the same name already exists.
      It sets the retention period of the created stream to 48 hours.
 * 2. Call PutMedia to stream video fragments into the stream.
 * 3. Calls GetMedia to stream video fragments out of the stream.
 * 4. It uses the StreamingMkvParser to parse the returned the stream and perform these steps:
 *   2.1 The GetMedia output stream has one mkv segment for each fragment. Merge the mkv segments that share track
 *        information into a single segment.
 *   2.2 Decodes the frames using h264 decoder (using JCodec) and
 *   2.3 It renders the image using  JFrame for viewing
 *
 * To run the example:
 *   Run the Unit test  KinesisVideoRendererExampleTest
 *
 */

@Slf4j
public class KinesisVideoRendererExample extends KinesisVideoCommon {

    private static final int FRAME_WIDTH=640;
    private static final int FRAME_HEIGHT=480;
    private final InputStream inputStream;
    private final StreamOps streamOps;
    private final ExecutorService executorService;
    private KinesisVideoRendererExample.GetMediaProcessingArguments getMediaProcessingArguments;

    @Builder
    private KinesisVideoRendererExample(Regions region,
                                           String streamName,
                                           AWSCredentialsProvider credentialsProvider, InputStream inputVideoStream) {
        super(region, credentialsProvider, streamName);
        this.inputStream = inputVideoStream;
        this.streamOps = new StreamOps(region,  streamName, credentialsProvider);
        this.executorService = Executors.newFixedThreadPool(2);
    }

    /**
     * This method executes the example.
     *
     * @throws InterruptedException the thread is interrupted while waiting for the streamOps to enter the correct state.
     * @throws IOException          fails to read video from the input stream or write to the output stream.
     */
    public void execute() throws InterruptedException, IOException {

        streamOps.recreateStreamIfNecessary();
        getMediaProcessingArguments = KinesisVideoRendererExample.GetMediaProcessingArguments.create();
        try (KinesisVideoRendererExample.GetMediaProcessingArguments getMediaProcessingArgumentsLocal = getMediaProcessingArguments) {

            //Start a PutMedia worker to write data to a Kinesis Video Stream.
          PutMediaWorker putMediaWorker = PutMediaWorker.create(getRegion(),
                    getCredentialsProvider(),
                    getStreamName(),
                    inputStream,
                  streamOps.amazonKinesisVideo);
            executorService.submit(putMediaWorker);

            //Start a GetMedia worker to read and process data from the Kinesis Video Stream.
            GetMediaWorker getMediaWorker = GetMediaWorker.create(getRegion(),
                    getCredentialsProvider(),
                    getStreamName(),
                    new StartSelector().withStartSelectorType(StartSelectorType.EARLIEST),
                    streamOps.amazonKinesisVideo,
                    getMediaProcessingArgumentsLocal.getFrameVisitor());
            executorService.submit(getMediaWorker);

            //Wait for the workers to finish.
            executorService.shutdown();
            executorService.awaitTermination(180, TimeUnit.SECONDS);
            if (!executorService.isTerminated()) {
                log.warn("Shutting down executor service by force");
                executorService.shutdownNow();
            } else {
                log.info("Executor service is shutdown");
            }
        }
    }

    private static class GetMediaProcessingArguments implements Closeable {
        @Getter private final FrameVisitor frameVisitor;

        GetMediaProcessingArguments(FrameVisitor frameVisitor) {
            this.frameVisitor = frameVisitor;
        }

        private static GetMediaProcessingArguments create() throws IOException {

            KinesisVideoFrameViewer kinesisVideoFrameViewer = new KinesisVideoFrameViewer(FRAME_WIDTH, FRAME_HEIGHT);
            return new GetMediaProcessingArguments(FrameVisitor.create(H264FrameRenderer.create(kinesisVideoFrameViewer)));
        }

        @Override
        public void close() throws IOException {

        }
    }
}
