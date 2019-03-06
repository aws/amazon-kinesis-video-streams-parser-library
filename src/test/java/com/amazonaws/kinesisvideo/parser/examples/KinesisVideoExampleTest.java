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
package com.amazonaws.kinesisvideo.parser.examples;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.kinesisvideo.parser.TestResourceUtil;
import com.amazonaws.regions.Regions;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

/**
 * Test to execute Kinesis Video Example.
 * The test in this class are currently ignored for unit tests since they require access to Kinesis Video through
 * valid credentials.
 * These can be re-enabled as integration tests.
 * They can be executed on any machine with valid aws credentials for access to Kinesis Video.
 */
public class KinesisVideoExampleTest {
    @Ignore
    @Test
    public void testExample() throws InterruptedException, IOException {
        KinesisVideoExample example = KinesisVideoExample.builder().region(Regions.US_WEST_2)
                .streamName("myTestStream")
                .credentialsProvider(new ProfileCredentialsProvider())
                .inputVideoStream(TestResourceUtil.getTestInputStream("vogels_480.mkv"))
                .build();

        example.execute();
        Assert.assertEquals(9, example.getFragmentsPersisted());
        Assert.assertEquals(9, example.getFragmentsRead());
    }

    @Ignore
    @Test
    public void testConsumerExample() throws InterruptedException, IOException {
        KinesisVideoExample example = KinesisVideoExample.builder().region(Regions.US_WEST_2)
                .streamName("myTestStream")
                .credentialsProvider(new ProfileCredentialsProvider())
                // Use existing stream in KVS (with Producer sending)
                .noSampleInputRequired(true)
                .build();

        example.execute();
    }

}
