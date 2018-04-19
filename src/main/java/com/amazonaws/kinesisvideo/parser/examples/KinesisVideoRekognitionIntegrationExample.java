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

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.kinesisvideo.parser.kinesis.KinesisDataStreamsWorker;
import com.amazonaws.kinesisvideo.parser.rekognition.pojo.RekognitionInput;
import com.amazonaws.kinesisvideo.parser.rekognition.pojo.RekognizedFragmentsIndex;
import com.amazonaws.kinesisvideo.parser.rekognition.processor.RekognitionStreamProcessor;
import com.amazonaws.kinesisvideo.parser.utilities.FrameVisitor;
import com.amazonaws.kinesisvideo.parser.utilities.H264BoundingBoxFrameRenderer;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesisvideo.model.StartSelector;
import com.amazonaws.services.kinesisvideo.model.StartSelectorType;
import lombok.Builder;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * This examples demonstrates how to integrate KVS with Rekognition and draw bounding boxes while
 * rendering each frame in KinesisVideoFrameViewer.
 */
@Slf4j
public class KinesisVideoRekognitionIntegrationExample extends KinesisVideoCommon {

    private static final int DEFAULT_FRAME_WIDTH =640;
    private static final int DEFAULT_FRAME_HEIGHT =480;
    private static final int INITIAL_DELAY=10_000;
    private final StreamOps streamOps;
    private final InputStream inputStream;
    private final ExecutorService executorService;
    private RekognitionStreamProcessor rekognitionStreamProcessor;
    private KinesisDataStreamsWorker kinesisDataStreamsWorker;
    private GetMediaWorker getMediaWorker;
    private String kdsStreamName;
    private RekognitionInput rekognitionInput;

    @Setter
    private Integer rekognitionMaxTimeoutInMillis;
    @Setter
    private int width = DEFAULT_FRAME_WIDTH;
    @Setter
    private int height = DEFAULT_FRAME_HEIGHT;
    private RekognizedFragmentsIndex rekognizedFragmentsIndex = new RekognizedFragmentsIndex();


    @Builder
    private KinesisVideoRekognitionIntegrationExample(Regions region,
                                                      InputStream inputStream,
                                                      String kvsStreamName, String kdsStreamName,
                                                      RekognitionInput rekognitionInput,
                                                      AWSCredentialsProvider credentialsProvider) {
        super(region, credentialsProvider, kvsStreamName);
        this.streamOps = new StreamOps(region,  kvsStreamName, credentialsProvider);
        this.inputStream = inputStream;
        this.kdsStreamName = kdsStreamName;
        this.rekognitionInput = rekognitionInput;
        this.executorService = Executors.newFixedThreadPool(3);
    }

    /**
     * This method executes the example.
     *  @param timeOutinSec Timeout in seconds
     * @throws InterruptedException the thread is interrupted while waiting for the stream to enter the correct state.
     * @throws IOException          fails to read video from the input stream or write to the output stream.
     */
    public void execute(Long timeOutinSec) throws InterruptedException, IOException {

        // Start the RekognitionStreamProcessor and the KinesisDataStreams worker to read and process rekognized
        // face results. NOTE: Starting up KinesisClientLibrary can take some time, so start that first.
        startRekognitionProcessor();
        startKinesisDataStreamsWorker();
        // Adding some initial delay to sync both KVS and KDS data
        Thread.sleep(INITIAL_DELAY);

        // Start a GetMedia worker to read and render KVS fragments.
        startGetMediaWorker();

        // Start a PutMedia worker to write data to a Kinesis Video Stream. NOTE: Video fragments can also be ingested
        // using real-time producer like the Kinesis Video GStreamer sample app or AmazonKinesisVideoDemoApp
        if (inputStream != null) {
            startPutMediaWorker();
        }

        // Wait for the workers to finish.
        waitForTermination(timeOutinSec);
        cleanup();
    }

    private void startPutMediaWorker() {
        PutMediaWorker putMediaWorker = PutMediaWorker.create(getRegion(),
                getCredentialsProvider(),
                getStreamName(),
                inputStream,
                streamOps.getAmazonKinesisVideo());
        executorService.submit(putMediaWorker);
    }

    private void startGetMediaWorker() {
        final KinesisVideoBoundingBoxFrameViewer kinesisVideoBoundingBoxFrameViewer =
                new KinesisVideoBoundingBoxFrameViewer(width, height);
        final H264BoundingBoxFrameRenderer h264BoundingBoxFrameRenderer = H264BoundingBoxFrameRenderer.create(
                kinesisVideoBoundingBoxFrameViewer,
                rekognizedFragmentsIndex);

        if (rekognitionMaxTimeoutInMillis != null) {
            // Change the below timeout value to if the frames need to be rendered with low latency when
            // rekognition results are not present.
            h264BoundingBoxFrameRenderer.setMaxTimeout(rekognitionMaxTimeoutInMillis);
        }

        final FrameVisitor frameVisitor = FrameVisitor.create(h264BoundingBoxFrameRenderer);
        this.getMediaWorker = GetMediaWorker.create(getRegion(),
                getCredentialsProvider(),
                getStreamName(),
                new StartSelector().withStartSelectorType(StartSelectorType.NOW),
                streamOps.getAmazonKinesisVideo(),
                frameVisitor);
        executorService.submit(getMediaWorker);
    }

    private void startRekognitionProcessor() {
        this.rekognitionStreamProcessor = RekognitionStreamProcessor.create(getRegion(),
                getCredentialsProvider(),
                rekognitionInput);
        this.rekognitionStreamProcessor.process();
    }

    private void startKinesisDataStreamsWorker() {
        this.kinesisDataStreamsWorker = KinesisDataStreamsWorker.create(getRegion(),
                getCredentialsProvider(),
                kdsStreamName,
                rekognizedFragmentsIndex);
        executorService.submit(kinesisDataStreamsWorker);
    }

    private void waitForTermination(final Long timeOutinSec) throws InterruptedException {
        executorService.shutdown();
        executorService.awaitTermination(timeOutinSec, TimeUnit.SECONDS);
    }

    private void cleanup() {
        if (!executorService.isTerminated()) {
            log.warn("Shutting down executor service by force");
            executorService.shutdownNow();
        } else {
            log.info("Executor service is shutdown");
        }
        this.rekognitionStreamProcessor.stopStreamProcessor();
    }
}

