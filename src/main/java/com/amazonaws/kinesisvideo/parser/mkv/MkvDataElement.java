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

import com.amazonaws.kinesisvideo.parser.ebml.EBMLElementMetaData;
import com.amazonaws.kinesisvideo.parser.ebml.EBMLUtils;
import com.amazonaws.kinesisvideo.parser.ebml.MkvTypeInfos;
import lombok.AccessLevel;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;


/**
 * Class representing a non-master mkv element, that contains actual data in an mkv stream.
 * It includes the bytes containing the id and size of the element, the size of the data in the element.
 * When first vended by a {@link StreamingMkvReader} it also includes the raw bytes of the data element in the dataBuffer.
 * However, the data buffer can only be accessed before nextIfAvailable is called again on the StreamingMkvReader.
 * To retain the value of the MkvDataElement for later use call getValueCopy() on it.
 * It copies the raw bytes and interprets it based on the type of the MkvDataElement.
 */
@Getter
@ToString(callSuper = true, exclude = {"dataBuffer","valueCopy", "idAndSizeRawBytes"})
@Slf4j
public class MkvDataElement extends MkvElement {
    private static final int DATE_SIZE = 8;
    private static final Instant DATE_BASE_INSTANT = Instant.ofEpochSecond(978307200);

    private final long dataSize;

    private final ByteBuffer idAndSizeRawBytes;

    private ByteBuffer dataBuffer;

    @Getter(AccessLevel.NONE)
    private MkvValue valueCopy;

    @Builder
    private MkvDataElement(EBMLElementMetaData elementMetaData,
            List<EBMLElementMetaData> elementPath,
            ByteBuffer idAndSizeRawBytes,
            long dataSize,
            ByteBuffer dataBuffer) {
        super(elementMetaData, elementPath);
        this.dataSize = dataSize;
        this.dataBuffer = dataBuffer;
        this.idAndSizeRawBytes = idAndSizeRawBytes;
    }

    public MkvValue getValueCopy() {
        if (valueCopy == null) {
            createValueByCopyingBytes();
        }
        return valueCopy;
    }

    private void createValueByCopyingBytes() {
        dataBuffer.rewind();
        try {
            switch (elementMetaData.getTypeInfo().getType()) {
                case INTEGER:
                    valueCopy = new MkvValue(EBMLUtils.readDataSignedInteger(dataBuffer, dataSize), dataSize);
                    break;
                case UINTEGER:
                    BigInteger unsignedValue =  EBMLUtils.readDataUnsignedInteger(dataBuffer, dataSize);
                    //We originally failed validation here, but users ran into streams where the
                    //Track UID was negative. So, we changed this to an error log.
                    if (unsignedValue.signum() < 0) {
                        log.error("Uinteger has negative value {} ", unsignedValue);
                    }
                    valueCopy = new MkvValue(unsignedValue, dataSize);
                    break;
                case FLOAT:
                    Validate.isTrue(dataSize == Float.BYTES || dataSize == Double.BYTES,
                            "Invalid size for float type" + dataSize);
                    if (dataSize == Float.BYTES) {
                        valueCopy = new MkvValue(dataBuffer.getFloat(), Float.BYTES);
                    } else {
                        valueCopy = new MkvValue(dataBuffer.getDouble(), Double.BYTES);
                    }
                    break;
                case STRING:
                    valueCopy = new MkvValue(StandardCharsets.US_ASCII.decode(dataBuffer).toString(), dataSize);
                    break;
                case UTF_8:
                    valueCopy = new MkvValue(StandardCharsets.UTF_8.decode(dataBuffer).toString(), dataSize);
                    break;
                case DATE:
                    Validate.isTrue(dataSize == DATE_SIZE, "Date element size can only be 8 bytes not " + dataSize);
                    long dateLongValue = EBMLUtils.readDataSignedInteger(dataBuffer, DATE_SIZE);
                    valueCopy = new MkvValue(DATE_BASE_INSTANT.plusNanos(dateLongValue),dataSize);
                    break;
                case BINARY:
                    if (elementMetaData.getTypeInfo().equals(MkvTypeInfos.SIMPLEBLOCK)) {
                        Frame frame = Frame.withCopy(dataBuffer);
                        valueCopy = new MkvValue(frame, dataSize);
                    } else {
                        ByteBuffer buffer = ByteBuffer.allocate((int) dataSize);
                        dataBuffer.get(buffer.array(), 0, (int) dataSize);
                        valueCopy = new MkvValue(buffer, dataSize);
                    }
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Cannot have value for ebml element type " + elementMetaData.getTypeInfo().getType());
            }
        } finally {
            dataBuffer.rewind();
        }
    }

    @Override
    public boolean isMaster() {
        return false;
    }

    @Override
    public void accept(MkvElementVisitor visitor) throws MkvElementVisitException {
        visitor.visit(this);
    }

    @Override
    public boolean equivalent(MkvElement other) {
        if (!typeEquals(other)) {
            return false;
        }
        MkvDataElement otherDataElement = (MkvDataElement) other;
        return this.dataSize == otherDataElement.dataSize && this.valueCopy.equals(otherDataElement.valueCopy);
    }


    @Override
    public void writeToChannel(WritableByteChannel outputChannel) throws MkvElementVisitException {
        writeByteBufferToChannel(idAndSizeRawBytes, outputChannel);
        writeByteBufferToChannel(dataBuffer, outputChannel);
    }

    public int getIdAndSizeRawBytesLength() {
        return idAndSizeRawBytes.limit();
    }

    public int getDataBufferSize() {
        if (dataBuffer == null ) {
            return 0;
        }
        return dataBuffer.limit();
    }

    void clearDataBuffer() {
        dataBuffer = null;
    }
}
