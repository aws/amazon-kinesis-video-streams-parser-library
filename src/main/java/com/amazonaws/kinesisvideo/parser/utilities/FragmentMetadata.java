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
package com.amazonaws.kinesisvideo.parser.utilities;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.Validate;

import java.math.BigInteger;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Metadata for a Kinesis Video Fragment.
 */
@Getter @ToString
public class FragmentMetadata {
    private static final String FRAGMENT_NUMBER_KEY = "AWS_KINESISVIDEO_FRAGMENT_NUMBER";
    private static final String SERVER_SIDE_TIMESTAMP_KEY = "AWS_KINESISVIDEO_SERVER_TIMESTAMP";
    private static final String PRODCUER_SIDE_TIMESTAMP_KEY = "AWS_KINESISVIDEO_PRODUCER_TIMESTAMP";
    private static final String ERROR_CODE_KEY = "AWS_KINESISVIDEO_ERROR_CODE";
    private static final String ERROR_ID_KEY = "AWS_KINESISVIDEO_ERROR_ID";

    private static final long MILLIS_PER_SECOND = TimeUnit.SECONDS.toMillis(1);

    private final String fragmentNumberString;
    private final long serverSideTimestampMillis;
    private final long producerSideTimestampMillis;
    private final BigInteger fragmentNumber;
    private final boolean success;
    private final long errorId;
    private final String errorCode;

    private FragmentMetadata(String fragmentNumberString,
            double serverSideTimestampSeconds,
            double producerSideTimestampSeconds) {
        this(fragmentNumberString,
                convertToMillis(serverSideTimestampSeconds),
                convertToMillis(producerSideTimestampSeconds),
                true,
                0,
                null);
    }

    private FragmentMetadata(String fragmentNumberString, long errorId, String errorCode) {
        this(fragmentNumberString, -1, -1, false, errorId, errorCode);
    }

    private FragmentMetadata(String fragmentNumberString,
            long serverSideTimestampMillis,
            long producerSideTimestampMillis,
            boolean success,
            long errorId,
            String errorCode) {
        this.fragmentNumberString = fragmentNumberString;
        this.fragmentNumber = new BigInteger(fragmentNumberString);
        this.serverSideTimestampMillis = serverSideTimestampMillis;
        this.producerSideTimestampMillis = producerSideTimestampMillis;
        this.success = success;
        this.errorId = errorId;
        this.errorCode = errorCode;
    }

    private static long convertToMillis(double serverSideTimestampSeconds) {
        return (long) Math.ceil(serverSideTimestampSeconds * MILLIS_PER_SECOND);
    }

    static FragmentMetadata createFromtagNametoValueMap(final Map<String, String> tagNameToTagValueMap) {
        if (tagNameToTagValueMap.containsKey(SERVER_SIDE_TIMESTAMP_KEY)) {
            //This is a successful fragment.

            return new FragmentMetadata(getValueForTag(tagNameToTagValueMap, FRAGMENT_NUMBER_KEY),
                    Double.parseDouble(getValueForTag(tagNameToTagValueMap, SERVER_SIDE_TIMESTAMP_KEY)),
                    Double.parseDouble(getValueForTag(tagNameToTagValueMap, PRODCUER_SIDE_TIMESTAMP_KEY)));
        } else {
            return new FragmentMetadata(getValueForTag(tagNameToTagValueMap, FRAGMENT_NUMBER_KEY),
                    Long.parseLong(getValueForTag(tagNameToTagValueMap, ERROR_ID_KEY)),
                    getValueForTag(tagNameToTagValueMap, ERROR_CODE_KEY));
        }
    }

    private static String getValueForTag(Map<String, String> tagNameToTagValueMap, String tagName) {
        String tagVal = tagNameToTagValueMap.get(tagName);
        return Validate.notEmpty(tagVal, "tagName " + tagName);
    }

    public Date getServerSideTimestampAsDate() {
        return new Date(this.serverSideTimestampMillis);
    }

    public Date getProducerSideTimetampAsDate() {
        return new Date(this.producerSideTimestampMillis);
    }

}
