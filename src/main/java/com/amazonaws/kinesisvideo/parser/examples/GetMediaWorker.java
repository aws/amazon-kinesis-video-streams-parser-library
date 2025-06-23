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

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesisvideo.KinesisVideoClient;

import software.amazon.awssdk.services.kinesisvideo.model.APIName;
import software.amazon.awssdk.services.kinesisvideo.model.GetDataEndpointRequest;
import software.amazon.awssdk.services.kinesisvideo.model.GetDataEndpointResponse;
import software.amazon.awssdk.services.kinesisvideomedia.KinesisVideoMediaClient;
import com.amazonaws.kinesisvideo.parser.ebml.InputStreamParserByteSource;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitException;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitor;
import com.amazonaws.kinesisvideo.parser.mkv.StreamingMkvReader;
import software.amazon.awssdk.services.kinesisvideomedia.model.GetMediaResponse;
import software.amazon.awssdk.services.kinesisvideomedia.model.StartSelector;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;

/**
 * Worker used to make a GetMedia call to Kinesis Video and stream in data and parse it and apply a visitor.
 */
@Slf4j
public class GetMediaWorker extends KinesisVideoCommon implements Runnable {
    private final KinesisVideoMediaClient kvsVideoMediaClient;
    private final MkvElementVisitor elementVisitor;
    private final StartSelector startSelector;

    private GetMediaWorker(Region region,
                           AwsCredentialsProvider credentialsProvider,
                           String streamName,
                           StartSelector startSelector,
                           String endPoint,
                           MkvElementVisitor elementVisitor) {
        super(region, credentialsProvider, streamName);

        KinesisVideoClient kvsVideoClient = KinesisVideoClient.builder()
                .region(Region.US_WEST_2)
                .endpointOverride(URI.create(endPoint))
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build();

        this.kvsVideoMediaClient = KinesisVideoMediaClient.builder()
                .endpointOverride(URI.create(endPoint))
                .region(kvsVideoClient.serviceClientConfiguration().region())
                .credentialsProvider(kvsVideoClient.serviceClientConfiguration().credentialsProvider())
                .build();

        this.elementVisitor = elementVisitor;
        this.startSelector = startSelector;
    }

    public static GetMediaWorker create(Region region,
                                        AwsCredentialsProvider credentialsProvider,
                                        String streamName,
                                        StartSelector startSelector,
                                        KinesisVideoClient kinesisVideoClient,
                                        MkvElementVisitor visitor) {

        GetDataEndpointResponse endpointResponse = kinesisVideoClient.getDataEndpoint(
                GetDataEndpointRequest.builder()
                        .streamName(streamName)
                        .apiName(APIName.GET_MEDIA)
                        .build());
        String endPoint = endpointResponse.dataEndpoint();

        return new GetMediaWorker(region, credentialsProvider, streamName, startSelector, endPoint, visitor);
    }

    @Override
    public void run() {
        try {
            log.info("Start GetMedia worker on stream {}", streamName);

            software.amazon.awssdk.services.kinesisvideomedia.model.GetMediaRequest getMediaRequest =
                    software.amazon.awssdk.services.kinesisvideomedia.model.GetMediaRequest.builder()
                            .streamName(streamName)
                            .startSelector(startSelector)
                            .build();

            ResponseInputStream<GetMediaResponse> mediaResponseResponseInputStream =
                    this.kvsVideoMediaClient.getMedia(getMediaRequest);

            log.info("GetMedia called on stream {} response {} requestId {}",
                    streamName,
                    mediaResponseResponseInputStream.response().sdkHttpResponse().statusCode(),
                    mediaResponseResponseInputStream.response().responseMetadata().requestId());

            StreamingMkvReader mkvStreamReader = StreamingMkvReader.createDefault(
                    new InputStreamParserByteSource(mediaResponseResponseInputStream));

            log.info("StreamingMkvReader created for stream {} ", streamName);

            try {
                mkvStreamReader.apply(this.elementVisitor);
            } catch (MkvElementVisitException e) {
                log.error("Exception while accepting visitor", e);
            }
        } catch (Throwable t) {
            log.error("Failure in GetMediaWorker for streamName {} {}", streamName, t.toString());
            throw t;
        } finally {
            log.info("Exiting GetMediaWorker for stream {}", streamName);
        }
    }
}
