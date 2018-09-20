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

import com.amazonaws.kinesisvideo.parser.TestResourceUtil;
import com.amazonaws.kinesisvideo.parser.ebml.InputStreamParserByteSource;
import com.amazonaws.kinesisvideo.parser.ebml.MkvTypeInfos;
import com.amazonaws.kinesisvideo.parser.mkv.MkvEndMasterElement;
import com.amazonaws.kinesisvideo.parser.mkv.StreamingMkvReader;
import com.amazonaws.kinesisvideo.parser.mkv.visitors.CompositeMkvElementVisitor;
import com.amazonaws.kinesisvideo.parser.mkv.Frame;
import com.amazonaws.kinesisvideo.parser.mkv.MkvDataElement;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElement;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitException;
import com.amazonaws.kinesisvideo.parser.mkv.MkvValue;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Test class to test {@link FragmentMetadataVisitor}.
 */

public class FragmentMetadataVisitorTest {

    @Test
    public void basicTest() throws IOException, MkvElementVisitException {
        final InputStream in = TestResourceUtil.getTestInputStream("output_get_media.mkv");
        List<String> continuationTokens = new ArrayList<>();
        continuationTokens.add("91343852333181432392682062607743920146159169392");
        continuationTokens.add("91343852333181432397633822764885441725874549018");
        continuationTokens.add("91343852333181432402585582922026963247510532162");

        final FragmentMetadataVisitor fragmentVisitor = FragmentMetadataVisitor.create();
        StreamingMkvReader mkvStreamReader =
                StreamingMkvReader.createDefault(new InputStreamParserByteSource(in));
        int segmentCount = 0;
        while(mkvStreamReader.mightHaveNext()) {
            Optional<MkvElement> mkvElement = mkvStreamReader.nextIfAvailable();
            if (mkvElement.isPresent()) {
                mkvElement.get().accept(fragmentVisitor);
                if (MkvTypeInfos.SIMPLEBLOCK.equals(mkvElement.get().getElementMetaData().getTypeInfo())) {
                    MkvDataElement dataElement = (MkvDataElement) mkvElement.get();
                    Frame frame = ((MkvValue<Frame>)dataElement.getValueCopy()).getVal();
                    MkvTrackMetadata trackMetadata = fragmentVisitor.getMkvTrackMetadata(frame.getTrackNumber());
                    assertTrackAndFragmentInfo(fragmentVisitor, frame, trackMetadata);
                }
                if (MkvTypeInfos.SEGMENT.equals(mkvElement.get().getElementMetaData().getTypeInfo())) {
                    if (mkvElement.get() instanceof MkvEndMasterElement) {
                        if (segmentCount < continuationTokens.size()) {
                            Optional<String> continuationToken = fragmentVisitor.getContinuationToken();
                            Assert.assertTrue(continuationToken.isPresent());
                            Assert.assertEquals(continuationTokens.get(segmentCount), continuationToken.get());
                        }
                        segmentCount++;
                    }
                }
            }

        }
    }

    private void assertTrackAndFragmentInfo(FragmentMetadataVisitor fragmentVisitor,
            Frame frame,
            MkvTrackMetadata trackMetadata) {
        Assert.assertEquals(frame.getTrackNumber(), trackMetadata.getTrackNumber().longValue());
        Assert.assertEquals(360L, trackMetadata.getPixelHeight().get().longValue());
        Assert.assertEquals(640L, trackMetadata.getPixelWidth().get().longValue());
        Assert.assertEquals("V_MPEG4/ISO/AVC", trackMetadata.getCodecId());
        Assert.assertTrue(fragmentVisitor.getCurrentFragmentMetadata().isPresent());
        Assert.assertTrue(fragmentVisitor.getCurrentFragmentMetadata().get().isSuccess());
        Assert.assertEquals(0, fragmentVisitor.getCurrentFragmentMetadata().get().getErrorId());
        Assert.assertNull(fragmentVisitor.getCurrentFragmentMetadata().get().getErrorCode());
        if (fragmentVisitor.getPreviousFragmentMetadata().isPresent()) {
            Assert.assertTrue(fragmentVisitor.getPreviousFragmentMetadata()
                    .get()
                    .getFragmentNumber()
                    .compareTo(fragmentVisitor.getCurrentFragmentMetadata().get().getFragmentNumber())
                    < 0);
        }
    }

    @Test
    public void withOutputSegmentMergerTest() throws IOException, MkvElementVisitException {
        final FragmentMetadataVisitor fragmentVisitor = FragmentMetadataVisitor.create();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        OutputSegmentMerger outputSegmentMerger =
                OutputSegmentMerger.createDefault(outputStream);

        CompositeMkvElementVisitor compositeVisitor =
                new TestCompositeVisitor(fragmentVisitor, outputSegmentMerger);
        final InputStream in = TestResourceUtil.getTestInputStream("output_get_media.mkv");

        StreamingMkvReader mkvStreamReader =
                StreamingMkvReader.createDefault(new InputStreamParserByteSource(in));
        while (mkvStreamReader.mightHaveNext()) {
            Optional<MkvElement> mkvElement = mkvStreamReader.nextIfAvailable();
            if (mkvElement.isPresent()) {
                mkvElement.get().accept(compositeVisitor);
                if (MkvTypeInfos.SIMPLEBLOCK.equals(mkvElement.get().getElementMetaData().getTypeInfo())) {
                    MkvDataElement dataElement = (MkvDataElement) mkvElement.get();
                    Frame frame = ((MkvValue<Frame>) dataElement.getValueCopy()).getVal();
                    Assert.assertTrue(frame.getFrameData().limit() > 0);
                    MkvTrackMetadata trackMetadata = fragmentVisitor.getMkvTrackMetadata(frame.getTrackNumber());
                    assertTrackAndFragmentInfo(fragmentVisitor, frame, trackMetadata);
                }
            }
        }

    }
    
    @Test
    public void testFragmentNumbers_NoClusterData() throws IOException, MkvElementVisitException {
        final FragmentMetadataVisitor fragmentVisitor = FragmentMetadataVisitor.create();
        String testFile = "empty-mkv-with-tags.mkv";        
        Set<String> expectedFragmentNumbers = new HashSet<>(
                Arrays.asList(
                        "91343852338378294813695855977007281634605393997"
                        ));
        
        final InputStream inputStream = TestResourceUtil.getTestInputStream(testFile);
        Set<String> visitedFragmentNumbers = new HashSet<>(); 
        StreamingMkvReader mkvStreamReader =
                StreamingMkvReader.createDefault(new InputStreamParserByteSource(inputStream));
        while (mkvStreamReader.mightHaveNext()) {
            Optional<MkvElement> mkvElement = mkvStreamReader.nextIfAvailable();
            if (mkvElement.isPresent()) {
                mkvElement.get().accept(fragmentVisitor);
                Optional<FragmentMetadata> fragmentMetadata = fragmentVisitor.getCurrentFragmentMetadata();
                if (fragmentMetadata.isPresent()) {
                    String fragmentNumber = fragmentMetadata.get().getFragmentNumberString();
                    visitedFragmentNumbers.add(fragmentNumber);
                }
            }
        }
        
        Assert.assertEquals(expectedFragmentNumbers, visitedFragmentNumbers);
    }

    @Test
    public void testFragmentMetadata_NoFragementMetadata_withWebm() throws IOException, MkvElementVisitException {
        final FragmentMetadataVisitor fragmentMetadataVisitor = FragmentMetadataVisitor.create();
        final String testFile = "big-buck-bunny_trailer.webm";
        int metadataCount = 0;
        final StreamingMkvReader mkvStreamReader =  StreamingMkvReader
                        .createDefault(new InputStreamParserByteSource(TestResourceUtil.getTestInputStream(testFile)));
        while (mkvStreamReader.mightHaveNext()) {
            Optional<MkvElement> mkvElement = mkvStreamReader.nextIfAvailable();
            if (mkvElement.isPresent()) {
                mkvElement.get().accept(fragmentMetadataVisitor);
                Optional<FragmentMetadata> fragmentMetadata = fragmentMetadataVisitor.getCurrentFragmentMetadata();
                if(fragmentMetadata.isPresent()) {
                    metadataCount ++;
                }
            }
        }
        Assert.assertEquals(0, metadataCount);
    }

    @Test
    public void testFragmentMetadata_NoFragementMetadata_withMkv() throws IOException, MkvElementVisitException {
        final FragmentMetadataVisitor fragmentMetadataVisitor = FragmentMetadataVisitor.create();
        final String testFile = "clusters.mkv";
        int metadataCount = 0;
        final StreamingMkvReader mkvStreamReader =  StreamingMkvReader
                .createDefault(new InputStreamParserByteSource(TestResourceUtil.getTestInputStream(testFile)));
        while (mkvStreamReader.mightHaveNext()) {
            Optional<MkvElement> mkvElement = mkvStreamReader.nextIfAvailable();
            if (mkvElement.isPresent()) {
                mkvElement.get().accept(fragmentMetadataVisitor);
                Optional<FragmentMetadata> fragmentMetadata = fragmentMetadataVisitor.getCurrentFragmentMetadata();
                if(fragmentMetadata.isPresent()) {
                    metadataCount ++;
                }
            }
        }
        Assert.assertEquals(0, metadataCount);
    }

    private static class TestCompositeVisitor extends CompositeMkvElementVisitor {
        public TestCompositeVisitor(FragmentMetadataVisitor fragmentMetadataVisitor, OutputSegmentMerger merger) {
            super(fragmentMetadataVisitor, merger);
        }
    }
}
