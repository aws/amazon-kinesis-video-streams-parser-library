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

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.kinesisvideo.parser.rekognition.pojo.RekognitionInput;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.CreateStreamProcessorRequest;
import com.amazonaws.services.rekognition.model.CreateStreamProcessorResult;
import com.amazonaws.services.rekognition.model.DeleteStreamProcessorRequest;
import com.amazonaws.services.rekognition.model.DeleteStreamProcessorResult;
import com.amazonaws.services.rekognition.model.DescribeStreamProcessorRequest;
import com.amazonaws.services.rekognition.model.DescribeStreamProcessorResult;
import com.amazonaws.services.rekognition.model.FaceSearchSettings;
import com.amazonaws.services.rekognition.model.KinesisDataStream;
import com.amazonaws.services.rekognition.model.KinesisVideoStream;
import com.amazonaws.services.rekognition.model.ListStreamProcessorsRequest;
import com.amazonaws.services.rekognition.model.ListStreamProcessorsResult;
import com.amazonaws.services.rekognition.model.ResourceNotFoundException;
import com.amazonaws.services.rekognition.model.StartStreamProcessorRequest;
import com.amazonaws.services.rekognition.model.StartStreamProcessorResult;
import com.amazonaws.services.rekognition.model.StopStreamProcessorRequest;
import com.amazonaws.services.rekognition.model.StopStreamProcessorResult;
import com.amazonaws.services.rekognition.model.StreamProcessor;
import com.amazonaws.services.rekognition.model.StreamProcessorInput;
import com.amazonaws.services.rekognition.model.StreamProcessorOutput;
import com.amazonaws.services.rekognition.model.StreamProcessorSettings;
import com.amazonaws.services.rekognition.model.StreamProcessorStatus;
import lombok.extern.slf4j.Slf4j;

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
    private AmazonRekognition rekognitionClient;

    private RekognitionStreamProcessor(final Regions regions, final AWSCredentialsProvider provider,
                                       final RekognitionInput rekognitionInput) {
        this.streamProcessorName = rekognitionInput.getStreamingProcessorName();
        this.kinesisVideoStreamArn = rekognitionInput.getKinesisVideoStreamArn();
        this.kinesisDataStreamArn = rekognitionInput.getKinesisDataStreamArn();
        this.roleArn = rekognitionInput.getIamRoleArn();
        this.collectionId = rekognitionInput.getFaceCollectionId();
        this.matchThreshold = rekognitionInput.getMatchThreshold();

        rekognitionClient = AmazonRekognitionClientBuilder
                .standard()
                .withRegion(regions)
                .withCredentials(provider)
                .build();
    }

    public static RekognitionStreamProcessor create(final Regions regions, final AWSCredentialsProvider provider,
                                                    final RekognitionInput rekognitionInput) {
        return new RekognitionStreamProcessor(regions, provider, rekognitionInput);
    }

    /**
     * Creates a StreamProcess if it doesn't exist already. Once the stream processor is created, it's started and then
     * described to know the result of the stream processor.
     */
    public void process() {
        // Creates a stream processor if it doesn't already exist and start.
        try {
            final DescribeStreamProcessorResult result = describeStreamProcessor();
            if (!result.getStatus().equals(StreamProcessorStatus.RUNNING.toString())) {
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

    public CreateStreamProcessorResult createStreamProcessor() {
        final KinesisVideoStream kinesisVideoStream = new KinesisVideoStream()
                .withArn(kinesisVideoStreamArn);
        final StreamProcessorInput streamProcessorInput = new StreamProcessorInput()
                .withKinesisVideoStream(kinesisVideoStream);
        final KinesisDataStream kinesisDataStream = new KinesisDataStream()
                .withArn(kinesisDataStreamArn);
        final StreamProcessorOutput streamProcessorOutput = new StreamProcessorOutput()
                .withKinesisDataStream(kinesisDataStream);
        final FaceSearchSettings faceSearchSettings = new FaceSearchSettings()
                .withCollectionId(collectionId)
                .withFaceMatchThreshold(matchThreshold);
        final StreamProcessorSettings streamProcessorSettings = new StreamProcessorSettings()
                .withFaceSearch(faceSearchSettings);

        final CreateStreamProcessorResult createStreamProcessorResult =
                rekognitionClient.createStreamProcessor(new CreateStreamProcessorRequest()
                        .withInput(streamProcessorInput)
                        .withOutput(streamProcessorOutput)
                        .withSettings(streamProcessorSettings)
                        .withRoleArn(roleArn)
                        .withName(streamProcessorName));
        log.info("StreamProcessorArn : {} ", createStreamProcessorResult.getStreamProcessorArn());
        return createStreamProcessorResult;
    }

    public StartStreamProcessorResult startStreamProcessor() {
        final StartStreamProcessorResult startStreamProcessorResult =
                rekognitionClient.startStreamProcessor(new StartStreamProcessorRequest().withName(streamProcessorName));
        log.info("SdkResponseMetadata : {} ", startStreamProcessorResult.getSdkResponseMetadata());
        return startStreamProcessorResult;
    }

    public StopStreamProcessorResult stopStreamProcessor() {
        final StopStreamProcessorResult stopStreamProcessorResult =
                rekognitionClient.stopStreamProcessor(new StopStreamProcessorRequest().withName(streamProcessorName));
        log.info("SdkResponseMetadata : {} ", stopStreamProcessorResult.getSdkResponseMetadata());
        return stopStreamProcessorResult;
    }

    public DeleteStreamProcessorResult deleteStreamProcessor() {
        final DeleteStreamProcessorResult deleteStreamProcessorResult = rekognitionClient
                .deleteStreamProcessor(new DeleteStreamProcessorRequest().withName(streamProcessorName));
        log.info("SdkResponseMetadata : {} ", deleteStreamProcessorResult.getSdkResponseMetadata());
        return deleteStreamProcessorResult;
    }

    public DescribeStreamProcessorResult describeStreamProcessor() {
        final DescribeStreamProcessorResult describeStreamProcessorResult = rekognitionClient
                .describeStreamProcessor(new DescribeStreamProcessorRequest().withName(streamProcessorName));
        log.info("Arn : {}", describeStreamProcessorResult.getStreamProcessorArn());
        log.info("Input kinesisVideo stream : {} ",
                describeStreamProcessorResult.getInput().getKinesisVideoStream().getArn());
        log.info("Output kinesisData stream {} ",
                describeStreamProcessorResult.getOutput().getKinesisDataStream().getArn());
        log.info("RoleArn {} ", describeStreamProcessorResult.getRoleArn());
        log.info(
                "CollectionId {} ", describeStreamProcessorResult.getSettings().getFaceSearch().getCollectionId());
        log.info("Status {} ", describeStreamProcessorResult.getStatus());
        log.info("Status message {} ", describeStreamProcessorResult.getStatusMessage());
        log.info("Creation timestamp {} ", describeStreamProcessorResult.getCreationTimestamp());
        log.info("Last update timestamp {} ", describeStreamProcessorResult.getLastUpdateTimestamp());
        return describeStreamProcessorResult;
    }

    public ListStreamProcessorsResult listStreamProcessor() {
        final ListStreamProcessorsResult listStreamProcessorsResult =
                rekognitionClient.listStreamProcessors(new ListStreamProcessorsRequest().withMaxResults(100));
        for (final StreamProcessor streamProcessor : listStreamProcessorsResult.getStreamProcessors()) {
            log.info("StreamProcessor name {} ", streamProcessor.getName());
            log.info("Status {} ", streamProcessor.getStatus());
        }
        return listStreamProcessorsResult;
    }

}
