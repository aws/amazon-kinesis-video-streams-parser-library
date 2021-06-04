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

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.kinesisvideo.parser.examples.GetMediaForFragmentListWorker;
import com.amazonaws.kinesisvideo.parser.examples.StreamOps;
import com.amazonaws.kinesisvideo.parser.kinesis.KinesisDataStreamsWorker;
import com.amazonaws.kinesisvideo.parser.rekognition.pojo.DetectedFace;
import com.amazonaws.kinesisvideo.parser.rekognition.pojo.FaceSearchResponse;
import com.amazonaws.kinesisvideo.parser.rekognition.pojo.MatchedFace;
import com.amazonaws.kinesisvideo.parser.rekognition.pojo.RekognitionOutput;
import com.amazonaws.kinesisvideo.parser.rekognition.pojo.RekognizedFragmentsIndex;
import com.amazonaws.kinesisvideo.parser.rekognition.pojo.RekognizedOutput;
import com.amazonaws.kinesisvideo.parser.utilities.FrameVisitor;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesis.model.Record;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Lambda example integratong Rekognition outputs with Kinesis Video streams fragments. This examples is triggered
 * when Rekognition publishes events in Kinesis Data Streams (KDS). It gets the corresponding fragments from
 * Kinesis Video Streams (KVS), decodes each frame, overlays bounding box on top of faces detected, encodes the
 * frame again (using Jcodec) and then publishes into new derived Kinesis Video streams. The new stream can be
 * viewed using Kinesis Video Streams console or using HLS playback.
 *
 * NOTE: For Instructions to run this Lambda, please refer README.
 * NOTE: As this lambda executes resource intense decoding and encoding (using Jcodec which is not optimal
 * https://github.com/jcodec/jcodec#performance--quality-considerations), the new Kinesis Video stream might be delayed significantly.
 */
@Slf4j
public final class KinesisVideoRekognitionLambdaExample implements RequestHandler<KinesisEvent, Context> {
    private static final int NUM_RETRIES = 10;
    private static final int KCL_INIT_DELAY_MILLIS = 10_000;
    private final ExecutorService kdsWorkers = Executors.newFixedThreadPool(100);
    private final AWSCredentialsProvider credentialsProvider = new DefaultAWSCredentialsProviderChain();
    private final RekognizedFragmentsIndex rekognizedFragmentsIndex = new RekognizedFragmentsIndex();

    private String inputKvsStreamName;
    private String outputKvsStreamName;
    private StreamOps kvsClient;
    private FragmentCheckpointManager fragmentCheckpointManager;
    private H264FrameProcessor h264FrameProcessor;
    /**
     * Main method to test the integration locally in desktop.
     *
     * NOTE: This uses a different approach to get the KDS events using KCL to trigger lambda.
     *
     */
    public static void main(final String[] args) throws Exception {
        final KinesisVideoRekognitionLambdaExample KinesisVideoRekognitionLambdaExample =
                new KinesisVideoRekognitionLambdaExample();
        KinesisVideoRekognitionLambdaExample.initialize(
                System.getProperty("KVSStreamName"), Regions.fromName(System.getenv("AWS_REGION")));
        KinesisVideoRekognitionLambdaExample.startKDSWorker(System.getProperty("KDSStreamName"));
        Thread.sleep(KCL_INIT_DELAY_MILLIS); // Initial delay to wait for KCL to initialize
        while (true) { // For local desktop testing.
            KinesisVideoRekognitionLambdaExample.processRekognizedOutputs();
        }
    }

    /**
     * Initialize method to set variables.
     */
    private void initialize(final String kvsStreamName, final Regions regionName) {
        this.inputKvsStreamName = kvsStreamName;
        outputKvsStreamName = kvsStreamName + "-Rekognized";
        kvsClient = new StreamOps(regionName, kvsStreamName, credentialsProvider);
        h264FrameProcessor = H264FrameProcessor.create(credentialsProvider, outputKvsStreamName, regionName);
        fragmentCheckpointManager = new DDBBasedFragmentCheckpointManager(kvsClient.getRegion(), credentialsProvider);
        log.info("Initialized with input KVS stream: {}, output {}, region : {}",
                inputKvsStreamName, outputKvsStreamName, regionName);
    }

    /**
     * Process Rekognized outputs for each rekognized output. For each kinesis event record i.e for each
     * fragment number create a call getMediaForFragmentList, parse fragments, decode frame, draw bounding box,
     * encode frame, call KVS PutFrame.
     *
     * @throws InterruptedException
     */
    private void processRekognizedOutputs() throws InterruptedException {
        // Get the last processed fragment number if any
        final Optional<FragmentCheckpoint> lastFragmentNumber = fragmentCheckpointManager
                .getLastProcessedItem(inputKvsStreamName);

        String fragmentNumber = null;
        while (!rekognizedFragmentsIndex.isEmpty()) {
            final RekognizedFragmentsIndex.RekognizedFragment rekognizedFragment = rekognizedFragmentsIndex.poll();
            fragmentNumber = rekognizedFragment.getFragmentNumber();
            final List<RekognizedOutput> rekognizedOutputList = rekognizedFragment.getRekognizedOutputs();
            if (lastFragmentNumber.isPresent()
                    && (fragmentNumber.equals(lastFragmentNumber.get().getFragmentNumber())
                    || rekognizedFragment.getServerTime() <= lastFragmentNumber.get().getServerTime())) {
                // If the current fragment number is equal to the last processed fragment number or if the current
                // fragment's server time is older than or equal than last processed fragment's server time then
                // skip this fragment number and proceed to next fragment.
                log.info("Current fragment number : {} is already processed or older than last processed fragment. "
                        + "So skipping..", fragmentNumber);
                continue;
            }
            try {
                final FrameVisitor frameVisitor = FrameVisitor.create(h264FrameProcessor);
                final GetMediaForFragmentListWorker worker = GetMediaForFragmentListWorker.create(
                        kvsClient.getStreamName(),
                        Collections.singletonList(fragmentNumber),
                        kvsClient.getCredentialsProvider(),
                        kvsClient.getRegion(),
                        kvsClient.getAmazonKinesisVideo(),
                        frameVisitor);
                h264FrameProcessor.setRekognizedOutputs(rekognizedOutputList);
                worker.run();
                // For every fragment, the rekognition output needs to be set and the encoder needs to be reset
                // as the JCodec encoder always treats first frame as IDR frame
                h264FrameProcessor.resetEncoder();
                log.info("Fragment {} processed successfully ...", fragmentNumber);

                // Once the current fragment number is processed save it as a checkpoint.
                fragmentCheckpointManager.saveCheckPoint(inputKvsStreamName, fragmentNumber,
                        rekognizedFragment.getProducerTime(), rekognizedFragment.getServerTime());

            } catch (final Exception e) {
                log.error("Error while processing fragment number: {}", fragmentNumber, e);
            }
        }
    }

    /**
     * Start Kinesis Data Streams worker.
     */
    public void startKDSWorker(final String kdsStreamName) {
        final KinesisDataStreamsWorker kinesisDataStreamsWorker = KinesisDataStreamsWorker.create(Regions.US_WEST_2,
                credentialsProvider, kdsStreamName, rekognizedFragmentsIndex);
        kdsWorkers.submit(kinesisDataStreamsWorker);
    }

    /**
     * Handle request for each lambda event.
     *
     * @param kinesisEvent Each kinesis event which describes the Rekognition output.
     * @param context      Lambda context
     * @return context
     */
    @Override
    public Context handleRequest(final KinesisEvent kinesisEvent, final Context context) {
        try {
            initialize(System.getProperty("KVSStreamName"), Regions.fromName(System.getenv("AWS_REGION")));
            loadProducerJNI(context);

            final List<Record> records = kinesisEvent.getRecords()
                    .stream()
                    .map(KinesisEvent.KinesisEventRecord::getKinesis)
                    .collect(Collectors.toList());
            processRecordsWithRetries(records);
            processRekognizedOutputs();

        } catch (final Exception e) {
            log.error("Unable to process lambda request !. Exiting... ", e);
        }
        return context;
    }

    /**
     * Load pre-built binary of Kinesis Video Streams Producer JNI.
     *
     * @param context
     */
    private void loadProducerJNI(final Context context) throws IOException {
        log.info("Context : {}", context);
        log.info("Working Directory = {}", System.getProperty("user.dir"));
        log.info("Java library path = {}", System.getProperty("java.library.path"));
        log.info("Class path %s", this.getClass().getProtectionDomain().getCodeSource().getLocation());
        log.info("Loading JNI .so file..");
        final ClassLoader classLoader = getClass().getClassLoader();
        final File cityFile = new File(classLoader.getResource("libKinesisVideoProducerJNI.so").getFile());
        System.load(cityFile.getAbsolutePath());
        log.info("Loaded JNI from {}", cityFile.getAbsolutePath());
    }

    /**
     * Process records performing retries as needed. Skip "poison pill" records.
     *
     * @param records Data records to be processed.
     */
    private void processRecordsWithRetries(final List<Record> records) {
        for (final Record record : records) {
            boolean processedSuccessfully = false;
            for (int i = 0; i < NUM_RETRIES; i++) {
                try {
                    log.info("Processing single record...");
                    processSingleRecord(record);
                    processedSuccessfully = true;
                    break;
                } catch (final Throwable t) {
                    log.error("Caught throwable while processing record {}", record, t);
                }
            }
            if (!processedSuccessfully) {
                log.warn("Couldn't processRekognizedOutputs record {}. Skipping the record.", record);
            }
        }
        log.info("Processed all {} KDS records.", records.size());
    }

    /**
     * Process a single record.
     *
     * @param record The record to be processed.
     */
    private void processSingleRecord(final Record record) {

        String data = null;
        final ObjectMapper mapper = new ObjectMapper();
        try {
            final ByteBuffer buffer = record.getData();
            data = new String(buffer.array(), "UTF-8");
            final RekognitionOutput output = mapper.readValue(data, RekognitionOutput.class);

            // Get the fragment number from Rekognition Output
            final String fragmentNumber = output
                    .getInputInformation()
                    .getKinesisVideo()
                    .getFragmentNumber();
            final Double frameOffsetInSeconds = output
                    .getInputInformation()
                    .getKinesisVideo()
                    .getFrameOffsetInSeconds();
            final Double serverTimestamp = output
                    .getInputInformation()
                    .getKinesisVideo()
                    .getServerTimestamp();
            final Double producerTimestamp = output
                    .getInputInformation()
                    .getKinesisVideo()
                    .getProducerTimestamp();
            final double detectedTime = output.getInputInformation().getKinesisVideo().getServerTimestamp()
                    + output.getInputInformation().getKinesisVideo().getFrameOffsetInSeconds() * 1000L;
            final RekognizedOutput rekognizedOutput = RekognizedOutput.builder()
                    .fragmentNumber(fragmentNumber)
                    .serverTimestamp(serverTimestamp)
                    .producerTimestamp(producerTimestamp)
                    .frameOffsetInSeconds(frameOffsetInSeconds)
                    .detectedTime(detectedTime)
                    .build();

            // Add face search response
            final List<FaceSearchResponse> responses = output.getFaceSearchResponse();

            responses.forEach(response -> {
                final DetectedFace detectedFace = response.getDetectedFace();
                final List<MatchedFace> matchedFaces = response.getMatchedFaces();
                final RekognizedOutput.FaceSearchOutput faceSearchOutput = RekognizedOutput.FaceSearchOutput.builder()
                        .detectedFace(detectedFace)
                        .matchedFaceList(matchedFaces)
                        .build();
                rekognizedOutput.addFaceSearchOutput(faceSearchOutput);
            });

            // Add it to the index
            log.info("Found Rekognized results for fragment number : {}", fragmentNumber);
            rekognizedFragmentsIndex.add(fragmentNumber, producerTimestamp.longValue(),
                    serverTimestamp.longValue(), rekognizedOutput);

        } catch (final NumberFormatException e) {
            log.warn("Record does not match sample record format. Ignoring record with data : {}", data, e);
        } catch (final Exception e) {
            log.error("Unable to process record !", e);
        }
    }
}
