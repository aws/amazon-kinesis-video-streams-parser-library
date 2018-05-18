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
package com.amazonaws.kinesisvideo.parser.utilities.consumer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This factory class creates MergedOutputPiper consumers based on a particular target ProcessBuilder.
 */
public class MergedOutputPiperFactory extends GetMediaResponseStreamConsumerFactory {
    private final Optional<String> directoryOptional;
    private final List<String> commandList;
    private final boolean redirectOutputAndError;

    public MergedOutputPiperFactory(String... commands) {
        this(Optional.empty(), commands);
    }

    public MergedOutputPiperFactory(Optional<String> directoryOptional, String... commands) {
        this(directoryOptional, false, commands);
    }

    private MergedOutputPiperFactory(Optional<String> directoryOptional,
            boolean redirectOutputAndError,
            String... commands) {
        this.directoryOptional = directoryOptional;
        this.commandList = new ArrayList();
        for (String command : commands) {
            commandList.add(command);
        }
        this.redirectOutputAndError = redirectOutputAndError;
    }

    public MergedOutputPiperFactory(Optional<String> directoryOptional,
            boolean redirectOutputAndError,
            List<String> commandList) {
        this.directoryOptional = directoryOptional;
        this.commandList = commandList;
        this.redirectOutputAndError = redirectOutputAndError;
    }

    @Override
    public GetMediaResponseStreamConsumer createConsumer() throws IOException{
        ProcessBuilder builder = new ProcessBuilder().command(commandList);
        directoryOptional.ifPresent(d -> builder.directory(new File(d)));
        if (redirectOutputAndError) {
            builder.redirectOutput(Files.createFile(Paths.get(redirectedFileName("stdout"))).toFile());
            builder.redirectError(Files.createFile(Paths.get(redirectedFileName("stderr"))).toFile());
        }
        return new MergedOutputPiper(builder);

    }

    private String redirectedFileName(String suffix) {
        return "MergedOutputPiper-"+System.currentTimeMillis()+"-"+suffix;
    }
}
