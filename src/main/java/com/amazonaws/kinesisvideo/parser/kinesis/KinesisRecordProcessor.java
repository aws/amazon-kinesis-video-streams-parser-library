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
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.List;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.kinesisvideo.parser.rekognition.pojo.DetectedFace;
import com.amazonaws.kinesisvideo.parser.rekognition.pojo.FaceSearchResponse;
import com.amazonaws.kinesisvideo.parser.rekognition.pojo.MatchedFace;
import com.amazonaws.kinesisvideo.parser.rekognition.pojo.RekognitionOutput;
import com.amazonaws.kinesisvideo.parser.rekognition.pojo.RekognizedFragmentsIndex;
import com.amazonaws.kinesisvideo.parser.rekognition.pojo.RekognizedOutput;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ThrottlingException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Processes records and checkpoints progress.
 */
public class KinesisRecordProcessor implements IRecordProcessor {

    private static final Log LOG = LogFactory.getLog(KinesisRecordProcessor.class);
    private String kinesisShardId;

    // Backoff and retry settings
    private static final long BACKOFF_TIME_IN_MILLIS = 3000L;
    private static final int NUM_RETRIES = 10;

    // Checkpoint about once a minute
    private static final long CHECKPOINT_INTERVAL_MILLIS = 1000L;
    private long nextCheckpointTimeInMillis;

    private final CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();

    private final RekognizedFragmentsIndex rekognizedFragmentsIndex;

    public KinesisRecordProcessor(RekognizedFragmentsIndex rekognizedFragmentsIndex, AWSCredentialsProvider awsCredentialsProvider) {
        this.rekognizedFragmentsIndex = rekognizedFragmentsIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(String shardId) {
        LOG.info("Initializing record processor for shard: " + shardId);
        this.kinesisShardId = shardId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processRecords(List<Record> records, IRecordProcessorCheckpointer checkpointer) {
        LOG.info("Processing " + records.size() + " records from " + kinesisShardId);

        // Process records and perform all exception handling.
        processRecordsWithRetries(records);

        // Checkpoint once every checkpoint interval.
        if (System.currentTimeMillis() > nextCheckpointTimeInMillis) {
            checkpoint(checkpointer);
            nextCheckpointTimeInMillis = System.currentTimeMillis() + CHECKPOINT_INTERVAL_MILLIS;
        }
    }

    /**
     * Process records performing retries as needed. Skip "poison pill" records.
     *
     * @param records Data records to be processed.
     */
    private void processRecordsWithRetries(List<Record> records) {
        for (Record record : records) {
            boolean processedSuccessfully = false;
            for (int i = 0; i < NUM_RETRIES; i++) {
                try {
                    processSingleRecord(record);
                    processedSuccessfully = true;
                    break;
                } catch (Throwable t) {
                    LOG.warn("Caught throwable while processing record " + record, t);
                }

                // backoff if we encounter an exception.
                try {
                    Thread.sleep(BACKOFF_TIME_IN_MILLIS);
                } catch (InterruptedException e) {
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
    private void processSingleRecord(Record record) {
        // TODO Add your own record processing logic here

        String data = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            // For this app, we interpret the payload as UTF-8 chars.
            ByteBuffer buffer = record.getData();
            data = new String(buffer.array(), "UTF-8");
            RekognitionOutput output = mapper.readValue(data, RekognitionOutput.class);

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
            List<FaceSearchResponse> responses = output.getFaceSearchResponse();

            for (FaceSearchResponse response : responses) {
                DetectedFace detectedFace = response.getDetectedFace();
                List<MatchedFace> matchedFaces = response.getMatchedFaces();
                RekognizedOutput.FaceSearchOutput faceSearchOutput = RekognizedOutput.FaceSearchOutput.builder()
                        .detectedFace(detectedFace)
                        .matchedFaceList(matchedFaces)
                        .build();
                rekognizedOutput.addFaceSearchOutput(faceSearchOutput);
            }

            // Add it to the index
            rekognizedFragmentsIndex.addToMap(fragmentNumber, rekognizedOutput);

        } catch (NumberFormatException e) {
            LOG.info("Record does not match sample record format. Ignoring record with data; " + data);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown(IRecordProcessorCheckpointer checkpointer, ShutdownReason reason) {
        LOG.info("Shutting down record processor for shard: " + kinesisShardId);
        // Important to checkpoint after reaching end of shard, so we can start processing data from child shards.
        if (reason == ShutdownReason.TERMINATE) {
            checkpoint(checkpointer);
        }
    }

    /** Checkpoint with retries.
     * @param checkpointer
     */
    private void checkpoint(IRecordProcessorCheckpointer checkpointer) {
        LOG.info("Checkpointing shard " + kinesisShardId);
        for (int i = 0; i < NUM_RETRIES; i++) {
            try {
                checkpointer.checkpoint();
                break;
            } catch (ShutdownException se) {
                // Ignore checkpoint if the processor instance has been shutdown (fail over).
                LOG.info("Caught shutdown exception, skipping checkpoint.", se);
                break;
            } catch (ThrottlingException e) {
                // Backoff and re-attempt checkpoint upon transient failures
                if (i >= (NUM_RETRIES - 1)) {
                    LOG.error("Checkpoint failed after " + (i + 1) + "attempts.", e);
                    break;
                } else {
                    LOG.info("Transient issue when checkpointing - attempt " + (i + 1) + " of "
                            + NUM_RETRIES, e);
                }
            } catch (InvalidStateException e) {
                // This indicates an issue with the DynamoDB table (check for table, provisioned IOPS).
                LOG.error("Cannot save checkpoint to the DynamoDB table used by the Amazon Kinesis Client Library.", e);
                break;
            }
            try {
                Thread.sleep(BACKOFF_TIME_IN_MILLIS);
            } catch (InterruptedException e) {
                LOG.debug("Interrupted sleep", e);
            }
        }
    }
}
