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

import com.amazonaws.kinesisvideo.parser.ebml.EBMLTypeInfo;
import com.amazonaws.kinesisvideo.parser.mkv.MkvDataElement;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElement;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitor;
import com.amazonaws.kinesisvideo.parser.mkv.MkvEndMasterElement;
import com.amazonaws.kinesisvideo.parser.mkv.MkvStartMasterElement;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;

/**
 * This collects and stores all the child elements for a particular master element.
 * For MkvDataElements, it copies the values and stores them to make sure they can
 * be accessed after the iterator for the mkvstream parser has moved on.
 */
@Slf4j
public class MkvChildElementCollector extends MkvElementVisitor {
    @Getter
    private final EBMLTypeInfo parentTypeInfo;
    private final List<MkvElement> collectedElements = new ArrayList<>();

    public MkvChildElementCollector(EBMLTypeInfo parentTypeInfo) {
        Validate.isTrue(parentTypeInfo.getType().equals(EBMLTypeInfo.TYPE.MASTER),
                "ChildElementCollectors can only collect children for master elements");
        log.info("MkvChildElementCollector for element {}", parentTypeInfo);
        this.parentTypeInfo = parentTypeInfo;
    }

    @Override
    public void visit(MkvStartMasterElement startMasterElement) {
        if (isParentType(startMasterElement) || shouldBeCollected(startMasterElement)) {
            //if this is the parent info itself, add it.
            log.debug("Add start master element {} to collector ", startMasterElement);
            collectedElements.add(startMasterElement);
        }
    }


    @Override
    public void visit(MkvEndMasterElement endMasterElement) {
        if (isParentType(endMasterElement) || shouldBeCollected(endMasterElement)) {
            //if this is the parent info itself, add it.
            log.debug("Add end master element {} to collector ", endMasterElement);
            collectedElements.add(endMasterElement);
        }
    }

    @Override
    public void visit(MkvDataElement dataElement) {
        if (shouldBeCollected(dataElement)) {
            log.debug("Copy value and add data element {}  to collector ", dataElement);
            dataElement.getValueCopy();
            collectedElements.add(dataElement);
        }
    }

    public List<MkvElement> copyOfCollection(){
        return new ArrayList<>(collectedElements);
    }

    public void clearCollection() {
        collectedElements.clear();
    }

    /**
     * Check if the collected children in this collector are the same as those from another collection ?
     * We are not checking for full equality since the element number embedded in the meta-data wll be different.
     * It compares the typeinfo and the saved values.
     * @param otherChildren The children of the other collector.
     * @return True if the typeinfo and the saved values of the children of the other collector are the same.
     */
    public boolean equivalent(List<MkvElement> otherChildren) {
        if (collectedElements.size() != otherChildren.size()) {
            return false;
        }
        for (int i=0; i < collectedElements.size(); i++) {
            MkvElement collectedElement = collectedElements.get(i);
            MkvElement otherElement = otherChildren.get(i);
            if (!collectedElement.getClass().equals(otherElement.getClass())) {
                return false;
            }
            if (!collectedElement.equivalent(otherElement)) {
                return false;
            }
        }
        return true;
    }

    private boolean isParentType(MkvElement startMasterElement) {
        return startMasterElement.getElementMetaData().getTypeInfo().equals(parentTypeInfo);
    }

    //NOTE: check if this should be relaxed to only look for the parent anywhere in
    //the path.
    //TODO: deal with recursive element with search
    private boolean shouldBeCollected(MkvElement mkvElement) {
        if (mkvElement.getElementPath().size() <= parentTypeInfo.getLevel()) {
            //If the element belongs to a level lower than the parent's level, the path may be shorter
            //than the parent's level. We do not want to collect such elements.
            if (mkvElement.getElementMetaData().getTypeInfo().getLevel() > parentTypeInfo.getLevel()) {
                log.warn("Element {} has a path {} shorter than parent type's {} level but does not belong to "
                                + "a lower level than the parent ",
                        mkvElement.getElementMetaData().toString(),
                        mkvElement.getElementPath().size(),
                        parentTypeInfo.toString());
            }
            return false;
        }
        return mkvElement.getElementPath().get(parentTypeInfo.getLevel()).getTypeInfo().equals(parentTypeInfo);
    }

}
