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

import javax.annotation.Nonnull;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public final class BufferedImageUtil {
    private static final int DEFAULT_FONT_SIZE = 13;
    private static final Font DEFAULT_FONT = new Font(null, Font.CENTER_BASELINE, DEFAULT_FONT_SIZE);

    public static void addTextToImage(@Nonnull BufferedImage bufferedImage, String text, int pixelX, int pixelY) {
        Graphics graphics = bufferedImage.getGraphics();
        graphics.setColor(Color.YELLOW);
        graphics.setFont(DEFAULT_FONT);
        for (String line : text.split(MkvTag.class.getSimpleName())) {
            graphics.drawString(line, pixelX, pixelY += graphics.getFontMetrics().getHeight());
        }
    }
}
