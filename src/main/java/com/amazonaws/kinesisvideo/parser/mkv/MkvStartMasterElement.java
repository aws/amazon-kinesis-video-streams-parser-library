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
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.List;

/**
 * Class representing the start of a mkv master element.
 * It includes the bytes containing the id and size of the element along with its {@link EBMLElementMetaData}
 * and its path (if specified).
 */
@Getter
@ToString(callSuper = true, exclude = "idAndSizeRawBytes")
public class MkvStartMasterElement extends MkvElement {
    private final long dataSize;

    private final ByteBuffer idAndSizeRawBytes = ByteBuffer.allocate(MAX_ID_AND_SIZE_BYTES);

    @Builder
    private MkvStartMasterElement(EBMLElementMetaData elementMetaData,
            List<EBMLElementMetaData> elementPath,
            long dataSize,
            ByteBuffer idAndSizeRawBytes) {
        super(elementMetaData, elementPath);
        this.dataSize = dataSize;
        this.idAndSizeRawBytes.put(idAndSizeRawBytes);
        this.idAndSizeRawBytes.flip();
        idAndSizeRawBytes.rewind();
    }

    @Override
    public boolean isMaster() {
        return true;
    }

    @Override
    public void accept(MkvElementVisitor visitor) throws MkvElementVisitException {
        visitor.visit(this);
    }

    @Override
    public boolean equivalent(MkvElement other) {
        return typeEquals(other) && this.dataSize == ((MkvStartMasterElement) other).dataSize;
    }

    public boolean isUnknownLength() {
        return dataSize == 0xFFFFFFFFFFFFFFL;
    }

    @Override
    public void writeToChannel(WritableByteChannel outputChannel) throws MkvElementVisitException {
        writeByteBufferToChannel(idAndSizeRawBytes, outputChannel);
    }

    public int getIdAndSizeRawBytesLength() {
        return idAndSizeRawBytes.limit();
    }
}
