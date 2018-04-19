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
import java.util.List;
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
public class RekognitionOutput implements Serializable
{

    @JsonProperty("InputInformation")
    private InputInformation inputInformation;
    @JsonProperty("StreamProcessorInformation")
    private StreamProcessorInformation streamProcessorInformation;
    @JsonProperty("FaceSearchResponse")
    private List<FaceSearchResponse> faceSearchResponse = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    private final static long serialVersionUID = -4243167512470204665L;

    @JsonProperty("InputInformation")
    public InputInformation getInputInformation() {
        return inputInformation;
    }

    @JsonProperty("InputInformation")
    public void setInputInformation(InputInformation inputInformation) {
        this.inputInformation = inputInformation;
    }

    @JsonProperty("StreamProcessorInformation")
    public StreamProcessorInformation getStreamProcessorInformation() {
        return streamProcessorInformation;
    }

    @JsonProperty("StreamProcessorInformation")
    public void setStreamProcessorInformation(StreamProcessorInformation streamProcessorInformation) {
        this.streamProcessorInformation = streamProcessorInformation;
    }

    @JsonProperty("FaceSearchResponse")
    public List<FaceSearchResponse> getFaceSearchResponse() {
        return faceSearchResponse;
    }

    @JsonProperty("FaceSearchResponse")
    public void setFaceSearchResponse(List<FaceSearchResponse> faceSearchResponse) {
        this.faceSearchResponse = faceSearchResponse;
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
                .append("inputInformation", inputInformation)
                .append("streamProcessorInformation", streamProcessorInformation)
                .append("faceSearchResponse", faceSearchResponse)
                .append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(inputInformation)
                .append(additionalProperties)
                .append(faceSearchResponse)
                .append(streamProcessorInformation).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RekognitionOutput) == false) {
            return false;
        }
        RekognitionOutput rhs = ((RekognitionOutput) other);
        return new EqualsBuilder()
                .append(inputInformation, rhs.inputInformation)
                .append(additionalProperties, rhs.additionalProperties)
                .append(faceSearchResponse, rhs.faceSearchResponse)
                .append(streamProcessorInformation, rhs.streamProcessorInformation).isEquals();
    }

}
