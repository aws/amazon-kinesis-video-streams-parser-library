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
package com.amazonaws.kinesisvideo.parser.ebml;

import org.apache.commons.lang3.Validate;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * An implementation of ParserByteSource that wraps an input stream containing the EBML stream.
 * TODO: Fix exception handling in this code.
 */
public class InputStreamParserByteSource implements ParserByteSource {
    private static final int BUFFER_SIZE = 8192;
    private final BufferedInputStream bufferedInputStream;

    public InputStreamParserByteSource(InputStream inputStream) {
        this(inputStream, BUFFER_SIZE);
    }

    InputStreamParserByteSource(InputStream inputStream, int bufferSize) {
        bufferedInputStream = new BufferedInputStream(inputStream, bufferSize);
        Validate.isTrue(bufferedInputStream.markSupported());
    }


    @Override
    public int readByte() {
        try {
            return bufferedInputStream.read();
        } catch (IOException e) {
            throw new RuntimeException("Add new ParserException type");
        }
    }

    @Override
    public int available() {
        try {
            return bufferedInputStream.available();
        } catch (IOException e) {
            throw new RuntimeException("Add new ParserException type");
        }
    }

    @Override
    public int readBytes(ByteBuffer dest, int numBytes) {
        try {
            Validate.isTrue(dest.remaining() >= numBytes);
            int numBytesRead = bufferedInputStream.read(dest.array(), dest.position(), numBytes);
            if (numBytesRead > 0) {
                dest.position(dest.position() + numBytesRead);
            }

            return numBytesRead;
        } catch (IOException e) {
            throw new RuntimeException("Add new ParserException type");
        }
    }

    @Override
    public boolean eof() {
        try {
            bufferedInputStream.mark(100);
            int readByte = bufferedInputStream.read();
            if (readByte == -1) {
                return true;
            }
            bufferedInputStream.reset();
            return false;
        } catch (IOException e) {
            throw new RuntimeException("Add new ParserException type");
        }
    }
}
