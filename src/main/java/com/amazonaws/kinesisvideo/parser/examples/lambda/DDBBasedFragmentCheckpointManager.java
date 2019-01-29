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
import com.amazonaws.kinesisvideo.parser.utilities.DynamoDBHelper;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

/**
 * DynamdDB based FragmentCheckpoint Manager which manages the checkpoints for last processed fragments.
 */
@Slf4j
public class DDBBasedFragmentCheckpointManager implements FragmentCheckpointManager {

    private static final String TABLE_NAME = "FragmentCheckpoint";
    private static final String KVS_STREAM_NAME = "KVSStreamName";
    private static final String FRAGMENT_NUMBER = "FragmentNumber";
    private static final String SERVER_TIME = "ServerTime";
    private static final String PRODUCER_TIME = "ProducerTime";
    private static final String UPDATED_TIME = "UpdatedTime";
    private final DynamoDBHelper dynamoDBHelper;

    public DDBBasedFragmentCheckpointManager(final Regions region, final AWSCredentialsProvider credentialsProvider) {
        dynamoDBHelper = new DynamoDBHelper(region, credentialsProvider);
        dynamoDBHelper.createTableIfDoesntExist();
    }

    /**
     * Get last processed fragment details from checkpoint for given stream name.
     *
     * @param streamName KVS Stream name
     * @return Optional of last processed fragment item if checkpoint exists. Empty otherwise
     */
    @Override
    public Optional<FragmentCheckpoint> getLastProcessedItem(final String streamName) {
        final Map<String, AttributeValue> result = dynamoDBHelper.getItem(streamName);
        if (result != null && result.containsKey(FRAGMENT_NUMBER)) {
            return Optional.of(new FragmentCheckpoint(streamName,
                    result.get(FRAGMENT_NUMBER).getS(),
                    Long.parseLong(result.get(PRODUCER_TIME).getN()),
                    Long.parseLong(result.get(SERVER_TIME).getN()),
                    Long.parseLong(result.get(UPDATED_TIME).getN())));
        }
        return Optional.empty();
    }

    /**
     * Save last processed fragment details checkpoint for the given stream name.
     *
     * @param streamName KVS Stream name
     * @param fragmentNumber Last processed fragment's fragment number
     * @param producerTime Last processed fragment's producer time
     * @param serverTime Last processed fragment's server time
     */
    @Override
    public void saveCheckPoint(final String streamName, final String fragmentNumber,
                               final Long producerTime, final Long serverTime) {
        if (fragmentNumber != null) {
            if (dynamoDBHelper.getItem(streamName) != null) {
                log.info("Checkpoint for stream name {} already exists. So updating checkpoint with fragment number: {}",
                        streamName, fragmentNumber);
                dynamoDBHelper.updateItem(streamName, fragmentNumber, producerTime, serverTime, System.currentTimeMillis());
            } else {
                log.info("Creating checkpoint for stream name {} with fragment number: {}", streamName, fragmentNumber);
                dynamoDBHelper.putItem(streamName, fragmentNumber, producerTime, serverTime, System.currentTimeMillis());
            }
        } else {
            log.info("Fragment number is null. Skipping save checkpoint...");
        }
    }
}
