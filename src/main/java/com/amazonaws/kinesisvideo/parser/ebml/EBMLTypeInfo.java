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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * The type information for an EBML element.
 * This specifies the semantics of the EBML elements in an EBML document.
 * For example the TypeInfo for MKV will specify the semantics for the EBML elements that make up a MKV document.
 */

@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Getter
@ToString
@EqualsAndHashCode
public class EBMLTypeInfo {
    private final int id;
    private final String name;
    private final int level;
    private final TYPE type;
    @Builder.Default
    private boolean isRecursive = false;

    public boolean isGlobal() {
        return level < 0;
    }

    public enum TYPE { INTEGER, UINTEGER, FLOAT, STRING, UTF_8, DATE, MASTER, BINARY }
}
