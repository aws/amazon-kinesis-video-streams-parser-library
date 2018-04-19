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
public class BoundingBox implements Serializable
{

    @JsonProperty("Height")
    private Double height;
    @JsonProperty("Width")
    private Double width;
    @JsonProperty("Left")
    private Double left;
    @JsonProperty("Top")
    private Double top;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    private final static long serialVersionUID = -3845089061670074615L;

    @JsonProperty("Height")
    public Double getHeight() {
        return height;
    }

    @JsonProperty("Height")
    public void setHeight(Double height) {
        this.height = height;
    }

    @JsonProperty("Width")
    public Double getWidth() {
        return width;
    }

    @JsonProperty("Width")
    public void setWidth(Double width) {
        this.width = width;
    }

    @JsonProperty("Left")
    public Double getLeft() {
        return left;
    }

    @JsonProperty("Left")
    public void setLeft(Double left) {
        this.left = left;
    }

    @JsonProperty("Top")
    public Double getTop() {
        return top;
    }

    @JsonProperty("Top")
    public void setTop(Double top) {
        this.top = top;
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
                .append("height", height)
                .append("width", width)
                .append("left", left)
                .append("top", top)
                .append("additionalProperties", additionalProperties)
                .toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(height)
                .append(additionalProperties)
                .append(width)
                .append(left)
                .append(top)
                .toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof BoundingBox) == false) {
            return false;
        }
        BoundingBox rhs = ((BoundingBox) other);
        return new EqualsBuilder()
                .append(height, rhs.height)
                .append(additionalProperties, rhs.additionalProperties)
                .append(width, rhs.width)
                .append(left, rhs.left)
                .append(top, rhs.top)
                .isEquals();
    }

}
