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
import lombok.Getter;
import lombok.ToString;


/**
 * Class that represents the metadata of a single EBML element in an EBML stream.
 * It does not contain the actual data or content of the EBML element.
 */
@Getter
@Builder
@ToString
@EqualsAndHashCode
public class EBMLElementMetaData {
    private final EBMLTypeInfo typeInfo;
    private final long elementNumber;

    public boolean isMaster() {
        return typeInfo.getType() == EBMLTypeInfo.TYPE.MASTER;
    }
}
