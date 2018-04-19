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
public class Face implements Serializable
{

    @JsonProperty("BoundingBox")
    private BoundingBox boundingBox;
    @JsonProperty("FaceId")
    private String faceId;
    @JsonProperty("Confidence")
    private Double confidence;
    @JsonProperty("ImageId")
    private String imageId;
    @JsonProperty("ExternalImageId")
    private String externalImageId;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    private final static long serialVersionUID = 4320869723686571816L;

    @JsonProperty("BoundingBox")
    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    @JsonProperty("BoundingBox")
    public void setBoundingBox(BoundingBox boundingBox) {
        this.boundingBox = boundingBox;
    }

    @JsonProperty("FaceId")
    public String getFaceId() {
        return faceId;
    }

    @JsonProperty("FaceId")
    public void setFaceId(String faceId) {
        this.faceId = faceId;
    }

    @JsonProperty("Confidence")
    public Double getConfidence() {
        return confidence;
    }

    @JsonProperty("Confidence")
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    @JsonProperty("ImageId")
    public String getImageId() {
        return imageId;
    }

    @JsonProperty("ImageId")
    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    @JsonProperty("ExternalImageId")
    public String getExternalImageId() {
        return externalImageId;
    }

    @JsonProperty("ExternalImageId")
    public void setExternalImageId(String externalImageId) {
        this.externalImageId = externalImageId;
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
        return new ToStringBuilder(this).append("boundingBox", boundingBox)
                .append("faceId", faceId).append("confidence", confidence)
                .append("imageId", imageId)
                .append("externalImageId", externalImageId)
                .append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(boundingBox)
                .append(imageId)
                .append(externalImageId)
                .append(faceId)
                .append(additionalProperties).append(confidence).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Face) == false) {
            return false;
        }
        Face rhs = ((Face) other);
        return new EqualsBuilder().append(boundingBox, rhs.boundingBox)
                .append(imageId, rhs.imageId)
                .append(externalImageId, rhs.externalImageId)
                .append(faceId, rhs.faceId).append(additionalProperties, rhs.additionalProperties)
                .append(confidence, rhs.confidence).isEquals();
    }

}
