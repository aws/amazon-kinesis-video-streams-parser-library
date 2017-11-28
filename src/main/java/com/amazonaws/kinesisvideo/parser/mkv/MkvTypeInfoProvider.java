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

import com.amazonaws.kinesisvideo.parser.ebml.EBMLTypeInfo;
import com.amazonaws.kinesisvideo.parser.ebml.EBMLTypeInfoProvider;
import com.amazonaws.kinesisvideo.parser.ebml.MkvTypeInfos;
import org.apache.commons.lang3.Validate;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A class to provide the type information for the EBML elements used by Mkv.
 * This type information is used by the EBML parser.
 */
public class MkvTypeInfoProvider implements EBMLTypeInfoProvider {
    private final Map<Integer,EBMLTypeInfo> typeInfoMap = new HashMap();

    public void load() throws IllegalAccessException {
        for (Field field : MkvTypeInfos.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType().equals(EBMLTypeInfo.class)) {
                EBMLTypeInfo type = (EBMLTypeInfo )field.get(null);
                Validate.isTrue(!typeInfoMap.containsKey(type.getId()));
                typeInfoMap.put(type.getId(), type);
            }
        }
    }


    @Override
    public Optional<EBMLTypeInfo> getType(int id) {
        return Optional.ofNullable(typeInfoMap.get(id));
    }
}
