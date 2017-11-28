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

import org.apache.commons.lang3.Validate;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


/**
 * A EBMLTypeInfoProvider for unit tests that provides only a very samll subset of the Mkv Elements.
 */
public class TestEBMLTypeInfoProvider implements EBMLTypeInfoProvider {
    public static final EBMLTypeInfo EBML = new EBMLTypeInfo.EBMLTypeInfoBuilder().id(0x1A45DFA3).name("EBML").level(0).type(
            EBMLTypeInfo.TYPE.MASTER).build();
    public static final EBMLTypeInfo EBMLVersion = new EBMLTypeInfo.EBMLTypeInfoBuilder().id(0x4286).name("EBMLVersion").level(1).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
    public static final EBMLTypeInfo EBMLReadVersion = new EBMLTypeInfo.EBMLTypeInfoBuilder().id(0x42F7).name("EBMLReadVersion").level(1).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
    public static final EBMLTypeInfo CRC = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("CRC-32").id(0xBF).level(-1).type(
            EBMLTypeInfo.TYPE.BINARY).build();

    public static final EBMLTypeInfo SEGMENT = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Segment").level(0).id(0x18538067).type(
            EBMLTypeInfo.TYPE.MASTER).build();
    public static final EBMLTypeInfo SEEKHEAD = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("SeekHead").level(1).id(0x114D9B74).type(
            EBMLTypeInfo.TYPE.MASTER).build();
    public static final EBMLTypeInfo SEEK = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Seek").level(2).id(0x4DBB).type(
            EBMLTypeInfo.TYPE.MASTER).build();
    public static final EBMLTypeInfo SEEKID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("SeekID").level(3).id(0x53AB).type(
            EBMLTypeInfo.TYPE.BINARY).build();
    public static final EBMLTypeInfo SEEKPOSITION = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("SeekPosition").level(3).id(0x53AC).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();



    private Map<Integer,EBMLTypeInfo> typeInfoMap = new HashMap();
    public TestEBMLTypeInfoProvider() throws IllegalAccessException {
        for (Field field : TestEBMLTypeInfoProvider.class.getDeclaredFields()) {
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
