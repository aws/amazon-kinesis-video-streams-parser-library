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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.amazonaws.kinesisvideo.parser.utilities.FragmentMetadataVisitor;
import com.amazonaws.kinesisvideo.parser.utilities.FrameVisitor;
import com.amazonaws.kinesisvideo.parser.utilities.H264FrameRenderer;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;


import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.kinesisvideomedia.model.StartSelector;
import software.amazon.awssdk.services.kinesisvideomedia.model.StartSelectorType;

/*
 * Example for integrating with Kinesis Video Streams GetMedia API.
 * This example does:

 * 1 Calls GetMedia to stream video fragments out of the stream.
 * 2. It uses the StreamingMkvParser to parse the returned the stream and perform these steps:
 *   2.1 The GetMedia output stream has one mkv segment for each fragment. Merge the mkv segments that share track
 *        information into a single segment.
 *   2.2 Decodes the frames using h264 decoder (using JCodec) and
 *   2.3 It renders the image using  JFrame for viewing
 *
 * To run the example:
 *   Run the Unit test  KinesisVideoRendererExampleTest (by commenting the @ignore)
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
    private boolean renderFragmentMetadata = true;

    @Builder
    private KinesisVideoRendererExample(Region region,
                                        String streamName,
                                        AwsCredentialsProvider credentialsProvider,
                                        InputStream inputVideoStream,
                                        boolean renderFragmentMetadata,
                                        boolean noSampleInputRequired) {
        super(region, credentialsProvider, streamName);
        this.inputStream = inputVideoStream;
        this.streamOps = new StreamOps(region,  streamName, credentialsProvider);
        this.executorService = Executors.newFixedThreadPool(2);
        this.renderFragmentMetadata = renderFragmentMetadata;
    }

    /**
     * This method executes the example.
     *
     * @throws InterruptedException the thread is interrupted while waiting for the streamOps to enter the correct state.
     * @throws IOException          fails to read video from the input stream or write to the output stream.
     */
    public void execute() throws InterruptedException, IOException {

        getMediaProcessingArguments = KinesisVideoRendererExample.GetMediaProcessingArguments.create(
                renderFragmentMetadata ?
                        Optional.of(new FragmentMetadataVisitor.BasicMkvTagProcessor()) : Optional.empty());

        try (KinesisVideoRendererExample.GetMediaProcessingArguments getMediaProcessingArgumentsLocal = getMediaProcessingArguments) {
            //Start a GetMedia worker to read and process data from the Kinesis Video Stream.
            GetMediaWorker getMediaWorker = GetMediaWorker.create(getRegion(),
                    getCredentialsProvider(),
                    getStreamName(),
                    StartSelector.builder()
                            .startSelectorType(StartSelectorType.NOW).build(),
                    streamOps.getKvsVideoClient(),
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

        private static GetMediaProcessingArguments create(
                Optional<FragmentMetadataVisitor.MkvTagProcessor> tagProcessor) throws IOException {

            KinesisVideoFrameViewer kinesisVideoFrameViewer = new KinesisVideoFrameViewer(FRAME_WIDTH, FRAME_HEIGHT);
            return new GetMediaProcessingArguments(
                    FrameVisitor.create(H264FrameRenderer.create(kinesisVideoFrameViewer), tagProcessor,
                            Optional.of(1L))); // Video track number
        }

        @Override
        public void close() throws IOException {

        }
    }
}
