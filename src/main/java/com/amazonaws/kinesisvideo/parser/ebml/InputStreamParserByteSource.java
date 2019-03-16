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
 */
public class InputStreamParserByteSource implements ParserByteSource {
    private static final int BUFFER_SIZE = 8192;
    private static final int MARK_SIZE = 100;
    private final BufferedInputStream bufferedInputStream;

    public InputStreamParserByteSource(final InputStream inputStream) {
        this(inputStream, BUFFER_SIZE);
    }

    InputStreamParserByteSource(final InputStream inputStream, final int bufferSize) {
        bufferedInputStream = new BufferedInputStream(inputStream, bufferSize);
        Validate.isTrue(bufferedInputStream.markSupported());
    }


    @Override
    public int readByte() {
        try {
            return bufferedInputStream.read();
        } catch (final IOException e) {
            throw new RuntimeException("Exception while reading byte from input stream!", e);
        }
    }

    @Override
    public int available() {
        try {
            return bufferedInputStream.available();
        } catch (final IOException e) {
            throw new RuntimeException("Exception while getting available bytes from input stream!", e);
        }
    }

    @Override
    public int readBytes(final ByteBuffer dest, final int numBytes) {
        try {
            Validate.isTrue(dest.remaining() >= numBytes);
            final int numBytesRead = bufferedInputStream.read(dest.array(), dest.position(), numBytes);
            if (numBytesRead > 0) {
                dest.position(dest.position() + numBytesRead);
            }

            return numBytesRead;
        } catch (final IOException e) {
            throw new RuntimeException("Exception while reading bytes from input stream!", e);
        }
    }

    @Override
    public boolean eof() {
        try {
            bufferedInputStream.mark(MARK_SIZE);
            if (readByte() == -1) {
                return true;
            }
            bufferedInputStream.reset();
            return false;
        } catch (final IOException e) {
            throw new RuntimeException("Exception while resetting input stream!", e);
        }
    }
}
