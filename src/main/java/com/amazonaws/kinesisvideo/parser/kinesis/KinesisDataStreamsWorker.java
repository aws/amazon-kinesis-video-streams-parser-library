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
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
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

    private final Regions region;
    private final AWSCredentialsProvider credentialsProvider;
    private final String kdsStreamName;
    private final RekognizedFragmentsIndex rekognizedFragmentsIndex;

    public static KinesisDataStreamsWorker create(final Regions region, final AWSCredentialsProvider credentialsProvider,
                              final String kdsStreamName, final RekognizedFragmentsIndex rekognizedFragmentsIndex) {
        return new KinesisDataStreamsWorker(region, credentialsProvider, kdsStreamName, rekognizedFragmentsIndex);
    }

    @Override
    public void run() {

        try {
            String workerId = InetAddress.getLocalHost().getCanonicalHostName() + ":" + UUID.randomUUID();
            KinesisClientLibConfiguration kinesisClientLibConfiguration =
                    new KinesisClientLibConfiguration(APPLICATION_NAME,
                            kdsStreamName,
                            credentialsProvider,
                            workerId);
            kinesisClientLibConfiguration.withInitialPositionInStream(SAMPLE_APPLICATION_INITIAL_POSITION_IN_STREAM)
                    .withRegionName(region.getName());

            final IRecordProcessorFactory recordProcessorFactory =
                    () -> new KinesisRecordProcessor(rekognizedFragmentsIndex, credentialsProvider);
            final Worker worker = new Worker(recordProcessorFactory, kinesisClientLibConfiguration);

            System.out.printf("Running %s to process stream %s as worker %s...",
                    APPLICATION_NAME,
                    kdsStreamName,
                    workerId);

            int exitCode = 0;
            try {
                worker.run();
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