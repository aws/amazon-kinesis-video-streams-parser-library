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
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitException;
import com.amazonaws.kinesisvideo.parser.utilities.FragmentMetadata;
import com.amazonaws.kinesisvideo.parser.mkv.FrameProcessException;
import com.amazonaws.kinesisvideo.parser.utilities.consumer.GetMediaResponseStreamConsumer;
import com.amazonaws.kinesisvideo.parser.utilities.consumer.GetMediaResponseStreamConsumerFactory;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideo;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideoMedia;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideoMediaClientBuilder;
import com.amazonaws.services.kinesisvideo.model.APIName;
import com.amazonaws.services.kinesisvideo.model.GetDataEndpointRequest;
import com.amazonaws.services.kinesisvideo.model.GetMediaRequest;
import com.amazonaws.services.kinesisvideo.model.GetMediaResult;
import com.amazonaws.services.kinesisvideo.model.StartSelector;
import com.amazonaws.services.kinesisvideo.model.StartSelectorType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Worker used to make a GetMedia call to Kinesis Video and stream in data and parse it and apply a visitor.
 */
@Slf4j
public class ContinuousGetMediaWorker extends KinesisVideoCommon implements Runnable {
    private static final int HTTP_STATUS_OK = 200;
    private final AmazonKinesisVideoMedia videoMedia;
    private final GetMediaResponseStreamConsumerFactory consumerFactory;
    private final StartSelector startSelector;
    private Optional<String> fragmentNumberToStartAfter = Optional.empty();
    private volatile AtomicBoolean shouldStop = new AtomicBoolean(false);

    private ContinuousGetMediaWorker(Regions region,
            AWSCredentialsProvider credentialsProvider,
            String streamName,
            StartSelector startSelector,
            String endPoint,
            GetMediaResponseStreamConsumerFactory consumerFactory) {
        super(region, credentialsProvider, streamName);

        AmazonKinesisVideoMediaClientBuilder builder = AmazonKinesisVideoMediaClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endPoint, region.getName()))
                .withCredentials(getCredentialsProvider());

        this.videoMedia = builder.build();
        this.consumerFactory = consumerFactory;
        this.startSelector = startSelector;
    }

    public static ContinuousGetMediaWorker create(Regions region,
            AWSCredentialsProvider credentialsProvider,
            String streamName,
            StartSelector startSelector,
            AmazonKinesisVideo amazonKinesisVideo,
            GetMediaResponseStreamConsumerFactory consumer) {
        String endPoint = amazonKinesisVideo.getDataEndpoint(new GetDataEndpointRequest().withAPIName(APIName.GET_MEDIA)
                .withStreamName(streamName)).getDataEndpoint();

        return new ContinuousGetMediaWorker(region, credentialsProvider, streamName, startSelector, endPoint, consumer);
    }

    public void stop() {
        log.info("Stop ContinuousGetMediaWorker");
        shouldStop.set(true);
    }

    @Override
    public void run() {
        log.info("Start ContinuousGetMedia worker for stream {}", streamName);
        while (!shouldStop.get()) {
            GetMediaResult getMediaResult = null;
            try {

                StartSelector selectorToUse = fragmentNumberToStartAfter.map(fn -> new StartSelector().withStartSelectorType(StartSelectorType.FRAGMENT_NUMBER)
                        .withAfterFragmentNumber(fn)).orElse(startSelector);

                getMediaResult = videoMedia.getMedia(new GetMediaRequest().withStreamName(streamName).withStartSelector(selectorToUse));
                log.info("Start processing GetMedia called for stream {} response {} requestId {}",
                        streamName,
                        getMediaResult.getSdkHttpMetadata().getHttpStatusCode(),
                        getMediaResult.getSdkResponseMetadata().getRequestId());

                if (getMediaResult.getSdkHttpMetadata().getHttpStatusCode() == HTTP_STATUS_OK) {
                    try (GetMediaResponseStreamConsumer consumer = consumerFactory.createConsumer()) {
                        consumer.process(getMediaResult.getPayload(), this::updateFragmentNumberToStartAfter);
                    }
                } else {
                    Thread.sleep(200);
                }
            } catch (FrameProcessException e) {
                log.error("FrameProcessException in ContinuousGetMedia worker for stream: " + streamName, e);
                break;
            } catch (IOException | MkvElementVisitException e) {
                log.error("Failure in ContinuousGetMedia worker for stream: " + streamName, e);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(ie);
            } catch (Throwable t) {
                log.error("Throwable",t);
            } finally {
                closeGetMediaResponse(getMediaResult);
                log.info("Exit processing GetMedia called for stream {}", streamName);
            }
        }
        log.info("Exit ContinuousGetMedia worker for stream {}", streamName);
    }

    private void closeGetMediaResponse(final GetMediaResult getMediaResult) {
        if (getMediaResult != null) {
            final InputStream payload = getMediaResult.getPayload();
            if (payload != null) {
                try {
                    payload.close();
                } catch (final IOException e) {
                    // Ignore close exception;
                }
            }
        }
    }

    private void updateFragmentNumberToStartAfter(FragmentMetadata f) {
        Validate.isTrue(!fragmentNumberToStartAfter.isPresent()
                || f.getFragmentNumberString().compareTo(fragmentNumberToStartAfter.get()) > 0);
        fragmentNumberToStartAfter = Optional.of(f.getFragmentNumberString());
    }
}
