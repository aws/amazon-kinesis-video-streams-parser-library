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


import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.OptionalLong;
import java.util.Queue;

/**
 * Implementation of {@link EBMLParserCallbacks} used for tests.
 */
@Slf4j
public class TestEBMLParserCallback implements EBMLParserCallbacks {
    @Setter
    boolean checkExpectedCallbacks;

    ByteBuffer contentBuffer = ByteBuffer.allocate(10_000);
    ByteArrayOutputStream rawBytesOutput = new ByteArrayOutputStream();

    Queue<CallbackDescription> callbackDescriptions = new LinkedList<>();



    @Override
    public void onStartElement(EBMLElementMetaData elementMetaData,
            long elementDataSize,
            ByteBuffer idAndSizeRawBytes,
            ElementPathSupplier pathSupplier) {
        long elementNumber = elementMetaData.getElementNumber();
        EBMLTypeInfo typeInfo = elementMetaData.getTypeInfo();
        log.info("On Start: elementNumber " + elementNumber + " typeInfo " + typeInfo.toString() + " size "
                + elementDataSize);
        if (log.isDebugEnabled()) {
            log.debug("Rawbytes { " + hexDump(idAndSizeRawBytes) + " }");
        }
        dumpByteBufferToRawOutput(idAndSizeRawBytes);

        if (checkExpectedCallbacks) {
            CallbackDescription expectedCallback = callbackDescriptions.remove();
            CallbackDescription actualCallback = CallbackDescription.builder()
                    .elementCount(elementNumber)
                    .callbackType(CallbackDescription.CallbackType.START)
                    .typeInfo(typeInfo)
                    .numBytes(OptionalLong.of(elementDataSize))
                    .bytes(convertToByteArray(idAndSizeRawBytes))
                    .build();
            Validate.isTrue(compareCallbackDescriptions(expectedCallback, actualCallback),
                    getMismatchExpectationMessage(expectedCallback, actualCallback));
        }
    }

    private static boolean compareCallbackDescriptions(CallbackDescription expectedCallback,
            CallbackDescription actualCallback) {
        return expectedCallback.equals(actualCallback) && expectedCallback.areBytesEqual(actualCallback.bytes);
    }

    @Override
    public void onPartialContent(EBMLElementMetaData elementMetaData,
            ParserBulkByteSource bulkByteSource,
            int bytesToRead) {
        contentBuffer.clear();
        bulkByteSource.readBytes(contentBuffer, bytesToRead);
        contentBuffer.flip();
        long elementNumber = elementMetaData.getElementNumber();
        EBMLTypeInfo typeInfo = elementMetaData.getTypeInfo();
        log.info("On PartialContent: elementCount " + elementNumber + " typeInfo " + typeInfo.toString()
                + " bytesToRead " + bytesToRead);
        if (log.isDebugEnabled()) {
            log.debug("Rawbytes { " + hexDump(contentBuffer) + " }");
        }

        dumpByteBufferToRawOutput(contentBuffer);
        if (checkExpectedCallbacks) {
            CallbackDescription expectedCallback = callbackDescriptions.remove();
            CallbackDescription actualCallback = CallbackDescription.builder()
                    .callbackType(CallbackDescription.CallbackType.CONTENT)
                    .elementCount(elementNumber)
                    .typeInfo(typeInfo)
                    .numBytes(OptionalLong.of(bytesToRead))
                    .bytes(convertToByteArray(contentBuffer))
                    .build();
            Validate.isTrue(compareCallbackDescriptions(expectedCallback, actualCallback),
                    getMismatchExpectationMessage(expectedCallback, actualCallback));
        }
    }

    private String getMismatchExpectationMessage(CallbackDescription expectedCallback,
            CallbackDescription actualCallback) {
        return " ExpectedCallback " + expectedCallback + " ActualCallback " + actualCallback;
    }

    @Override
    public void onEndElement(EBMLElementMetaData elementMetaData, ElementPathSupplier pathSupplier) {
        long elementNumber = elementMetaData.getElementNumber();
        EBMLTypeInfo typeInfo = elementMetaData.getTypeInfo();

        log.info("On EndElement: elementNumber " + elementNumber + " typeInfo " + typeInfo.toString());

        if (checkExpectedCallbacks) {
            CallbackDescription expectedCallback = callbackDescriptions.remove();
            CallbackDescription actualCallback = CallbackDescription.builder()
                    .callbackType(CallbackDescription.CallbackType.END)
                    .elementCount(elementNumber)
                    .typeInfo(typeInfo)
                    .build();
            Validate.isTrue(compareCallbackDescriptions(expectedCallback, actualCallback),
                    getMismatchExpectationMessage(expectedCallback, actualCallback));
        }
    }

    byte [] rawBytes() {
        return rawBytesOutput.toByteArray();
    }

    void validateEmptyCallback() {
        Validate.isTrue(callbackDescriptions.isEmpty(), "remaining size "+callbackDescriptions.size());
    }

    TestEBMLParserCallback expectCallback(CallbackDescription callbackDescription) {
        callbackDescriptions.add(callbackDescription);
        return this;
    }

    public static String hexDump(ByteBuffer byteBuffer) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < byteBuffer.limit(); i++) {
            builder.append(String.format("0x%02x ", byteBuffer.get(i)));
        }
        return builder.toString();
    }


    private void dumpByteBufferToRawOutput(ByteBuffer byteBuffer) {
        rawBytesOutput.write(byteBuffer.array(),
                byteBuffer.position(),
                byteBuffer.limit() - byteBuffer.position());
    }


    private byte[] convertToByteArray(ByteBuffer byteBuffer) {
        byte [] array = new byte[byteBuffer.limit() - byteBuffer.position()];
        byteBuffer.get(array);
        return array;
    }


    @Builder
    @EqualsAndHashCode(doNotUseGetters=true,exclude = "bytes")
    @ToString
    static class CallbackDescription {
        enum CallbackType {START, CONTENT, END};
        final CallbackType callbackType;
        EBMLTypeInfo typeInfo;
        long elementCount;
        @Builder.Default
        OptionalLong numBytes = OptionalLong.empty();

        @Builder.Default
        byte[] bytes = null;

        boolean areBytesEqual(byte[] otherBytes) {
            return Arrays.equals(bytes, otherBytes);
        }
    }

}
