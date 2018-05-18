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

import com.amazonaws.kinesisvideo.parser.TestResourceUtil;
import com.amazonaws.kinesisvideo.parser.ebml.InputStreamParserByteSource;
import com.amazonaws.kinesisvideo.parser.ebml.MkvTypeInfos;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitException;
import com.amazonaws.kinesisvideo.parser.mkv.StreamingMkvReader;
import com.amazonaws.kinesisvideo.parser.mkv.visitors.CountVisitor;
import com.amazonaws.kinesisvideo.parser.utilities.FragmentMetadata;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Tests for MergedOutputPiper
 */
public class MergedOutputPiperTest {
    //Can be overridden by setting the environment variable PATH_TO_CAT
    private static final String DEFAULT_PATH_TO_CAT = "/bin/cat";
    //Can be overridden by setting the environment variable PATH_TO_GSTREAMER
    private static final String DEFAULT_PATH_TO_GSTREAMER = "/usr/bin/gst-launch-1.0";
    private boolean canRunBasicTest;
    private String pathToCat;
    private boolean canRunGStreamer;
    private String pathToGStreamer;

    private Optional<String> processedFragmentNumberString;

    @Before
    public void setup() {
        pathToCat = pathToExecutable("PATH_TO_CAT", DEFAULT_PATH_TO_CAT);
        canRunBasicTest = checkIfFileExists(pathToCat);
        pathToGStreamer = pathToExecutable("PATH_TO_GSTREAMER", DEFAULT_PATH_TO_GSTREAMER);
        canRunGStreamer = checkIfFileExists(pathToGStreamer);
        processedFragmentNumberString = Optional.empty();
    }

    private String pathToExecutable(String environmentVariable, String defaultPath) {
        final String environmentVariableValue = System.getenv(environmentVariable);
        return StringUtils.isEmpty(environmentVariableValue) ? defaultPath : environmentVariableValue;
    }


    private boolean checkIfFileExists(String pathToFile) {
        return new File(pathToFile).exists();
    }


    @Test
    public void testBasic() throws IOException, MkvElementVisitException {
        if (canRunBasicTest) {
            String fileName = "output_get_media.mkv";
            runBasicTestForFile(fileName, "91343852333181432412489103236310005892133364608", 5);
        }
    }

    @Test
    public void testBasicNonIncreasingTimecode() throws IOException, MkvElementVisitException {
        if (canRunBasicTest) {
            String fileName = "output-get-media-non-increasing-timecode.mkv";
            runBasicTestForFile(fileName, "91343852338381293673923423239754896920603583280", 1);
        }
    }

    @Ignore
    @Test
    public void testGStreamerVideoSink() throws IOException, MkvElementVisitException {
        if (canRunGStreamer) {
            Path tmpFilePathToStdout = Files.createTempFile("testGStreamer", "stdout");
            Path tmpFilePathToStdErr = Files.createTempFile("testGStreamer", "stderr");
            try {
                InputStream is = TestResourceUtil.getTestInputStream("output_get_media.mkv");
                ProcessBuilder processBuilder = new ProcessBuilder().command(pathToGStreamer,
                        "-v",
                        "fdsrc",
                        "!",
                        "decodebin",
                        "!",
                        "videoconvert",
                        "!",
                        "autovideosink")
                        .redirectOutput(tmpFilePathToStdout.toFile())
                        .redirectError(tmpFilePathToStdErr.toFile());
                MergedOutputPiper piper = new MergedOutputPiper(processBuilder);
                piper.process(is, this::setProcessedFragmentNumberString);
                Assert.assertEquals("91343852333181432412489103236310005892133364608",
                        processedFragmentNumberString.get());
                Assert.assertEquals(5, piper.getMergedSegments());
            } finally {
                Files.delete(tmpFilePathToStdout);
                Files.delete(tmpFilePathToStdErr);
            }
        }
    }

    @Test
    public void testGStreamerFileSink() throws IOException, MkvElementVisitException {
        if (canRunGStreamer) {
            Path tmpFilePathToStdout = Files.createTempFile("testGStreamerFileSink", "stdout");
            Path tmpFilePathToStdErr = Files.createTempFile("testGStreamerFileSink", "stderr");
            Path tmpFilePathToOutputFile = Files.createTempFile("testGStreamerFileSink", "output.mkv");
            try {
                InputStream is = TestResourceUtil.getTestInputStream("output_get_media.mkv");
                ProcessBuilder processBuilder = new ProcessBuilder().command(pathToGStreamer,
                        "-v",
                        "fdsrc",
                        "!",
                        "filesink",
                        "location=" + tmpFilePathToOutputFile.toAbsolutePath().toString())
                        .redirectOutput(tmpFilePathToStdout.toFile())
                        .redirectError(tmpFilePathToStdErr.toFile());
                MergedOutputPiper piper = new MergedOutputPiper(processBuilder);
                piper.process(is, this::setProcessedFragmentNumberString);

                CountVisitor countVisitor =
                        CountVisitor.create(MkvTypeInfos.CLUSTER, MkvTypeInfos.SEGMENT, MkvTypeInfos.SIMPLEBLOCK);
                StreamingMkvReader.createDefault(new InputStreamParserByteSource(new FileInputStream(
                        tmpFilePathToOutputFile.toFile()))).apply(countVisitor);
                Assert.assertEquals(5, countVisitor.getCount(MkvTypeInfos.CLUSTER));
                Assert.assertEquals(1, countVisitor.getCount(MkvTypeInfos.SEGMENT));
                Assert.assertEquals(300, countVisitor.getCount(MkvTypeInfos.SIMPLEBLOCK));
            } finally {
                Files.delete(tmpFilePathToStdout);
                Files.delete(tmpFilePathToStdErr);
                Files.delete(tmpFilePathToOutputFile);
            }
        }
    }

    @Test
    public void testGStreamerFileSinkWithFactory() throws IOException, MkvElementVisitException {
        if (canRunGStreamer) {
            Path tmpFilePathToOutputFile = Files.createTempFile("testGStreamerFileSink", "output.mkv");
            MergedOutputPiperFactory piperFactory = new MergedOutputPiperFactory(pathToGStreamer,
                    "-v",
                    "fdsrc",
                    "!",
                    "filesink",
                    "location=" + tmpFilePathToOutputFile.toAbsolutePath().toString());
            try {
                InputStream is = TestResourceUtil.getTestInputStream("output_get_media.mkv");
                GetMediaResponseStreamConsumer piper = piperFactory.createConsumer();
                Assert.assertTrue(piper.getClass().equals(MergedOutputPiper.class));
                piper.process(is, this::setProcessedFragmentNumberString);

                CountVisitor countVisitor =
                        CountVisitor.create(MkvTypeInfos.CLUSTER, MkvTypeInfos.SEGMENT, MkvTypeInfos.SIMPLEBLOCK);
                StreamingMkvReader.createDefault(new InputStreamParserByteSource(new FileInputStream(
                        tmpFilePathToOutputFile.toFile()))).apply(countVisitor);
                Assert.assertEquals(5, countVisitor.getCount(MkvTypeInfos.CLUSTER));
                Assert.assertEquals(1, countVisitor.getCount(MkvTypeInfos.SEGMENT));
                Assert.assertEquals(300, countVisitor.getCount(MkvTypeInfos.SIMPLEBLOCK));
            } finally {
                Files.delete(tmpFilePathToOutputFile);
            }
        }
    }

    private void runBasicTestForFile(String fileName,
            String expectedFragmentNumberToStartAfter,
            int expectedNumMergedSegments) throws IOException, MkvElementVisitException {
        StopWatch timer = new StopWatch();
        timer.start();

        InputStream is = TestResourceUtil.getTestInputStream(fileName);
        Path tmpFilePath = Files.createTempFile("basicTest:" + fileName + ":", "merged.mkv");

        try {
            ProcessBuilder processBuilder =
                    new ProcessBuilder().command(pathToCat).redirectOutput(tmpFilePath.toFile());
            MergedOutputPiper piper = new MergedOutputPiper(processBuilder);
            piper.process(is, this::setProcessedFragmentNumberString);
            timer.stop();
            Assert.assertEquals(expectedFragmentNumberToStartAfter, processedFragmentNumberString.get());
            Assert.assertEquals(expectedNumMergedSegments, piper.getMergedSegments());
        } finally {
            Files.delete(tmpFilePath);
        }
    }

    private Optional<String> setProcessedFragmentNumberString(FragmentMetadata f) {
        return processedFragmentNumberString = Optional.of(f.getFragmentNumberString());
    }

}
