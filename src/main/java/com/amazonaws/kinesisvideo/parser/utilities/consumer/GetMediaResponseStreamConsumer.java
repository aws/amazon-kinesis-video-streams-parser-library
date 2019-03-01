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

import com.amazonaws.kinesisvideo.parser.ebml.InputStreamParserByteSource;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitException;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitor;
import com.amazonaws.kinesisvideo.parser.mkv.StreamingMkvReader;

import java.io.IOException;
import java.io.InputStream;

/**
 * This base class is used to consume the output of a GetMedia* call to Kinesis Video in a streaming fashion.
 * The first parameter for process method is the payload inputStream in a GetMediaResult returned by a call to GetMedia.
 * Implementations of the process method of this interface should block until all the data in the inputStream has been
 * processed or the process method decides to stop for some other reason. The FragmentMetadataCallback is invoked at
 * the end of every processed fragment.
 */
public abstract class GetMediaResponseStreamConsumer implements AutoCloseable {

    public abstract void process(InputStream inputStream, FragmentMetadataCallback callback)
            throws MkvElementVisitException, IOException;

    protected void processWithFragmentEndCallbacks(InputStream inputStream,
            FragmentMetadataCallback endOfFragmentCallback,
            MkvElementVisitor mkvElementVisitor) throws MkvElementVisitException {
        StreamingMkvReader.createDefault(new InputStreamParserByteSource(inputStream))
                .apply(FragmentProgressTracker.create(mkvElementVisitor, endOfFragmentCallback));
    }

    @Override
    public void close() {
        //No op close. Derived classes should implement this method to meaningfully handle cleanup of the resources.
    }
}
