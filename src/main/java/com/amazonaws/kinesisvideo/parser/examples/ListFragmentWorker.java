package com.amazonaws.kinesisvideo.parser.examples;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesisvideo.KinesisVideoAsyncClient;
import software.amazon.awssdk.services.kinesisvideo.model.APIName;
import software.amazon.awssdk.services.kinesisvideo.model.GetDataEndpointRequest;
import software.amazon.awssdk.services.kinesisvideoarchivedmedia.KinesisVideoArchivedMediaClient;
import software.amazon.awssdk.services.kinesisvideoarchivedmedia.model.Fragment;
import software.amazon.awssdk.services.kinesisvideoarchivedmedia.model.FragmentSelector;
import software.amazon.awssdk.services.kinesisvideoarchivedmedia.model.ListFragmentsRequest;
import software.amazon.awssdk.services.kinesisvideoarchivedmedia.model.ListFragmentsResponse;

/* This worker retrieves all fragments within the specified TimestampRange from a specified Kinesis Video Stream and returns them in a list */

@Slf4j
public class ListFragmentWorker extends KinesisVideoCommon implements Callable {
    private final FragmentSelector fragmentSelector;
    private final KinesisVideoArchivedMediaClient archivedMediaClient;
    private final long fragmentsPerRequest = 100;

    public ListFragmentWorker(final String streamName,
                              final AwsCredentialsProvider awsCredentialsProvider, final String endPoint,
                              final Region region,
                              final FragmentSelector fragmentSelector) {
        super(region, awsCredentialsProvider, streamName);
        this.fragmentSelector = fragmentSelector;

        archivedMediaClient = KinesisVideoArchivedMediaClient
                .builder()
                .credentialsProvider(awsCredentialsProvider)
                .region(region)
                .endpointOverride(URI.create(endPoint))
                .build();
    }

    public static ListFragmentWorker create(final String streamName,
                                            final AwsCredentialsProvider awsCredentialsProvider,
                                            final Region region,
                                            final KinesisVideoAsyncClient kinesisVideoAsyncClient,
                                            final FragmentSelector fragmentSelector) {
        final GetDataEndpointRequest getDataEndpointRequest = GetDataEndpointRequest.builder()
                .apiName(APIName.LIST_FRAGMENTS).build();

        final String endpoint;
        try {
            endpoint = kinesisVideoAsyncClient.getDataEndpoint(getDataEndpointRequest).get().dataEndpoint();
        } catch (final InterruptedException | ExecutionException e) {
            log.error("Failed to get data endpoint for streamName {}", streamName);
            throw new RuntimeException(e);
        }

        return new ListFragmentWorker(
                streamName, awsCredentialsProvider, endpoint, region, fragmentSelector);
    }

    @Override
    public List<String> call() {
        List<String> fragmentNumbers = new ArrayList<>();
        try {
            log.info("Start ListFragment worker on stream {}", streamName);

            ListFragmentsRequest listFragmentsRequest = ListFragmentsRequest.builder()
                    .streamName(streamName)
                    .fragmentSelector(fragmentSelector)
                    .maxResults(fragmentsPerRequest)
                    .build();

            ListFragmentsResponse listFragmentsResponse = archivedMediaClient.listFragments(listFragmentsRequest);


            log.info("List Fragments called on stream {} response {} request ID {}",
                    streamName,
                    listFragmentsResponse.sdkHttpResponse().statusCode() ,
                    listFragmentsResponse.responseMetadata().requestId());

            for (Fragment f: listFragmentsResponse.fragments()) {
                fragmentNumbers.add(f.fragmentNumber());
            }
            String nextToken = listFragmentsResponse.nextToken();

            /* If result is truncated, keep making requests until nextToken is empty */
            while (nextToken != null) {
                listFragmentsRequest = ListFragmentsRequest.builder()
                        .streamName(streamName).nextToken(nextToken).build();

                listFragmentsResponse = archivedMediaClient.listFragments(listFragmentsRequest);

                for (Fragment f: listFragmentsResponse.fragments()) {
                    fragmentNumbers.add(f.fragmentNumber());
                }
                nextToken = listFragmentsResponse.nextToken();
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
