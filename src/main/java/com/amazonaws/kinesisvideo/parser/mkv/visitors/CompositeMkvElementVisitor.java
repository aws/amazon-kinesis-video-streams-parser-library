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
package com.amazonaws.kinesisvideo.parser.mkv.visitors;

import com.amazonaws.kinesisvideo.parser.mkv.MkvDataElement;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElement;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitException;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitor;
import com.amazonaws.kinesisvideo.parser.mkv.MkvEndMasterElement;
import com.amazonaws.kinesisvideo.parser.mkv.MkvStartMasterElement;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Class represents a composite visitor made out of multiple visitors.
 *
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
public class CompositeMkvElementVisitor extends MkvElementVisitor {
    protected final List<MkvElementVisitor> childVisitors;

    public CompositeMkvElementVisitor(MkvElementVisitor... visitors){
        childVisitors = new ArrayList<>();
        for (MkvElementVisitor visitor : visitors) {
            childVisitors.add(visitor);
        }
    }

    @Override
    public void visit(MkvStartMasterElement startMasterElement) throws MkvElementVisitException {
        visitAll(startMasterElement);
    }

    @Override
    public void visit(MkvEndMasterElement endMasterElement) throws MkvElementVisitException {
        visitAll(endMasterElement);
    }

    @Override
    public void visit(MkvDataElement dataElement) throws MkvElementVisitException {
        visitAll(dataElement);
    }

    private void visitAll(MkvElement element) throws MkvElementVisitException {
        try {
            for (MkvElementVisitor childVisitor : childVisitors) {
                if (log.isDebugEnabled()) {
                    log.debug("Composite visitor calling {} on element {}",
                            childVisitor.getClass().toString(),
                            element.toString());
                }
                element.accept(childVisitor);
            }
        } catch (MkvElementVisitException e) {
            throw new MkvElementVisitException("Composite Visitor caught exception ", e);
        }
    }
}
