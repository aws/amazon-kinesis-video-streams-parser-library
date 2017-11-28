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
import lombok.ToString;

import java.util.List;

/**
 * Class representing the end of a mkv master element.
 * It contains the metadata {@link EBMLElementMetaData} and the path if specified.
 */

@ToString(callSuper = true)
public class MkvEndMasterElement extends MkvElement {
    @Builder
    private MkvEndMasterElement(EBMLElementMetaData elementMetaData, List<EBMLElementMetaData> elementPath) {
        super(elementMetaData, elementPath);
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
        return typeEquals(other);
    }
}
