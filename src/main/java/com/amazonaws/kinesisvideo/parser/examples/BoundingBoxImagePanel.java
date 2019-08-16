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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.amazonaws.kinesisvideo.parser.rekognition.pojo.BoundingBox;
import com.amazonaws.kinesisvideo.parser.rekognition.pojo.FaceType;
import com.amazonaws.kinesisvideo.parser.rekognition.pojo.MatchedFace;
import com.amazonaws.kinesisvideo.parser.rekognition.pojo.RekognizedOutput;
import lombok.extern.slf4j.Slf4j;

/**
 * Panel which is used for rendering frames and embedding bounding boxes on the frames.
 */
@Slf4j
public class BoundingBoxImagePanel extends ImagePanel {
    private static final String DELIMITER = "-";

    @Override
    public void paintComponent(final Graphics g) {
        super.paintComponent(g);
    }

    public void processRekognitionOutput(final Graphics2D g2, final int width, final int height,
                                          final RekognizedOutput rekognizedOutput) {
        if (rekognizedOutput != null) {

            // Draw bounding boxes for faces.
            if (rekognizedOutput.getFaceSearchOutputs() != null) {
                log.debug("Number of detected faces in a frame {}", rekognizedOutput.getFaceSearchOutputs().size());
                for (final RekognizedOutput.FaceSearchOutput faceSearchOutput : rekognizedOutput.getFaceSearchOutputs()) {
                    final FaceType detectedFaceType;
                    final String title;
                    if (!faceSearchOutput.getMatchedFaceList().isEmpty()) {
                        // Taking First match as Rekognition returns set of matched faces sorted by confidence level
                        final MatchedFace matchedFace = faceSearchOutput.getMatchedFaceList().get(0);
                        final String externalImageId = matchedFace.getFace().getExternalImageId();
                        // Rekognition doesn't allow any extra attributes/tags to be associated with the 'Face'.
                        // External Image Id is used here to draw title on top of the bounding box and change color
                        // of the bounding box (based on the FaceType). External Image Id needs to be specified in
                        // below format in order get this working.
                        // Eg: PersonName1-Criminal, PersonName2-Trusted, PersonName3-Intruder etc.
                        if (externalImageId == null) {
                            // If the external image id is not specified, then draw confidence level as title.
                            title = matchedFace.getFace().getConfidence() + "";
                            detectedFaceType = FaceType.NOT_RECOGNIZED;
                        } else {
                            final String[] imageIds = externalImageId.split(DELIMITER);
                            if (imageIds.length > 1) {
                                title = imageIds[0];
                                detectedFaceType = FaceType.fromString(imageIds[1]);
                            } else {
                                title = "No prefix";
                                detectedFaceType = FaceType.NOT_RECOGNIZED;
                            }
                        }
                        log.debug("Number of matched faces for the detected face {}", faceSearchOutput.getMatchedFaceList().size());
                    } else {
                        detectedFaceType = FaceType.NOT_RECOGNIZED;
                        title = "Not recognized";
                    }
                    drawFaces(g2, width, height, faceSearchOutput.getDetectedFace().getBoundingBox(),
                            title, detectedFaceType.getColor());
                }
            }
        }
    }

    private void drawFaces(final Graphics2D g2, final int width, final int height,
                                  final BoundingBox boundingBox, final String personName, final Color color) {
        final Color c = g2.getColor();

        g2.setColor(color);
        // Draw bounding box
        drawBoundingBox(g2, width, height, boundingBox);

        // Draw title
        drawFaceTitle(g2, width, height, boundingBox, personName);
        g2.setColor(c);
    }

    private void drawFaceTitle(final Graphics2D g2, final int width, final int height,
                                      final BoundingBox boundingBox, final String personName) {
        final int left = (int) (boundingBox.getLeft() * width);
        final int top = (int) (boundingBox.getTop() * height);
        g2.drawString(personName, left, top);
    }

    private void drawBoundingBox(final Graphics2D g2, final int width, final int height,
                                 final BoundingBox boundingBox) {
        final int left = (int) (boundingBox.getLeft() * width);
        final int top = (int) (boundingBox.getTop() * height);
        final int bbWidth = (int) (boundingBox.getWidth() * width);
        final int bbHeight = (int) (boundingBox.getHeight() * height);

        // Draw bounding box
        g2.drawRect(left, top, bbWidth, bbHeight);
    }

    public void setImage(final BufferedImage bufferedImage, final RekognizedOutput rekognizedOutput) {
        this.image = bufferedImage;
        processRekognitionOutput(image.createGraphics(), image.getWidth(), image.getHeight(), rekognizedOutput);
        repaint();
    }
}
