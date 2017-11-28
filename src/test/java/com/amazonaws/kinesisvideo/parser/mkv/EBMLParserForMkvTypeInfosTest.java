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
import com.amazonaws.kinesisvideo.parser.ebml.EBMLParser;
import com.amazonaws.kinesisvideo.parser.ebml.InputStreamParserByteSource;
import com.amazonaws.kinesisvideo.parser.ebml.TestEBMLParserCallback;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * Test to see that the elements in a typical input mkv file are recogized by the {@link MkvTypeInfoProvider}.
 */
public class EBMLParserForMkvTypeInfosTest {
    private EBMLParser parser;
    private TestEBMLParserCallback parserCallback;


    @Before
    public void setup() throws IllegalAccessException {
        parserCallback = new TestEBMLParserCallback();
        MkvTypeInfoProvider typeInfoProvider = new  MkvTypeInfoProvider();
        typeInfoProvider.load();

        parser = new EBMLParser(typeInfoProvider, parserCallback);
    }

    @Test
    public void testClustersMkv() throws IOException {
        final String fileName = "clusters.mkv";
        final InputStream in = TestResourceUtil.getTestInputStream(fileName);

        InputStreamParserByteSource parserByteSource = new InputStreamParserByteSource(in);

        while(!parserByteSource.eof()) {
            parser.parse(parserByteSource);
        }
        parser.closeParser();
    }
}
