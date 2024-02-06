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

import com.amazonaws.services.kinesisvideo.AmazonKinesisVideoPutMediaClientBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesisvideo.KinesisVideoAsyncClientBuilder;

/**
 * Abstract class for all example classes that use the Kinesis Video clients.
 */
@RequiredArgsConstructor
@Getter
public abstract class KinesisVideoCommon {
    private final Region region;
    private final AwsCredentialsProvider credentialsProvider;
    protected final String streamName;

    protected void configureClient(KinesisVideoAsyncClientBuilder clientBuilder) {
        clientBuilder.credentialsProvider(credentialsProvider).region(region);
    }

    protected void conifgurePutMediaClient(AmazonKinesisVideoPutMediaClientBuilder builder) {
        builder.withCredentials(credentialsProvider).withRegion(region);
    }

}
