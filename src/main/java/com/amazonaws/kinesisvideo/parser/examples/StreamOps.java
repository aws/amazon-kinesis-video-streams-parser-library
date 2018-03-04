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

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideo;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideoClientBuilder;
import com.amazonaws.services.kinesisvideo.model.*;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@Slf4j
public class StreamOps extends KinesisVideoCommon {
    private static final long SLEEP_PERIOD_MILLIS = TimeUnit.SECONDS.toMillis(3);
    private static final int DATA_RETENTION_IN_HOURS = 48;
    private final String streamName;
    final AmazonKinesisVideo amazonKinesisVideo;

    @Builder
    public StreamOps(Regions region,
                     String streamName, AWSCredentialsProvider credentialsProvider) {
        super(region, credentialsProvider, streamName);
        this.streamName = streamName;
        final AmazonKinesisVideoClientBuilder builder = AmazonKinesisVideoClientBuilder.standard();
        configureClient(builder);
        this.amazonKinesisVideo = builder.build();
    }

    /**
     * If the stream exists delete it and then recreate it.
     * Otherwise just create the stream.
     */
    public void recreateStreamIfNecessary() throws InterruptedException {

        deleteStreamIfPresent();

        //create the stream.
        amazonKinesisVideo.createStream(new CreateStreamRequest().withStreamName(streamName)
                .withDataRetentionInHours(DATA_RETENTION_IN_HOURS)
                .withMediaType("video/h264"));
        log.info("CreateStream called for stream {}", streamName);
        //wait for stream to become active.
        final Optional<StreamInfo> createdStreamInfo =
                waitForStateToMatch((s) -> s.isPresent() && "ACTIVE".equals(s.get().getStatus()));
        //some basic validations on the response of the create stream
        Validate.isTrue(createdStreamInfo.isPresent());
        Validate.isTrue(createdStreamInfo.get().getDataRetentionInHours() == DATA_RETENTION_IN_HOURS);
        log.info("StreamOps {} created ARN {}", streamName, createdStreamInfo.get().getStreamARN());
    }

    private void deleteStreamIfPresent() throws InterruptedException {
        final Optional<StreamInfo> streamInfo = getStreamInfo();
        log.info("StreamOps {} exists {}", streamName, streamInfo.isPresent());
        if (streamInfo.isPresent()) {
            //Delete the stream
            amazonKinesisVideo.deleteStream(new DeleteStreamRequest().withStreamARN(streamInfo.get().getStreamARN()));
            log.info("DeleteStream called for stream {} ARN {} ", streamName, streamInfo.get().getStreamARN());
            //Wait for stream to be deleted
            waitForStateToMatch((s) -> !s.isPresent());
            log.info("StreamOps {} deleted", streamName);
        }
    }

    private Optional<StreamInfo> waitForStateToMatch(Predicate<Optional<StreamInfo>> statePredicate)
            throws InterruptedException {
        Optional<StreamInfo> streamInfo;
        do {
            streamInfo = getStreamInfo();
            if (!statePredicate.test(streamInfo)) {
                Thread.sleep(SLEEP_PERIOD_MILLIS);
            }
        } while (!statePredicate.test(streamInfo));
        return streamInfo;
    }

    private Optional<StreamInfo> getStreamInfo() {
        try {
            return Optional.ofNullable(amazonKinesisVideo.describeStream(new DescribeStreamRequest().withStreamName(
                    streamName)).getStreamInfo());
        } catch (ResourceNotFoundException e) {
            return Optional.empty();
        }
    }
}
