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

import java.nio.ByteBuffer;
import java.util.List;

/**
 * The EBMLParser invokes these callbacks when it detects the start, end and contents of elements.
 */
public interface EBMLParserCallbacks {
    void onStartElement(EBMLElementMetaData elementMetaData,
            long elementDataSize,
            ByteBuffer idAndSizeRawBytes,
            ElementPathSupplier pathSupplier);

    void onPartialContent(EBMLElementMetaData elementMetaData, ParserBulkByteSource bulkByteSource, int bytesToRead);

    void onEndElement(EBMLElementMetaData elementMetaData, ElementPathSupplier pathSupplier);

    default boolean continueParsing() {
        return true;
    }

    @FunctionalInterface
    interface ElementPathSupplier {
        List<EBMLElementMetaData> getAncestors();
    }

}
