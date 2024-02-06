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

import com.amazonaws.SdkClientException;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;
import software.amazon.awssdk.utils.ImmutableMap;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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
    private final DynamoDbAsyncClient ddbClient;

    public DynamoDBHelper(final Region region, final AwsCredentialsProvider credentialsProvider) {

        ddbClient = DynamoDbAsyncClient.builder()
                .httpClientBuilder(AwsCrtAsyncHttpClient
                        .builder()
                        .connectionTimeout(Duration.ofSeconds(3))
                        .maxConcurrency(100))
                .credentialsProvider(credentialsProvider)
                .region(region)
                .build();
    }

    /**
     * Creates the FragmentCheckpoint table if it doesn't exist already.
     */
    public void createTableIfDoesntExist() {
        // Check if table exists
        if (!checkIfTableExists()) {
            log.info("Creating table : {}", TABLE_NAME);

            final CreateTableRequest createTableRequest = CreateTableRequest.builder()
                    .tableName(TABLE_NAME)
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName(KVS_STREAM_NAME)
                            .attributeType(ScalarAttributeType.S)
                            .build())
                    .keySchema(KeySchemaElement.builder()
                            .attributeName(KVS_STREAM_NAME)
                            .keyType(KeyType.HASH)
                            .build())
                    .provisionedThroughput(ProvisionedThroughput.builder()
                            .readCapacityUnits(1000L)
                            .writeCapacityUnits(1000L)
                            .build())
                    .build();

            try {
                final CreateTableResponse createTableResponse = ddbClient.createTable(createTableRequest).get();
                log.info("Table created : {}", createTableResponse.tableDescription());
            } catch (final InterruptedException | ExecutionException e) {
                log.error("Error creating DDB table {}...", TABLE_NAME, e);
                throw new RuntimeException(e);
            }
        }
    }

    private boolean checkIfTableExists() {
        try {

            final DescribeTableRequest describeTableRequest = DescribeTableRequest.builder()
                    .tableName(TABLE_NAME).build();

            final TableDescription table_info =
                    ddbClient.describeTable(describeTableRequest).get().table();

            log.info("Table exists : {}", table_info.tableName());
            return true;
        } catch (final ResourceNotFoundException e) {
            log.warn("{} table doesn't exist !", TABLE_NAME);
        } catch (final InterruptedException | ExecutionException | SdkException | SdkClientException e) {
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
            final Map<String,AttributeValue> key = Collections.singletonMap(KVS_STREAM_NAME, AttributeValue.builder().s(streamName).build());

            final GetItemRequest getItemRequest = GetItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(key)
                    .build();

            return ddbClient.getItem(getItemRequest).get().item();
        } catch (final InterruptedException | ExecutionException | SdkException | SdkClientException e) {
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

            final Map<String, AttributeValue> item = ImmutableMap.of(KVS_STREAM_NAME, AttributeValue.builder().s(streamName).build(),
            FRAGMENT_NUMBER, AttributeValue.builder().s(fragmentNumber).build(),
            UPDATED_TIME, AttributeValue.builder().n(updatedTime.toString()).build(),
            PRODUCER_TIME, AttributeValue.builder().n(producerTime.toString()).build(),
            SERVER_TIME, AttributeValue.builder().n(serverTime.toString()).build());

            final PutItemRequest putItemRequest = PutItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .item(item)
                    .build();

            final PutItemResponse putItemResponse = ddbClient.putItem(putItemRequest).get();
            log.info("Item saved : {}", putItemResponse.attributes());
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

            final Map<String,AttributeValue> key = Collections.singletonMap(KVS_STREAM_NAME, AttributeValue.builder().s(streamName).build());

            final Map<String, AttributeValueUpdate> updates = ImmutableMap.of(FRAGMENT_NUMBER, AttributeValueUpdate.builder().value(AttributeValue.builder().s(fragmentNumber).build()).build(),
                    UPDATED_TIME, AttributeValueUpdate.builder().value(AttributeValue.builder().n(updatedTime.toString()).build()).build(),
                    PRODUCER_TIME, AttributeValueUpdate.builder().value(AttributeValue.builder().n(producerTime.toString()).build()).build(),
                    SERVER_TIME, AttributeValueUpdate.builder().value(AttributeValue.builder().n(serverTime.toString()).build()).build());


            final UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(key)
                    .attributeUpdates(updates)
                    .build();

            final UpdateItemResponse updateItemResponse = ddbClient.updateItem(updateItemRequest).get();
            log.info("Item updated : {}", updateItemResponse.attributes());
        } catch (final Exception e) {
            log.warn("Error while updating item in the table!", e);
        }
    }
}
