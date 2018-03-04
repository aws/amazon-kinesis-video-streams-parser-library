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
package com.amazonaws.kinesisvideo.parser.mkv.visitors;

import com.amazonaws.kinesisvideo.parser.TestResourceUtil;
import com.amazonaws.kinesisvideo.parser.ebml.InputStreamParserByteSource;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitException;
import com.amazonaws.kinesisvideo.parser.mkv.StreamingMkvReader;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CopyVisitorTest {

    @Test
    public void testOneCopy() throws IOException, MkvElementVisitException {
        testOneCopyForFile("clusters.mkv");
    }

    @Test
    public void testOneCopyGetMediaOutput() throws IOException, MkvElementVisitException {
        testOneCopyForFile("output_get_media.mkv");
    }

    @Test
    public void testTwoCopiesAtOnce() throws IOException, MkvElementVisitException {
        testTwoCopiesAtOnceForFile("clusters.mkv");
    }

    @Test
    public void testTwoCopiesAtOnceGetMediaOutput() throws IOException, MkvElementVisitException {
        testTwoCopiesAtOnceForFile("output_get_media.mkv");
    }

    private void testTwoCopiesAtOnceForFile(String fileName) throws IOException, MkvElementVisitException {
        byte [] inputBytes = TestResourceUtil.getTestInputByteArray(fileName);

        ByteArrayOutputStream outputStream1 = new ByteArrayOutputStream();
        ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();

        try (CopyVisitor copyVisitor1 = new CopyVisitor(outputStream1)) {
            try (CopyVisitor copyVisitor2 = new CopyVisitor(outputStream2)) {
                StreamingMkvReader.createDefault(getInputStreamParserByteSource(fileName))
                        .apply(new CompositeMkvElementVisitor(copyVisitor1, copyVisitor2));
            }
        }

        Assert.assertArrayEquals(inputBytes, outputStream1.toByteArray());
        Assert.assertArrayEquals(inputBytes, outputStream2.toByteArray());
    }

    private void testOneCopyForFile(String fileName) throws IOException, MkvElementVisitException {
        byte [] inputBytes = TestResourceUtil.getTestInputByteArray(fileName);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (CopyVisitor copyVisitor = new CopyVisitor(outputStream)) {
            StreamingMkvReader.createDefault(getInputStreamParserByteSource(fileName)).apply(copyVisitor);
        }

        Assert.assertArrayEquals(inputBytes, outputStream.toByteArray());
    }

    private InputStreamParserByteSource getInputStreamParserByteSource(String fileName) throws IOException {
        final InputStream in = TestResourceUtil.getTestInputStream(fileName);
        return new InputStreamParserByteSource(in);
    }

}
