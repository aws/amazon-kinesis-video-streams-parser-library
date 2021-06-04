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
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitException;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitor;
import com.amazonaws.kinesisvideo.parser.mkv.StreamingMkvReader;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideo;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideoArchivedMedia;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideoArchivedMediaClient;
import com.amazonaws.services.kinesisvideo.model.APIName;
import com.amazonaws.services.kinesisvideo.model.GetDataEndpointRequest;
import com.amazonaws.services.kinesisvideo.model.GetMediaForFragmentListRequest;
import com.amazonaws.services.kinesisvideo.model.GetMediaForFragmentListResult;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
public class GetMediaForFragmentListWorker extends KinesisVideoCommon implements Runnable {
    private final AmazonKinesisVideoArchivedMedia amazonKinesisVideoArchivedMedia;
    private final MkvElementVisitor elementVisitor;
    private final List<String> fragmentNumbers;

    public GetMediaForFragmentListWorker(final String streamName, final List<String> fragmentNumbers,
                                         final AWSCredentialsProvider awsCredentialsProvider, final String endPoint,
                                         final Regions region, final MkvElementVisitor elementVisitor) {
        super(region, awsCredentialsProvider, streamName);
        this.fragmentNumbers = fragmentNumbers;
        this.elementVisitor = elementVisitor;
        amazonKinesisVideoArchivedMedia = AmazonKinesisVideoArchivedMediaClient
                .builder()
                .withCredentials(awsCredentialsProvider)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endPoint, region.getName()))
                .build();
    }


    public static GetMediaForFragmentListWorker create(final String streamName, final List<String> fragmentNumbers,
                                                       final AWSCredentialsProvider awsCredentialsProvider,
                                                       final Regions region,
                                                       final AmazonKinesisVideo amazonKinesisVideo,
                                                       final MkvElementVisitor elementVisitor) {
        final GetDataEndpointRequest request = new GetDataEndpointRequest()
                .withAPIName(APIName.GET_MEDIA_FOR_FRAGMENT_LIST).withStreamName(streamName);
        final String endpoint = amazonKinesisVideo.getDataEndpoint(request).getDataEndpoint();
        return new GetMediaForFragmentListWorker(
                streamName, fragmentNumbers, awsCredentialsProvider, endpoint, region, elementVisitor);
    }

    @Override
    public void run() {
        try {
            log.info("Start GetMediaForFragmentList worker on stream {}", streamName);
            final GetMediaForFragmentListResult result = amazonKinesisVideoArchivedMedia.getMediaForFragmentList(
                    new GetMediaForFragmentListRequest()
                            .withFragments(fragmentNumbers)
                            .withStreamName(streamName));

            log.info("GetMediaForFragmentList called on stream {} response {} requestId {}",
                    streamName,
                    result.getSdkHttpMetadata().getHttpStatusCode(),
                    result.getSdkResponseMetadata().getRequestId());
            final StreamingMkvReader mkvStreamReader = StreamingMkvReader.createDefault(
                    new InputStreamParserByteSource(result.getPayload()));
            log.info("StreamingMkvReader created for stream {} ", streamName);
            try {
                mkvStreamReader.apply(this.elementVisitor);
            } catch (final MkvElementVisitException e) {
                log.error("Exception while accepting visitor {}", e);
            }
        } catch (final Throwable t) {
            log.error("Failure in GetMediaForFragmentListWorker for streamName {} {}", streamName, t);
            throw t;
        } finally {
            log.info("Exiting GetMediaWorker for stream {}", streamName);
        }
    }
}
