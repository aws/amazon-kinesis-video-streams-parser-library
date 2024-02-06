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
package com.amazonaws.kinesisvideo.parser.kinesis;

/*
 * Copyright 2012-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.kinesisvideo.parser.rekognition.pojo.DetectedFace;
import com.amazonaws.kinesisvideo.parser.rekognition.pojo.FaceSearchResponse;
import com.amazonaws.kinesisvideo.parser.rekognition.pojo.MatchedFace;
import com.amazonaws.kinesisvideo.parser.rekognition.pojo.RekognitionOutput;
import com.amazonaws.kinesisvideo.parser.rekognition.pojo.RekognizedFragmentsIndex;
import com.amazonaws.kinesisvideo.parser.rekognition.pojo.RekognizedOutput;

import software.amazon.kinesis.exceptions.ThrottlingException;
import software.amazon.kinesis.exceptions.InvalidStateException;
import software.amazon.kinesis.exceptions.ShutdownException;

import software.amazon.kinesis.lifecycle.events.InitializationInput;
import software.amazon.kinesis.lifecycle.events.LeaseLostInput;
import software.amazon.kinesis.lifecycle.events.ProcessRecordsInput;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.kinesis.lifecycle.events.ShardEndedInput;
import software.amazon.kinesis.lifecycle.events.ShutdownRequestedInput;
import software.amazon.kinesis.processor.RecordProcessorCheckpointer;
import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

/**
 * Processes records and checkpoints progress.
 */
public class KinesisRecordProcessor implements ShardRecordProcessor {

    private static final Log LOG = LogFactory.getLog(KinesisRecordProcessor.class);
    private String kinesisShardId;

    // Backoff and retry settings
    private static final long BACKOFF_TIME_IN_MILLIS = 3000L;
    private static final int NUM_RETRIES = 10;
    private static final String DELIMITER = "$";

    // Checkpoint about once a minute
    private static final long CHECKPOINT_INTERVAL_MILLIS = 1000L;
    private long nextCheckpointTimeInMillis;

    private final RekognizedFragmentsIndex rekognizedFragmentsIndex;
    private StringBuilder stringBuilder = new StringBuilder();

    public KinesisRecordProcessor(final RekognizedFragmentsIndex rekognizedFragmentsIndex, final AWSCredentialsProvider awsCredentialsProvider) {
        this.rekognizedFragmentsIndex = rekognizedFragmentsIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(final InitializationInput initializationInput) {
        LOG.info("Initializing record processor for shard: " + initializationInput.shardId());
        this.kinesisShardId = initializationInput.shardId();
    }


    //     public void processRecords(final List<Record> records, final IRecordProcessorCheckpointer checkpointer) {
    /**
     * {@inheritDoc}
     */
    @Override
    public void processRecords(final ProcessRecordsInput processRecordsInput) {
        LOG.info("Processing " + processRecordsInput.records().size() + " records from " + this.kinesisShardId);

        // Process records and perform all exception handling.
        processRecordsWithRetries(processRecordsInput.records());

        // Checkpoint once every checkpoint interval.
        if (System.currentTimeMillis() > this.nextCheckpointTimeInMillis) {
            checkpoint(processRecordsInput.checkpointer());
            this.nextCheckpointTimeInMillis = System.currentTimeMillis() + CHECKPOINT_INTERVAL_MILLIS;
        }
    }

    /**
     * Process records performing retries as needed. Skip "poison pill" records.
     *
     * @param records Data records to be processed.
     */
    private void processRecordsWithRetries(final List<KinesisClientRecord> records) {
        for (final KinesisClientRecord record : records) {
            boolean processedSuccessfully = false;
            for (int i = 0; i < NUM_RETRIES; i++) {
                try {
                    processSingleRecord(record);
                    processedSuccessfully = true;
                    break;
                } catch (final Throwable t) {
                    LOG.warn("Caught throwable while processing record " + record, t);
                }

                // backoff if we encounter an exception.
                try {
                    Thread.sleep(BACKOFF_TIME_IN_MILLIS);
                } catch (final InterruptedException e) {
                    LOG.debug("Interrupted sleep", e);
                }
            }

            if (!processedSuccessfully) {
                LOG.error("Couldn't process record " + record + ". Skipping the record.");
            }
        }
    }

    /**
     * Process a single record.
     *
     * @param record The record to be processed.
     */
    private void processSingleRecord(final KinesisClientRecord record) {

        String data = null;
        final ObjectMapper mapper = new ObjectMapper();
        try {
            // For this app, we interpret the payload as UTF-8 chars.
            final ByteBuffer buffer = record.data();
            data = new String(buffer.array(), StandardCharsets.UTF_8);
            stringBuilder.append(data).append(DELIMITER);

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

            for (final FaceSearchResponse response : responses) {
                final DetectedFace detectedFace = response.getDetectedFace();
                final List<MatchedFace> matchedFaces = response.getMatchedFaces();
                final RekognizedOutput.FaceSearchOutput faceSearchOutput = RekognizedOutput.FaceSearchOutput.builder()
                        .detectedFace(detectedFace)
                        .matchedFaceList(matchedFaces)
                        .build();
                rekognizedOutput.addFaceSearchOutput(faceSearchOutput);
            }

            // Add it to the index
            rekognizedFragmentsIndex.add(fragmentNumber, producerTimestamp.longValue(),
                    serverTimestamp.longValue(), rekognizedOutput);

        } catch (final NumberFormatException e) {
            LOG.info("Record does not match sample record format. Ignoring record with data; " + data);
        } catch (final JsonParseException e) {
            e.printStackTrace();
        } catch (final JsonMappingException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void leaseLost(final LeaseLostInput leaseLostInput) {
        LOG.info("Lost lease, so terminating.");
    }

    @Override
    public void shardEnded(final ShardEndedInput shardEndedInput) {
        try {
            LOG.info("Reached shard end, Checkpointing.");
            shardEndedInput.checkpointer().checkpoint();
        } catch (final ShutdownException | InvalidStateException e) {
            LOG.error("Exception while checkpointing at shard end. Giving up.", e);
        }
    }

    @Override
    public void shutdownRequested(final ShutdownRequestedInput shutdownRequestedInput) {
        LOG.info("Scheduler is shutting down, checkpointing.");
        shutdownRequestedInput.checkpointer().checkpoint();
    }

    /** Checkpoint with retries.
     * @param checkpointer
     */
    private void checkpoint(final RecordProcessorCheckpointer checkpointer) {
        LOG.info("Checkpointing shard " + kinesisShardId);
        for (int i = 0; i < NUM_RETRIES; i++) {
            try {
                checkpointer.checkpoint();
                break;
            } catch (final ShutdownException se) {
                // Ignore checkpoint if the processor instance has been shutdown (fail over).
                LOG.info("Caught shutdown exception, skipping checkpoint.", se);
                break;
            } catch (final ThrottlingException e) {
                // Backoff and re-attempt checkpoint upon transient failures
                if (i >= (NUM_RETRIES - 1)) {
                    LOG.error("Checkpoint failed after " + (i + 1) + "attempts.", e);
                    break;
                } else {
                    LOG.info("Transient issue when checkpointing - attempt " + (i + 1) + " of "
                            + NUM_RETRIES, e);
                }
            } catch (final InvalidStateException e) {
                // This indicates an issue with the DynamoDB table (check for table, provisioned IOPS).
                LOG.error("Cannot save checkpoint to the DynamoDB table used by the Amazon Kinesis Client Library.", e);
                break;
            }

            try {
                Thread.sleep(BACKOFF_TIME_IN_MILLIS);
            } catch (final InterruptedException e) {
                LOG.debug("Interrupted sleep", e);
            }
        }
    }
}
