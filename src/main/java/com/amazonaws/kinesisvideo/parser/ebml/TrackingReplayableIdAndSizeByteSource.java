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

/**
 * An interface representing a byte source that can replay the bytes for ebml id and size.
 * It also keeps track of the total number of bytes read by the parser from the underlying
 * byte source.
 * This wraps a parser byte source passed in by the user.
 */
interface TrackingReplayableIdAndSizeByteSource {
    boolean checkAndReadIntoReplayBuffer(int len);

    int readByte();

    int availableForContent();

    void setReadOffsetForReplayBuffer(long readOffset);

    long getTotalBytesRead();
}
