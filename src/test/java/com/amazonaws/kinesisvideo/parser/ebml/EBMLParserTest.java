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
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.OptionalLong;

/**
 * Tests for the {@link EBMLParser}.
 */
public class EBMLParserTest {
    private EBMLParser parser;
    private TestEBMLParserCallback parserCallback;
    private boolean testRawBytesMatch = true;

    byte [] EBML_id_bytes = new byte [] { (byte )0x1A, (byte ) 0x45, (byte ) 0xDF, (byte ) 0xA3 };
    byte [] EBML_ZERO_LENGTH_RAWBYTES = new byte [] { (byte )0x1A, (byte ) 0x45, (byte ) 0xDF, (byte ) 0xA3, (byte ) 0x80 };
    byte [] EBMLVersion_id_bytes = new byte [] { (byte )0x42, (byte )0x86 };
    byte [] EBMLReadVersion_id_bytes = new byte [] { (byte )0x42, (byte )0xF7 };
    byte [] UNKNOWN_LENGTH = new byte [] { (byte) 0x01, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    byte [] SEGMENT_id_bytes = new byte[] {  (byte) 0x18, (byte) 0x53, (byte) 0x80, (byte) 0x67};
    byte [] SEEKHEAD_id_bytes = new byte[] {  (byte) 0x11, (byte) 0x4D, (byte) 0x9B, (byte) 0x74};
    byte [] SEEK_id_bytes = new byte[] {  (byte) 0x4D, (byte) 0xBB};
    byte [] SEEKID_id_bytes = new byte[] {  (byte) 0x53, (byte) 0xAB};
    byte [] SEEKPOSITION_id_bytes = new byte[] {  (byte) 0x53, (byte) 0xAC};
    byte [] COLOUR_id_bytes = new byte [] { (byte)0x55, (byte) 0xB0 };
    byte [] MATRIX_COEFFICIENTS_id_bytes = new byte[] { (byte) 0x55, (byte) 0xB1 };


    @Before
    public void setup() throws IllegalAccessException {
        parserCallback = new TestEBMLParserCallback();
        parser = new EBMLParser(new TestEBMLTypeInfoProvider(), parserCallback);
    }


    @Test
    public void testEBMLElementInOneStep() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(EBML_ZERO_LENGTH_RAWBYTES);

        setExpectedCallbacksForEBMLElement();

        callParser(outputStream, outputStream.size());
    }

    private void setExpectedCallbacksForEBMLElement() {
        parserCallback.setCheckExpectedCallbacks(true);
        parserCallback.expectCallback(TestEBMLParserCallback.CallbackDescription.builder()
                .callbackType(TestEBMLParserCallback.CallbackDescription.CallbackType.START)
                .elementCount(0)
                .typeInfo(TestEBMLTypeInfoProvider.EBML)
                .numBytes(OptionalLong.of(0))
                .bytes(EBML_ZERO_LENGTH_RAWBYTES)
                .build())
                .expectCallback(createExpectedCallbackForEndEBML(0));
    }

    @Test
    public void testEBMLElementInThreeSteps() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(EBML_ZERO_LENGTH_RAWBYTES);

        setExpectedCallbacksForEBMLElement();
        callParser(outputStream, 2);
    }

    @Test
    public void testMasterElementOneChildElementSizeBasedTermination() throws IOException {
        ByteArrayOutputStream outputStream = setupTestForMasterElementWithOneChildAndElementSizedBasedTermination();

        callParser(outputStream, outputStream.size());
    }


    @Test
    public void testMasterElementOneChildElementSizeBasedTerminationMultipleChunks() throws IOException {
        ByteArrayOutputStream outputStream = setupTestForMasterElementWithOneChildAndElementSizedBasedTermination();

        callParser(outputStream, 1);
    }

    @Test
    public void testMasterElementOneChildElementUnknownLength() throws IOException {
        ByteArrayOutputStream outputStream = setupTestForMasterElementWithOneChildAndUnknownlength();

        callParser(outputStream, outputStream.size());
    }

    @Test
    public void testMasterElementOneChildElementUnknownLengthMultipleChunks() throws IOException {
        ByteArrayOutputStream outputStream = setupTestForMasterElementWithOneChildAndUnknownlength();

        callParser(outputStream, 3);
    }

    @Test
    public void testMasterElementWithUnknownLengthAndEndOfStream() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte [] EBML_element_rawbytes = writeElement(EBML_id_bytes, UNKNOWN_LENGTH, outputStream);
        byte [] EBMLVersion_element_rawbytes = writeElement(EBMLVersion_id_bytes, wrapByte(0x83), outputStream);
        byte [] EBMLVersion_data_bytes = new byte[] {0x1, 0x4, 0xD};

        outputStream.write(EBMLVersion_data_bytes);
        parserCallback.setCheckExpectedCallbacks(true);
        parserCallback.expectCallback(createExpectedCallbackForStartOfEBMLUnknownLength(EBML_element_rawbytes));
        addExpectedCallbacksForEBMLVersion(EBMLVersion_element_rawbytes, EBMLVersion_data_bytes, 1)
                .expectCallback(createExpectedCallbackForEndEBML(0));


        callParser(outputStream, outputStream.size());
        parser.closeParser();
    }

    @Test
    public void testInputStreamReturningEoF() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte [] EBML_element_rawbytes = writeElement(EBML_id_bytes, UNKNOWN_LENGTH, outputStream);
        byte [] EBMLVersion_element_rawbytes = writeElement(EBMLVersion_id_bytes, wrapByte(0x83), outputStream);
        byte [] EBMLVersion_data_bytes = new byte[] {0x1, 0x4, 0xD};

        outputStream.write(EBMLVersion_data_bytes, 0, 2);
        outputStream.close();
        parserCallback.setCheckExpectedCallbacks(true);
        parserCallback.expectCallback(createExpectedCallbackForStartOfEBMLUnknownLength(EBML_element_rawbytes))
                .expectCallback(TestEBMLParserCallback.CallbackDescription.builder()
                        .callbackType(TestEBMLParserCallback.CallbackDescription.CallbackType.START)
                        .typeInfo(TestEBMLTypeInfoProvider.EBMLVersion)
                        .elementCount(1)
                        .numBytes(OptionalLong.of(EBMLVersion_data_bytes.length))
                        .bytes(EBMLVersion_element_rawbytes)
                        .build())
                .expectCallback(TestEBMLParserCallback.CallbackDescription.builder()
                        .callbackType(TestEBMLParserCallback.CallbackDescription.CallbackType.CONTENT)
                        .typeInfo(TestEBMLTypeInfoProvider.EBMLVersion)
                        .elementCount(1)
                        .numBytes(OptionalLong.of(EBMLVersion_data_bytes.length))
                        .bytes(Arrays.copyOf(EBMLVersion_data_bytes, 2))
                        .build())
                .expectCallback(TestEBMLParserCallback.CallbackDescription.builder()
                        .callbackType(TestEBMLParserCallback.CallbackDescription.CallbackType.CONTENT)
                        .typeInfo(TestEBMLTypeInfoProvider.EBMLVersion)
                        .elementCount(1)
                        .numBytes(OptionalLong.of(1))
                        .bytes(new byte[] {})
                        .build())
                .expectCallback(TestEBMLParserCallback.CallbackDescription.builder()
                        .callbackType(TestEBMLParserCallback.CallbackDescription.CallbackType.END)
                        .typeInfo(TestEBMLTypeInfoProvider.EBMLVersion)
                        .elementCount(1)
                        .build())
                .expectCallback(createExpectedCallbackForEndEBML(0));


        callParser(outputStream, outputStream.size() + 1, true);
        Assert.assertTrue(parser.isEndOfStream());
        Assert.assertTrue(parser.isClosed());

    }

    /**
     * This test writes and parses
     * EBML with unknown length
     * EBMLVersion with 3 bytes
     * EBMLReadVersion with 2 bytes
     * SEGMENT with unknownlength
     * SEEKHEAD with unknownlength
     * CRC with 4 bytes
     * SEEK with data of 11 bytes
     * SEEKID with 2 bytes (id(2+1)+ 2 = 5 bytes)
     * SEEKPOSITION with 3 bytes (id(2+1) + 3 = 6 bytes)
     * EBML with 0 length
     * @throws IOException
     */
    @Test
    public void testWithMultipleMasterElementsAndChildElements() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        //EBML
        byte[] EBML_element_rawbytes = writeElement(EBML_id_bytes, UNKNOWN_LENGTH, outputStream);
        byte[] EBMLVersion_element_rawbytes = writeElement(EBMLVersion_id_bytes, wrapByte(0x83), outputStream);
        byte[] EBMLVersion_data_bytes = new byte[] { 0x1, 0x4, 0xD };
        outputStream.write(EBMLVersion_data_bytes);
        byte[] EBMLReadVersion_element_rawbytes = writeElement(EBMLReadVersion_id_bytes, wrapByte(0x82), outputStream);
        byte[] EBMLReadVersion_data_bytes = new byte[] { 0x72, 0x1C };
        outputStream.write(EBMLReadVersion_data_bytes);

        //SEGMENT
        byte[] SEGMENT_element_rawbytes = writeElement(SEGMENT_id_bytes, UNKNOWN_LENGTH, outputStream);
        byte[] SEEKHEAD_element_rawbytes = writeElement(SEEKHEAD_id_bytes, UNKNOWN_LENGTH, outputStream);
        byte[] CRC_element_rawbytes = writeElement(wrapByte(0xBF), wrapByte(0x84), outputStream);
        byte[] CRC_data_bytes = new byte[] { 0x12, 0x3C, 0x43, 0x4D };
        outputStream.write(CRC_data_bytes);
        byte[] SEEK_element_rawbytes = writeElement(SEEK_id_bytes, wrapByte(0x8B), outputStream);
        byte[] SEEKID_element_rawbytes = writeElement(SEEKID_id_bytes, wrapByte(0x82), outputStream);
        byte[] SEEKID_data_bytes = new byte[] { 0x34, 0x35 };
        outputStream.write(SEEKID_data_bytes);
        byte[] SEEKPOSITON_element_rawbytes = writeElement(SEEKPOSITION_id_bytes, wrapByte(0x83), outputStream);
        byte[] SEEKPOSITON_data_bytes = new byte[] { 0x36, 0x37, 0x38 };
        outputStream.write(SEEKPOSITON_data_bytes);

        outputStream.write(EBML_ZERO_LENGTH_RAWBYTES);

        int chunkSize = 1;
        parserCallback.setCheckExpectedCallbacks(true);
        parserCallback.expectCallback(createExpectedCallbackForStartOfEBMLUnknownLength(EBML_element_rawbytes));
        addExpectedCallbacksForBaseElement(TestEBMLTypeInfoProvider.EBMLVersion,
                EBMLVersion_element_rawbytes,
                EBMLVersion_data_bytes,
                1,
                chunkSize);
        addExpectedCallbacksForBaseElement(TestEBMLTypeInfoProvider.EBMLReadVersion,
                EBMLReadVersion_element_rawbytes,
                EBMLReadVersion_data_bytes,
                2, chunkSize).expectCallback(createExpectedCallbackForEndEBML(0))
                .expectCallback(TestEBMLParserCallback.CallbackDescription.builder()
                        .callbackType(TestEBMLParserCallback.CallbackDescription.CallbackType.START)
                        .typeInfo(TestEBMLTypeInfoProvider.SEGMENT)
                        .elementCount(3)
                        .numBytes(OptionalLong.of(EBMLParserInternalElement.UNKNOWN_LENGTH_VALUE))
                        .bytes(SEGMENT_element_rawbytes)
                        .build())
                .expectCallback(TestEBMLParserCallback.CallbackDescription.builder()
                        .callbackType(TestEBMLParserCallback.CallbackDescription.CallbackType.START)
                        .typeInfo(TestEBMLTypeInfoProvider.SEEKHEAD)
                        .elementCount(4)
                        .numBytes(OptionalLong.of(EBMLParserInternalElement.UNKNOWN_LENGTH_VALUE))
                        .bytes(SEEKHEAD_element_rawbytes)
                        .build());
        addExpectedCallbacksForBaseElement(TestEBMLTypeInfoProvider.CRC,
                CRC_element_rawbytes,
                CRC_data_bytes,
                5, chunkSize).expectCallback(TestEBMLParserCallback.CallbackDescription.builder()
                .callbackType(TestEBMLParserCallback.CallbackDescription.CallbackType.START)
                .typeInfo(TestEBMLTypeInfoProvider.SEEK)
                .elementCount(6)
                .numBytes(OptionalLong.of(
                        SEEKID_element_rawbytes.length + SEEKID_data_bytes.length + SEEKPOSITON_element_rawbytes.length
                                + SEEKPOSITON_data_bytes.length))
                .bytes(SEEK_element_rawbytes)
                .build());
        addExpectedCallbacksForBaseElement(TestEBMLTypeInfoProvider.SEEKID,
                SEEKID_element_rawbytes,
                SEEKID_data_bytes,
                7, chunkSize);
        addExpectedCallbacksForBaseElement(TestEBMLTypeInfoProvider.SEEKPOSITION,
                SEEKPOSITON_element_rawbytes,
                SEEKPOSITON_data_bytes,
                8, chunkSize).expectCallback(createExpectedCallbackForEndMasterElement(6, TestEBMLTypeInfoProvider.SEEK))
                .expectCallback(createExpectedCallbackForEndMasterElement(4, TestEBMLTypeInfoProvider.SEEKHEAD))
                .expectCallback(createExpectedCallbackForEndMasterElement(3, TestEBMLTypeInfoProvider.SEGMENT))
                .expectCallback(TestEBMLParserCallback.CallbackDescription.builder()
                        .callbackType(TestEBMLParserCallback.CallbackDescription.CallbackType.START)
                        .elementCount(9)
                        .typeInfo(TestEBMLTypeInfoProvider.EBML)
                        .numBytes(OptionalLong.of(0))
                        .bytes(EBML_ZERO_LENGTH_RAWBYTES)
                        .build())
                .expectCallback(createExpectedCallbackForEndEBML(9));

        callParser(outputStream, 1);
    }


    @Test
    public void unknownElementsTest() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte [] EBML_element_rawbytes = writeElement(EBML_id_bytes, UNKNOWN_LENGTH, outputStream);
        byte [] EBMLVersion_element_rawbytes = writeElement(EBMLVersion_id_bytes, wrapByte(0x83), outputStream);
        byte [] EBMLVersion_data_bytes = new byte[] {0x1, 0x4, 0xD};

        outputStream.write(EBMLVersion_data_bytes);

        //unknown elements
        byte [] COLOUR_element_rawbytes = writeElement(COLOUR_id_bytes, wrapByte(0x85),outputStream);
        byte [] MATRIX_COEFFICIENT_element_rawbytes = writeElement(MATRIX_COEFFICIENTS_id_bytes, wrapByte(0x82), outputStream);
        byte [] MATRIX_COEFFICIENT_data_bytes = new byte [] {0x1, 0x2};

        outputStream.write(MATRIX_COEFFICIENT_data_bytes);

        outputStream.write(EBML_ZERO_LENGTH_RAWBYTES);

        parserCallback.setCheckExpectedCallbacks(true);
        parserCallback.expectCallback(createExpectedCallbackForStartOfEBMLUnknownLength(EBML_element_rawbytes));

        addExpectedCallbacksForBaseElement(TestEBMLTypeInfoProvider.EBMLVersion,
                EBMLVersion_element_rawbytes,
                EBMLVersion_data_bytes,
                1,
                1).expectCallback(createExpectedCallbackForEndEBML(0))
                .expectCallback(TestEBMLParserCallback.CallbackDescription.builder()
                        .callbackType(TestEBMLParserCallback.CallbackDescription.CallbackType.START)
                        .elementCount(3)
                        .typeInfo(TestEBMLTypeInfoProvider.EBML)
                        .numBytes(OptionalLong.of(0))
                        .bytes(EBML_ZERO_LENGTH_RAWBYTES)
                        .build())
                .expectCallback(createExpectedCallbackForEndEBML(3));

        testRawBytesMatch = false;
        callParser(outputStream, 1);
    }


    private ByteArrayOutputStream setupTestForMasterElementWithOneChildAndUnknownlength() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte [] EBML_element_rawbytes = writeElement(EBML_id_bytes, UNKNOWN_LENGTH, outputStream);
        byte [] EBMLVersion_element_rawbytes = writeElement(EBMLVersion_id_bytes, wrapByte(0x83), outputStream);
        byte [] EBMLVersion_data_bytes = new byte[] {0x1, 0x4, 0xD};

        outputStream.write(EBMLVersion_data_bytes);
        outputStream.write(EBML_ZERO_LENGTH_RAWBYTES);

        parserCallback.setCheckExpectedCallbacks(true);
        parserCallback.expectCallback(createExpectedCallbackForStartOfEBMLUnknownLength(EBML_element_rawbytes));
        addExpectedCallbacksForEBMLVersion(EBMLVersion_element_rawbytes, EBMLVersion_data_bytes, 1)
                .expectCallback(createExpectedCallbackForEndEBML(0))
                .expectCallback(TestEBMLParserCallback.CallbackDescription.builder()
                        .callbackType(TestEBMLParserCallback.CallbackDescription.CallbackType.START)
                        .elementCount(2)
                        .typeInfo(TestEBMLTypeInfoProvider.EBML)
                        .numBytes(OptionalLong.of(0))
                        .bytes(EBML_ZERO_LENGTH_RAWBYTES)
                        .build())
                .expectCallback(createExpectedCallbackForEndEBML(2));
        return outputStream;
    }

    private TestEBMLParserCallback.CallbackDescription createExpectedCallbackForEndEBML(long elementCount) {
        return createExpectedCallbackForEndMasterElement(elementCount, TestEBMLTypeInfoProvider.EBML);
    }

    private TestEBMLParserCallback.CallbackDescription createExpectedCallbackForEndMasterElement(long elementCount,
            EBMLTypeInfo typeInfo) {
        return TestEBMLParserCallback.CallbackDescription.builder()
                .callbackType(TestEBMLParserCallback.CallbackDescription.CallbackType.END)
                .typeInfo(typeInfo)
                .elementCount(elementCount)
                .build();
    }

    private TestEBMLParserCallback addExpectedCallbacksForEBMLVersion(byte[] EBMLVersion_element_rawbytes,
            byte[] EBMLVersion_data_bytes, long elementCount) {
        return addExpectedCallbacksForBaseElement(TestEBMLTypeInfoProvider.EBMLVersion,
                EBMLVersion_element_rawbytes,
                EBMLVersion_data_bytes,
                elementCount,
                EBMLVersion_data_bytes.length);
    }

    private TestEBMLParserCallback addExpectedCallbacksForBaseElement(EBMLTypeInfo typeInfo,
            byte[] element_rawbytes,
            byte[] data_bytes,
            long elementCount,
            int chunkSize) {
        parserCallback.expectCallback(TestEBMLParserCallback.CallbackDescription.builder()
                .callbackType(TestEBMLParserCallback.CallbackDescription.CallbackType.START)
                .typeInfo(typeInfo)
                .elementCount(elementCount)
                .numBytes(OptionalLong.of(data_bytes.length))
                .bytes(element_rawbytes)
                .build());
                int count = 0;
                while (count < data_bytes.length) {
                    int length = Math.min(chunkSize, data_bytes.length - count);
                    parserCallback.expectCallback(TestEBMLParserCallback.CallbackDescription.builder()
                            .callbackType(TestEBMLParserCallback.CallbackDescription.CallbackType.CONTENT)
                            .typeInfo(typeInfo)
                            .elementCount(elementCount)
                            .numBytes(OptionalLong.of(length))
                            .bytes(Arrays.copyOfRange(data_bytes, count, count + length))
                            .build());
                    count += length;
                }
        return parserCallback
                .expectCallback(TestEBMLParserCallback.CallbackDescription.builder()
                        .callbackType(TestEBMLParserCallback.CallbackDescription.CallbackType.END)
                        .typeInfo(typeInfo)
                        .elementCount(elementCount)
                        .build());
    }

    private TestEBMLParserCallback.CallbackDescription createExpectedCallbackForStartOfEBMLUnknownLength(byte[] EBML_element_rawbytes) {
        return TestEBMLParserCallback.CallbackDescription.builder()
                .callbackType(TestEBMLParserCallback.CallbackDescription.CallbackType.START)
                .typeInfo(TestEBMLTypeInfoProvider.EBML)
                .elementCount(0)
                .numBytes(OptionalLong.of(EBMLParserInternalElement.UNKNOWN_LENGTH_VALUE))
                .bytes(EBML_element_rawbytes)
                .build();
    }

    private ByteArrayOutputStream setupTestForMasterElementWithOneChildAndElementSizedBasedTermination()
            throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte [] EBML_element_rawbytes = writeElement(EBML_id_bytes, wrapByte(0x84), outputStream);
        byte [] EBMLVersion_element_rawbytes = writeElement(EBMLVersion_id_bytes, wrapByte(0x81), outputStream);
        byte [] EBMLVersion_data_bytes = new byte [] { (byte) 0x4};

        outputStream.write(EBMLVersion_data_bytes);

        parserCallback.setCheckExpectedCallbacks(true);

        parserCallback.expectCallback(TestEBMLParserCallback.CallbackDescription.builder()
                .callbackType(TestEBMLParserCallback.CallbackDescription.CallbackType.START)
                .typeInfo(TestEBMLTypeInfoProvider.EBML)
                .elementCount(0)
                .numBytes(OptionalLong.of(4))
                .bytes(EBML_element_rawbytes)
                .build());
        addExpectedCallbacksForEBMLVersion(EBMLVersion_element_rawbytes, EBMLVersion_data_bytes, 1)
                .expectCallback(createExpectedCallbackForEndEBML(0));
        return outputStream;
    }

    private static byte[] writeElement(byte[] id,
            byte[] size,
            ByteArrayOutputStream overAllOutputStream) throws IOException {
        ByteArrayOutputStream elementOutputStream = new ByteArrayOutputStream();
        elementOutputStream.write(id);
        elementOutputStream.write(size);
        byte[] result = elementOutputStream.toByteArray();
        overAllOutputStream.write(result);
        return result;
    }

    private static byte [] wrapByte(int data) {
        return new byte [] {(byte)data};
    }

    private void callParser(ByteArrayOutputStream outputStream, int chunkSize)
            throws IOException {
        callParser(outputStream, chunkSize, false);
    }

    private void callParser(ByteArrayOutputStream outputStream, int chunkSize, boolean closeLast)
            throws IOException {
        byte [] data = outputStream.toByteArray();
        int count = 0;
        while (count < data.length) {
            int length = Math.min(chunkSize, data.length - count);
            InputStream underlyingDataStream = new ByteArrayInputStream(data, count, length);
            InputStreamParserByteSource byteSource;
            if (closeLast && count + length >= data.length) {
                underlyingDataStream.close();
                byteSource = new InputStreamParserByteSource(underlyingDataStream) {
                    @Override
                    public int available() {
                        return super.available() + 1;
                    }
                };
            } else {
                 byteSource = new InputStreamParserByteSource(underlyingDataStream);
            }
            parser.parse(byteSource);
            count += length;
        }
        if (!closeLast) {
            parser.closeParser();
        }

        if(testRawBytesMatch) {
            Assert.assertArrayEquals(data, parserCallback.rawBytes());
        }
        //TODO: enable later
        parserCallback.validateEmptyCallback();
    }
}
