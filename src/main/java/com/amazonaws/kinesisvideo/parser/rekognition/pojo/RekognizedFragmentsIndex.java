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
import java.util.concurrent.ConcurrentLinkedQueue;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Index which stores results for each fragment number from Rekognition output i.e Kinesis Data Streams.
 * It normalizes each kinesis event record (which is for every sampled frame of a fragment) and stores
 * per fragment number in memory. Rekognition output can be mapped to the KVS fragments using either real-time
 * GetMedia or archived GetMediaForFragmentList call. So internally RekognizedFragmentsIndex uses two different data
 * structures for this reason.
 *
 * 1. ConcurrentLinkedQueue: Rekognition output is stored in LinkedQueue as soon as it's retrieved from Kinesis
 * Data Streams. This can be used to integrate with KVS GetMediaForFragmentList API, as the caller gets the fragment
 * number from Kinesis Data Streams. So the caller needs Rekognition output and fragment number in FIFO order which
 * is achieved by this Queue.
 * 2. ConcurrentHashMap: Rekognition output is stored as the value with the corresponding fragment number as the key
 * for the hash map. This is used while integrating with KVS GetMedia API, as the caller gets the fragment number
 * from real-time fragments retrieved. So the index needs an efficient search mechanism to search the Rekognition
 * outputs for a given fragment number. Searching in the above LinkedDeque becomes expensive as the number of items
 * stored in it increases if none of the items are processed. So this hash map serves as the index for the queue for
 * fast retrieval O(1) compared to linear search O(N).
 *
 */
@Slf4j
@ToString
public class RekognizedFragmentsIndex {

    private final ConcurrentHashMap<String, RekognizedFragment> rekognizedOutputMap = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<RekognizedFragment> rekognizedOutputQueue = new ConcurrentLinkedQueue<>();

    /**
     * Add Rekognized output to the index for a fragment number and its other attributes like producer time,
     * server time etc.
     *
     * @param fragmentNumber Fragment Number of the fragment
     * @param producerTime Producer time of the fragment
     * @param serverTime Server time of the fragment
     * @param rekognizedOutput Rekognition output of the fragment
     */
    public synchronized void add(final String fragmentNumber, final Long producerTime, final Long serverTime,
                                 final RekognizedOutput rekognizedOutput) {

        if (rekognizedOutputMap.containsKey(fragmentNumber)) {
            final RekognizedFragment rekognizedFragment = rekognizedOutputMap.get(fragmentNumber);
            rekognizedFragment.addRekognizedOutput(rekognizedOutput);
        } else {
            final RekognizedFragment rekognizedFragment =
                    new RekognizedFragment(fragmentNumber, producerTime, serverTime);
            rekognizedFragment.addRekognizedOutput(rekognizedOutput);
            rekognizedOutputQueue.add(rekognizedFragment);
            rekognizedOutputMap.put(fragmentNumber, rekognizedFragment);
        }
        log.debug("Added rekognized fragment number {} to the index.", fragmentNumber);
    }

    /**
     * Polls the index for first available rekognized fragment.
     *
     * @return RekognizedFragment if exists. If not returns null.
     */
    public synchronized RekognizedFragment poll() {

        final RekognizedFragment rekognizedFragment = rekognizedOutputQueue.poll();
        rekognizedOutputMap.remove(rekognizedFragment.getFragmentNumber());
        return rekognizedFragment;
    }

    public int size() {
        log.debug("Rekognized index Map size : {} queue size : {}",
                rekognizedOutputMap.size(), rekognizedOutputQueue.size());
        if (rekognizedOutputMap.size() != rekognizedOutputQueue.size()) {
            throw new IllegalStateException("RekognizedFragmentsIndex map and queue size doesn't match");
        }
        return this.rekognizedOutputQueue.size();
    }

    /**
     * Checks the index for any available rekognized fragment.
     *
     * @return true if exists. false otherwise.
     */
    public synchronized boolean isEmpty() {
        return rekognizedOutputQueue.isEmpty();
    }

    /**
     * Gets the list of Rekognized Output for the given fragment number.
     *
     * @param fragmentNumber Input fragment number.
     * @return List of rekognized outputs if exists. null otherwise.
     */
    public synchronized List<RekognizedOutput> getRekognizedOutputList(final String fragmentNumber) {
        return (rekognizedOutputMap.containsKey(fragmentNumber))
                ? rekognizedOutputMap.get(fragmentNumber).getRekognizedOutputs()
                : null;
    }

    /**
     * Removes the rekognized fragment from the index for the given fragment number.
     *
     * @param fragmentNumber Input fragment number.
     */
    public synchronized void remove(final String fragmentNumber) {
        if (rekognizedOutputMap.containsKey(fragmentNumber)) {
            final RekognizedFragment rekognizedFragment = rekognizedOutputMap.remove(fragmentNumber);
            rekognizedOutputQueue.remove(rekognizedFragment);
        }
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    @RequiredArgsConstructor
    public static class RekognizedFragment {
        private final String fragmentNumber;
        private final Long producerTime;
        private final Long serverTime;
        private final List<RekognizedOutput> rekognizedOutputs = new ArrayList<>();

        public void addRekognizedOutput(final RekognizedOutput rekognizedOutput) {
            this.rekognizedOutputs.add(rekognizedOutput);
        }
    }
}
