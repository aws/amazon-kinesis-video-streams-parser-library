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
import com.amazonaws.kinesisvideo.parser.mkv.*;
import com.amazonaws.kinesisvideo.parser.mkv.visitors.CompositeMkvElementVisitor;
import com.amazonaws.kinesisvideo.parser.utilities.FragmentMetadataVisitor;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideo;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideoClientBuilder;
import com.amazonaws.services.kinesisvideo.model.StartSelector;
import com.amazonaws.services.kinesisvideo.model.StartSelectorType;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jcodec.codecs.h264.H264Decoder;

import static org.jcodec.codecs.h264.H264Utils.splitMOVPacket;

/**
 * Note: PutMedia is prerequisite before invoking this example.
 *
 * Example for integrating with Kinesis Video.
 * This example does:
 * 1. Calls GetMedia to stream video fragments out of the stream.
 * 2. It uses the StreamingMkvParser to parse the returned the stream and perform these steps:
 *   2.1 The GetMedia output stream has one mkv segment for each fragment. Merge the mkv segments that share track
 *        information into a single segment.
 *   2.2 Decodes the frames using h264 decoder (using jcodec) and
 *   2.3 It renders the image using  JFrame for viewing
 *
 * To run the example:
 * Run the Unit test  KinesisVideoRendererExampleTest
 *
 */

@Slf4j
public class KinesisVideoRendererExample extends KinesisVideoCommon {

    private final AmazonKinesisVideo amazonKinesisVideo;
    private final ExecutorService executorService;
    private KinesisVideoRendererExample.GetMediaProcessingArguments getMediaProcessingArguments;
    private static BufferedImage renderImage;
    private static KinesisVideoFrameViewer kinesisVideoFrameViewer;

    @Builder
    private KinesisVideoRendererExample(Regions region,
                                        String streamName,
                                        AWSCredentialsProvider credentialsProvider) {
        super(region, credentialsProvider, streamName);
        final AmazonKinesisVideoClientBuilder builder = AmazonKinesisVideoClientBuilder.standard();
        configureClient(builder);
        this.amazonKinesisVideo = builder.build();
        this.executorService = Executors.newFixedThreadPool(1);
        //Set frame width and height to match with video resolution of the stream
        // for this example the video width and height are chosen to be 640 x 480.
        kinesisVideoFrameViewer = new KinesisVideoFrameViewer(640, 480);
        kinesisVideoFrameViewer.setVisible(true);
    }

    /**
     * This method executes the example.
     *
     * @throws InterruptedException the thread is interrupted while waiting for the stream to enter the correct state.
     * @throws IOException          fails to read video from the input stream or write to the output stream.
     */
    public void execute() throws InterruptedException, IOException {
        getMediaProcessingArguments = KinesisVideoRendererExample.GetMediaProcessingArguments.create();
        try (KinesisVideoRendererExample.GetMediaProcessingArguments getMediaProcessingArgumentsLocal = getMediaProcessingArguments) {
            //Start a GetMedia worker to read and process data from the Kinesis Video Stream.
            GetMediaWorker getMediaWorker = GetMediaWorker.create(getRegion(),
                    getCredentialsProvider(),
                    getStreamName(),
                    new StartSelector().withStartSelectorType(StartSelectorType.EARLIEST),
                    amazonKinesisVideo,
                    getMediaProcessingArgumentsLocal.getMkvElementVisitor());
            executorService.submit(getMediaWorker);

            //Wait for the workers to finish.
            executorService.shutdown();
            executorService.awaitTermination(120, TimeUnit.SECONDS);
            if (!executorService.isTerminated()) {
                log.warn("Shutting down executor service by force");
                executorService.shutdownNow();
            } else {
                log.info("Exeutor service is shutdown");
            }
        }
    }


    private static class GetMediaProcessingArguments implements Closeable {
        private final KinesisVideoRendererExample.ParsingVisitor parsingVisitor;

        @Getter
        private final CompositeMkvElementVisitor mkvElementVisitor;
        GetMediaProcessingArguments(
                KinesisVideoRendererExample.ParsingVisitor parsingVisitor,
                CompositeMkvElementVisitor mkvElementVisitor) {

            this.mkvElementVisitor = mkvElementVisitor;
            this.parsingVisitor = parsingVisitor;
        }

        private static KinesisVideoRendererExample.GetMediaProcessingArguments create() throws IOException {
            //Fragment metadata visitor to extract Kinesis Video fragment metadata from the GetMedia stream.
            FragmentMetadataVisitor fragmentMetadataVisitor = FragmentMetadataVisitor.create();
            KinesisVideoRendererExample.ParsingVisitor parsingVisitor = new KinesisVideoRendererExample.ParsingVisitor();
            CompositeMkvElementVisitor mkvElementVisitor =
                    new CompositeMkvElementVisitor(fragmentMetadataVisitor, parsingVisitor);
            return new KinesisVideoRendererExample.GetMediaProcessingArguments(parsingVisitor, mkvElementVisitor);
        }

        @Override
        public void close() throws IOException {

        }
    }


    @RequiredArgsConstructor
    private static class ParsingVisitor extends MkvElementVisitor {
        private byte[] codecPrivate;
        private String codecID;
        private int pixelWidth = 0;
        private int pixelHeight = 0;
        private final H264Decoder decoder = new H264Decoder();
        private final Transform transform = new Yuv420jToRgb();

        @Override
        public void visit(MkvStartMasterElement startMasterElement) throws MkvElementVisitException {
        }

        @Override
        public void visit(MkvEndMasterElement endMasterElement) throws MkvElementVisitException {
        }

        @Override
        public void visit(MkvDataElement dataElement) throws MkvElementVisitException {
            log.info("Got data element: {}", dataElement.getElementMetaData().getTypeInfo().getName());
            String dataElementName = dataElement.getElementMetaData().getTypeInfo().getName();

            if ("CodecID".equals(dataElementName)) {
                codecID = (String) dataElement.getValueCopy().getVal();
                // This is a C-style string, so remove the trailing null characters
                codecID = codecID.trim();
                log.info("Codec ID: {}", codecID);
            }

            if ("CodecPrivate".equals(dataElementName)) {
                codecPrivate = ((ByteBuffer) dataElement.getValueCopy().getVal()).array().clone();
                log.info("CodecPrivate: {}", codecPrivate);
            }

            if ("PixelWidth".equals(dataElementName)) {
                pixelWidth = ((BigInteger) dataElement.getValueCopy().getVal()).intValue();
                log.info("Pixel Width: {}", pixelWidth);
            }

            if ("PixelHeight".equals(dataElementName)) {
                pixelHeight = ((BigInteger) dataElement.getValueCopy().getVal()).intValue();
                log.info("Pixel Height: {}", pixelHeight);
            }

            if ("SimpleBlock".equals(dataElementName)) {
                log.info("Decoding Frames ... ");
                ByteBuffer dataBuffer = dataElement.getDataBuffer();

                // Read the bytes that appear to comprise the header
                // See: https://www.matroska.org/technical/specs/index.html#simpleblock_structure

                byte[] header = new byte[4];
                dataBuffer.get(header, 0, 4);

                KinesisVideoRendererExample.renderImage = new BufferedImage(pixelWidth, pixelHeight, BufferedImage.TYPE_3BYTE_BGR);
                AvcCBox avcC = AvcCBox.parseAvcCBox(ByteBuffer.wrap(codecPrivate));

                decoder.addSps(avcC.getSpsList());
                decoder.addPps(avcC.getPpsList());
                
                Picture buf = Picture.create(pixelWidth, pixelHeight, ColorSpace.YUV420J);
                List<ByteBuffer> byteBuffers = splitMOVPacket(dataBuffer, avcC);
                Picture pic = decoder.decodeFrameFromNals(byteBuffers, buf.getData());
                if (pic != null) {
                    // Work around for color issues in JCodec
                    // https://github.com/jcodec/jcodec/issues/59
                    // https://github.com/jcodec/jcodec/issues/192
                    byte[][] dataTemp = new byte[3][pic.getData().length];
                    dataTemp[0] = pic.getPlaneData(0);
                    dataTemp[1] = pic.getPlaneData(2);
                    dataTemp[2] = pic.getPlaneData(1);
                    Picture tmpBuf = Picture.createPicture(pixelWidth, pixelHeight, dataTemp, ColorSpace.YUV420J);
                    Picture rgb = Picture.create(pixelWidth, pixelHeight, ColorSpace.RGB);
                    transform.transform(tmpBuf, rgb);
                    AWTUtil.toBufferedImage(rgb, renderImage);
                    kinesisVideoFrameViewer.update(renderImage);
                }
            }
        }
    }
}
