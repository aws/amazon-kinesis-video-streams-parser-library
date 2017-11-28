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

import java.nio.ByteBuffer;

/**
 * Buffer used to replay the id and size of ebml elements in the ebml parser
 */
class ReplayIdAndSizeBuffer {
    private int count;
    private final byte[] buffer;
    private long startingOffset;

    ReplayIdAndSizeBuffer(int length) {
        buffer = new byte[length];
    }

    void init(long startingOffset) {
        this.startingOffset = startingOffset;
        count = 0;
    }

    void addByte(byte val) {
        Validate.isTrue(count < buffer.length, "Too many bytes being added to replay buffer " + count);
        buffer[count] = val;
        count++;
    }

    boolean inReplayBuffer(long readOffset) {
        return (readOffset - startingOffset) < count;
    }

    int availableAfter(long readOffset) {
        return (int) Math.max(0, startingOffset + count - readOffset);
    }

    byte getByteFromOffset(long readOffset) {
        Validate.isTrue(inReplayBuffer(readOffset),
                "Attempt to read from replay buffer at " + readOffset + "while buffer starts at" + startingOffset
                        + "and has " + count + "bytes");
        return buffer[(int) (readOffset - startingOffset)];
    }

    ByteBuffer getByteBuffer() {
        return ByteBuffer.wrap(buffer, 0, count);
    }
}
