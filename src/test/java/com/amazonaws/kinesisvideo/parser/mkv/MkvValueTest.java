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

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

/**
 * Tests for MkvValue equality
 */
public class MkvValueTest {

    @Test
    public void integerTest() {
        MkvValue<Integer> val1 = new MkvValue<>(2, 1);
        MkvValue<Integer> val2 = new MkvValue<>(2, 1);

        Assert.assertTrue(val1.equals(val2));

        MkvValue<Integer> val3 = new MkvValue<>(3, 1);
        Assert.assertFalse(val1.equals(val3));

        MkvValue<Integer> val4 = new MkvValue<>(2, 2);
        Assert.assertFalse(val1.equals(val4));
    }

    @Test
    public void byteBufferTest() {
        ByteBuffer buf1 = ByteBuffer.wrap(new byte[] {(byte) 0x32, (byte) 0x45, (byte) 0x73 });
        ByteBuffer buf2 = ByteBuffer.wrap(new byte[] {(byte) 0x32, (byte) 0x45, (byte) 0x73 });

        MkvValue<ByteBuffer> val1 = new MkvValue<>(buf1, buf1.limit());
        MkvValue<ByteBuffer> val2 = new MkvValue<>(buf2, buf2.limit());

        Assert.assertTrue(val1.equals(val2));

        //Even if a buffer has been partially read, equality should still succeed
        buf2.get();
        Assert.assertTrue(val1.equals(val2));

        ByteBuffer buf3 = ByteBuffer.wrap(new byte[] {(byte) 0x28});
        MkvValue val3 = new MkvValue(buf3, buf3.limit());
        Assert.assertFalse(val1.equals(val3));
    }
}
