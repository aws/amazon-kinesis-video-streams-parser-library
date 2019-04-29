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
import java.util.Optional;

import com.amazonaws.kinesisvideo.parser.examples.KinesisVideoFrameViewer;
import com.amazonaws.kinesisvideo.parser.mkv.Frame;
import com.amazonaws.kinesisvideo.parser.mkv.FrameProcessException;
import lombok.extern.slf4j.Slf4j;

import static com.amazonaws.kinesisvideo.parser.utilities.BufferedImageUtil.addTextToImage;


@Slf4j
public class H264FrameRenderer extends H264FrameDecoder {
    private static final int PIXEL_TO_LEFT = 10;
    private static final int PIXEL_TO_TOP_LINE_1 = 20;
    private static final int PIXEL_TO_TOP_LINE_2 = 40;

    private final KinesisVideoFrameViewer kinesisVideoFrameViewer;

    protected H264FrameRenderer(final KinesisVideoFrameViewer kinesisVideoFrameViewer) {
        super();
        this.kinesisVideoFrameViewer = kinesisVideoFrameViewer;
        this.kinesisVideoFrameViewer.setVisible(true);
    }

    public static H264FrameRenderer create(KinesisVideoFrameViewer kinesisVideoFrameViewer) {
        return new H264FrameRenderer(kinesisVideoFrameViewer);
    }

    @Override
    public void process(Frame frame, MkvTrackMetadata trackMetadata, Optional<FragmentMetadata> fragmentMetadata,
                        Optional<FragmentMetadataVisitor.MkvTagProcessor> tagProcessor) throws FrameProcessException {
        final BufferedImage bufferedImage = decodeH264Frame(frame, trackMetadata);
        if (tagProcessor.isPresent()) {
            final FragmentMetadataVisitor.BasicMkvTagProcessor processor =
                    (FragmentMetadataVisitor.BasicMkvTagProcessor) tagProcessor.get();

            if (fragmentMetadata.isPresent()) {
                addTextToImage(bufferedImage,
                        String.format("Fragment Number: %s", fragmentMetadata.get().getFragmentNumberString()),
                        PIXEL_TO_LEFT, PIXEL_TO_TOP_LINE_1);
            }

            if (processor.getTags().size() > 0) {
                addTextToImage(bufferedImage, "Fragment Metadata: " + processor.getTags().toString(),
                        PIXEL_TO_LEFT, PIXEL_TO_TOP_LINE_2);
            } else {
                addTextToImage(bufferedImage, "Fragment Metadata: No Metadata Available",
                        PIXEL_TO_LEFT, PIXEL_TO_TOP_LINE_2);
            }
        }
        kinesisVideoFrameViewer.update(bufferedImage);
    }


}
