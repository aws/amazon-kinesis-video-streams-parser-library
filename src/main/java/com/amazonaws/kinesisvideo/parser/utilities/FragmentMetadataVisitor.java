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

import com.amazonaws.kinesisvideo.parser.ebml.EBMLElementMetaData;
import com.amazonaws.kinesisvideo.parser.ebml.EBMLTypeInfo;
import com.amazonaws.kinesisvideo.parser.ebml.MkvTypeInfos;
import com.amazonaws.kinesisvideo.parser.mkv.MkvDataElement;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElement;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitException;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitor;
import com.amazonaws.kinesisvideo.parser.mkv.MkvEndMasterElement;
import com.amazonaws.kinesisvideo.parser.mkv.MkvStartMasterElement;
import com.amazonaws.kinesisvideo.parser.mkv.MkvValue;
import com.amazonaws.kinesisvideo.parser.mkv.visitors.CompositeMkvElementVisitor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class captures the fragment and track meta-data from the GetMedia output.
 * It uses multiple visitors to capture all the tags, track information as well as detecting the start and end
 * of segments and clusters.
 */
@Slf4j
public class FragmentMetadataVisitor extends CompositeMkvElementVisitor {
    private static final String MILLIS_BEHIND_NOW_KEY = "AWS_KINESISVIDEO_MILLIS_BEHIND_NOW";
    private static final String CONTINUATION_TOKEN_KEY = "AWS_KINESISVIDEO_CONTINUATION_TOKEN";

    private static final EBMLTypeInfo[] TRACK_TYPES = new EBMLTypeInfo [] {
        MkvTypeInfos.TRACKNUMBER,
            MkvTypeInfos.TRACKUID,
            MkvTypeInfos.NAME,
            MkvTypeInfos.CODECID,
            MkvTypeInfos.CODECNAME,
            MkvTypeInfos.CODECPRIVATE,
            MkvTypeInfos.PIXELWIDTH,
            MkvTypeInfos.PIXELHEIGHT
    };
    private static final String AWS_KINESISVIDEO_TAGNAME_PREFIX = "AWS_KINESISVIDEO";

    public interface MkvTagProcessor {
        default void process(MkvTag mkvTag, Optional<FragmentMetadata> currentFragmentMetadata) {
            throw new NotImplementedException("Default FragmentMetadataVisitor.MkvTagProcessor");
        }
        default void clear() {
            throw new NotImplementedException("Default FragmentMetadataVisitor.MkvTagProcessor");
        }
    }

    private final MkvChildElementCollector tagCollector;
    private final MkvChildElementCollector trackCollector;
    private final StateMachineVisitor stateMachineVisitor;

    private final Optional<MkvTagProcessor> mkvTagProcessor;

    private final Set<EBMLTypeInfo> trackTypesForTrackMetadata = new HashSet();

    @Getter
    private Optional<FragmentMetadata> previousFragmentMetadata = Optional.empty();

    @Getter
    private Optional<FragmentMetadata> currentFragmentMetadata = Optional.empty();

    private OptionalLong millisBehindNow = OptionalLong.empty();

    private Optional<String> continuationToken = Optional.empty();

    private final Map<BigInteger, MkvTrackMetadata> trackMetadataMap = new HashMap();

    private String tagName = null;
    private String tagValue = null;

    private FragmentMetadataVisitor(List<MkvElementVisitor> childVisitors,
                                    MkvChildElementCollector tagCollector,
                                    MkvChildElementCollector trackCollector,
                                    Optional<MkvTagProcessor> mkvTagProcessor) {
        super(childVisitors);
        Validate.isTrue(tagCollector.getParentTypeInfo().equals(MkvTypeInfos.TAGS));
        Validate.isTrue(trackCollector.getParentTypeInfo().equals(MkvTypeInfos.TRACKS));
        this.tagCollector = tagCollector;
        this.trackCollector = trackCollector;
        this.stateMachineVisitor = new StateMachineVisitor();
        this.childVisitors.add(stateMachineVisitor);
        this.mkvTagProcessor = mkvTagProcessor;
        for (EBMLTypeInfo trackType : TRACK_TYPES) {
            this.trackTypesForTrackMetadata.add(trackType);
        }
    }

    public static FragmentMetadataVisitor create() {
        return create(Optional.empty());
    }

    public static FragmentMetadataVisitor create(Optional<MkvTagProcessor> mkvTagProcessor) {
        final List<MkvElementVisitor> childVisitors = new ArrayList<>();
        final MkvChildElementCollector tagCollector = new MkvChildElementCollector(MkvTypeInfos.TAGS);
        final MkvChildElementCollector trackCollector = new MkvChildElementCollector(MkvTypeInfos.TRACKS);
        childVisitors.add(tagCollector);
        childVisitors.add(trackCollector);

        return new FragmentMetadataVisitor(childVisitors, tagCollector, trackCollector, mkvTagProcessor);
    }

    enum State {NEW, PRE_CLUSTER, IN_CLUSTER, POST_CLUSTER}
    private class StateMachineVisitor extends MkvElementVisitor {
        State state = State.NEW;

        @Override
        public void visit(MkvStartMasterElement startMasterElement) throws MkvElementVisitException {
            switch (state) {
                case NEW:
                    if (MkvTypeInfos.SEGMENT.equals(startMasterElement.getElementMetaData().getTypeInfo())) {
                        log.debug("Segment start {} changing state to PRE_CLUSTER", startMasterElement);
                        resetCollectedData();
                        state = State.PRE_CLUSTER;
                    }
                    break;
                case PRE_CLUSTER:
                    if (MkvTypeInfos.CLUSTER.equals(startMasterElement.getElementMetaData().getTypeInfo())) {
                        log.debug("Cluster start {} changing state to IN_CLUSTER", startMasterElement);
                        collectPreClusterInfo();
                        state = State.IN_CLUSTER;
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        public void visit(MkvEndMasterElement endMasterElement) throws MkvElementVisitException {
            switch (state) {
                case IN_CLUSTER:
                    if (MkvTypeInfos.CLUSTER.equals(endMasterElement.getElementMetaData().getTypeInfo())) {
                        state = State.POST_CLUSTER;
                    }
                    break;
                case POST_CLUSTER:
                    if (MkvTypeInfos.SEGMENT.equals(endMasterElement.getElementMetaData().getTypeInfo())) {
                        log.debug("Segment end {} changing state to NEW", endMasterElement);
                        state = State.NEW;
                    }
                    break;
                case PRE_CLUSTER:
                    if (MkvTypeInfos.SEGMENT.equals(endMasterElement.getElementMetaData().getTypeInfo())) {
                        log.warn("Segment end {} while in PRE_CLUSTER. Collecting cluster info", endMasterElement);
                        collectPreClusterInfo();
                    }
                    break;
                default:
                    break;
            }
            // If any tags section finishes, try to update the millisbehind latest and continuation token
            // since there can be multiple in the same segment.
            if (MkvTypeInfos.TAGS.equals(endMasterElement.getElementMetaData().getTypeInfo())) {
                if (log.isDebugEnabled()) {
                    log.debug("TAGS end {}, potentially updating millisbehindlatest and continuation token",
                            endMasterElement);
                }
                setMillisBehindLatestAndContinuationToken();
            }
        }

        @Override
        public void visit(MkvDataElement dataElement) throws MkvElementVisitException {
            if (mkvTagProcessor.isPresent()) {
                if (MkvTypeInfos.TAGNAME.equals(dataElement.getElementMetaData().getTypeInfo())) {
                    tagName = getMkvElementStringVal(dataElement);
                } else if (MkvTypeInfos.TAGSTRING.equals(dataElement.getElementMetaData().getTypeInfo())) {
                    tagValue = getMkvElementStringVal(dataElement);
                }

                if (tagName != null && tagValue != null) {
                    // Only process non-internal tags
                    if (!tagName.startsWith(AWS_KINESISVIDEO_TAGNAME_PREFIX)) {
                        mkvTagProcessor.get().process(new MkvTag(tagName, tagValue), currentFragmentMetadata);
                    }

                    // Empty the values for new tag
                    tagName = null;
                    tagValue = null;
                }
            }
        }
    }

    public MkvTrackMetadata getMkvTrackMetadata(long trackNumber) {
        return trackMetadataMap.get(BigInteger.valueOf(trackNumber));
    }

    public OptionalLong getMillisBehindNow() {
        return millisBehindNow;
    }

    public Optional<String> getContinuationToken() {
        return continuationToken;
    }

    private void setMillisBehindLatestAndContinuationToken() {
        final Map<String, String> tagNameToTagValueMap = getTagNameToValueMap();
        //Do not overwrite an existing value with Optional.absent

        String millisBehindString = tagNameToTagValueMap.get(MILLIS_BEHIND_NOW_KEY);
        if (millisBehindString != null) {
            millisBehindNow = (OptionalLong.of(Long.parseLong(millisBehindString)));
            currentFragmentMetadata.ifPresent(f -> f.setMillisBehindNow(millisBehindNow));
        }
        String continutationTokenString = tagNameToTagValueMap.get(CONTINUATION_TOKEN_KEY);
        if (continutationTokenString != null) {
            continuationToken = Optional.of(continutationTokenString);
            currentFragmentMetadata.ifPresent(f -> f.setContinuationToken(continuationToken));
        }
    }

    private void collectPreClusterInfo() {
        final Map<String, String> tagNameToTagValueMap = getTagNameToValueMap();

        currentFragmentMetadata = Optional.ofNullable(FragmentMetadata.createFromtagNametoValueMap(tagNameToTagValueMap));

        final Map<Long, List<MkvElement>> trackEntryElementNumberToMkvElement = getTrackEntryMap();
        trackEntryElementNumberToMkvElement.values().stream().forEach(this::createTrackMetadata);
    }

    private Map<Long, List<MkvElement>> getTrackEntryMap() {
        final Map<Long,List<MkvElement>> trackEntryElementNumberToMkvElement = new HashMap<>();
        List<MkvElement> trackElements =  trackCollector.copyOfCollection();

        trackElements
                .stream()
                .filter(e -> MkvTypeInfos.TRACKENTRY.equals(e.getElementMetaData().getTypeInfo()))
                .forEach(e -> trackEntryElementNumberToMkvElement.put(e.getElementMetaData().getElementNumber(),
                        new ArrayList<>()));

        trackElements.stream()
                .filter(e -> e.getElementMetaData().getTypeInfo().getLevel() > MkvTypeInfos.TRACKENTRY.getLevel())
                .forEach(e -> {
                    EBMLElementMetaData trackEntryParent = e.getElementPath().get(MkvTypeInfos.TRACKENTRY.getLevel());
                    Validate.isTrue(MkvTypeInfos.TRACKENTRY.equals(trackEntryParent.getTypeInfo()));
                    trackEntryElementNumberToMkvElement.get(trackEntryParent.getElementNumber()).add(e);
                });
        return trackEntryElementNumberToMkvElement;
    }

    private void createTrackMetadata(List<MkvElement> trackEntryPropertyLists) {
        Map<EBMLTypeInfo, MkvElement> metaDataProperties = trackEntryPropertyLists.stream()
                .filter(e -> trackTypesForTrackMetadata.contains(e.getElementMetaData().getTypeInfo()))
                .collect(Collectors.toMap(e -> e.getElementMetaData().getTypeInfo(), Function.identity()));

        MkvTrackMetadata mkvTrackMetadata = MkvTrackMetadata.builder()
                .trackNumber(getUnsignedLongVal(metaDataProperties, MkvTypeInfos.TRACKNUMBER))
                .trackUID(getUnsignedLongValOptional(metaDataProperties, MkvTypeInfos.TRACKUID))
                .trackName(getStringVal(metaDataProperties, MkvTypeInfos.NAME))
                .codecId(getStringVal(metaDataProperties, MkvTypeInfos.CODECID))
                .codecName(getStringVal(metaDataProperties, MkvTypeInfos.CODECNAME))
                .codecPrivateData(getByteBuffer(metaDataProperties, MkvTypeInfos.CODECPRIVATE))
                .pixelWidth(getUnsignedLongValOptional(metaDataProperties, MkvTypeInfos.PIXELWIDTH))
                .pixelHeight(getUnsignedLongValOptional(metaDataProperties, MkvTypeInfos.PIXELHEIGHT))
                .samplingFrequency(getFloatingPointValOptional(metaDataProperties, MkvTypeInfos.SAMPLINGFREQUENCY))
                .channels(getUnsignedLongValOptional(metaDataProperties, MkvTypeInfos.CHANNELS))
                .bitDepth(getUnsignedLongValOptional(metaDataProperties, MkvTypeInfos.BITDEPTH))
                .allElementsInTrack(trackEntryPropertyLists)
                .build();

        trackMetadataMap.put(mkvTrackMetadata.getTrackNumber(), mkvTrackMetadata);
    }

    private static String getStringVal(Map<EBMLTypeInfo, MkvElement> metaDataProperties, EBMLTypeInfo key) {
        MkvElement element = metaDataProperties.get(key);
        if (element == null) {
            return null;
        }
        MkvDataElement dataElement = (MkvDataElement)element;
        Validate.isTrue(EBMLTypeInfo.TYPE.STRING.equals(dataElement.getElementMetaData().getTypeInfo().getType())
                || EBMLTypeInfo.TYPE.UTF_8.equals(dataElement.getElementMetaData().getTypeInfo().getType()));
        return ((MkvValue<String>)dataElement.getValueCopy()).getVal();
    }

    private static Optional<BigInteger> getUnsignedLongValOptional(Map<EBMLTypeInfo, MkvElement> metaDataProperties,
            EBMLTypeInfo key) {
        return Optional.ofNullable(getUnsignedLongVal(metaDataProperties, key));
    }

    private static BigInteger getUnsignedLongVal(Map<EBMLTypeInfo, MkvElement> metaDataProperties, EBMLTypeInfo key) {
        MkvElement element = metaDataProperties.get(key);
        if (element == null) {
            return null;
        }
        MkvDataElement dataElement = (MkvDataElement) element;
        Validate.isTrue(EBMLTypeInfo.TYPE.UINTEGER.equals(dataElement.getElementMetaData().getTypeInfo().getType()));
        return ((MkvValue<BigInteger>) dataElement.getValueCopy()).getVal();
    }

    private static Optional<Double> getFloatingPointValOptional(Map<EBMLTypeInfo, MkvElement> metaDataProperties,
            EBMLTypeInfo key) {
        return Optional.ofNullable(getFloatingPointVal(metaDataProperties, key));
    }

    private static Double getFloatingPointVal(Map<EBMLTypeInfo, MkvElement> metaDataProperties, EBMLTypeInfo key) {
        MkvElement element = metaDataProperties.get(key);
        if (element == null) {
            return null;
        }
        MkvDataElement dataElement = (MkvDataElement) element;
        Validate.isTrue(EBMLTypeInfo.TYPE.FLOAT.equals(dataElement.getElementMetaData().getTypeInfo().getType()));
        return ((MkvValue<Double>) dataElement.getValueCopy()).getVal();
    }

    private static ByteBuffer getByteBuffer(Map<EBMLTypeInfo, MkvElement> metaDataProperties, EBMLTypeInfo key) {
        MkvElement element = metaDataProperties.get(key);
        if (element == null) {
            return null;
        }
        MkvDataElement dataElement = (MkvDataElement)element;
        Validate.isTrue(EBMLTypeInfo.TYPE.BINARY.equals(dataElement.getElementMetaData().getTypeInfo().getType()));
        return ((MkvValue<ByteBuffer>)dataElement.getValueCopy()).getVal();
    }

    private Map<String, String> getTagNameToValueMap() {
        List<MkvElement> tagElements = tagCollector.copyOfCollection();
        Map<String, Long> tagNameToParentElementNumber = tagElements.stream()
                .filter(e -> MkvTypeInfos.TAGNAME.equals(e.getElementMetaData().getTypeInfo()))
                .filter(e -> MkvTypeInfos.SIMPLETAG.equals(getParentElement(e).getTypeInfo()))
                .filter(e -> isTagFromKinesisVideo((MkvDataElement) e))
                .collect(Collectors.toMap(this::getMkvElementStringVal,
                        e -> getParentElement(e).getElementNumber(), (a,b)->b));
        Map<Long, String> parentElementNumberToTagValue = tagElements.stream()
                .filter(e -> MkvTypeInfos.TAGSTRING.equals(e.getElementMetaData().getTypeInfo()))
                .filter(e -> MkvTypeInfos.SIMPLETAG.equals(getParentElement(e).getTypeInfo()))
                .collect(Collectors.toMap(e -> getParentElement(e).getElementNumber(), this::getMkvElementStringVal));
        return tagNameToParentElementNumber.entrySet()
                .stream()
                .collect(Collectors.toMap(e -> e.getKey(),
                        e -> parentElementNumberToTagValue.getOrDefault(e.getValue(),"")));
    }

    private static boolean isTagFromKinesisVideo(MkvDataElement e) {
        MkvValue<String> tagNameValue = e.getValueCopy();
        return tagNameValue.getVal().startsWith(AWS_KINESISVIDEO_TAGNAME_PREFIX);
    }

    private  String getMkvElementStringVal(MkvElement e) {
        return ((MkvValue<String>) ((MkvDataElement) e).getValueCopy()).getVal();
    }

    private static EBMLElementMetaData getParentElement(MkvElement e) {
        return e.getElementPath().get(e.getElementPath().size()-1);
    }

    private void resetCollectedData() {
        previousFragmentMetadata = currentFragmentMetadata;
        currentFragmentMetadata = Optional.empty();
        trackMetadataMap.clear();
        tagName = tagValue = null;

        tagCollector.clearCollection();
        trackCollector.clearCollection();
    }


    public static final class BasicMkvTagProcessor implements FragmentMetadataVisitor.MkvTagProcessor {
        @Getter
        private List<MkvTag> tags = new ArrayList<>();

        @Override
        public void process(MkvTag mkvTag, Optional<FragmentMetadata> currentFragmentMetadata) {
            tags.add(mkvTag);
        }

        @Override
        public void clear() {
            tags.clear();
        }
    }
}
