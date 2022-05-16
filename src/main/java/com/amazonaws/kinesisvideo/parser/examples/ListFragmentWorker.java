package com.amazonaws.kinesisvideo.parser.examples;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideo;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideoArchivedMedia;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideoArchivedMediaClient;
import com.amazonaws.services.kinesisvideo.model.*;
import lombok.extern.slf4j.Slf4j;

/* This worker retrieves all fragments within the specified TimestampRange from a specified Kinesis Video Stream and returns them in a list */

@Slf4j
public class ListFragmentWorker extends KinesisVideoCommon implements Callable {
    private final FragmentSelector fragmentSelector;
    private final AmazonKinesisVideoArchivedMedia amazonKinesisVideoArchivedMedia;
    private final long fragmentsPerRequest = 100;

    public ListFragmentWorker(final String streamName,
                              final AWSCredentialsProvider awsCredentialsProvider, final String endPoint,
                              final Regions region,
                              final FragmentSelector fragmentSelector) {
        super(region, awsCredentialsProvider, streamName);
        this.fragmentSelector = fragmentSelector;

        amazonKinesisVideoArchivedMedia = AmazonKinesisVideoArchivedMediaClient
                .builder()
                .withCredentials(awsCredentialsProvider)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endPoint, region.getName()))
                .build();
    }

    public static ListFragmentWorker create(final String streamName,
                                            final AWSCredentialsProvider awsCredentialsProvider,
                                            final Regions region,
                                            final AmazonKinesisVideo amazonKinesisVideo,
                                            final FragmentSelector fragmentSelector) {
        final GetDataEndpointRequest request = new GetDataEndpointRequest()
                .withAPIName(APIName.LIST_FRAGMENTS).withStreamName(streamName);
        final String endpoint = amazonKinesisVideo.getDataEndpoint(request).getDataEndpoint();

        return new ListFragmentWorker(
                streamName, awsCredentialsProvider, endpoint, region, fragmentSelector);
    }

    @Override
    public List<String> call() {
        List<String> fragmentNumbers = new ArrayList<>();
        try {
            log.info("Start ListFragment worker on stream {}", streamName);

            ListFragmentsRequest request = new ListFragmentsRequest()
                    .withStreamName(streamName).withFragmentSelector(fragmentSelector).withMaxResults(fragmentsPerRequest);

            ListFragmentsResult result = amazonKinesisVideoArchivedMedia.listFragments(request);


            log.info("List Fragments called on stream {} response {} request ID {}",
                    streamName,
                    result.getSdkHttpMetadata().getHttpStatusCode(),
                    result.getSdkResponseMetadata().getRequestId());

            for (Fragment f: result.getFragments()) {
                fragmentNumbers.add(f.getFragmentNumber());
            }
            String nextToken = result.getNextToken();

            /* If result is truncated, keep making requests until nextToken is empty */
            while (nextToken != null) {
                request = new ListFragmentsRequest()
                        .withStreamName(streamName).withNextToken(nextToken);
                result = amazonKinesisVideoArchivedMedia.listFragments(request);

                for (Fragment f: result.getFragments()) {
                    fragmentNumbers.add(f.getFragmentNumber());
                }
                nextToken = result.getNextToken();
            }
            Collections.sort(fragmentNumbers);

            for (String f: fragmentNumbers) {
                log.info("Retrieved fragment number {} ", f);
            }
        }
        catch (Throwable t) {
            log.error("Failure in ListFragmentWorker for streamName {} {}", streamName, t.toString());
            throw t;
        } finally {
            log.info("Retrieved {} Fragments and exiting ListFragmentWorker for stream {}", fragmentNumbers.size(), streamName);
            return fragmentNumbers;
        }
    }
}
