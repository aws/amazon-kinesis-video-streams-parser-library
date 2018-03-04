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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.List;

/**
 * Common base class for all MkvElements
 */
@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class MkvElement {
    protected static final int MAX_ID_AND_SIZE_BYTES = EBMLUtils.EBML_ID_MAX_BYTES + EBMLUtils.EBML_SIZE_MAX_BYTES;
    protected final EBMLElementMetaData elementMetaData;
    protected final List<EBMLElementMetaData> elementPath;

    protected boolean typeEquals(MkvElement other) {
        if (!other.getClass().equals(this.getClass())) {
            return false;
        }
        return elementMetaData.getTypeInfo().equals(other.getElementMetaData().getTypeInfo());
    }

    public abstract boolean isMaster();

    public abstract void accept(MkvElementVisitor visitor) throws MkvElementVisitException;

    public abstract boolean equivalent(MkvElement other);

    public void writeToChannel(WritableByteChannel outputChannel) throws MkvElementVisitException {

    }

    protected void writeByteBufferToChannel(ByteBuffer src, WritableByteChannel outputChannel)
            throws MkvElementVisitException {
        src.rewind();
        int size = src.remaining();
        try {
            int numBytes = outputChannel.write(src);
            Validate.isTrue(size == numBytes, "Output channel wrote " + size + " bytes instead of " + numBytes);
        } catch (IOException e) {
            throw new MkvElementVisitException("Writing to output channel failed", e);
        } finally {
            src.rewind();
        }
    }
}
