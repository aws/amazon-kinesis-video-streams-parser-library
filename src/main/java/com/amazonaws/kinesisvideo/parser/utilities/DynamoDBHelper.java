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

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AmazonDynamoDBException;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * DynamoDB helper class to access FragmentCheckpoint table. Used by the KinesisVideoRekognitionLambdaExample to save
 * fragment checkpoints between successive lambda executions.
 */
@Slf4j
public class DynamoDBHelper {

    private static final String TABLE_NAME = "FragmentCheckpoint";
    private static final String KVS_STREAM_NAME = "KVSStreamName";
    private static final String FRAGMENT_NUMBER = "FragmentNumber";
    private static final String SERVER_TIME = "ServerTime";
    private static final String PRODUCER_TIME = "ProducerTime";
    private static final String UPDATED_TIME = "UpdatedTime";
    private final AmazonDynamoDB ddbClient;

    public DynamoDBHelper(final Regions region, final AWSCredentialsProvider credentialsProvider) {
        final ClientConfiguration clientConfiguration = new ClientConfiguration()
                .withConnectionTimeout(ClientConfiguration.DEFAULT_CONNECTION_TIMEOUT)
                .withRetryPolicy(ClientConfiguration.DEFAULT_RETRY_POLICY)
                .withRequestTimeout(ClientConfiguration.DEFAULT_REQUEST_TIMEOUT)
                .withSocketTimeout(ClientConfiguration.DEFAULT_SOCKET_TIMEOUT);
        ddbClient = AmazonDynamoDBClient.builder()
                .withClientConfiguration(clientConfiguration)
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();
    }

    /**
     * Creates the FragmentCheckpoint table if it doesn't exist already.
     */
    public void createTableIfDoesntExist() {
        // Check if table exists
        if (!checkIfTableExists()) {
            log.info("Creating table : {}", TABLE_NAME);
            final CreateTableRequest request = new CreateTableRequest() {{
                setAttributeDefinitions(
                        Collections.singletonList(
                                new AttributeDefinition(
                                        KVS_STREAM_NAME,
                                        ScalarAttributeType.S)));
                setKeySchema(
                        Collections.singletonList(
                                new KeySchemaElement(
                                        KVS_STREAM_NAME,
                                        KeyType.HASH)));
                setProvisionedThroughput(
                        new ProvisionedThroughput(1000L, 1000L));
                setTableName(TABLE_NAME);
            }};

            try {
                final CreateTableResult result = ddbClient.createTable(request);
                log.info("Table created : {}", result.getTableDescription());
            } catch (final AmazonDynamoDBException e) {
                log.error("Error creating DDB table {}...", TABLE_NAME, e);
                throw e;
            }
        }
    }

    private boolean checkIfTableExists() {
        try {
            final DescribeTableRequest request = new DescribeTableRequest() {{
                setTableName(TABLE_NAME);
            }};
            final TableDescription table_info =
                    ddbClient.describeTable(request).getTable();
            log.info("Table exists : {}", table_info.getTableName());
            return true;
        } catch (final ResourceNotFoundException e) {
            log.warn("{} table doesn't exist !", TABLE_NAME);
        } catch (final AmazonDynamoDBException e) {
            log.warn("Error while describing table!", e);
        }
        return false;
    }

    /**
     * Gets the FragmentCheckpoint item from the table for the specified stream name.
     *
     * @param streamName Input stream name
     * @return FragmentCheckpoint entry. null if any exception is thrown.
     */
    public Map<String, AttributeValue> getItem(final String streamName) {
        try {
            final Map<String,AttributeValue> key = new HashMap<>();
            key.put(KVS_STREAM_NAME, new AttributeValue().withS(streamName));
            final GetItemRequest getItemRequest = new GetItemRequest() {{
                setTableName(TABLE_NAME);
                setKey(key);
            }};

            return ddbClient.getItem(getItemRequest).getItem();
        } catch (final AmazonDynamoDBException e) {
            log.warn("Error while getting item from table!", e);
        }
        return null;
    }

    /**
     * Put item into FragmentCheckpoint table for the given input parameters
     *
     * @param streamName KVS Stream name
     * @param fragmentNumber Last processed fragment's fragment number
     * @param producerTime Last processed fragment's producer time
     * @param serverTime Last processed fragment's server time
     * @param updatedTime Time when the entry is going to be updated.
     */
    public void putItem(final String streamName, final String fragmentNumber,
                         final Long producerTime, final Long serverTime, final Long updatedTime) {
        try {
            final Map<String,AttributeValue> item = new HashMap<>();
            item.put(KVS_STREAM_NAME, new AttributeValue().withS(streamName));
            item.put(FRAGMENT_NUMBER, new AttributeValue().withS(fragmentNumber));
            item.put(UPDATED_TIME, new AttributeValue().withN(updatedTime.toString()));
            item.put(PRODUCER_TIME, new AttributeValue().withN(producerTime.toString()));
            item.put(SERVER_TIME, new AttributeValue().withN(serverTime.toString()));
            final PutItemRequest putItemRequest = new PutItemRequest()
            {{
                setTableName(TABLE_NAME);
                setItem(item);
            }};

            final PutItemResult result = ddbClient.putItem(putItemRequest);
            log.info("Item saved : ", result.getAttributes());
        } catch (final Exception e) {
            log.warn("Error while putting item into the table!", e);
        }
    }

    /**
     * Update item into FragmentCheckpoint table for the given input parameters
     *
     * @param streamName KVS Stream name
     * @param fragmentNumber Last processed fragment's fragment number
     * @param producerTime Last processed fragment's producer time
     * @param serverTime Last processed fragment's server time
     * @param updatedTime Time when the entry is going to be updated.
     */
    public void updateItem(final String streamName, final String fragmentNumber,
                            final Long producerTime, final Long serverTime, final Long updatedTime) {
        try {
            final Map<String,AttributeValue> key = new HashMap<>();
            key.put(KVS_STREAM_NAME, new AttributeValue().withS(streamName));
            final Map<String,AttributeValueUpdate> updates = new HashMap<>();
            updates.put(FRAGMENT_NUMBER, new AttributeValueUpdate().withValue(
                    new AttributeValue().withS(fragmentNumber)));
            updates.put(UPDATED_TIME, new AttributeValueUpdate().withValue(
                    new AttributeValue().withN(updatedTime.toString())));
            updates.put(PRODUCER_TIME, new AttributeValueUpdate().withValue(
                    new AttributeValue().withN(producerTime.toString())));
            updates.put(SERVER_TIME, new AttributeValueUpdate().withValue(
                    new AttributeValue().withN(serverTime.toString())));
            final UpdateItemRequest updateItemRequest = new UpdateItemRequest()
            {{
                setTableName(TABLE_NAME);
                setKey(key);
                setAttributeUpdates(updates);
            }};

            final UpdateItemResult result = ddbClient.updateItem(updateItemRequest);
            log.info("Item updated : {}", result.getAttributes());
        } catch (final Exception e) {
            log.warn("Error while updating item in the table!", e);
        }
    }
}
