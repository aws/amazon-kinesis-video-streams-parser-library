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
package com.amazonaws.kinesisvideo.parser.utilities.consumer;

import com.amazonaws.kinesisvideo.parser.ebml.InputStreamParserByteSource;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitException;
import com.amazonaws.kinesisvideo.parser.mkv.StreamingMkvReader;
import com.amazonaws.kinesisvideo.parser.mkv.visitors.CompositeMkvElementVisitor;
import com.amazonaws.kinesisvideo.parser.utilities.OutputSegmentMerger;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class merges consecutive mkv streams and pipes the merged stream to the stdin of a child process.
 * It is meant to be used to pipe the output of a GetMedia* call to a processing application that can not deal
 * with having multiple consecutive mkv streams. Gstreamer is one such application that requires a merged stream.
 * A merged stream is where the consecutive mkv streams are merged as long as they share the same track
 * and EBML information and the cluster level timecodes in those streams keep increasing.
 * If a non-matching mkv stream is detected, the piper stops.
 */

@RequiredArgsConstructor
public class MergedOutputPiper extends GetMediaResponseStreamConsumer {
    /**
     * The process builder to create the child proccess to which the merged output
     */
    private final ProcessBuilder childProcessBuilder;


    private OutputSegmentMerger merger;
    private Process targetProcess;

    @Override
    public void process(final InputStream inputStream, FragmentMetadataCallback endOfFragmentCallback)
            throws MkvElementVisitException, IOException {
        targetProcess = childProcessBuilder.start();
        try (OutputStream os = targetProcess.getOutputStream()) {
            merger = OutputSegmentMerger.createToStopAtFirstNonMatchingSegment(os);
            processWithFragmentEndCallbacks(inputStream, endOfFragmentCallback, merger);
        }
    }

    /**
     * Get the number of segments that were merged by the piper.
     * If the merger is done because the last segment it read cannot be merged, then the number of merged segments
     * is the number of segments read minus the last segment.
     * If the merger is not done then the number of merged segments is the number of read segments.
     * @return
     */
    public int getMergedSegments() {
        if (merger.isDone()) {
            return merger.getSegmentsCount() - 1;
        } else {
            return merger.getSegmentsCount();
        }
    }

    @Override
    public void close() {
        targetProcess.destroyForcibly();
    }
}
