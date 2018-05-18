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
import com.amazonaws.kinesisvideo.parser.utilities.consumer.MergedOutputPiperFactory;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideo;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideoClientBuilder;
import com.amazonaws.services.kinesisvideo.model.StartSelector;
import com.amazonaws.services.kinesisvideo.model.StartSelectorType;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Example for continuously piping the output of GetMedia calls from a Kinesis Video stream to GStreamer.
 */
@Slf4j
public class KinesisVideoGStreamerPiperExample extends KinesisVideoCommon {
    private static final String DEFAULT_PATH_TO_GSTREAMER = "/usr/bin/gst-launch-1.0";
    private static final String [] FDSRC_ARGS = new String[] {
            "-v",
            "fdsrc",
            "!"
    };

    private final AmazonKinesisVideo amazonKinesisVideo;
    private final InputStream inputStream;
    private final ExecutorService executorService;
    private PutMediaWorker putMediaWorker;
    private final StreamOps streamOps;
    //The arguments to construct the gstreamer pipeline.
    //The merged output of GetMedia will be piped to the gstreamer pipeline created using these arguments.
    private final List<String> gStreamerPipelineArguments;


    @Builder
    private KinesisVideoGStreamerPiperExample(Regions region,
            String streamName,
            AWSCredentialsProvider credentialsProvider,
            InputStream inputVideoStream,
            String gStreamerPipelineArgument) {
        super(region, credentialsProvider, streamName);
        final AmazonKinesisVideoClientBuilder builder = AmazonKinesisVideoClientBuilder.standard();
        configureClient(builder);
        this.amazonKinesisVideo = builder.build();
        this.inputStream = inputVideoStream;
        this.streamOps = new StreamOps(region,  streamName, credentialsProvider);
        this.executorService = Executors.newFixedThreadPool(2);
        this.gStreamerPipelineArguments = new ArrayList<>();
        addGStreamerPipelineArguments(gStreamerPipelineArgument);
    }

    private void addGStreamerPipelineArguments(String gStreamerPipeLineArgument) {
        this.gStreamerPipelineArguments.add(pathToExecutable("PATH_TO_GSTREAMER", DEFAULT_PATH_TO_GSTREAMER));
        addToPipelineArguments(FDSRC_ARGS);
        addToPipelineArguments(gStreamerPipeLineArgument.split("\\s+"));
    }

    private String pathToExecutable(String environmentVariable, String defaultPath) {
        final String environmentVariableValue = System.getenv(environmentVariable);
        return StringUtils.isEmpty(environmentVariableValue) ? defaultPath : environmentVariableValue;
    }

    private void addToPipelineArguments(String []pipelineArguments) {
        for (String pipelineArgument : pipelineArguments) {
            this.gStreamerPipelineArguments.add(pipelineArgument);
        }
    }

    /**
     * This method executes the example.
     *
     * @throws InterruptedException the thread is interrupted while waiting for the stream to enter the correct state.
     * @throws IOException fails to read video from the input stream or write to the output stream.
     */
    public void execute () throws InterruptedException, IOException {
        //Create the Kinesis Video stream, deleting and recreating if necessary.
        streamOps.recreateStreamIfNecessary();

        ContinuousGetMediaWorker getWorker = ContinuousGetMediaWorker.create(getRegion(),
                getCredentialsProvider(),
                getStreamName(),
                new StartSelector().withStartSelectorType(StartSelectorType.EARLIEST),
                amazonKinesisVideo,
                new MergedOutputPiperFactory(Optional.empty(),
                        true,
                        gStreamerPipelineArguments));

        executorService.submit(getWorker);

        //Start a PutMedia worker to write data to a Kinesis Video Stream.
        putMediaWorker = PutMediaWorker.create(getRegion(),
                getCredentialsProvider(),
                getStreamName(),
                inputStream,
                amazonKinesisVideo);
        executorService.submit(putMediaWorker);

        Thread.sleep(3000);
        getWorker.stop();

        executorService.shutdown();
        executorService.awaitTermination(120, TimeUnit.SECONDS);
        if (!executorService.isTerminated()) {
            log.warn("Shutting down executor service by force");
            executorService.shutdownNow();
        } else {
            log.info("Executor service is shutdown");
        }
    }
}
