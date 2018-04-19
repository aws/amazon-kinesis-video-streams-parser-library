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
package com.amazonaws.kinesisvideo.parser.rekognition.pojo;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class KinesisVideo implements Serializable
{

    @JsonProperty("StreamArn")
    private String streamArn;
    @JsonProperty("FragmentNumber")
    private String fragmentNumber;
    @JsonProperty("ServerTimestamp")
    private Double serverTimestamp;
    @JsonProperty("ProducerTimestamp")
    private Double producerTimestamp;
    @JsonProperty("FrameOffsetInSeconds")
    private Double frameOffsetInSeconds;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    private final static long serialVersionUID = 4018546116449531242L;

    @JsonProperty("StreamArn")
    public String getStreamArn() {
        return streamArn;
    }

    @JsonProperty("StreamArn")
    public void setStreamArn(String streamArn) {
        this.streamArn = streamArn;
    }

    @JsonProperty("FragmentNumber")
    public String getFragmentNumber() {
        return fragmentNumber;
    }

    @JsonProperty("FragmentNumber")
    public void setFragmentNumber(String fragmentNumber) {
        this.fragmentNumber = fragmentNumber;
    }

    @JsonProperty("ServerTimestamp")
    public Double getServerTimestamp() {
        return serverTimestamp;
    }

    @JsonProperty("ServerTimestamp")
    public void setServerTimestamp(Double serverTimestamp) {
        this.serverTimestamp = serverTimestamp;
    }

    @JsonProperty("ProducerTimestamp")
    public Double getProducerTimestamp() {
        return producerTimestamp;
    }

    @JsonProperty("ProducerTimestamp")
    public void setProducerTimestamp(Double producerTimestamp) {
        this.producerTimestamp = producerTimestamp;
    }

    @JsonProperty("FrameOffsetInSeconds")
    public Double getFrameOffsetInSeconds() {
        return frameOffsetInSeconds;
    }

    @JsonProperty("FrameOffsetInSeconds")
    public void setFrameOffsetInSeconds(Double frameOffsetInSeconds) {
        this.frameOffsetInSeconds = frameOffsetInSeconds;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("streamArn", streamArn)
                .append("fragmentNumber", fragmentNumber)
                .append("serverTimestamp", serverTimestamp)
                .append("producerTimestamp", producerTimestamp)
                .append("frameOffsetInSeconds", frameOffsetInSeconds)
                .append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(frameOffsetInSeconds)
                .append(fragmentNumber)
                .append(streamArn)
                .append(additionalProperties)
                .append(producerTimestamp)
                .append(serverTimestamp).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof KinesisVideo) == false) {
            return false;
        }
        KinesisVideo rhs = ((KinesisVideo) other);
        return new EqualsBuilder()
                .append(frameOffsetInSeconds, rhs.frameOffsetInSeconds)
                .append(fragmentNumber, rhs.fragmentNumber)
                .append(streamArn, rhs.streamArn)
                .append(additionalProperties, rhs.additionalProperties)
                .append(producerTimestamp, rhs.producerTimestamp)
                .append(serverTimestamp, rhs.serverTimestamp).isEquals();
    }

}
