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

import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.Validate;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * This class is used by the parser to represent an EBML Element internally.
 */
@ToString
class EBMLParserInternalElement {
    enum ElementReadState { NEW, ID_DONE, SIZE_DONE, CONTENT_READING, CONTENT_SKIPPING, FINISHED }

    static final long UNKNOWN_LENGTH_VALUE = 0xFFFFFFFFFFFFFFL;

    private final long startingOffset;
    @Getter
    private final long elementCount;


    ElementReadState currentElementReadState = ElementReadState.NEW;

    @Getter
    private int id;
    private long idNumBytes;
    @Getter
    private long dataSize;
    private long dataSizeNumBytes;

    private Optional<EBMLElementMetaData> elementMetaData = Optional.empty();


    public EBMLParserInternalElement(long startingOffset, long elementCount) {
        this.startingOffset = startingOffset;
        this.elementCount = elementCount;
    }

    public void readId(TrackingReplayableIdAndSizeByteSource idAndSizeByteSource) {
        Validate.isTrue(currentElementReadState == ElementReadState.NEW);
        idAndSizeByteSource.setReadOffsetForReplayBuffer(startingOffset);
        EBMLUtils.readId(idAndSizeByteSource, this::setId);
    }

    public void readSize(TrackingReplayableIdAndSizeByteSource idAndSizeByteSource) {
        Validate.isTrue(currentElementReadState == ElementReadState.ID_DONE);
        idAndSizeByteSource.setReadOffsetForReplayBuffer(startingOffset + idNumBytes);
        EBMLUtils.readSize(idAndSizeByteSource, this::setSize);
    }

    public void updateTypeInfo(EBMLTypeInfoProvider typeInfoProvider) {
        Validate.isTrue(currentElementReadState == ElementReadState.SIZE_DONE);
        Optional<EBMLTypeInfo> typeInfo = typeInfoProvider.getType(id);
        if (typeInfo.isPresent()) {
            elementMetaData = Optional.of(new EBMLElementMetaData(typeInfo.get(), elementCount));
        }
    }

    public boolean isKnownType() {
        return elementMetaData.isPresent();
    }

    public EBMLTypeInfo getTypeInfo() {
        return elementMetaData.get().getTypeInfo();
    }

    public EBMLElementMetaData getMetadata() {
        Validate.isTrue(elementMetaData.isPresent(), "EBML element metadata ");
        return elementMetaData.get();
    }

    public void startReadingContent() {
        Validate.isTrue(currentElementReadState == ElementReadState.SIZE_DONE);
        currentElementReadState = ElementReadState.CONTENT_READING;
    }

    public void startSkippingContent() {
        Validate.isTrue(currentElementReadState == ElementReadState.SIZE_DONE);
        currentElementReadState = ElementReadState.CONTENT_SKIPPING;
    }

    public void readContent(TrackingReplayableIdAndSizeByteSource idAndSizeByteSource,
            ParserBulkByteSource bulkByteSource,
            EBMLParserCallbacks callbacks,
            int maxContentBytesInOnePass) {
        Validate.isTrue(currentElementReadState == ElementReadState.CONTENT_READING);
        long bytesToRead = getBytesToRead(idAndSizeByteSource, maxContentBytesInOnePass);

        //Call onPartialContent if bytesToRead > 0.
        if (bytesToRead > 0) {
            callbacks.onPartialContent(elementMetaData.get(), bulkByteSource, (int) bytesToRead);
        }

        if (!isUnknownLength() && idAndSizeByteSource.getTotalBytesRead() >= getContentStartOffset() + dataSize) {
            currentElementReadState = ElementReadState.FINISHED;
        }
    }

    public void skipContent(TrackingReplayableIdAndSizeByteSource idAndSizeByteSource,
            ParserBulkByteSource bulkByteSource,
            ByteBuffer skipBuffer) {
        Validate.isTrue(currentElementReadState == ElementReadState.CONTENT_SKIPPING);
        long bytesToRead = getBytesToRead(idAndSizeByteSource, skipBuffer.remaining());

        if (bytesToRead > 0) {
            bulkByteSource.readBytes(skipBuffer, (int )bytesToRead);
        }

        if (idAndSizeByteSource.getTotalBytesRead() >= getContentStartOffset() + dataSize) {
            currentElementReadState = ElementReadState.FINISHED;
        }
    }

    public boolean isUnknownLength() {
        return dataSize == UNKNOWN_LENGTH_VALUE;
    }

    public long endOffSet() {
        Validate.isTrue(!isUnknownLength());
        return getContentStartOffset() + dataSize;
    }


    private long getContentStartOffset() {
        return startingOffset + idNumBytes + dataSizeNumBytes;
    }

    private void setId(int idArg, long idNumBytes) {
        Validate.isTrue(currentElementReadState == ElementReadState.NEW);
        this.id = idArg;
        this.idNumBytes = idNumBytes;
        currentElementReadState = ElementReadState.ID_DONE;
    }

    private void setSize(long sizeArg, long sizeNumBytes) {
        Validate.isTrue(currentElementReadState == ElementReadState.ID_DONE);
        this.dataSize = sizeArg;
        this.dataSizeNumBytes = sizeNumBytes;
        currentElementReadState = ElementReadState.SIZE_DONE;
    }

    private long getBytesToRead(TrackingReplayableIdAndSizeByteSource idAndSizeByteSource,
            int maxContentBytesInOnePass) {
        long bytesToRead = dataSize + getContentStartOffset() - (idAndSizeByteSource.getTotalBytesRead());
        bytesToRead = Math.min(bytesToRead, maxContentBytesInOnePass);
        bytesToRead = Math.min(bytesToRead, idAndSizeByteSource.availableForContent());
        return bytesToRead;
    }

}
