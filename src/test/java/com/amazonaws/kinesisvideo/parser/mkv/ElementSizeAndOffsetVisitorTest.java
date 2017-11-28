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
import com.amazonaws.kinesisvideo.parser.ebml.InputStreamParserByteSource;
import com.amazonaws.kinesisvideo.parser.mkv.visitors.ElementSizeAndOffsetVisitor;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Test for ElementSizeAndOffsetVisitor.
 */
public class ElementSizeAndOffsetVisitorTest {
    @Test
    public void basicTest() throws IOException, MkvElementVisitException {
        final String fileName = "clusters.mkv";
        final InputStream in = TestResourceUtil.getTestInputStream(fileName);

        StreamingMkvReader offsetReader =
                new StreamingMkvReader(false, new ArrayList<>(), new InputStreamParserByteSource(in));

        Path tmpFilePath =  Files.createTempFile("basicTest:"+fileName+":","offset");
        try (BufferedWriter writer = Files.newBufferedWriter(tmpFilePath,
                StandardCharsets.US_ASCII,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE)) {
            ElementSizeAndOffsetVisitor offsetVisitor = new ElementSizeAndOffsetVisitor(writer);

            while(offsetReader.mightHaveNext()) {
                Optional<MkvElement> mkvElement = offsetReader.nextIfAvailable();
                if (mkvElement.isPresent()) {
                    mkvElement.get().accept(offsetVisitor);
                }
            }
        } finally {
            Files.delete(tmpFilePath);
        }
    }
}
