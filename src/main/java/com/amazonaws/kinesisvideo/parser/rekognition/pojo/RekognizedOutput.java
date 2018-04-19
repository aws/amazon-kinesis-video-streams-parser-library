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

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Builder
@Getter
@ToString
public class RekognizedOutput {

    private String fragmentNumber;
    private Double frameOffsetInSeconds;
    private Double serverTimestamp;
    private Double producerTimestamp;

    @Setter
    private String faceId;
    private double detectedTime;
    @Builder.Default
    private List<FaceSearchOutput> faceSearchOutputs = new ArrayList<>();

    public void addFaceSearchOutput(FaceSearchOutput faceSearchOutput) {
        this.faceSearchOutputs.add(faceSearchOutput);
    }

    @Getter
    @Builder
    @ToString
    public static class FaceSearchOutput {

        private DetectedFace detectedFace;
        @Builder.Default
        private List<MatchedFace> matchedFaceList = new ArrayList<>();
    }
}
