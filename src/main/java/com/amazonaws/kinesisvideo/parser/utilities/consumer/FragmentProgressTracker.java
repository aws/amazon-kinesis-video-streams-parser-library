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

import com.amazonaws.kinesisvideo.parser.ebml.MkvTypeInfos;
import com.amazonaws.kinesisvideo.parser.mkv.MkvDataElement;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitException;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitor;
import com.amazonaws.kinesisvideo.parser.mkv.MkvEndMasterElement;
import com.amazonaws.kinesisvideo.parser.mkv.MkvStartMasterElement;
import com.amazonaws.kinesisvideo.parser.mkv.visitors.CompositeMkvElementVisitor;
import com.amazonaws.kinesisvideo.parser.mkv.visitors.CountVisitor;
import com.amazonaws.kinesisvideo.parser.utilities.FragmentMetadataVisitor;
import lombok.RequiredArgsConstructor;

/**
 * This class is used to track the progress in processing the output of a GetMedia call.
 *
 */
public class FragmentProgressTracker extends CompositeMkvElementVisitor {
    private final CountVisitor countVisitor;


    private FragmentProgressTracker(MkvElementVisitor processingVisitor,
            FragmentMetadataVisitor metadataVisitor,
            CountVisitor countVisitor,
            EndOfSegmentVisitor endOfSegmentVisitor) {
        super(metadataVisitor, processingVisitor, countVisitor, endOfSegmentVisitor);
        this.countVisitor = countVisitor;
    }

    public static FragmentProgressTracker create(MkvElementVisitor processingVisitor,
            FragmentMetadataCallback callback) {
        FragmentMetadataVisitor metadataVisitor = FragmentMetadataVisitor.create();
        return new FragmentProgressTracker(processingVisitor,
                metadataVisitor,
                CountVisitor.create(MkvTypeInfos.CLUSTER,
                        MkvTypeInfos.SEGMENT,
                        MkvTypeInfos.SIMPLEBLOCK,
                        MkvTypeInfos.TAG),
                new EndOfSegmentVisitor(metadataVisitor, callback));
    }

    public int getClustersCount() {
        return countVisitor.getCount(MkvTypeInfos.CLUSTER);
    }

    public int getSegmentsCount() {
        return countVisitor.getCount(MkvTypeInfos.SEGMENT);
    }

    public int getSimpleBlocksCount() {
        return countVisitor.getCount(MkvTypeInfos.SIMPLEBLOCK);
    }

    @RequiredArgsConstructor
    private static class EndOfSegmentVisitor extends MkvElementVisitor {
        private final FragmentMetadataVisitor metadataVisitor;
        private final FragmentMetadataCallback endOfFragmentCallback;

        @Override
        public void visit(MkvStartMasterElement startMasterElement) throws MkvElementVisitException {

        }

        @Override
        public void visit(MkvEndMasterElement endMasterElement) throws MkvElementVisitException {
            if (MkvTypeInfos.SEGMENT.equals(endMasterElement.getElementMetaData().getTypeInfo())) {
                metadataVisitor.getCurrentFragmentMetadata().ifPresent(endOfFragmentCallback::call);
            }
        }

        @Override
        public void visit(MkvDataElement dataElement) throws MkvElementVisitException {
        }
    }

}
