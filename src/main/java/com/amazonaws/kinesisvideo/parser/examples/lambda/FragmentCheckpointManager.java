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
package com.amazonaws.kinesisvideo.parser.examples.lambda;

import java.util.Optional;

/**
 * FragmentCheckpoint Manager interface which manages the checkpoints for last processed fragments.
 */
public interface FragmentCheckpointManager {
    /**
     * Get last processed fragment details from checkpoint for given stream name.
     *
     * @param streamName KVS Stream name
     * @return Optional of last processed fragment item if checkpoint exists. Empty otherwise
     */
    Optional<FragmentCheckpoint> getLastProcessedItem(String streamName);

    /**
     * Save last processed fragment details checkpoint for the given stream name.
     *
     * @param streamName KVS Stream name
     * @param fragmentNumber Last processed fragment's fragment number
     * @param producerTime Last processed fragment's producer time
     * @param serverTime Last processed fragment's server time
     */
    void saveCheckPoint(String streamName, String fragmentNumber, Long producerTime, Long serverTime);
}
