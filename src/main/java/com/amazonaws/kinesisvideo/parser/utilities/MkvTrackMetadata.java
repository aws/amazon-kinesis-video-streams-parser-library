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

import com.amazonaws.kinesisvideo.parser.mkv.MkvElement;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Class that captures the meta-data for a particular track in the mkv response.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@ToString(exclude = {"codecPrivateData", "allElementsInTrack"})
public class MkvTrackMetadata {
    private BigInteger trackNumber;
    @Builder.Default
    private Optional<BigInteger> trackUID = Optional.empty();
    @Builder.Default
    private String trackName = "";

    @Builder.Default
    private String codecId = "";
    @Builder.Default
    private String codecName = "";
    private ByteBuffer codecPrivateData;

    @Builder.Default
    private Optional<BigInteger> pixelWidth = Optional.empty();
    @Builder.Default
    private Optional<BigInteger> pixelHeight = Optional.empty();

    private List<MkvElement> allElementsInTrack;
}
