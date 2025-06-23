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

import com.amazonaws.kinesisvideo.parser.mkv.FrameProcessException;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitException;
import com.amazonaws.kinesisvideo.parser.utilities.FragmentMetadata;
import com.amazonaws.kinesisvideo.parser.utilities.consumer.GetMediaResponseStreamConsumer;
import com.amazonaws.kinesisvideo.parser.utilities.consumer.GetMediaResponseStreamConsumerFactory;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import software.amazon.awssdk.services.kinesisvideo.KinesisVideoClient;
import software.amazon.awssdk.services.kinesisvideo.model.APIName;
import software.amazon.awssdk.services.kinesisvideo.model.GetDataEndpointRequest;
import software.amazon.awssdk.services.kinesisvideo.model.GetDataEndpointResponse;
import software.amazon.awssdk.services.kinesisvideomedia.KinesisVideoMediaClient;
import software.amazon.awssdk.services.kinesisvideomedia.model.GetMediaRequest;
import software.amazon.awssdk.services.kinesisvideomedia.model.GetMediaResponse;
import software.amazon.awssdk.services.kinesisvideomedia.model.StartSelector;
import software.amazon.awssdk.services.kinesisvideomedia.model.StartSelectorType;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Worker used to make a GetMedia call to Kinesis Video and stream in data and parse it and apply a visitor.
 */
@Slf4j
public class ContinuousGetMediaWorker extends KinesisVideoCommon implements Runnable {
    private static final int HTTP_STATUS_OK = 200;
    private final KinesisVideoMediaClient kinesisVideoMediaClient;
    private final GetMediaResponseStreamConsumerFactory consumerFactory;
    private final StartSelector startSelector;
    private Optional<String> fragmentNumberToStartAfter = Optional.empty();
    private AtomicBoolean shouldStop = new AtomicBoolean(false);

    private ContinuousGetMediaWorker(Region region,
                                     AwsCredentialsProvider credentialsProvider,
                                     String streamName,
                                     StartSelector startSelector,
                                     String endPoint,
                                     GetMediaResponseStreamConsumerFactory consumerFactory) {
        super(region, credentialsProvider, streamName);

        this.kinesisVideoMediaClient = KinesisVideoMediaClient.builder()
                .region(Region.US_WEST_2)
                .endpointOverride(URI.create(endPoint))
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build();

        this.consumerFactory = consumerFactory;
        this.startSelector = startSelector;
    }

    public static ContinuousGetMediaWorker create(Region region,
                                                  AwsCredentialsProvider credentialsProvider,
                                                  String streamName,
                                                  StartSelector startSelector,
                                                  KinesisVideoClient kinesisVideoClient,
                                                  GetMediaResponseStreamConsumerFactory consumer) {

        GetDataEndpointResponse endpointResponse = kinesisVideoClient.getDataEndpoint(
                GetDataEndpointRequest.builder()
                        .streamName(streamName)
                        .apiName(APIName.GET_MEDIA)
                        .build());
        String endPoint = endpointResponse.dataEndpoint();


        return new ContinuousGetMediaWorker(region, credentialsProvider, streamName, startSelector, endPoint, consumer);
    }

    public void stop() {
        log.info("Stop ContinuousGetMediaWorker");
        shouldStop.set(true);
    }

    @Override
    public void run() {
        ResponseInputStream<GetMediaResponse> mediaResponseResponseInputStream = null;
        log.info("Start ContinuousGetMedia worker for stream {}", streamName);
        while (!shouldStop.get()) {

            log.info("StreamingMkvReader created for stream {} ", streamName);

            try {
                StartSelector selectorToUse = fragmentNumberToStartAfter.map(fn -> StartSelector.builder().startSelectorType(StartSelectorType.FRAGMENT_NUMBER)
                        .afterFragmentNumber(fn)).orElse(startSelector.toBuilder()).build();

                GetMediaRequest getMediaRequest =
                        software.amazon.awssdk.services.kinesisvideomedia.model.GetMediaRequest.builder()
                                .streamName(streamName)
                                .startSelector(selectorToUse)
                                .build();

                mediaResponseResponseInputStream =
                        this.kinesisVideoMediaClient.getMedia(getMediaRequest);
                log.info("Start processing GetMedia called for stream {} response {} requestId {}",
                        streamName,
                        mediaResponseResponseInputStream.response().sdkHttpResponse().statusCode(),
                        mediaResponseResponseInputStream.response().responseMetadata().requestId());

                if (mediaResponseResponseInputStream.response().sdkHttpResponse().statusCode() == HTTP_STATUS_OK) {
                    try (GetMediaResponseStreamConsumer consumer = consumerFactory.createConsumer()) {
                        consumer.process(mediaResponseResponseInputStream, this::updateFragmentNumberToStartAfter);
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
                log.error("Throwable", t);
            } finally {
                closeGetMediaResponse(mediaResponseResponseInputStream);
                log.info("Exit processing GetMedia called for stream {}", streamName);
            }
        }
        log.info("Exit ContinuousGetMedia worker for stream {}", streamName);
    }

    private void closeGetMediaResponse(final ResponseInputStream<GetMediaResponse> mediaResponseResponseInputStream) {
        if (mediaResponseResponseInputStream != null) {
            try {
                mediaResponseResponseInputStream.close();
            } catch (final IOException e) {
                // Ignore close exception;
            }
        }
    }

    private void updateFragmentNumberToStartAfter(FragmentMetadata f) {
        Validate.isTrue(!fragmentNumberToStartAfter.isPresent()
                || f.getFragmentNumberString().compareTo(fragmentNumberToStartAfter.get()) > 0);
        fragmentNumberToStartAfter = Optional.of(f.getFragmentNumberString());
    }
}
