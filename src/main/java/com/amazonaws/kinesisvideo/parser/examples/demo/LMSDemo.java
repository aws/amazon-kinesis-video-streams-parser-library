package com.amazonaws.kinesisvideo.parser.demo;

import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.AWSSessionCredentialsProvider;
import com.amazonaws.kinesisvideo.parser.examples.LMSExample;
import com.amazonaws.regions.Regions;


import java.io.FileOutputStream;
import java.io.IOException;

public class LMSDemo {

    public static void main(String args[]) throws InterruptedException, IOException {
        LMSExample example = new LMSExample(
            Regions.US_WEST_2,
            "<<StreamName>>",
            "<<FragmentNumber>>",
            new AWSSessionCredentialsProvider() {
                @Override
                public AWSSessionCredentials getCredentials() {
                    return new AWSSessionCredentials() {
                        @Override
                        public String getSessionToken() {
                            return "<<AWSSessionToken>>";
                        }

                        @Override
                        public String getAWSAccessKeyId() {
                            return "<<AWSAccessKey>>";
                        }

                        @Override
                        public String getAWSSecretKey() {
                            return "<<AWSSecretKey>>";
                        }
                    };
                }

                @Override
                public void refresh() {}
            },
            new FileOutputStream("<<FileName>>.raw"),
            new FileOutputStream("<<FileName>>.raw")
        );

        example.execute();
    }
}