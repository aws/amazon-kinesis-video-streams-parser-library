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
public class Pose implements Serializable
{

    @JsonProperty("Pitch")
    private Double pitch;
    @JsonProperty("Roll")
    private Double roll;
    @JsonProperty("Yaw")
    private Double yaw;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    private final static long serialVersionUID = 5134659150043632590L;

    @JsonProperty("Pitch")
    public Double getPitch() {
        return pitch;
    }

    @JsonProperty("Pitch")
    public void setPitch(Double pitch) {
        this.pitch = pitch;
    }

    @JsonProperty("Roll")
    public Double getRoll() {
        return roll;
    }

    @JsonProperty("Roll")
    public void setRoll(Double roll) {
        this.roll = roll;
    }

    @JsonProperty("Yaw")
    public Double getYaw() {
        return yaw;
    }

    @JsonProperty("Yaw")
    public void setYaw(Double yaw) {
        this.yaw = yaw;
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
                .append("pitch", pitch)
                .append("roll", roll)
                .append("yaw", yaw)
                .append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(yaw)
                .append(roll)
                .append(additionalProperties)
                .append(pitch).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Pose) == false) {
            return false;
        }
        Pose rhs = ((Pose) other);
        return new EqualsBuilder()
                .append(yaw, rhs.yaw)
                .append(roll, rhs.roll)
                .append(additionalProperties, rhs.additionalProperties)
                .append(pitch, rhs.pitch).isEquals();
    }

}
