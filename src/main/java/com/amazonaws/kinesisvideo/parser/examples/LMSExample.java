package com.amazonaws.kinesisvideo.parser.examples;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.kinesisvideo.parser.ebml.MkvTypeInfos;
import com.amazonaws.kinesisvideo.parser.mkv.MkvDataElement;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitException;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitor;
import com.amazonaws.kinesisvideo.parser.mkv.MkvEndMasterElement;
import com.amazonaws.kinesisvideo.parser.mkv.MkvStartMasterElement;
import com.amazonaws.kinesisvideo.parser.utilities.FragmentMetadataVisitor;
import com.amazonaws.kinesisvideo.parser.utilities.FrameVisitor;
import com.amazonaws.kinesisvideo.parser.utilities.LMSFrameProcessor;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesisvideo.model.StartSelector;
import com.amazonaws.services.kinesisvideo.model.StartSelectorType;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LMSExample extends KinesisVideoCommon {

    private final ExecutorService executorService;
    private GetMediaProcessingArguments getMediaProcessingArguments;
    private final StreamOps streamOps;
    private final OutputStream outputStreamFromCustomer;
    private final OutputStream outputStreamToCustomer;
    private final String fragmentNumber;

    public LMSExample(Regions region,
                      String streamName,
                      String fragmentNumber,
                      AWSCredentialsProvider credentialsProvider,
                      OutputStream outputStreamFromCustomer,
                      OutputStream outputStreamToCustomer) throws IOException {
        super(region, credentialsProvider, streamName);
        this.streamOps = new StreamOps(region,  streamName, credentialsProvider);
        this.executorService = Executors.newFixedThreadPool(2);
        this.outputStreamFromCustomer = outputStreamFromCustomer;
        this.outputStreamToCustomer = outputStreamToCustomer;
        this.fragmentNumber = fragmentNumber;
    }

    public void execute () throws InterruptedException, IOException {
        getMediaProcessingArguments = GetMediaProcessingArguments.create(outputStreamFromCustomer, outputStreamToCustomer);
        try (GetMediaProcessingArguments getMediaProcessingArgumentsLocal = getMediaProcessingArguments) {
            //Start a GetMedia worker to read and process data from the Kinesis Video Stream.
            GetMediaWorker getMediaWorker = GetMediaWorker.create(getRegion(),
            getCredentialsProvider(),
            getStreamName(),
            new StartSelector().withStartSelectorType(StartSelectorType.FRAGMENT_NUMBER).withAfterFragmentNumber(fragmentNumber),
            streamOps.amazonKinesisVideo,
            getMediaProcessingArgumentsLocal.getFrameVisitor());
            executorService.submit(getMediaWorker);

            //Wait for the workers to finish.
            executorService.shutdown();
            executorService.awaitTermination(120, TimeUnit.SECONDS);
            if (!executorService.isTerminated()) {
                System.out.println("Shutting down executor service by force");
                executorService.shutdownNow();
            } else {
                System.out.println("Executor service is shutdown");
            }
        } finally {
            outputStreamFromCustomer.close();
            outputStreamToCustomer.close();
        }

    }

    private static class LogVisitor extends MkvElementVisitor {
        private final FragmentMetadataVisitor fragmentMetadataVisitor;

        private LogVisitor(FragmentMetadataVisitor fragmentMetadataVisitor) {
            this.fragmentMetadataVisitor = fragmentMetadataVisitor;
        }

        public long getFragmentCount() {
            return fragmentCount;
        }

        private long fragmentCount = 0;

        @Override
        public void visit(MkvStartMasterElement startMasterElement) throws MkvElementVisitException {
            if (MkvTypeInfos.EBML.equals(startMasterElement.getElementMetaData().getTypeInfo())) {
                fragmentCount++;
                System.out.println("Start of segment");
            }
        }

        @Override
        public void visit(MkvEndMasterElement endMasterElement) throws MkvElementVisitException {
            if (MkvTypeInfos.SEGMENT.equals(endMasterElement.getElementMetaData().getTypeInfo())) {
                System.out.println("End of segment");

            }
        }

        @Override
        public void visit(MkvDataElement dataElement) throws MkvElementVisitException {
        }
    }

    private static class GetMediaProcessingArguments implements Closeable {

        public FrameVisitor getFrameVisitor() {
            return frameVisitor;
        }

        private final FrameVisitor frameVisitor;

        public GetMediaProcessingArguments(FrameVisitor frameVisitor) {
            this.frameVisitor = frameVisitor;
        }

        public static GetMediaProcessingArguments create(OutputStream outputStreamFromCustomer, OutputStream outputStreamToCustomer) throws IOException {
            //Fragment metadata visitor to extract Kinesis Video fragment metadata from the GetMedia stream.
            FragmentMetadataVisitor fragmentMetadataVisitor = FragmentMetadataVisitor.create();

            //A visitor used to log as the GetMedia stream is processed.
            LogVisitor logVisitor = new LogVisitor(fragmentMetadataVisitor);

            //A composite visitor to encapsulate the three visitors.
            FrameVisitor frameVisitor =
                    FrameVisitor.create(LMSFrameProcessor.create(outputStreamFromCustomer, outputStreamToCustomer, fragmentMetadataVisitor));

            return new GetMediaProcessingArguments(frameVisitor);
        }

        @Override
        public void close() throws IOException {

        }

    }
}