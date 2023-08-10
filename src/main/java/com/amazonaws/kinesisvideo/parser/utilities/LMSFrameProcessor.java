package com.amazonaws.kinesisvideo.parser.utilities;

import com.amazonaws.kinesisvideo.parser.mkv.Frame;
import com.amazonaws.kinesisvideo.parser.utilities.FragmentMetadataVisitor;
import com.amazonaws.kinesisvideo.parser.utilities.MkvTrackMetadata;
import com.google.common.base.Strings;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Optional;

public class LMSFrameProcessor implements FrameVisitor.FrameProcessor {

    private OutputStream outputStreamFromCustomer;
    private OutputStream outputStreamToCustomer;
    private FragmentMetadataVisitor fragmentMetadataVisitor;

    protected LMSFrameProcessor(OutputStream outputStreamFromCustomer, OutputStream outputStreamToCustomer, FragmentMetadataVisitor fragmentMetadataVisitor) {
        this.outputStreamFromCustomer = outputStreamFromCustomer;
        this.outputStreamToCustomer = outputStreamToCustomer;
    }

    public static LMSFrameProcessor create(OutputStream outputStreamFromCustomer, OutputStream outputStreamToCustomer, FragmentMetadataVisitor fragmentMetadataVisitor) {
        return new LMSFrameProcessor(outputStreamFromCustomer, outputStreamToCustomer, fragmentMetadataVisitor);
    }

    @Override
    public void process(Frame frame, MkvTrackMetadata trackMetadata, Optional<FragmentMetadata> fragmentMetadata) {
        saveToOutPutStream(frame);
    }

    private void saveToOutPutStream(final Frame frame) {
        ByteBuffer frameBuffer = frame.getFrameData();
        long trackNumber = frame.getTrackNumber();
        MkvTrackMetadata metadata = fragmentMetadataVisitor.getMkvTrackMetadata(trackNumber);
        String trackName = metadata.getTrackName();

        try {
            byte[] frameBytes = new byte[frameBuffer.remaining()];
            frameBuffer.get(frameBytes);
            if (Strings.isNullOrEmpty(trackName) || "AUDIO_FROM_CUSTOMER".equals(trackName)) {
            outputStreamFromCustomer.write(frameBytes);
            } else if ("AUDIO_TO_CUSTOMER".equals(trackName)) {
            outputStreamToCustomer.write(frameBytes);
            } else {
              // Unknown track name. Not writing to output stream.
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}