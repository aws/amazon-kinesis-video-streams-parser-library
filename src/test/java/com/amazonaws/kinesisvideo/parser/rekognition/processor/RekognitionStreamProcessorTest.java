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

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.kinesisvideo.parser.rekognition.pojo.RekognitionInput;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.model.CreateStreamProcessorResult;
import com.amazonaws.services.rekognition.model.DescribeStreamProcessorResult;
import com.amazonaws.services.rekognition.model.ListStreamProcessorsResult;
import com.amazonaws.services.rekognition.model.StartStreamProcessorResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Slf4j
@Ignore // Used for controlling rekognition stream processor used in Rekognition integration examples.
public class RekognitionStreamProcessorTest {

    RekognitionStreamProcessor streamProcessor;

    @Before
    public void testSetup() {
        final RekognitionInput rekognitionInput = RekognitionInput.builder()
                .kinesisVideoStreamArn("<kvs-stream-arn>")
                .kinesisDataStreamArn("<kds-stream-arn>")
                .streamingProcessorName("<stream-processor-name>")
                // Refer how to add face collection :
                // https://docs.aws.amazon.com/rekognition/latest/dg/add-faces-to-collection-procedure.html
                .faceCollectionId("<face-collection-id>")
                .iamRoleArn("<iam-role>")
                .matchThreshold(0.08f)
                .build();
        streamProcessor = RekognitionStreamProcessor.create(Regions.US_WEST_2, new ProfileCredentialsProvider(), rekognitionInput);
    }

    @Test
    public void createStreamProcessor() {
        final CreateStreamProcessorResult result = streamProcessor.createStreamProcessor();
        Assert.assertNotNull(result.getStreamProcessorArn());
    }

    @Test
    public void startStreamProcessor() {
        final StartStreamProcessorResult result = streamProcessor.startStreamProcessor();
        log.info("Result : {}", result);
    }

    @Test
    public void describeStreamProcessor() {
        final DescribeStreamProcessorResult result = streamProcessor.describeStreamProcessor();
        log.info("Status for stream processor : {}", result.getStatus());
    }

    @Test
    public void listStreamProcessor() {
        final ListStreamProcessorsResult result = streamProcessor.listStreamProcessor();
        log.info("List StreamProcessors : {}", result);
    }

}
