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
package com.amazonaws.kinesisvideo.parser.rekognition.processor;

import com.amazonaws.kinesisvideo.parser.rekognition.pojo.RekognitionInput;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.CreateStreamProcessorRequest;
import software.amazon.awssdk.services.rekognition.model.CreateStreamProcessorResponse;
import software.amazon.awssdk.services.rekognition.model.DeleteStreamProcessorRequest;
import software.amazon.awssdk.services.rekognition.model.DeleteStreamProcessorResponse;
import software.amazon.awssdk.services.rekognition.model.DescribeStreamProcessorRequest;
import software.amazon.awssdk.services.rekognition.model.DescribeStreamProcessorResponse;
import software.amazon.awssdk.services.rekognition.model.FaceSearchSettings;
import software.amazon.awssdk.services.rekognition.model.KinesisDataStream;
import software.amazon.awssdk.services.rekognition.model.KinesisVideoStream;
import software.amazon.awssdk.services.rekognition.model.ListStreamProcessorsRequest;
import software.amazon.awssdk.services.rekognition.model.ListStreamProcessorsResponse;
import software.amazon.awssdk.services.rekognition.model.ResourceNotFoundException;
import software.amazon.awssdk.services.rekognition.model.StartStreamProcessorRequest;
import software.amazon.awssdk.services.rekognition.model.StartStreamProcessorResponse;
import software.amazon.awssdk.services.rekognition.model.StopStreamProcessorRequest;
import software.amazon.awssdk.services.rekognition.model.StopStreamProcessorResponse;
import software.amazon.awssdk.services.rekognition.model.StreamProcessor;
import software.amazon.awssdk.services.rekognition.model.StreamProcessorInput;
import software.amazon.awssdk.services.rekognition.model.StreamProcessorOutput;
import software.amazon.awssdk.services.rekognition.model.StreamProcessorSettings;
import software.amazon.awssdk.services.rekognition.model.StreamProcessorStatus;

/**
 * Rekognition Stream Processor client class which acts as a wrapper for invoking corresponding Rekognition APIs.
 *
 */
@Slf4j
public class RekognitionStreamProcessor {

    private String streamProcessorName;
    private String kinesisVideoStreamArn;
    private String kinesisDataStreamArn;
    private String roleArn;
    private String collectionId;
    private float matchThreshold;
    private String region;
    private RekognitionClient rekognitionClient;

    private RekognitionStreamProcessor(final Region region, final AwsCredentialsProvider provider,
                                       final RekognitionInput rekognitionInput) {
        this.streamProcessorName = rekognitionInput.getStreamingProcessorName();
        this.kinesisVideoStreamArn = rekognitionInput.getKinesisVideoStreamArn();
        this.kinesisDataStreamArn = rekognitionInput.getKinesisDataStreamArn();
        this.roleArn = rekognitionInput.getIamRoleArn();
        this.collectionId = rekognitionInput.getFaceCollectionId();
        this.matchThreshold = rekognitionInput.getMatchThreshold();

        rekognitionClient = RekognitionClient.builder()
                .region(region)
                .credentialsProvider(provider)
                .build();
    }

    public static RekognitionStreamProcessor create(final Region region, final AwsCredentialsProvider provider,
                                                    final RekognitionInput rekognitionInput) {
        return new RekognitionStreamProcessor(region, provider, rekognitionInput);
    }

    /**
     * Creates a StreamProcess if it doesn't exist already. Once the stream processor is created, it's started and then
     * described to know the result of the stream processor.
     */
    public void process() {
        // Creates a stream processor if it doesn't already exist and start.
        try {
            final DescribeStreamProcessorResponse response = describeStreamProcessor();
            if (!response.status().equals(StreamProcessorStatus.RUNNING)) {
                startStreamProcessor();
            }
        } catch (final ResourceNotFoundException e) {
            log.info("StreamProcessor with name : {} doesnt exist. Creating...", streamProcessorName);
            createStreamProcessor();
            startStreamProcessor();
        }

        // Describe the Stream Processor results to log the status.
        describeStreamProcessor();
    }

    public CreateStreamProcessorResponse createStreamProcessor() {
        final KinesisVideoStream kinesisVideoStream = KinesisVideoStream.builder()
                .arn(kinesisVideoStreamArn)
                .build();
        final StreamProcessorInput streamProcessorInput = StreamProcessorInput.builder()
                .kinesisVideoStream(kinesisVideoStream)
                .build();
        final KinesisDataStream kinesisDataStream = KinesisDataStream.builder()
                .arn(kinesisDataStreamArn)
                .build();
        final StreamProcessorOutput streamProcessorOutput = StreamProcessorOutput.builder()
                .kinesisDataStream(kinesisDataStream)
                .build();
        final FaceSearchSettings faceSearchSettings = FaceSearchSettings.builder()
                .collectionId(collectionId)
                .faceMatchThreshold(matchThreshold)
                .build();
        final StreamProcessorSettings streamProcessorSettings = StreamProcessorSettings.builder()
                .faceSearch(faceSearchSettings)
                .build();

        final CreateStreamProcessorResponse createStreamProcessorResponse =
                rekognitionClient.createStreamProcessor(CreateStreamProcessorRequest.builder()
                        .input(streamProcessorInput)
                        .output(streamProcessorOutput)
                        .settings(streamProcessorSettings)
                        .roleArn(roleArn)
                        .name(streamProcessorName)
                        .build());
        log.info("StreamProcessorArn : {} ", createStreamProcessorResponse.streamProcessorArn());
        return createStreamProcessorResponse;
    }

    public StartStreamProcessorResponse startStreamProcessor() {
        final StartStreamProcessorResponse startStreamProcessorResponse =
                rekognitionClient.startStreamProcessor(StartStreamProcessorRequest.builder().name(streamProcessorName).build());
        log.info("SdkResponseMetadata : {} ", startStreamProcessorResponse.responseMetadata());
        return startStreamProcessorResponse;
    }

    public StopStreamProcessorResponse stopStreamProcessor() {
        final StopStreamProcessorResponse stopStreamProcessorResponse =
                rekognitionClient.stopStreamProcessor(StopStreamProcessorRequest.builder().name(streamProcessorName).build());
        log.info("SdkResponseMetadata : {} ", stopStreamProcessorResponse.responseMetadata());
        return stopStreamProcessorResponse;
    }

    public DeleteStreamProcessorResponse deleteStreamProcessor() {
        final DeleteStreamProcessorResponse deleteStreamProcessorResult = rekognitionClient
                .deleteStreamProcessor(DeleteStreamProcessorRequest.builder().name(streamProcessorName).build());
        log.info("SdkResponseMetadata : {} ", deleteStreamProcessorResult.responseMetadata());
        return deleteStreamProcessorResult;
    }

    public DescribeStreamProcessorResponse describeStreamProcessor() {
        final DescribeStreamProcessorResponse describeStreamProcessorResponse = rekognitionClient
                .describeStreamProcessor(DescribeStreamProcessorRequest.builder().name(streamProcessorName).build());
        //                .describeStreamProcessor(new DescribeStreamProcessorRequest().withName(streamProcessorName));
        log.info("Arn : {}", describeStreamProcessorResponse.streamProcessorArn());
        log.info("Input kinesisVideo stream : {} ",
                describeStreamProcessorResponse.input().kinesisVideoStream().arn());
        log.info("Output kinesisData stream {} ",
                describeStreamProcessorResponse.output().kinesisDataStream().arn());
        log.info("RoleArn {} ", describeStreamProcessorResponse.roleArn());
        log.info(
                "CollectionId {} ", describeStreamProcessorResponse.settings().faceSearch().collectionId());
        log.info("Status {} ", describeStreamProcessorResponse.status());
        log.info("Status message {} ", describeStreamProcessorResponse.statusMessage());
        log.info("Creation timestamp {} ", describeStreamProcessorResponse.creationTimestamp());
        log.info("Last update timestamp {} ", describeStreamProcessorResponse.lastUpdateTimestamp());
        return describeStreamProcessorResponse;
    }

    public ListStreamProcessorsResponse listStreamProcessor() {
        final ListStreamProcessorsResponse listStreamProcessorsResult =
                rekognitionClient.listStreamProcessors(ListStreamProcessorsRequest.builder().maxResults(100).build());
        for (final StreamProcessor streamProcessor : listStreamProcessorsResult.streamProcessors()) {
            log.info("StreamProcessor name {} ", streamProcessor.name());
            log.info("Status {} ", streamProcessor.status());
        }
        return listStreamProcessorsResult;
    }

}
