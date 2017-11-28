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
package com.amazonaws.kinesisvideo.parser.mkv;



import com.amazonaws.kinesisvideo.parser.TestResourceUtil;
import com.amazonaws.kinesisvideo.parser.ebml.EBMLTypeInfo;
import com.amazonaws.kinesisvideo.parser.ebml.InputStreamParserByteSource;
import com.amazonaws.kinesisvideo.parser.ebml.MkvTypeInfos;

import com.amazonaws.kinesisvideo.parser.mkv.visitors.CompositeMkvElementVisitor;
import com.amazonaws.kinesisvideo.parser.mkv.visitors.CountVisitor;
import com.amazonaws.kinesisvideo.parser.mkv.visitors.ElementSizeAndOffsetVisitor;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Tests for {@link StreamingMkvReader}.
 */
public class StreamingMkvReaderTest {

    @Test
    public void testClustersMkvAllElementsWithoutPath() throws IOException, MkvElementVisitException {
        InputStreamParserByteSource parserByteSource = getClustersByteSource();

        StreamingMkvReader streamReader =
                new StreamingMkvReader(false, new ArrayList<>(), parserByteSource);

        CountVisitor visitor = readAllReturnedElements(streamReader);
        assertCountsOfTypes(visitor, 1, 5, 300, 3);
    }

    @Test
    public void testClustersMkvAllElementsWithPath() throws IOException, MkvElementVisitException {
        InputStreamParserByteSource parserByteSource = getClustersByteSource();

        StreamingMkvReader streamReader = StreamingMkvReader.createDefault(parserByteSource);

        CountVisitor visitor = readAllReturnedElements(streamReader);

        assertCountsOfTypes(visitor, 1, 5, 300, 3);
    }

    private void assertCountsOfTypes(CountVisitor visitor,
            int numSegments,
            int numClusters,
            int numFrames,
            int tagsPerSegment) {
        Assert.assertEquals(numSegments, visitor.getCount(MkvTypeInfos.EBML));
        Assert.assertEquals(numSegments, visitor.getCount(MkvTypeInfos.SEGMENT));
        Assert.assertEquals(numClusters, visitor.getCount(MkvTypeInfos.CLUSTER));
        Assert.assertEquals(numClusters, visitor.getCount(MkvTypeInfos.TIMECODE));
        Assert.assertEquals(numSegments, visitor.getCount(MkvTypeInfos.TIMECODESCALE));
        Assert.assertEquals(0, visitor.getCount(MkvTypeInfos.DURATION));
        Assert.assertEquals(numFrames, visitor.getCount(MkvTypeInfos.SIMPLEBLOCK));
        Assert.assertEquals(numSegments, visitor.getCount(MkvTypeInfos.TRACKS));
        Assert.assertEquals(numSegments, visitor.getCount(MkvTypeInfos.TRACKNUMBER));
        Assert.assertEquals(numSegments*tagsPerSegment, visitor.getCount(MkvTypeInfos.TAG));
    }


    @Test
    public void testGetDataOutputMkvTagNameWithPathFileWrite() throws IOException {
        InputStreamParserByteSource parserByteSource = getInputStreamParserByteSource("output_get_media.mkv");

        List<EBMLTypeInfo> mkvTypeInfosToRead = new ArrayList<>();
        mkvTypeInfosToRead.add(MkvTypeInfos.TAGNAME);

        StreamingMkvReader streamReader =
                new StreamingMkvReader(true, mkvTypeInfosToRead, parserByteSource);

        Path tmpFilePath = Files.createTempFile("StreamingMkvOutputGetMedia","output.txt");
        int count = 0;
        try (BufferedWriter writer = Files.newBufferedWriter(tmpFilePath,
                StandardCharsets.US_ASCII,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE)) {
            while (streamReader.mightHaveNext()) {
                Optional<MkvElement> mkvElement = streamReader.nextIfAvailable();
                if (mkvElement.isPresent()) {
                    if (mkvElement.get().getClass().equals(MkvDataElement.class)) {
                        count++;
                    }
                    String elementString = mkvElement.toString();
                    writer.write(elementString, 0, elementString.length());
                    writer.newLine();
                }
            }
        } finally {
            Files.delete(tmpFilePath);
        }
        Assert.assertEquals(5*12, count);
    }

    @Test
    public void testGetDataOutputMkvTagNameWithPath() throws IOException {
        InputStreamParserByteSource parserByteSource = getInputStreamParserByteSource("output_get_media.mkv");

        List<EBMLTypeInfo> mkvTypeInfosToRead = new ArrayList<>();
        mkvTypeInfosToRead.add(MkvTypeInfos.TAGNAME);

        StreamingMkvReader streamReader =
                new StreamingMkvReader(true, mkvTypeInfosToRead, parserByteSource);

        int count = 0;
        while (streamReader.mightHaveNext()) {
            Optional<MkvElement> mkvElement = streamReader.nextIfAvailable();
            if (mkvElement.isPresent()) {
                if (mkvElement.get().getClass().equals(MkvDataElement.class)) {
                    count++;
                }
            }
        }
        Assert.assertEquals(5 * 12, count);
    }

    @Test
    public void testGetDataOutputMkvAllElementsWithPath() throws IOException, MkvElementVisitException {
        InputStreamParserByteSource parserByteSource = getInputStreamParserByteSource("output_get_media.mkv");

        StreamingMkvReader streamReader = StreamingMkvReader.createDefault(parserByteSource);

        CountVisitor visitor = readAllReturnedElements(streamReader);

        assertCountsOfTypes(visitor, 5, 5, 300, 5);
    }


    @Test
    public void testClustersMkvSimpleBlockWithPath() throws IOException, MkvElementVisitException {
        InputStreamParserByteSource parserByteSource = getClustersByteSource();
        List<EBMLTypeInfo>  mkvTypeInfosToRead = new ArrayList<>();
        mkvTypeInfosToRead.add(MkvTypeInfos.SIMPLEBLOCK);
        StreamingMkvReader streamReader =
                new StreamingMkvReader(true, mkvTypeInfosToRead, parserByteSource);

        CountVisitor visitor = readAllReturnedElements(streamReader);
        Assert.assertEquals(300, visitor.getCount(MkvTypeInfos.SIMPLEBLOCK));
    }

    @Test
    public void testClustersMkvIdAndOffset() throws IOException, MkvElementVisitException {
        InputStreamParserByteSource parserByteSource = getClustersByteSource();

        StreamingMkvReader streamReader = StreamingMkvReader.createDefault(parserByteSource);

        Path tempOutputFile = Files.createTempFile("StreamingMkvClusters","offset.txt");
        try (BufferedWriter writer = Files.newBufferedWriter(tempOutputFile,
                StandardCharsets.US_ASCII,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE)) {
            ElementSizeAndOffsetVisitor offsetVisitor = new ElementSizeAndOffsetVisitor(writer);
            while (streamReader.mightHaveNext()) {
                Optional<MkvElement> mkvElement = streamReader.nextIfAvailable();
                if (mkvElement.isPresent()) {
                    mkvElement.get().accept(offsetVisitor);
                }
            }
        } finally {
            //Comment this out if the test fails and you need the partial output
            Files.delete(tempOutputFile);
        }
    }


    private CountVisitor readAllReturnedElements(StreamingMkvReader streamReader)
            throws MkvElementVisitException {
        List<EBMLTypeInfo> typeInfosToRead = new ArrayList<>();
        typeInfosToRead.add(MkvTypeInfos.EBML);
        typeInfosToRead.add(MkvTypeInfos.SEGMENT);
        typeInfosToRead.add(MkvTypeInfos.CLUSTER);
        typeInfosToRead.add(MkvTypeInfos.TIMECODE);
        typeInfosToRead.add(MkvTypeInfos.TIMECODESCALE);
        typeInfosToRead.add(MkvTypeInfos.SIMPLEBLOCK);
        typeInfosToRead.add(MkvTypeInfos.TAGS);
        typeInfosToRead.add(MkvTypeInfos.TAG);
        typeInfosToRead.add(MkvTypeInfos.TRACKS);
        typeInfosToRead.add(MkvTypeInfos.TRACKNUMBER);

        CountVisitor countVisitor = new CountVisitor(typeInfosToRead);
        CompositeMkvElementVisitor compositeTestVisitor =
                new CompositeMkvElementVisitor(countVisitor, new TestDataElementVisitor());

        while(streamReader.mightHaveNext()) {
            Optional<MkvElement> mkvElement = streamReader.nextIfAvailable();
            if(mkvElement.isPresent()) {
                mkvElement.get().accept(compositeTestVisitor);
            }
        }
        return countVisitor;
    }

    private static class TestDataElementVisitor extends MkvElementVisitor {
        private Optional <MkvDataElement> previousDataElement = Optional.empty();

        @Override
        public void visit(MkvStartMasterElement startMasterElement) throws MkvElementVisitException {
            assertNullPreviousDataElement();
        }

        @Override
        public void visit(MkvEndMasterElement endMasterElement) throws MkvElementVisitException {
            assertNullPreviousDataElement();
        }

        @Override
        public void visit(MkvDataElement dataElement) throws MkvElementVisitException {
            assertNullPreviousDataElement();
            Assert.assertFalse(dataElement.isMaster());

            Assert.assertNotNull(dataElement);
            Assert.assertNotNull(dataElement.getDataBuffer());
            Assert.assertEquals(dataElement.getDataSize(), dataElement.getDataBuffer().limit());
            previousDataElement = Optional.of(dataElement);
        }

        private void assertNullPreviousDataElement() {
            if (previousDataElement.isPresent()) {
                Assert.assertNull(previousDataElement.get().getDataBuffer());
            }
        }

    }

    private InputStreamParserByteSource getClustersByteSource() throws IOException {
        final String fileName = "clusters.mkv";
        return getInputStreamParserByteSource(fileName);
    }

    private InputStreamParserByteSource getInputStreamParserByteSource(String fileName) throws IOException {
        final InputStream in = TestResourceUtil.getTestInputStream(fileName);
        return new InputStreamParserByteSource(in);
    }

}
