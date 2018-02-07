package com.amazonaws.kinesisvideo.parser.examples;

import javax.swing.*;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

public class KinesisImageViewer extends JFrame {
    private final ImagePanel panel;

    public KinesisImageViewer(int width, int height) {
        this.setTitle("Kinesis Image Viewer Sample");
        this.setBackground(Color.BLACK);
        panel = new ImagePanel();
        panel.setPreferredSize(new Dimension(width, height));
        this.add(panel);
        this.pack();
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.out.println("Kinesis Video Viewr Frame Closed");
                System.exit(0);
            }
        });
    }

    public void update(BufferedImage image) {
        panel.setImage(image);
    }
}

class ImagePanel extends JPanel {
    private BufferedImage image;

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        if (image != null) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.clearRect(0, 0, image.getWidth(), image.getHeight());
            g2.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
        }
    }

    public void setImage(BufferedImage bufferedImage) {
        image = bufferedImage;
        repaint();
    }
}
