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
package com.amazonaws.kinesisvideo.parser.ebml;

/**
 * Type information for the EBML elements in a Mkv file or stream.
 * This provides the semantics of the EBML elements in a Mkv file or stream.
 * This is based on the xml file hosted by the matroska org at
 * https://github.com/Matroska-Org/foundation-source/blob/master/spectool/specdata.xml (commit e074b5d)
 */
public class MkvTypeInfos {
	public static final EBMLTypeInfo EBML = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("EBML").id(0x1A45DFA3).level(0).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo EBMLVERSION = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("EBMLVersion").id(0x4286).level(1).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo EBMLREADVERSION = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("EBMLReadVersion").id(0x42F7).level(1).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo EBMLMAXIDLENGTH = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("EBMLMaxIDLength").id(0x42F2).level(1).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo EBMLMAXSIZELENGTH = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("EBMLMaxSizeLength").id(0x42F3).level(1).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo DOCTYPE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("DocType").id(0x4282).level(1).type(
            EBMLTypeInfo.TYPE.STRING).build();
	public static final EBMLTypeInfo DOCTYPEVERSION = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("DocTypeVersion").id(0x4287).level(1).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo DOCTYPEREADVERSION = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("DocTypeReadVersion").id(0x4285).level(1).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo VOID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Void").id(0xEC).level(-1).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo CRC_32 = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("CRC-32").id(0xBF).level(-1).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo SIGNATURESLOT = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("SignatureSlot").id(0x1B538667).level(-1).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo SIGNATUREALGO = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("SignatureAlgo").id(0x7E8A).level(1).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo SIGNATUREHASH = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("SignatureHash").id(0x7E9A).level(1).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo SIGNATUREPUBLICKEY = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("SignaturePublicKey").id(0x7EA5).level(1).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo SIGNATURE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Signature").id(0x7EB5).level(1).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo SIGNATUREELEMENTS = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("SignatureElements").id(0x7E5B).level(1).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo SIGNATUREELEMENTLIST = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("SignatureElementList").id(0x7E7B).level(2).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo SIGNEDELEMENT = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("SignedElement").id(0x6532).level(3).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo SEGMENT = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Segment").id(0x18538067).level(0).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo SEEKHEAD = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("SeekHead").id(0x114D9B74).level(1).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo SEEK = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Seek").id(0x4DBB).level(2).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo SEEKID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("SeekID").id(0x53AB).level(3).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo SEEKPOSITION = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("SeekPosition").id(0x53AC).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo INFO = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Info").id(0x1549A966).level(1).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo SEGMENTUID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("SegmentUID").id(0x73A4).level(2).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo SEGMENTFILENAME = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("SegmentFilename").id(0x7384).level(2).type(
            EBMLTypeInfo.TYPE.UTF_8).build();
	public static final EBMLTypeInfo PREVUID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("PrevUID").id(0x3CB923).level(2).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo PREVFILENAME = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("PrevFilename").id(0x3C83AB).level(2).type(
            EBMLTypeInfo.TYPE.UTF_8).build();
	public static final EBMLTypeInfo NEXTUID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("NextUID").id(0x3EB923).level(2).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo NEXTFILENAME = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("NextFilename").id(0x3E83BB).level(2).type(
            EBMLTypeInfo.TYPE.UTF_8).build();
	public static final EBMLTypeInfo SEGMENTFAMILY = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("SegmentFamily").id(0x4444).level(2).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo CHAPTERTRANSLATE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChapterTranslate").id(0x6924).level(2).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo CHAPTERTRANSLATEEDITIONUID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChapterTranslateEditionUID").id(0x69FC).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CHAPTERTRANSLATECODEC = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChapterTranslateCodec").id(0x69BF).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CHAPTERTRANSLATEID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChapterTranslateID").id(0x69A5).level(3).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo TIMECODESCALE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TimecodeScale").id(0x2AD7B1).level(2).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo DURATION = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Duration").id(0x4489).level(2).type(
            EBMLTypeInfo.TYPE.FLOAT).build();
	public static final EBMLTypeInfo DATEUTC = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("DateUTC").id(0x4461).level(2).type(
            EBMLTypeInfo.TYPE.DATE).build();
	public static final EBMLTypeInfo TITLE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Title").id(0x7BA9).level(2).type(
            EBMLTypeInfo.TYPE.UTF_8).build();
	public static final EBMLTypeInfo MUXINGAPP = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("MuxingApp").id(0x4D80).level(2).type(
            EBMLTypeInfo.TYPE.UTF_8).build();
	public static final EBMLTypeInfo WRITINGAPP = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("WritingApp").id(0x5741).level(2).type(
            EBMLTypeInfo.TYPE.UTF_8).build();
	public static final EBMLTypeInfo CLUSTER = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Cluster").id(0x1F43B675).level(1).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo TIMECODE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Timecode").id(0xE7).level(2).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo SILENTTRACKS = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("SilentTracks").id(0x5854).level(2).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo SILENTTRACKNUMBER = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("SilentTrackNumber").id(0x58D7).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo POSITION = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Position").id(0xA7).level(2).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo PREVSIZE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("PrevSize").id(0xAB).level(2).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo SIMPLEBLOCK = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("SimpleBlock").id(0xA3).level(2).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo BLOCKGROUP = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("BlockGroup").id(0xA0).level(2).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo BLOCK = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Block").id(0xA1).level(3).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo BLOCKVIRTUAL = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("BlockVirtual").id(0xA2).level(3).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo BLOCKADDITIONS = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("BlockAdditions").id(0x75A1).level(3).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo BLOCKMORE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("BlockMore").id(0xA6).level(4).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo BLOCKADDID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("BlockAddID").id(0xEE).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo BLOCKADDITIONAL = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("BlockAdditional").id(0xA5).level(5).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo BLOCKDURATION = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("BlockDuration").id(0x9B).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo REFERENCEPRIORITY = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ReferencePriority").id(0xFA).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo REFERENCEBLOCK = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ReferenceBlock").id(0xFB).level(3).type(
            EBMLTypeInfo.TYPE.INTEGER).build();
	public static final EBMLTypeInfo REFERENCEVIRTUAL = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ReferenceVirtual").id(0xFD).level(3).type(
            EBMLTypeInfo.TYPE.INTEGER).build();
	public static final EBMLTypeInfo CODECSTATE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("CodecState").id(0xA4).level(3).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo DISCARDPADDING = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("DiscardPadding").id(0x75A2).level(3).type(
            EBMLTypeInfo.TYPE.INTEGER).build();
	public static final EBMLTypeInfo SLICES = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Slices").id(0x8E).level(3).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo TIMESLICE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TimeSlice").id(0xE8).level(4).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo LACENUMBER = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("LaceNumber").id(0xCC).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo FRAMENUMBER = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("FrameNumber").id(0xCD).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo BLOCKADDITIONID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("BlockAdditionID").id(0xCB).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo DELAY = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Delay").id(0xCE).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo SLICEDURATION = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("SliceDuration").id(0xCF).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo REFERENCEFRAME = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ReferenceFrame").id(0xC8).level(3).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo REFERENCEOFFSET = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ReferenceOffset").id(0xC9).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo REFERENCETIMECODE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ReferenceTimeCode").id(0xCA).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo ENCRYPTEDBLOCK = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("EncryptedBlock").id(0xAF).level(2).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo TRACKS = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Tracks").id(0x1654AE6B).level(1).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo TRACKENTRY = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TrackEntry").id(0xAE).level(2).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo TRACKNUMBER = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TrackNumber").id(0xD7).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo TRACKUID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TrackUID").id(0x73C5).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo TRACKTYPE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TrackType").id(0x83).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo FLAGENABLED = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("FlagEnabled").id(0xB9).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo FLAGDEFAULT = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("FlagDefault").id(0x88).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo FLAGFORCED = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("FlagForced").id(0x55AA).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo FLAGLACING = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("FlagLacing").id(0x9C).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo MINCACHE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("MinCache").id(0x6DE7).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo MAXCACHE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("MaxCache").id(0x6DF8).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo DEFAULTDURATION = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("DefaultDuration").id(0x23E383).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo DEFAULTDECODEDFIELDDURATION = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("DefaultDecodedFieldDuration").id(0x234E7A).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo TRACKTIMECODESCALE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TrackTimecodeScale").id(0x23314F).level(3).type(
            EBMLTypeInfo.TYPE.FLOAT).build();
	public static final EBMLTypeInfo TRACKOFFSET = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TrackOffset").id(0x537F).level(3).type(
            EBMLTypeInfo.TYPE.INTEGER).build();
	public static final EBMLTypeInfo MAXBLOCKADDITIONID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("MaxBlockAdditionID").id(0x55EE).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo NAME = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Name").id(0x536E).level(3).type(
            EBMLTypeInfo.TYPE.UTF_8).build();
	public static final EBMLTypeInfo LANGUAGE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Language").id(0x22B59C).level(3).type(
            EBMLTypeInfo.TYPE.STRING).build();
	public static final EBMLTypeInfo CODECID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("CodecID").id(0x86).level(3).type(
            EBMLTypeInfo.TYPE.STRING).build();
	public static final EBMLTypeInfo CODECPRIVATE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("CodecPrivate").id(0x63A2).level(3).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo CODECNAME = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("CodecName").id(0x258688).level(3).type(
            EBMLTypeInfo.TYPE.UTF_8).build();
	public static final EBMLTypeInfo ATTACHMENTLINK = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("AttachmentLink").id(0x7446).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CODECSETTINGS = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("CodecSettings").id(0x3A9697).level(3).type(
            EBMLTypeInfo.TYPE.UTF_8).build();
	public static final EBMLTypeInfo CODECINFOURL = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("CodecInfoURL").id(0x3B4040).level(3).type(
            EBMLTypeInfo.TYPE.STRING).build();
	public static final EBMLTypeInfo CODECDOWNLOADURL = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("CodecDownloadURL").id(0x26B240).level(3).type(
            EBMLTypeInfo.TYPE.STRING).build();
	public static final EBMLTypeInfo CODECDECODEALL = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("CodecDecodeAll").id(0xAA).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo TRACKOVERLAY = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TrackOverlay").id(0x6FAB).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CODECDELAY = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("CodecDelay").id(0x56AA).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo SEEKPREROLL = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("SeekPreRoll").id(0x56BB).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo TRACKTRANSLATE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TrackTranslate").id(0x6624).level(3).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo TRACKTRANSLATEEDITIONUID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TrackTranslateEditionUID").id(0x66FC).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo TRACKTRANSLATECODEC = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TrackTranslateCodec").id(0x66BF).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo TRACKTRANSLATETRACKID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TrackTranslateTrackID").id(0x66A5).level(4).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo VIDEO = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Video").id(0xE0).level(3).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo FLAGINTERLACED = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("FlagInterlaced").id(0x9A).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo FIELDORDER = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("FieldOrder").id(0x9D).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo STEREOMODE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("StereoMode").id(0x53B8).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo ALPHAMODE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("AlphaMode").id(0x53C0).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo OLDSTEREOMODE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("OldStereoMode").id(0x53B9).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo PIXELWIDTH = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("PixelWidth").id(0xB0).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo PIXELHEIGHT = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("PixelHeight").id(0xBA).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo PIXELCROPBOTTOM = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("PixelCropBottom").id(0x54AA).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo PIXELCROPTOP = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("PixelCropTop").id(0x54BB).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo PIXELCROPLEFT = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("PixelCropLeft").id(0x54CC).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo PIXELCROPRIGHT = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("PixelCropRight").id(0x54DD).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo DISPLAYWIDTH = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("DisplayWidth").id(0x54B0).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo DISPLAYHEIGHT = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("DisplayHeight").id(0x54BA).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo DISPLAYUNIT = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("DisplayUnit").id(0x54B2).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo ASPECTRATIOTYPE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("AspectRatioType").id(0x54B3).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo COLOURSPACE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ColourSpace").id(0x2EB524).level(4).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo GAMMAVALUE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("GammaValue").id(0x2FB523).level(4).type(
            EBMLTypeInfo.TYPE.FLOAT).build();
	public static final EBMLTypeInfo FRAMERATE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("FrameRate").id(0x2383E3).level(4).type(
            EBMLTypeInfo.TYPE.FLOAT).build();
	public static final EBMLTypeInfo COLOUR = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Colour").id(0x55B0).level(4).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo MATRIXCOEFFICIENTS = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("MatrixCoefficients").id(0x55B1).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo BITSPERCHANNEL = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("BitsPerChannel").id(0x55B2).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CHROMASUBSAMPLINGHORZ = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChromaSubsamplingHorz").id(0x55B3).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CHROMASUBSAMPLINGVERT = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChromaSubsamplingVert").id(0x55B4).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CBSUBSAMPLINGHORZ = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("CbSubsamplingHorz").id(0x55B5).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CBSUBSAMPLINGVERT = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("CbSubsamplingVert").id(0x55B6).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CHROMASITINGHORZ = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChromaSitingHorz").id(0x55B7).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CHROMASITINGVERT = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChromaSitingVert").id(0x55B8).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo RANGE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Range").id(0x55B9).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo TRANSFERCHARACTERISTICS = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TransferCharacteristics").id(0x55BA).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo PRIMARIES = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Primaries").id(0x55BB).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo MAXCLL = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("MaxCLL").id(0x55BC).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo MAXFALL = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("MaxFALL").id(0x55BD).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo MASTERINGMETADATA = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("MasteringMetadata").id(0x55D0).level(5).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo PRIMARYRCHROMATICITYX = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("PrimaryRChromaticityX").id(0x55D1).level(6).type(
            EBMLTypeInfo.TYPE.FLOAT).build();
	public static final EBMLTypeInfo PRIMARYRCHROMATICITYY = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("PrimaryRChromaticityY").id(0x55D2).level(6).type(
            EBMLTypeInfo.TYPE.FLOAT).build();
	public static final EBMLTypeInfo PRIMARYGCHROMATICITYX = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("PrimaryGChromaticityX").id(0x55D3).level(6).type(
            EBMLTypeInfo.TYPE.FLOAT).build();
	public static final EBMLTypeInfo PRIMARYGCHROMATICITYY = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("PrimaryGChromaticityY").id(0x55D4).level(6).type(
            EBMLTypeInfo.TYPE.FLOAT).build();
	public static final EBMLTypeInfo PRIMARYBCHROMATICITYX = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("PrimaryBChromaticityX").id(0x55D5).level(6).type(
            EBMLTypeInfo.TYPE.FLOAT).build();
	public static final EBMLTypeInfo PRIMARYBCHROMATICITYY = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("PrimaryBChromaticityY").id(0x55D6).level(6).type(
            EBMLTypeInfo.TYPE.FLOAT).build();
	public static final EBMLTypeInfo WHITEPOINTCHROMATICITYX = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("WhitePointChromaticityX").id(0x55D7).level(6).type(
            EBMLTypeInfo.TYPE.FLOAT).build();
	public static final EBMLTypeInfo WHITEPOINTCHROMATICITYY = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("WhitePointChromaticityY").id(0x55D8).level(6).type(
            EBMLTypeInfo.TYPE.FLOAT).build();
	public static final EBMLTypeInfo LUMINANCEMAX = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("LuminanceMax").id(0x55D9).level(6).type(
            EBMLTypeInfo.TYPE.FLOAT).build();
	public static final EBMLTypeInfo LUMINANCEMIN = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("LuminanceMin").id(0x55DA).level(6).type(
            EBMLTypeInfo.TYPE.FLOAT).build();
	public static final EBMLTypeInfo AUDIO = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Audio").id(0xE1).level(3).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo SAMPLINGFREQUENCY = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("SamplingFrequency").id(0xB5).level(4).type(
            EBMLTypeInfo.TYPE.FLOAT).build();
	public static final EBMLTypeInfo OUTPUTSAMPLINGFREQUENCY = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("OutputSamplingFrequency").id(0x78B5).level(4).type(
            EBMLTypeInfo.TYPE.FLOAT).build();
	public static final EBMLTypeInfo CHANNELS = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Channels").id(0x9F).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CHANNELPOSITIONS = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChannelPositions").id(0x7D7B).level(4).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo BITDEPTH = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("BitDepth").id(0x6264).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo TRACKOPERATION = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TrackOperation").id(0xE2).level(3).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo TRACKCOMBINEPLANES = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TrackCombinePlanes").id(0xE3).level(4).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo TRACKPLANE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TrackPlane").id(0xE4).level(5).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo TRACKPLANEUID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TrackPlaneUID").id(0xE5).level(6).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo TRACKPLANETYPE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TrackPlaneType").id(0xE6).level(6).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo TRACKJOINBLOCKS = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TrackJoinBlocks").id(0xE9).level(4).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo TRACKJOINUID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TrackJoinUID").id(0xED).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo TRICKTRACKUID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TrickTrackUID").id(0xC0).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo TRICKTRACKSEGMENTUID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TrickTrackSegmentUID").id(0xC1).level(3).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo TRICKTRACKFLAG = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TrickTrackFlag").id(0xC6).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo TRICKMASTERTRACKUID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TrickMasterTrackUID").id(0xC7).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo TRICKMASTERTRACKSEGMENTUID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TrickMasterTrackSegmentUID").id(0xC4).level(3).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo CONTENTENCODINGS = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ContentEncodings").id(0x6D80).level(3).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo CONTENTENCODING = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ContentEncoding").id(0x6240).level(4).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo CONTENTENCODINGORDER = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ContentEncodingOrder").id(0x5031).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CONTENTENCODINGSCOPE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ContentEncodingScope").id(0x5032).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CONTENTENCODINGTYPE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ContentEncodingType").id(0x5033).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CONTENTCOMPRESSION = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ContentCompression").id(0x5034).level(5).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo CONTENTCOMPALGO = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ContentCompAlgo").id(0x4254).level(6).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CONTENTCOMPSETTINGS = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ContentCompSettings").id(0x4255).level(6).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo CONTENTENCRYPTION = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ContentEncryption").id(0x5035).level(5).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo CONTENTENCALGO = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ContentEncAlgo").id(0x47E1).level(6).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CONTENTENCKEYID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ContentEncKeyID").id(0x47E2).level(6).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo CONTENTSIGNATURE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ContentSignature").id(0x47E3).level(6).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo CONTENTSIGKEYID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ContentSigKeyID").id(0x47E4).level(6).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo CONTENTSIGALGO = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ContentSigAlgo").id(0x47E5).level(6).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CONTENTSIGHASHALGO = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ContentSigHashAlgo").id(0x47E6).level(6).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CUES = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Cues").id(0x1C53BB6B).level(1).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo CUEPOINT = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("CuePoint").id(0xBB).level(2).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo CUETIME = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("CueTime").id(0xB3).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CUETRACKPOSITIONS = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("CueTrackPositions").id(0xB7).level(3).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo CUETRACK = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("CueTrack").id(0xF7).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CUECLUSTERPOSITION = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("CueClusterPosition").id(0xF1).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CUERELATIVEPOSITION = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("CueRelativePosition").id(0xF0).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CUEDURATION = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("CueDuration").id(0xB2).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CUEBLOCKNUMBER = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("CueBlockNumber").id(0x5378).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CUECODECSTATE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("CueCodecState").id(0xEA).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CUEREFERENCE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("CueReference").id(0xDB).level(4).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo CUEREFTIME = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("CueRefTime").id(0x96).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CUEREFCLUSTER = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("CueRefCluster").id(0x97).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CUEREFNUMBER = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("CueRefNumber").id(0x535F).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CUEREFCODECSTATE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("CueRefCodecState").id(0xEB).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo ATTACHMENTS = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Attachments").id(0x1941A469).level(1).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo ATTACHEDFILE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("AttachedFile").id(0x61A7).level(2).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo FILEDESCRIPTION = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("FileDescription").id(0x467E).level(3).type(
            EBMLTypeInfo.TYPE.UTF_8).build();
	public static final EBMLTypeInfo FILENAME = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("FileName").id(0x466E).level(3).type(
            EBMLTypeInfo.TYPE.UTF_8).build();
	public static final EBMLTypeInfo FILEMIMETYPE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("FileMimeType").id(0x4660).level(3).type(
            EBMLTypeInfo.TYPE.STRING).build();
	public static final EBMLTypeInfo FILEDATA = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("FileData").id(0x465C).level(3).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo FILEUID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("FileUID").id(0x46AE).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo FILEREFERRAL = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("FileReferral").id(0x4675).level(3).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo FILEUSEDSTARTTIME = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("FileUsedStartTime").id(0x4661).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo FILEUSEDENDTIME = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("FileUsedEndTime").id(0x4662).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CHAPTERS = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Chapters").id(0x1043A770).level(1).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo EDITIONENTRY = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("EditionEntry").id(0x45B9).level(2).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo EDITIONUID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("EditionUID").id(0x45BC).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo EDITIONFLAGHIDDEN = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("EditionFlagHidden").id(0x45BD).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo EDITIONFLAGDEFAULT = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("EditionFlagDefault").id(0x45DB).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo EDITIONFLAGORDERED = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("EditionFlagOrdered").id(0x45DD).level(3).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CHAPTERATOM = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChapterAtom").id(0xB6).level(3).type(
            EBMLTypeInfo.TYPE.MASTER).isRecursive(true).build();
	public static final EBMLTypeInfo CHAPTERUID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChapterUID").id(0x73C4).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CHAPTERSTRINGUID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChapterStringUID").id(0x5654).level(4).type(
            EBMLTypeInfo.TYPE.UTF_8).build();
	public static final EBMLTypeInfo CHAPTERTIMESTART = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChapterTimeStart").id(0x91).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CHAPTERTIMEEND = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChapterTimeEnd").id(0x92).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CHAPTERFLAGHIDDEN = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChapterFlagHidden").id(0x98).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CHAPTERFLAGENABLED = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChapterFlagEnabled").id(0x4598).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CHAPTERSEGMENTUID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChapterSegmentUID").id(0x6E67).level(4).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo CHAPTERSEGMENTEDITIONUID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChapterSegmentEditionUID").id(0x6EBC).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CHAPTERPHYSICALEQUIV = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChapterPhysicalEquiv").id(0x63C3).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CHAPTERTRACK = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChapterTrack").id(0x8F).level(4).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo CHAPTERTRACKNUMBER = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChapterTrackNumber").id(0x89).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CHAPTERDISPLAY = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChapterDisplay").id(0x80).level(4).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo CHAPSTRING = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChapString").id(0x85).level(5).type(
            EBMLTypeInfo.TYPE.UTF_8).build();
	public static final EBMLTypeInfo CHAPLANGUAGE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChapLanguage").id(0x437C).level(5).type(
            EBMLTypeInfo.TYPE.STRING).build();
	public static final EBMLTypeInfo CHAPCOUNTRY = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChapCountry").id(0x437E).level(5).type(
            EBMLTypeInfo.TYPE.STRING).build();
	public static final EBMLTypeInfo CHAPPROCESS = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChapProcess").id(0x6944).level(4).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo CHAPPROCESSCODECID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChapProcessCodecID").id(0x6955).level(5).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CHAPPROCESSPRIVATE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChapProcessPrivate").id(0x450D).level(5).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo CHAPPROCESSCOMMAND = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChapProcessCommand").id(0x6911).level(5).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo CHAPPROCESSTIME = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChapProcessTime").id(0x6922).level(6).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo CHAPPROCESSDATA = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("ChapProcessData").id(0x6933).level(6).type(
            EBMLTypeInfo.TYPE.BINARY).build();
	public static final EBMLTypeInfo TAGS = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Tags").id(0x1254C367).level(1).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo TAG = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Tag").id(0x7373).level(2).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo TARGETS = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("Targets").id(0x63C0).level(3).type(
            EBMLTypeInfo.TYPE.MASTER).build();
	public static final EBMLTypeInfo TARGETTYPEVALUE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TargetTypeValue").id(0x68CA).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo TARGETTYPE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TargetType").id(0x63CA).level(4).type(
            EBMLTypeInfo.TYPE.STRING).build();
	public static final EBMLTypeInfo TAGTRACKUID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TagTrackUID").id(0x63C5).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo TAGEDITIONUID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TagEditionUID").id(0x63C9).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo TAGCHAPTERUID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TagChapterUID").id(0x63C4).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo TAGATTACHMENTUID = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TagAttachmentUID").id(0x63C6).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo SIMPLETAG = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("SimpleTag").id(0x67C8).level(3).type(
            EBMLTypeInfo.TYPE.MASTER).isRecursive(true).build();
	public static final EBMLTypeInfo TAGNAME = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TagName").id(0x45A3).level(4).type(
            EBMLTypeInfo.TYPE.UTF_8).build();
	public static final EBMLTypeInfo TAGLANGUAGE = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TagLanguage").id(0x447A).level(4).type(
            EBMLTypeInfo.TYPE.STRING).build();
	public static final EBMLTypeInfo TAGDEFAULT = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TagDefault").id(0x4484).level(4).type(
            EBMLTypeInfo.TYPE.UINTEGER).build();
	public static final EBMLTypeInfo TAGSTRING = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TagString").id(0x4487).level(4).type(
            EBMLTypeInfo.TYPE.UTF_8).build();
	public static final EBMLTypeInfo TAGBINARY = new EBMLTypeInfo.EBMLTypeInfoBuilder().name("TagBinary").id(0x4485).level(4).type(
            EBMLTypeInfo.TYPE.BINARY).build();
}

