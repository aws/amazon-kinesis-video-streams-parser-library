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
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;

public class KinesisVideoFrameViewer extends JFrame {
    private final int width;
    private final int height;
    private final String title;

    protected ImagePanel panel;

    protected KinesisVideoFrameViewer(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;
        this.setTitle(title);
        this.setBackground(Color.BLACK);
    }

    public KinesisVideoFrameViewer(int width, int height) {
        this(width, height, "Kinesis Video Frame Viewer ");
        panel = new ImagePanel();
        addImagePanel(panel);
    }

    protected void addImagePanel(final ImagePanel panel) {
        panel.setPreferredSize(new Dimension(width, height));
        this.add(panel);
        this.pack();
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.out.println(title + " closed");
                System.exit(0);
            }
        });
    }

    public void update(BufferedImage image) {
        panel.setImage(image);
    }
}

