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
package com.amazonaws.kinesisvideo.parser.examples;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.kinesisvideo.parser.TestResourceUtil;
import com.amazonaws.kinesisvideo.parser.ebml.InputStreamParserByteSource;
import com.amazonaws.kinesisvideo.parser.ebml.MkvTypeInfos;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitException;
import com.amazonaws.kinesisvideo.parser.mkv.StreamingMkvReader;
import com.amazonaws.kinesisvideo.parser.mkv.visitors.CountVisitor;
import com.amazonaws.regions.Regions;
import lombok.Getter;
import org.jcodec.common.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test to execute Kinesis Video GStreamer Piper Example.
 * It passes in a pipeline that demuxes and remuxes the input mkv stream and writes to a file sink.
 * This can be used to demonstrate that the stream passed into the gstreamer pipeline is acceptable to it.
 */
public class KinesisVideoGStreamerPiperExampleTest {

    @Ignore
    @Test
    public void testExample() throws InterruptedException, IOException, MkvElementVisitException {
        final Path outputFilePath = Paths.get("output_from_gstreamer-"+System.currentTimeMillis()+".mkv");
        String gStreamerPipelineArgument =
                "matroskademux ! matroskamux! filesink location=" + outputFilePath.toAbsolutePath().toString();

        //Might need to update DEFAULT_PATH_TO_GSTREAMER variable in KinesisVideoGStreamerPiperExample class
        KinesisVideoGStreamerPiperExample example = KinesisVideoGStreamerPiperExample.builder().region(Regions.US_WEST_2)
                .streamName("myTestStream2")
                .credentialsProvider(new ProfileCredentialsProvider())
                .inputVideoStream(TestResourceUtil.getTestInputStream("clusters.mkv"))
                .gStreamerPipelineArgument(gStreamerPipelineArgument)
                .build();

        example.execute();

        //Verify that the generated output file has the expected number of segments, clusters and simple blocks.
        CountVisitor countVisitor =
                CountVisitor.create(MkvTypeInfos.SEGMENT, MkvTypeInfos.CLUSTER, MkvTypeInfos.SIMPLEBLOCK);
        StreamingMkvReader.createDefault(new InputStreamParserByteSource(Files.newInputStream(outputFilePath)))
                .apply(countVisitor);

        Assert.assertEquals(1,countVisitor.getCount(MkvTypeInfos.SEGMENT));
        Assert.assertEquals(8,countVisitor.getCount(MkvTypeInfos.CLUSTER));
        Assert.assertEquals(444,countVisitor.getCount(MkvTypeInfos.SIMPLEBLOCK));
    }
}
