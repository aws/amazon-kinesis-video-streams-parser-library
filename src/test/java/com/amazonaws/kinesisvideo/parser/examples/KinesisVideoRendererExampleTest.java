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
import com.amazonaws.regions.Regions;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

public class KinesisVideoRendererExampleTest {

    @Ignore
    @Test
    public void testExample() throws InterruptedException, IOException {
        KinesisVideoRendererExample example = KinesisVideoRendererExample.builder().region(Regions.US_WEST_2)
                .streamName("getmedia-sample-stream")
                .credentialsProvider(new ProfileCredentialsProvider())
                .build();

        example.execute();
    }

}


