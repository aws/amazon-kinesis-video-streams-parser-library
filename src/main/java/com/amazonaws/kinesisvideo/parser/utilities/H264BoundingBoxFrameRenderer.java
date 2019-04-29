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
package com.amazonaws.kinesisvideo.parser.utilities;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;

import com.amazonaws.kinesisvideo.parser.examples.KinesisVideoBoundingBoxFrameViewer;
import com.amazonaws.kinesisvideo.parser.mkv.Frame;
import com.amazonaws.kinesisvideo.parser.mkv.FrameProcessException;
import com.amazonaws.kinesisvideo.parser.rekognition.pojo.RekognizedFragmentsIndex;
import com.amazonaws.kinesisvideo.parser.rekognition.pojo.RekognizedOutput;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class H264BoundingBoxFrameRenderer extends H264FrameRenderer {

    private static final int DEFAULT_MAX_TIMEOUT = 100;
    private static final int WAIT_TIMEOUT = 3;
    private static final int MILLIS_IN_SEC = 1000;
    private static final int OFFSET_DELTA_THRESHOLD = 10;

    private final KinesisVideoBoundingBoxFrameViewer kinesisVideoBoundingBoxFrameViewer;
    private final RekognizedFragmentsIndex rekognizedFragmentsIndex;
    private RekognizedOutput currentRekognizedOutput = null;

    @Setter
    private int maxTimeout = DEFAULT_MAX_TIMEOUT;

    private long keyFrameTimecode;

    private H264BoundingBoxFrameRenderer(final KinesisVideoBoundingBoxFrameViewer kinesisVideoBoundingBoxFrameViewer,
                                         final RekognizedFragmentsIndex rekognizedFragmentsIndex) {
        super(kinesisVideoBoundingBoxFrameViewer);
        this.kinesisVideoBoundingBoxFrameViewer = kinesisVideoBoundingBoxFrameViewer;
        this.rekognizedFragmentsIndex = rekognizedFragmentsIndex;
    }

    public static H264BoundingBoxFrameRenderer create(final KinesisVideoBoundingBoxFrameViewer kinesisVideoBoundingBoxFrameViewer,
                                                      final RekognizedFragmentsIndex rekognizedFragmentsIndex) {
        return new H264BoundingBoxFrameRenderer(kinesisVideoBoundingBoxFrameViewer, rekognizedFragmentsIndex);
    }

    @Override
    public void process(final Frame frame, final MkvTrackMetadata trackMetadata, final Optional<FragmentMetadata> fragmentMetadata,
                        final Optional<FragmentMetadataVisitor.MkvTagProcessor> tagProcessor) throws FrameProcessException {
        final BufferedImage bufferedImage = decodeH264Frame(frame, trackMetadata);
        final Optional<RekognizedOutput> rekognizedOutput = getRekognizedOutput(frame, fragmentMetadata);
        renderFrame(bufferedImage, rekognizedOutput);
    }

    private Optional<RekognizedOutput> getRekognizedOutput(final Frame frame,
                                                           final Optional<FragmentMetadata> fragmentMetadata) {

        Optional<RekognizedOutput> rekognizedOutput = Optional.empty();
        if (rekognizedFragmentsIndex != null && fragmentMetadata.isPresent()) {
            final String fragmentNumber = fragmentMetadata.get().getFragmentNumberString();

            int timeout = 0;
            List<RekognizedOutput> rekognizedOutputs = null;

            // if rekognizedOutputs is null then Rekognition did not return the results for this fragment.
            // Wait until the results are received.
            while (true) {
                rekognizedOutputs = rekognizedFragmentsIndex.getRekognizedOutputList(fragmentNumber);
                if (rekognizedOutputs != null) {
                    break;
                } else {
                    timeout += waitForResults(WAIT_TIMEOUT);
                    if (timeout >= maxTimeout) {
                        log.warn("No rekognized result after waiting for {} ms ", timeout);
                        break;
                    }
                }
            }
            if (rekognizedOutputs != null) {

                // Currently Rekognition samples frames and calculates the frame offset from the fragment start time.
                // So, in order to match with rekognition results, we have to compute the same frame offset from the
                // beginning of the fragments.
                if (frame.isKeyFrame()) {
                    keyFrameTimecode = frame.getTimeCode();
                    log.debug("Key frame timecode : {}", keyFrameTimecode);
                }
                final long frameOffset = (frame.getTimeCode() > keyFrameTimecode)
                        ? frame.getTimeCode() - keyFrameTimecode : 0;
                log.debug("Current Fragment Number : {} Computed Frame offset : {}", fragmentNumber, frameOffset);
                if (log.isDebugEnabled()) {
                    rekognizedOutputs
                            .forEach(p -> log.debug("frameOffsetInSeconds from Rekognition : {}",
                                    p.getFrameOffsetInSeconds()));
                }

                // Check whether the computed offset matches the rekognized output frame offset. Rekognition
                // output is in seconds whereas the frame offset is calculated in milliseconds.
                // NOTE: Rekognition frame offset doesn't exactly match with the computed offset below. So
                // take the closest one possible within 10ms delta.
                rekognizedOutput = rekognizedOutputs.stream()
                        .filter(p -> isOffsetDeltaWithinThreshold(frameOffset, p))
                        .findFirst();

                // Remove from the index once the RekognizedOutput is processed. Else it would increase the memory
                // footprint and blow up the JVM.
                if (rekognizedOutput.isPresent()) {
                    log.debug("Computed offset matched with retrieved offset. Delta : {}",
                            Math.abs(frameOffset - (rekognizedOutput.get().getFrameOffsetInSeconds() * MILLIS_IN_SEC)));
                    rekognizedOutputs.remove(rekognizedOutput.get());
                    if (rekognizedOutputs.isEmpty()) {
                        log.debug("All frames processed for this fragment number : {}", fragmentNumber);
                        rekognizedFragmentsIndex.remove(fragmentNumber);
                    }
                }
            }
        }
        return rekognizedOutput;
    }

    private boolean isOffsetDeltaWithinThreshold(final long frameOffset, final RekognizedOutput output) {
        return Math.abs(frameOffset - (output.getFrameOffsetInSeconds() * MILLIS_IN_SEC)) <= OFFSET_DELTA_THRESHOLD;
    }

    void renderFrame(final BufferedImage bufferedImage, final Optional<RekognizedOutput> rekognizedOutput) {
        if (rekognizedOutput.isPresent()) {
            System.out.println("Rendering Rekognized sampled frame...");
            kinesisVideoBoundingBoxFrameViewer.update(bufferedImage, rekognizedOutput.get());
            currentRekognizedOutput = rekognizedOutput.get();
        } else if (currentRekognizedOutput != null) {
            System.out.println("Rendering non-sampled frame with previous rekognized results...");
            kinesisVideoBoundingBoxFrameViewer.update(bufferedImage, currentRekognizedOutput);
        } else {
            System.out.println("Rendering frame without any rekognized results...");
            kinesisVideoBoundingBoxFrameViewer.update(bufferedImage);
        }
    }


    private long waitForResults(final long timeout) {
        final long startTime = System.currentTimeMillis();
        try {
            log.info("No rekognized results for this fragment number. Waiting ....");
            Thread.sleep(timeout);
        } catch (final InterruptedException e) {
            log.warn("Error while waiting for rekognized output !", e);
        }
        return System.currentTimeMillis() - startTime;
    }

}
