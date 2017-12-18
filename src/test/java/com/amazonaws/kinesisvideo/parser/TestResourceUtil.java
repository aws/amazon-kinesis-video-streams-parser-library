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
package com.amazonaws.kinesisvideo.parser;

import org.apache.commons.lang3.Validate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.nio.file.Files.newInputStream;

/**
 * Class used by the test classes to get acess to input test files
 */
public class TestResourceUtil {
    public static InputStream getTestInputStream(String name) throws IOException {
        //First check if we are running in maven.
        InputStream inputStream = ClassLoader.getSystemResourceAsStream(name);
        if (inputStream == null) {
            inputStream = newInputStream(Paths.get("testdata", name));
        }
        Validate.isTrue(inputStream != null, "Could not read input file " + name);
        return inputStream;

    }

    public static byte[] getTestInputByteArray(String name) throws IOException {
        //First check if we are running in maven.
        InputStream inputStream = ClassLoader.getSystemResourceAsStream(name);
        if (inputStream == null) {
            inputStream = Files.newInputStream(Paths.get("testdata", name));
        }
        Validate.isTrue(inputStream != null, "Could not read input file " + name);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte []buf = new byte [8192];

        int readBytes;
        do {
            readBytes = inputStream.read(buf);
            if (readBytes > 0) {
                outputStream.write(buf, 0, readBytes);
            }
        }while (readBytes >= 0);

        return outputStream.toByteArray();
    }

    /**
     * Utility debug method to print the class path.
     * Useful for verifying test setup in different build systems.
     */
    public static void printClassPath() {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        URL[] urls = ((URLClassLoader)cl).getURLs();

        for(URL url: urls){
            System.out.println(url.getFile());
        }
    }
}
