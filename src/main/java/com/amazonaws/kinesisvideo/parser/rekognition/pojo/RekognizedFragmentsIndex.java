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
import java.util.concurrent.ConcurrentHashMap;

/**
 * FragmentIndex which exposes abstract method to store and retrieve results from Rekognition output i.e Kinesis Data
 * Streams. Internally it uses ConcurrentHashMap to store the result from Rekognition. This index can be extended with
 * different data structure if required.
 *
 */
public class RekognizedFragmentsIndex {

    private final ConcurrentHashMap<String, List<RekognizedOutput>> rekognizedOutputMap = new ConcurrentHashMap<>();

    public void addToMap(String fragmentNumber, RekognizedOutput rekognizedOutput) {
        List<RekognizedOutput> rekognizedOutputs = rekognizedOutputMap.getOrDefault(fragmentNumber, new ArrayList<>());
        rekognizedOutputs.add(rekognizedOutput);
        rekognizedOutputMap.put(fragmentNumber, rekognizedOutputs);
    }

    public List<RekognizedOutput> getRekognizedOutputList(String fragmentNumber) {
        return rekognizedOutputMap.get(fragmentNumber);
    }

    public void removeFromIndex(String fragmentNumber) {
        rekognizedOutputMap.remove(fragmentNumber);
    }

    public boolean isEmpty() {
        return rekognizedOutputMap.isEmpty();
    }
}
