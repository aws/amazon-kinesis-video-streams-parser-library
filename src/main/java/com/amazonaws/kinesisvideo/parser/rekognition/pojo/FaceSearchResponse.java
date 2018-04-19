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
public class FaceSearchResponse implements Serializable
{

    @JsonProperty("DetectedFace")
    private DetectedFace detectedFace;
    @JsonProperty("MatchedFaces")
    private List<MatchedFace> matchedFaces = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    private final static long serialVersionUID = -5645575235038800306L;

    @JsonProperty("DetectedFace")
    public DetectedFace getDetectedFace() {
        return detectedFace;
    }

    @JsonProperty("DetectedFace")
    public void setDetectedFace(DetectedFace detectedFace) {
        this.detectedFace = detectedFace;
    }

    @JsonProperty("MatchedFaces")
    public List<MatchedFace> getMatchedFaces() {
        return matchedFaces;
    }

    @JsonProperty("MatchedFaces")
    public void setMatchedFaces(List<MatchedFace> matchedFaces) {
        this.matchedFaces = matchedFaces;
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
                .append("detectedFace", detectedFace)
                .append("matchedFaces", matchedFaces)
                .append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(matchedFaces)
                .append(detectedFace)
                .append(additionalProperties).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof FaceSearchResponse) == false) {
            return false;
        }
        FaceSearchResponse rhs = ((FaceSearchResponse) other);
        return new EqualsBuilder()
                .append(matchedFaces, rhs.matchedFaces)
                .append(detectedFace, rhs.detectedFace)
                .append(additionalProperties, rhs.additionalProperties).isEquals();
    }

}
