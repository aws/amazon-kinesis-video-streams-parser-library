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
import com.amazonaws.kinesisvideo.parser.ebml.InputStreamParserByteSource;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElement;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitException;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitor;
import com.amazonaws.kinesisvideo.parser.mkv.StreamingMkvReader;
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

import java.util.Optional;

/**
 * Worker used to make a GetMedia call to Kinesis Video and stream in data and parse it and apply a visitor.
 */
@Slf4j
public class GetMediaWorker extends KinesisVideoCommon implements Runnable {
    private final AmazonKinesisVideoMedia videoMedia;
    private final MkvElementVisitor elementVisitor;
    private final StartSelector startSelector;

    private GetMediaWorker(Regions region,
            AWSCredentialsProvider credentialsProvider,
            String streamName,
            StartSelector startSelector,
            String endPoint,
            MkvElementVisitor elementVisitor) {
        super(region, credentialsProvider, streamName);

        AmazonKinesisVideoMediaClientBuilder builder = AmazonKinesisVideoMediaClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endPoint, region.getName()))
                .withCredentials(getCredentialsProvider());

        this.videoMedia = builder.build();
        this.elementVisitor = elementVisitor;
        this.startSelector = startSelector;
    }

    public static GetMediaWorker create(Regions region,
            AWSCredentialsProvider credentialsProvider,
            String streamName,
            StartSelector startSelector,
            AmazonKinesisVideo amazonKinesisVideo,
            MkvElementVisitor visitor) {
        String endPoint = amazonKinesisVideo.getDataEndpoint(new GetDataEndpointRequest().withAPIName(APIName.GET_MEDIA)
                .withStreamName(streamName)).getDataEndpoint();

        return new GetMediaWorker(region, credentialsProvider, streamName, startSelector, endPoint, visitor);
    }

    @Override
    public void run() {
        try {
            log.info("Start GetMedia worker on stream {}", streamName);

                GetMediaResult result = videoMedia.getMedia(new GetMediaRequest().withStreamName(streamName).withStartSelector(startSelector));
            log.info("GetMedia called on stream {} response {} requestId {}",
                    streamName,
                    result.getSdkHttpMetadata().getHttpStatusCode(),
                    result.getSdkResponseMetadata().getRequestId());

            StreamingMkvReader mkvStreamReader = StreamingMkvReader.createDefault(new InputStreamParserByteSource(result.getPayload()));
            log.info("StreamingMkvReader created for stream {} ", streamName);
            try {
                mkvStreamReader.apply(this.elementVisitor);
            } catch (MkvElementVisitException e) {
                log.error("Exception while accepting visitor {}", e);
            }
        } catch (Throwable t) {
            log.error("Failure in GetMediaWorker for streamName {} {}", streamName, t.toString());
            throw t;
        } finally {
            log.info("Exiting GetMediaWorker for stream {}", streamName);
        }
    }
}
