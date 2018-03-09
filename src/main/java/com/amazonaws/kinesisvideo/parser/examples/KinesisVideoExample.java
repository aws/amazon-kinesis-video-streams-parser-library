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
import com.amazonaws.kinesisvideo.parser.ebml.MkvTypeInfos;
import com.amazonaws.kinesisvideo.parser.mkv.Frame;
import com.amazonaws.kinesisvideo.parser.mkv.MkvDataElement;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitException;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitor;
import com.amazonaws.kinesisvideo.parser.mkv.MkvEndMasterElement;
import com.amazonaws.kinesisvideo.parser.mkv.MkvStartMasterElement;
import com.amazonaws.kinesisvideo.parser.mkv.visitors.CompositeMkvElementVisitor;
import com.amazonaws.kinesisvideo.parser.utilities.FragmentMetadataVisitor;
import com.amazonaws.kinesisvideo.parser.utilities.OutputSegmentMerger;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideo;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideoClientBuilder;
import com.amazonaws.services.kinesisvideo.model.CreateStreamRequest;
import com.amazonaws.services.kinesisvideo.model.DeleteStreamRequest;
import com.amazonaws.services.kinesisvideo.model.DescribeStreamRequest;
import com.amazonaws.services.kinesisvideo.model.ResourceNotFoundException;
import com.amazonaws.services.kinesisvideo.model.StartSelector;
import com.amazonaws.services.kinesisvideo.model.StartSelectorType;
import com.amazonaws.services.kinesisvideo.model.StreamInfo;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.jcodec.scale.Transform;
import org.jcodec.scale.Yuv420pToRgb;

import static org.jcodec.codecs.h264.H264Utils.splitMOVPacket;

import java.awt.*;

/**
 * Example for integrating with Kinesis Video.
 * This example does:
 * 1. Create a stream, deleting and recreating if the stream of the same name already exists.
 * It sets the retention period of the created stream to 48 hours.
 * 2. Call PutMedia to stream video fragments into the stream.
 * 3. Simultaneously call GetMedia to stream video fragments out of the stream.
 * 4. It uses the StreamingMkvParser to parse the returned the stream and perform these steps:
 *   4.1 The GetMedia output stream has one mkv segment for each fragment. Merge the mkv segments that share track
 *        information into a single segment.
 *   4.2 Log when we receive the start and end of each fragment including the fragment sequence number and
 *        millis behind now.
 *
 *
 */
@Slf4j
public class KinesisVideoExample extends KinesisVideoCommon {
    private static final long SLEEP_PERIOD_MILLIS = TimeUnit.SECONDS.toMillis(3);
    private static final int DATA_RETENTION_IN_HOURS = 48;

    private final AmazonKinesisVideo amazonKinesisVideo;
    private final InputStream inputStream;
    private final ExecutorService executorService;
    private PutMediaWorker putMediaWorker;
    private final StreamOps streamOps;
    private GetMediaProcessingArguments getMediaProcessingArguments;

    @Builder
    private KinesisVideoExample(Regions region,
            String streamName,
            AWSCredentialsProvider credentialsProvider,
            InputStream inputVideoStream) {
        super(region, credentialsProvider, streamName);
        final AmazonKinesisVideoClientBuilder builder = AmazonKinesisVideoClientBuilder.standard();
        configureClient(builder);
        this.amazonKinesisVideo = builder.build();
        this.inputStream = inputVideoStream;
        this.streamOps = new StreamOps(region,  streamName, credentialsProvider);
        this.executorService = Executors.newFixedThreadPool(2);
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

        getMediaProcessingArguments = GetMediaProcessingArguments.create();

        try (GetMediaProcessingArguments getMediaProcessingArgumentsLocal = getMediaProcessingArguments) {
            //Start a GetMedia worker to read and process data from the Kinesis Video Stream.
            GetMediaWorker getMediaWorker = GetMediaWorker.create(getRegion(),
                    getCredentialsProvider(),
                    getStreamName(),
                    new StartSelector().withStartSelectorType(StartSelectorType.EARLIEST),
                    amazonKinesisVideo,
                    getMediaProcessingArgumentsLocal.getMkvElementVisitor());
            executorService.submit(getMediaWorker);

            //Start a PutMedia worker to write data to a Kinesis Video Stream.
            putMediaWorker = PutMediaWorker.create(getRegion(),
                    getCredentialsProvider(),
                    getStreamName(),
                    inputStream,
                    amazonKinesisVideo);
            executorService.submit(putMediaWorker);

            //Wait for the workers to finish.
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

    public long getFragmentsPersisted() {
        return putMediaWorker.getNumFragmentsPersisted();
    }

    public long getFragmentsRead() {
        return getMediaProcessingArguments.getFragmentCount();
    }

    @RequiredArgsConstructor
    private static class LogVisitor extends MkvElementVisitor {
        private byte[] codecPrivate;
        private String codecID;
        private int pixelWidth = 0;
        private int pixelHeight = 0;
        private H264Decoder decoder = new H264Decoder();
        private Transform transform = new Yuv420pToRgb();

        private final FragmentMetadataVisitor fragmentMetadataVisitor;

        UIRenderer uiRenderer = new UIRenderer();

        @Getter
        private long fragmentCount = 0;

        @Override
        public void visit(MkvStartMasterElement startMasterElement) throws MkvElementVisitException {
            if (MkvTypeInfos.EBML.equals(startMasterElement.getElementMetaData().getTypeInfo())) {
                fragmentCount++;
                log.info("Start of segment  {} ", fragmentCount);
            }
        }

        @Override
        public void visit(MkvEndMasterElement endMasterElement) throws MkvElementVisitException {
            if (MkvTypeInfos.SEGMENT.equals(endMasterElement.getElementMetaData().getTypeInfo())) {
                log.info("End of segment  {} fragment # {} millisBehindNow {} ", fragmentCount,
                        fragmentMetadataVisitor.getCurrentFragmentMetadata().get().getFragmentNumberString(),
                        fragmentMetadataVisitor.getMillisBehindNow().getAsLong());
            }
        }

        @Override
        public void visit(MkvDataElement dataElement) throws MkvElementVisitException {
            uiRenderer.setVisible(true);
            log.info("Got data element: {}", dataElement.getElementMetaData().getTypeInfo().getName());
            final String dataElementName = dataElement.getElementMetaData().getTypeInfo().getName();

            if ("CodecID".equals(dataElementName)) {
                codecID = (String)dataElement.getValueCopy().getVal();
                // This is a C-style string, so remove the trailing null characters
                codecID = codecID.trim();
                log.info("Codec ID: {}", codecID);
            }

            if ("CodecPrivate".equals(dataElementName)) {
                codecPrivate = ((ByteBuffer)dataElement.getValueCopy().getVal()).array().clone();
                log.info("CodecPrivate: {}", codecPrivate);
            }

            if ("PixelWidth".equals(dataElementName)) {
                pixelWidth = ((BigInteger)dataElement.getValueCopy().getVal()).intValue();
                log.info("Pixel Width: {}", pixelWidth);
            }

            if ("PixelHeight".equals(dataElementName)) {
                pixelHeight = ((BigInteger)dataElement.getValueCopy().getVal()).intValue();
                log.info("Pixel Height: {}", pixelHeight);
            }

            if ("SimpleBlock".equals(dataElementName)) {
                final ByteBuffer dataBuffer = ((Frame)dataElement.getValueCopy().getVal()).getFrameData();

                final Picture rgb = Picture.create(pixelWidth, pixelHeight, ColorSpace.RGB);
                final BufferedImage bi = new BufferedImage(pixelWidth, pixelHeight, BufferedImage.TYPE_3BYTE_BGR);
                final AvcCBox avcC = AvcCBox.parseAvcCBox(ByteBuffer.wrap(codecPrivate));

                decoder.addSps(avcC.getSpsList());
                decoder.addPps(avcC.getPpsList());

                final Picture buf = Picture.create(pixelWidth, pixelHeight, ColorSpace.YUV422);
                final Picture pic = decoder.decodeFrameFromNals(splitMOVPacket(dataBuffer, avcC), buf.getData());
                transform.transform(pic, rgb);
                AWTUtil.toBufferedImage(rgb, bi);

                uiRenderer.setImage(bi, pixelWidth, pixelHeight);
            }
        }
    }

    private static class UIRenderer extends java.awt.Frame {
        BufferedImage bi;
        int weidth = 400;
        int height = 400;

        public UIRenderer(){
            addExitListener();
        }

        private void addExitListener(){
            addWindowListener(new WindowAdapter() {
                public void windowClosing(final WindowEvent windowEvent){
                    System.exit(0);
                }
            });
        }

        public void setImage(final BufferedImage bi, final int weidth, final int height) {
            this.bi = bi;
            this.weidth = weidth;
            this.height = height;
            repaint();
        }

        @Override
        public void paint(Graphics g) {
            setSize(weidth, height);
            final Graphics2D g2 = (Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            /* Draw the image, applying the filter */
            if (bi != null) {
                g2.drawImage(bi, 0, 0, null);
            }
        }
    }

    private static class GetMediaProcessingArguments implements Closeable {
        private final BufferedOutputStream outputStream;
        private final LogVisitor logVisitor;

        @Getter
        private final CompositeMkvElementVisitor mkvElementVisitor;

        public GetMediaProcessingArguments(BufferedOutputStream outputStream,
                LogVisitor logVisitor,
                CompositeMkvElementVisitor mkvElementVisitor) {
            this.outputStream = outputStream;
            this.mkvElementVisitor = mkvElementVisitor;
            this.logVisitor = logVisitor;
        }

        public static GetMediaProcessingArguments create() throws IOException {
            //Fragment metadata visitor to extract Kinesis Video fragment metadata from the GetMedia stream.
            FragmentMetadataVisitor fragmentMetadataVisitor = FragmentMetadataVisitor.create();

            //A visitor used to log as the GetMedia stream is processed.
            LogVisitor logVisitor = new LogVisitor(fragmentMetadataVisitor);

            //An OutputSegmentMerger to combine multiple segments that share track and ebml metadata into one
            //mkv segment.
            OutputStream fileOutputStream = Files.newOutputStream(Paths.get("kinesis_video_example_merged_output2.mkv"),
                    StandardOpenOption.WRITE, StandardOpenOption.CREATE);
            BufferedOutputStream outputStream = new BufferedOutputStream(fileOutputStream);
            OutputSegmentMerger outputSegmentMerger = OutputSegmentMerger.createDefault(outputStream);

            //A composite visitor to encapsulate the three visitors.
            CompositeMkvElementVisitor mkvElementVisitor =
                    new CompositeMkvElementVisitor(fragmentMetadataVisitor, outputSegmentMerger, logVisitor);

            return new GetMediaProcessingArguments(outputStream, logVisitor, mkvElementVisitor);
        }

        @Override
        public void close() throws IOException {
            outputStream.close();
        }

        public long getFragmentCount() {
            return logVisitor.fragmentCount;
        }
    }
}
