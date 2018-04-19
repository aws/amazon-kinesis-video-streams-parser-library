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
public class DetectedFace implements Serializable
{

    @JsonProperty("BoundingBox")
    private BoundingBox boundingBox;
    @JsonProperty("Confidence")
    private Double confidence;
    @JsonProperty("Landmarks")
    private List<Landmark> landmarks = null;
    @JsonProperty("Pose")
    private Pose pose;
    @JsonProperty("Quality")
    private Quality quality;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    private final static long serialVersionUID = 4389260550207592384L;

    @JsonProperty("BoundingBox")
    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    @JsonProperty("BoundingBox")
    public void setBoundingBox(BoundingBox boundingBox) {
        this.boundingBox = boundingBox;
    }

    @JsonProperty("Confidence")
    public Double getConfidence() {
        return confidence;
    }

    @JsonProperty("Confidence")
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    @JsonProperty("Landmarks")
    public List<Landmark> getLandmarks() {
        return landmarks;
    }

    @JsonProperty("Landmarks")
    public void setLandmarks(List<Landmark> landmarks) {
        this.landmarks = landmarks;
    }

    @JsonProperty("Pose")
    public Pose getPose() {
        return pose;
    }

    @JsonProperty("Pose")
    public void setPose(Pose pose) {
        this.pose = pose;
    }

    @JsonProperty("Quality")
    public Quality getQuality() {
        return quality;
    }

    @JsonProperty("Quality")
    public void setQuality(Quality quality) {
        this.quality = quality;
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
                .append("boundingBox", boundingBox)
                .append("confidence", confidence)
                .append("landmarks", landmarks)
                .append("pose", pose)
                .append("quality", quality)
                .append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(pose)
                .append(boundingBox)
                .append(landmarks)
                .append(additionalProperties)
                .append(quality)
                .append(confidence).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof DetectedFace) == false) {
            return false;
        }
        DetectedFace rhs = ((DetectedFace) other);
        return new EqualsBuilder()
                .append(pose, rhs.pose)
                .append(boundingBox, rhs.boundingBox)
                .append(landmarks, rhs.landmarks)
                .append(additionalProperties, rhs.additionalProperties)
                .append(quality, rhs.quality)
                .append(confidence, rhs.confidence).isEquals();
    }

}
