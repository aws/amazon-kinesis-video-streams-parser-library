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
package com.amazonaws.kinesisvideo.parser.rekognition.pojo;

import java.awt.Color;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum which lists down the sample types of the faces detected in given frame. This list can be expanded
 * based on the face type given in external image id while creating face collection.
 *
 * For more information please refer
 * https://docs.aws.amazon.com/rekognition/latest/dg/add-faces-to-collection-procedure.html
 */
@Getter
@RequiredArgsConstructor
public enum FaceType {
    TRUSTED (Color.GREEN, "Trusted"),
    CRIMINAL (Color.RED, "Criminal"),
    UNKNOWN (Color.YELLOW, "Unknown"),
    NOT_RECOGNIZED (Color.PINK, "NotRecognized"),
    ALL (Color.BLACK, "All");

    private final Color color;
    private final String prefix;

    public static FaceType fromString(String value) {
        for (int i = 0; i < FaceType.values().length; i++) {
            if(FaceType.values()[i].getPrefix().toUpperCase().equals(value.toUpperCase()))
                return FaceType.values()[i];
        }
        return FaceType.UNKNOWN;
    }
}
