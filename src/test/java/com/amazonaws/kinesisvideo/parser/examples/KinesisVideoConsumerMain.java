package com.amazonaws.kinesisvideo.parser.examples;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

public class KinesisVideoConsumerMain {

    @Test
    public void testExample() throws InterruptedException, IOException {
        KinesisVideoConsumer example = KinesisVideoConsumer.builder().region(Regions.US_WEST_2)
                .streamName("example")
                .credentialsProvider(new ProfileCredentialsProvider())
                .build();

        example.execute();
    }

}


