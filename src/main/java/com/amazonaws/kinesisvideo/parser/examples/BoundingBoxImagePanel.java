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

/**
 * Panel which is used for rendering frames and embedding bounding boxes on the frames.
 */
class BoundingBoxImagePanel extends ImagePanel {
    private static final String DELIMITER = "-";
    private RekognizedOutput rekognizedOutput;

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        super.paintComponent(g);

        if (rekognizedOutput != null) {

            // Draw bounding boxes for faces.
            if (rekognizedOutput.getFaceSearchOutputs() != null) {
                for (RekognizedOutput.FaceSearchOutput faceSearchOutput: rekognizedOutput.getFaceSearchOutputs()) {
                    FaceType detectedFaceType;
                    String title;
                    if (!faceSearchOutput.getMatchedFaceList().isEmpty()) {
                        // Taking First match as Rekognition returns set of matched faces sorted by confidence level
                        MatchedFace matchedFace = faceSearchOutput.getMatchedFaceList().get(0);
                        String externalImageId = matchedFace.getFace().getExternalImageId();
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
                            String[] imageIds = externalImageId.split(DELIMITER);
                            if (imageIds.length > 1) {
                                title = imageIds[0];
                                detectedFaceType = FaceType.valueOf(imageIds[1].toUpperCase());
                            } else {
                                title = "No prefix";
                                detectedFaceType = FaceType.NOT_RECOGNIZED;
                            }
                        }
                    } else {
                        detectedFaceType = FaceType.NOT_RECOGNIZED;
                        title = "Not recognized";
                    }
                    drawFaces(g2, faceSearchOutput.getDetectedFace().getBoundingBox(),
                            title, detectedFaceType.getColor());
                }
            }
        }
    }

    private void drawFaces(Graphics2D g2, BoundingBox boundingBox, String personName, Color color) {
        Color c = g2.getColor();

        g2.setColor(color);
        // Draw bounding box
        drawBoundingBox(g2, boundingBox);

        // Draw title
        drawFaceTitle(g2, boundingBox, personName);
        g2.setColor(c);
    }

    private void drawFaceTitle(Graphics2D g2, BoundingBox boundingBox, String personName) {
        int left = (int) (boundingBox.getLeft() * image.getWidth());
        int top = (int) (boundingBox.getTop() * image.getHeight());
        g2.drawString(personName, left, top);
    }

    private void drawBoundingBox(Graphics2D g2, BoundingBox boundingBox) {
        int left = (int) (boundingBox.getLeft() * image.getWidth());
        int top = (int) (boundingBox.getTop() * image.getHeight());
        int width = (int) (boundingBox.getWidth() * image.getWidth());
        int height = (int) (boundingBox.getHeight() * image.getHeight());

        // Draw bounding box
        g2.drawRect(left, top, width, height);
    }

    public void setImage(BufferedImage bufferedImage, RekognizedOutput rekognizedOutput) {
        this.image = bufferedImage;
        this.rekognizedOutput = rekognizedOutput;
        repaint();
    }
}
