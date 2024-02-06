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

import java.net.InetAddress;
import java.util.UUID;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.kinesisvideo.parser.rekognition.pojo.RekognizedFragmentsIndex;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.common.InitialPositionInStream;
import software.amazon.kinesis.common.KinesisClientUtil;
import software.amazon.kinesis.processor.ShardRecordProcessorFactory;

import software.amazon.kinesis.coordinator.Scheduler;
import software.amazon.kinesis.common.ConfigsBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Sample Amazon Kinesis Application.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class KinesisDataStreamsWorker implements Runnable {

    private static final String APPLICATION_NAME = "rekognition-kds-stream-application";

    // Initial position in the stream when the application starts up for the first time.
    // Position can be one of LATEST (most recent data) or TRIM_HORIZON (oldest available data)
    private static final InitialPositionInStream SAMPLE_APPLICATION_INITIAL_POSITION_IN_STREAM =
            InitialPositionInStream.LATEST;

    private final Region region;
    private final AWSCredentialsProvider credentialsProvider;
    private final String kdsStreamName;
    private final RekognizedFragmentsIndex rekognizedFragmentsIndex;

    public static KinesisDataStreamsWorker create(final Region region, final AWSCredentialsProvider credentialsProvider,
                              final String kdsStreamName, final RekognizedFragmentsIndex rekognizedFragmentsIndex) {
        return new KinesisDataStreamsWorker(region, credentialsProvider, kdsStreamName, rekognizedFragmentsIndex);
    }

    @Override
    public void run() {

        try {
            String workerId = InetAddress.getLocalHost().getCanonicalHostName() + ":" + UUID.randomUUID();
            final KinesisAsyncClient kinesisClient = KinesisClientUtil.createKinesisAsyncClient(KinesisAsyncClient.builder().region(region));
            final DynamoDbAsyncClient dynamoClient = DynamoDbAsyncClient.builder().region(region).build();
            final CloudWatchAsyncClient cloudWatchClient = CloudWatchAsyncClient.builder().region(region).build();
            final ShardRecordProcessorFactory shardRecordProcessorFactory =
                    () -> new KinesisRecordProcessor(rekognizedFragmentsIndex, credentialsProvider);

            final ConfigsBuilder configsBuilder = new ConfigsBuilder(kdsStreamName, APPLICATION_NAME, kinesisClient, dynamoClient, cloudWatchClient, workerId, shardRecordProcessorFactory);
            final Scheduler scheduler = new Scheduler(
                    configsBuilder.checkpointConfig(),
                    configsBuilder.coordinatorConfig(),
                    configsBuilder.leaseManagementConfig(),
                    configsBuilder.lifecycleConfig(),
                    configsBuilder.metricsConfig(),
                    configsBuilder.processorConfig(),
                    configsBuilder.retrievalConfig()
            );

            System.out.printf("Running %s to process stream %s as worker %s...",
                    APPLICATION_NAME,
                    kdsStreamName,
                    workerId);

            int exitCode = 0;
            try {
                scheduler.run();
            } catch (Throwable t) {
                System.err.println("Caught throwable while processing data.");
                t.printStackTrace();
                exitCode = 1;
            }
            System.out.println("Exit code : " + exitCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}