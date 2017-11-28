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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import java.nio.ByteBuffer;

/**
 * A class to contain the values of MKV elements.
 */

@AllArgsConstructor
public class MkvValue<T extends Object> {
    @NonNull
    private final T val;
    @Getter
    private final long originalDataSize;

    public T getVal() {
        if (ByteBuffer.class.isAssignableFrom(val.getClass())) {
            ByteBuffer byteBufferVal = (ByteBuffer)val;
            byteBufferVal.rewind();
        }
        return val;
    }

    public boolean equals(Object otherObj) {
        if (otherObj == null) {
            return false;
        }
        if (!otherObj.getClass().equals(this.getClass())) {
            return false;
        }
        MkvValue other = (MkvValue) otherObj;
        if (!val.getClass().equals(other.val.getClass())) {
            return false;
        }
        if (originalDataSize != other.originalDataSize) {
            return false;
        }
        return getVal().equals(other.getVal());
    }

    @Override
    public int hashCode() {
        int result = val.hashCode();
        result = 31 * result + (int) (originalDataSize ^ (originalDataSize >>> 32));
        return result;
    }
}
