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
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideoPutMedia;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideoPutMediaClient;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideoPutMediaClientBuilder;
import com.amazonaws.services.kinesisvideo.PutMediaAckResponseHandler;
import com.amazonaws.services.kinesisvideo.model.APIName;
import com.amazonaws.services.kinesisvideo.model.AckEvent;
import com.amazonaws.services.kinesisvideo.model.AckEventType;
import com.amazonaws.services.kinesisvideo.model.FragmentTimecodeType;
import com.amazonaws.services.kinesisvideo.model.GetDataEndpointRequest;
import com.amazonaws.services.kinesisvideo.model.PutMediaRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

/**
 * Worker used to make a PutMedia call to Kinesis Video for a stream and stream in some video.
 */
@Slf4j
public class PutMediaWorker extends KinesisVideoCommon implements Runnable {
    private final InputStream inputStream;
    private final AmazonKinesisVideoPutMedia putMedia;
    @Getter
    private long numFragmentsPersisted = 0;

    private PutMediaWorker(Regions region,
            AWSCredentialsProvider credentialsProvider,
            String streamName,
            InputStream inputStream,
            String endPoint) {
        super(region, credentialsProvider, streamName);
        this.inputStream = inputStream;

        AmazonKinesisVideoPutMediaClientBuilder builder =
                AmazonKinesisVideoPutMediaClient.builder().withEndpoint(endPoint);
        conifgurePutMediaClient(builder);
        this.putMedia = builder.build();
    }

    public static PutMediaWorker create(Regions region,
            AWSCredentialsProvider credentialsProvider,
            String streamName,
            InputStream inputStream,
            AmazonKinesisVideo amazonKinesisVideo) {
        String endPoint = amazonKinesisVideo.getDataEndpoint(new GetDataEndpointRequest().withAPIName(APIName.PUT_MEDIA)
                .withStreamName(streamName)).getDataEndpoint();

        return new PutMediaWorker(region, credentialsProvider, streamName, inputStream, endPoint);
    }

    @Override
    public void run() {
        CountDownLatch latch = new CountDownLatch(1);
        putMedia.putMedia(new PutMediaRequest().withStreamName(streamName)
                .withFragmentTimecodeType(FragmentTimecodeType.RELATIVE)
                .withProducerStartTimestamp(new Date())
                .withPayload(inputStream), new PutMediaAckResponseHandler() {
            @Override
            public void onAckEvent(AckEvent event) {
                log.info("PutMedia Ack for stream {}: {} ", streamName, event.toString());
                if (AckEventType.Values.PERSISTED.equals(event.getAckEventType().getEnumValue())) {
                    numFragmentsPersisted++;
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("PutMedia for {} has suffered error {}", streamName, t);
                latch.countDown();
            }

            @Override
            public void onComplete() {
                log.info("PutMedia for {} is complete ", streamName);
                latch.countDown();
            }
        });
        log.info("Made PutMedia call for stream {}", streamName);
        try {
            latch.await();
            log.info("PutMedia worker exiting for stream {} number of fragments persisted {} ",
                    streamName,
                    numFragmentsPersisted);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failure while waiting for PutMedia to finish", e);
        } finally {
            putMedia.close();
        }
    }

}
