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

import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Tests for the EBMLUtils class.
 */
public class EBMLUtilsTest {
    @Test
    public void readSignedInteger8bytes() {
        byte[] data = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFE };
        runTestForNegativeTwo(data);
    }

    private void runTestForNegativeTwo(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);

        long value = EBMLUtils.readDataSignedInteger(buffer, data.length);
        Assert.assertEquals(-2, value);
    }

    @Test
    public void readSignedInteger7bytes() {
        byte[] data = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFE };
        runTestForNegativeTwo(data);
    }

    @Test
    public void readSignedInteger2bytes() {
        byte[] data = { (byte) 0xFF, (byte) 0xFE };
        runTestForNegativeTwo(data);
    }

    @Test
    public void readSignedInteger1byte() {
        byte[] data = { (byte) 0xFE };
        runTestForNegativeTwo(data);
    }

    @Test
    public void readUnsignedIntegerSevenBytesOrLessTwoBytes() {
        byte[] data = { (byte) 0xFF, (byte) 0xFE };
        ByteBuffer buffer = ByteBuffer.wrap(data);

        long value = EBMLUtils.readUnsignedIntegerSevenBytesOrLess(buffer, data.length);
        Assert.assertEquals(0xFFFE, value);
    }

    @Test
    public void readPositiveSignedInteger1byte() {
        byte[] data = { (byte) 0x7E };
        ByteBuffer buffer = ByteBuffer.wrap(data);

        long value = EBMLUtils.readDataSignedInteger(buffer, data.length);
        Assert.assertEquals(0x7E, value);
    }

    @Test
    public void readUnsignedInteger() {
        ByteBuffer b = ByteBuffer.allocate(8);
        b.putLong(-309349387097750278L);
        b.order(ByteOrder.BIG_ENDIAN);

        b.rewind();
        BigInteger unsigned = EBMLUtils.readDataUnsignedInteger(b, 8);
        Assert.assertTrue(unsigned.signum() > 0);
        b.rewind();
        long value = EBMLUtils.readDataSignedInteger(b, 8);
        Assert.assertEquals(value, -309349387097750278L);

        System.out.println(unsigned.toString(16));
    }
    //:-309349387097750278
}
