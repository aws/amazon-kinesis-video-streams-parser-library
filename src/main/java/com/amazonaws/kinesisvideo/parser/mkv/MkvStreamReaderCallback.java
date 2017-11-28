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
import com.amazonaws.kinesisvideo.parser.ebml.EBMLParserCallbacks;
import com.amazonaws.kinesisvideo.parser.ebml.EBMLTypeInfo;
import com.amazonaws.kinesisvideo.parser.ebml.ParserBulkByteSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Predicate;


/**
 * EBML parser callback used by the MKVStream reader
 */
@Slf4j
@RequiredArgsConstructor
class MkvStreamReaderCallback implements EBMLParserCallbacks{

    //NOTE: if object creation rate becomes a performance bottleneck convert these to nullables
    private Optional<CurrentMkvDataElementInfo> currentMkvDataElementInfo = Optional.empty();
    private final Queue<MkvElement> elementsToReturn = new ArrayDeque<>();

    private final boolean shouldStoreElementPaths;
    private final Predicate<EBMLTypeInfo> elementFilter;

    //TODO: make this dynamic
    private static final int MAX_BUFFER_SIZE = 1_000_000;
    ByteBuffer readBuffer = ByteBuffer.allocate(MAX_BUFFER_SIZE);


    @Override
    public void onStartElement(EBMLElementMetaData elementMetaData,
            long elementDataSize,
            ByteBuffer idAndSizeRawBytes,
            ElementPathSupplier pathSupplier) {
        Validate.isTrue(!currentMkvDataElementInfo.isPresent());

        if(elementMetaData.isMaster()) {
            log.debug("Start Master Element to return {} data size {} ", elementMetaData, elementDataSize);
            addMkvElementToReturn(MkvStartMasterElement.builder().elementMetaData(elementMetaData)
                    .elementPath(getPath(pathSupplier))
                    .dataSize(elementDataSize)
                    .idAndSizeRawBytes(idAndSizeRawBytes).build());
        } else {
            if (elementDataSize > readBuffer.capacity()) {
                int sizeToAllocate = ((int )Math.ceil((double )elementDataSize/MAX_BUFFER_SIZE))*MAX_BUFFER_SIZE;
                log.info("Resizing readBuffer to {}", sizeToAllocate);
                readBuffer = ByteBuffer.allocate(sizeToAllocate);
            }
            readBuffer.clear();

            if (elementFilter.test(elementMetaData.getTypeInfo())) {
                log.debug("Data Element to start building {} data size {} ", elementMetaData, elementDataSize);
                List<EBMLElementMetaData> elementPath = getPath(pathSupplier);
                currentMkvDataElementInfo = Optional.of(new CurrentMkvDataElementInfo(elementMetaData,
                        elementDataSize,
                        elementPath,
                        idAndSizeRawBytes));
            }
        }
    }

    private List<EBMLElementMetaData> getPath(ElementPathSupplier pathSupplier) {
        List<EBMLElementMetaData> elementPath;
        if (shouldStoreElementPaths) {
            elementPath = pathSupplier.getAncestors();
        } else {
            elementPath = new ArrayList<>();
        }
        return elementPath;
    }

    @Override
    public void onPartialContent(EBMLElementMetaData elementMetaData,
            ParserBulkByteSource bulkByteSource,
            int bytesToRead) {
        Validate.isTrue(elementsToReturn.isEmpty());
        if (elementFilter.test(elementMetaData.getTypeInfo())) {
            Validate.isTrue(currentMkvDataElementInfo.isPresent());
            currentMkvDataElementInfo.get().validateExpectedElement(elementMetaData);
            log.debug("Data Element to start buffering data {} bytes to read {} ", elementMetaData, bytesToRead);
        }

        if(!elementMetaData.isMaster()) {
            bulkByteSource.readBytes(readBuffer, bytesToRead);
        }
    }

    @Override
    public void onEndElement(EBMLElementMetaData elementMetaData, ElementPathSupplier pathSupplier) {
        if(elementMetaData.isMaster()) {
            Validate.isTrue(!currentMkvDataElementInfo.isPresent());
            log.debug("End Master Element to return {}", elementMetaData);
            addMkvElementToReturn(MkvEndMasterElement.builder()
                    .elementMetaData(elementMetaData)
                    .elementPath(getPath(pathSupplier))
                    .build());
        } else {
            if (elementFilter.test(elementMetaData.getTypeInfo())) {
                Validate.isTrue(currentMkvDataElementInfo.isPresent());
                log.debug("Data Element to return {} data size {} ", elementMetaData, readBuffer.position());
                readBuffer.flip();
                addMkvElementToReturn(currentMkvDataElementInfo.get().build(readBuffer));
                currentMkvDataElementInfo = Optional.empty();
            }
        }
    }


    @Override
    public boolean continueParsing() {
        return elementsToReturn.isEmpty();
    }

    boolean hasElementsToReturn() {
        return !elementsToReturn.isEmpty();
    }

    public Optional<MkvElement> getMkvElementIfAvailable() {
        if (elementsToReturn.isEmpty()) {
            return Optional.empty();
        }
       return Optional.of(elementsToReturn.remove());
    }

    private void addMkvElementToReturn(MkvElement elementToReturn) {
        this.elementsToReturn.add(elementToReturn);
    }


    private static class CurrentMkvDataElementInfo {
        private final EBMLElementMetaData elementMetadata;
        private final long dataSize;
        private final List<EBMLElementMetaData> elementPath;
        private final ByteBuffer idAndSizeRawBytes = ByteBuffer.allocate(MkvElement.MAX_ID_AND_SIZE_BYTES);


        CurrentMkvDataElementInfo(EBMLElementMetaData elementMetadata,
                long dataSize,
                List<EBMLElementMetaData> elementPath,
                ByteBuffer idAndSizeRawBytes) {
            this.elementMetadata = elementMetadata;
            this.dataSize = dataSize;
            this.elementPath = elementPath;
            //Copy the id and size raw bytes since these will be retained.
            this.idAndSizeRawBytes.put(idAndSizeRawBytes);
            this.idAndSizeRawBytes.flip();
            idAndSizeRawBytes.rewind();
        }

        MkvDataElement build(ByteBuffer data) {
            Validate.isTrue(data.limit() == dataSize);
            return MkvDataElement.builder()
                    .elementMetaData(elementMetadata)
                    .dataSize(dataSize)
                    .idAndSizeRawBytes(idAndSizeRawBytes)
                    .elementPath(elementPath)
                    .dataBuffer(data)
                    .build();
        }

        public void validateExpectedElement(EBMLElementMetaData elementMetaData) {
            Validate.isTrue(elementMetaData.equals(this.elementMetadata));
        }
    }
}
