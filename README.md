# Amazon Kinesis Video Streams Parser Library 

## License

This library is licensed under the Apache 2.0 License. 

## Introduction
The Amazon Kinesis Video Streams Parser Library for Java enables Java developers to parse the streams returned by `GetMedia` calls to Amazon Kinesis Video. 
It contains:
* A streaming Mkv Parser called `StreamingMkvReader` that provides an iterative interface to read the `MkvElement`s in a stream.
* Applications such as `OutputSegmentMerger` and `FragmentMetadataVisitor` built using the `StreamingMkvReader` .
* A callback based parser called `EBMLParser` that minimizes data buffering and copying. `StreamingMkvReader` is built on top of `EBMLParser`
* Unit tests for the applications and parsers that demonstrate how the applications work.

## Building from Source
After you've downloaded the code from GitHub, you can build it using Maven. Use this command: `mvn clean install`


## Details
### StreamingMkvReader
`StreamingMkvReader` which provides an iterative interface to read `MkvElement`s from a stream.
A caller calls `nextIfAvailable` to get the next `MkvElement`. An `MkvElement` wrapped in an `Optional` is returned if a complete element is available.
It buffers an individual `MkvElement` until it can return a complete `MkvElement`.  
The `mightHaveNext` method returns true if there is a chance that additional `MkvElements` can be returned. 
It returns false when the end of the input stream has been reached.
 
### MkvElement
There are three types of `MkvElement` vended by a `MkvStreamReader`:
* `MkvDataElement`: This encapsulates Mkv Elements that are not master elements and contain data. 
* `MkvStartMasterElement` : This represents the start of a *master* Mkv element that contains child elements. Child elements can be other master elements or data elements.
* `MkvEndMasterElement` : This represents the end of *master* element that contains child elements.

### MkvElementVisitor
The `MkvElementVisitor` is a visitor pattern that helps process the events in the `MkvElement` hierarchy. It has a visit 
method for each type of `MkvElement`.

## Visitors
A `GetMedia` call to Kinesis Video vends a stream of fragments where each fragment is encapsulated in a Mkv stream containing *EBML* and *Segment* elements.
`OutputSegmentMerger` can be used to merge consecutive fragments that share the same *EBML* and *Track* data into a single Mkv stream with 
a shared *EBML* and *Segment*. This is useful for passing the output of `GetMedia` to any downstream processor that expects a single Mkv stream
with one *Segment*. Its use can be seen in `OutputSegmentMergerTest`

`FragmentMetadataVisitor` is a `MkvElementVisitor` that collects the Kinesis Video specific meta-data (such as *FragmentNumber* and *Server Side Timestamp* )
 for the current fragment being processed. The `getCurrentFragmentMetadata` method can be used to get the current fragment's metadata. Similarly 
`getPreviousFragmentMetadata` can be used get the previous fragment's metadata. The `getMkvTrackMetadata` method can be used to get
the details of a particular track.

`ElementSizeAndOffsetVisitor` is a visitor that writes out the metadata of the Mkv elements in a stream. For each element
 the name, offset, header size and data size is written out. The output uses indentation to indicate the hierarchy of master elements
 and their child elements. `ElementSizeAndOffsetVisitor` is useful for looking into Mkv streams, where mkvinfo fails.

`CountVisitor` is a visitor that can be used to count the number of Mkv elements of different types in a Mkv stream.

`CompositeMkvElementVisitor` is a visitor that is made up of a number of constituent visitors. It calls accept on the 
visited `MkvElement` for each constituent visitor in the order in which the visitors are specified.

`FrameVisitor` is a visitor used to process the frames in the output of a GetMedia call. It invokes an implementation of the
 `FrameVisitor.FrameProcessor` and provides it with a `Frame` object and the metadata of the track to which the `Frame` belongs.

`CopyVisitor` is a visitor used to copy the raw bytes of the Mkv elements in a stream to an output stream.

## ResponseStreamConsumers
The `GetMediaResponseStreamConsumer` is an abstract class used to consume the output of a GetMedia* call to Kinesis Video in a streaming fashion.
It supports a single abstract method called process that is invoked to process the streaming payload of a GetMedia response.
The first parameter for process method is the payload inputStream in a GetMediaResult returned by a call to GetMedia.
Implementations of the process method of this interface should block until all the data in the inputStream has been
 processed or the process method decides to stop for some other reason. The second argument is a FragmentMetadataCallback 
 which is invoked at the end of every processed fragment. The `GetMediaResponseStreamConsumer` provides a utility method 
 `processWithFragmentEndCallbacks` that can be used by child classes to  implement the end of fragment callbacks.
 The process method can be implemented using a combination of the visitors described earlier.
 
### MergedOutputPiper
The `MergedOutputPiper` extends `GetMediaResponseStreamConsumer` to merge consecutive mkv streams in the output of GetMedia
 and pipes the merged stream to the stdin of a child process. It is meant to be used to pipe the output of a GetMedia* call to a processing application that can not deal
with having multiple consecutive mkv streams. Gstreamer is one such application that requires a merged stream.


## Example
### KinesisVideoExample
`KinesisVideoExample` is an example that shows how the `StreamingMkvReader` and the different visitors can be integrated
with the AWS SDK for the Kinesis Video. This example provides examples for
* Create a stream, deleting and recreating if the stream of the same name already exists.
* Call PutMedia to stream video fragments into the stream.
* Simultaneously call GetMedia to stream video fragments out of the stream.
* It uses the StreamingMkvReader to parse the returned the stream and apply the `OutputSegmentMerger`, `FragmentMetadataVisitor` visitors
along with a local one as part of the same `CompositeMkvElementVisitor` visitor.

### KinesisVideoRendererExample
`KinesisVideoRendererExample` shows parsing and rendering of KVS video stream fragments using JCodec(http://jcodec.org/) that were ingested using Producer SDK GStreamer sample application.
* To run the example:
  Run the Unit test `testExample` in `KinesisVideoRendererExampleTest`. After starting the unitTest you should be able to view the frames in a JFrame.
* If you want to store it as image files you could do it by adding (in KinesisVideoRendererExample after AWTUtil.toBufferedImage(rgb, renderImage); )

```
try {
    ImageIO.write(renderImage, "png", new File(String.format("frame-capture-%s.png", UUID.randomUUID())));
 } catch (IOException e) {
    log.warn("Couldn't convert to a PNG", e);
}
```
* It has been tested not only for streams ingested by `PutMediaWorker` but also streams sent to Kinesis Video Streams using GStreamer Demo application (https://github.com/awslabs/amazon-kinesis-video-streams-producer-sdk-cpp)

### KinesisVideoGStreamerPiperExample
`KinesisVideoGStreamerPiperExample` is an example for continuously piping the output of GetMedia calls from a Kinesis Video stream to GStreamer.
 The test `KinesisVideoGStreamerPiperExampleTest` provides an example that pipes the output of a KVS GetMedia call to a Gstreamer pipeline.
 The Gstreamer pipeline is a toy example that demonstrates that Gstreamer can parse the mkv passed into it.

### KinesisVideo - Rekognition Examples
Kinesis Video - Rekognition examples demonstrate how to combine Rekognition outputs (JSON) with KinesisVideo Streams output (H264 fragments) and
 render the frames with overlapping bounding boxes.

#### KinesisVideoRekognitionIntegrationExample
`KinesisVideoRekognitionIntegrationExample` decodes H264 frames and renders them with bounding boxes locally using JFrame. To run the sample, run `KinesisVideoRekognitionIntegrationExampleTest` with
appropriate inputs.

#### KinesisVideoRekognitionLambdaExample
`KinesisVideoRekognitionLambdaExample` decodes H264 frames, overlaps bounding boxes, encodes to H264 frames again and ingests them into a new Kinesis Video streams using Kinesis Video Producer SDK.
To run the sample follow the below steps:
* Run `mvn package` from 'amazon-kinesis-video-streams-parser-library' folder. Upload './target/amazon-kinesis-video-streams-parser-library-$VERSION-shaded.jar' file to a S3 bucket.
* Note the S3 bucket and key name.
* Find the file `LambdaExampleCFnTemplate.yml` in the github package.
* Goto AWS CloudFormation console and create stack using above template, follow the description to the input parameters.
* Now start the producer to ingest data into Kinesis Video Streams for which the Rekognition stream processor is configured.
* Lambda will be triggered as soon as the Rekognition stream processor starts emitting records in Kinesis Data Streams. Lambda will also create a new Kinesis Video streams
 with the input stream name + 'Rekognized' suffix and ingest frames overlapped with bounding boxes which should be viewable in Kinesis Video Streams console.
* To troubleshoot any issues, use Monitoring tab in lambda and click 'View logs in Cloudwatch'.
* NOTE: As this lambda executes resource intense decoding and encoding (using Jcodec which is not optimal https://github.com/jcodec/jcodec#performance--quality-considerations),
 the new Kinesis Video stream might be delayed significantly.

## Release Notes

### Release 1.2.4 (Mar 2022)
* Update amazon-kinesis-client from 1.14.7 to 1.14.8

### Release 1.2.3 (Feb 2022)
* Update slf4j-reload4j and slf4j-api from 1.7.35 to 1.7.36
* Update aws-lambda-java-events from 1.2.0 to 2.2.9
* Update amazon-kinesis-video-streams-producer-sdk-java from 1.8.0 to 1.12.0
* Update aws-java-sdk-bom from 1.11.487 to 1.12.162

### Release 1.2.2 (Jan 2022)
* Update slf4j-reload4j(slf4j-log4j12) and slf4j-api from 1.7.33 to 1.7.35
* Update amazon-kinesis-client from 1.9.3 to 1.14.7
* Update aws-lambda-java-core from 1.2.0 to 1.2.1
* Update junit from 4.13.1 to 4.13.2
* Update lombok from 1.18.16 to 1.18.22
* Update commons-lang3 from 3.6 to 3.12.0
* Update powermock-mockito-release-full from 1.6.3 to 1.6.4
* Update maven-compiler-plugin from 3.2 to 3.9.0
* Update lombok-maven-plugin from 1.18.16.0 to 1.18.20.0
* Update maven-javadoc-plugin from 3.1.1 to 3.3.1
* Update maven-source-plugin from 3.0.1 to 3.2.1
* Update maven-shade-plugin from 2.3 to 3.2.4

### Release 1.2.1 (Jan 2022)
* Update slf4j-log4j12 and slf4j-api from 1.7.25 to 1.7.33
* Update log4j-slf4j-impl from 2.8.2 to 2.17.1

### Release 1.2.0 (Jan 2022)
* Move from aws-lambda-java-log4j 1.1.0 to aws-lambda-java-log4j2 1.5.1 to address CVE
* Update log4j to 2.17.1 to address CVE

### Release 1.1.0 (Dec 2021)
* Add ListFragment worker and update GetMediaForFragmentListWorker
* Upgrade Log4j to 2.16 to address CVE


### Release 1.0.15 (Aug 2020)
* Added new cluster packing option to the `OputputSegmentMerger` to enable creation of a playable MKV file from a sparse KVS stream.
* Added parsing of audio specific fields from the MKV track header.
* Bump some dependency versions.
* Modify the log level on some log messages.

### Release 1.0.14 (Aug 2019)
* Fixed frame timecode during re-encoding in KinesisVideoRekognitionLambdaExample
* Fixed region for derived KVS Stream
* Using default FaceType for external image ids that doesn't follow specified format
* Upgraded JCodec version to 0.2.3 which provides scaling list support
* Log improvements

### Release 1.0.13 (Apr 2019)
* Fix: Make process method in H264FrameProcessor and H264FrameDecoder throw FrameProcessException.

### Release 1.0.12 (Mar 2019)
* Bugfix: Fix KinesisVideoExampleTest example issue that was using non-exist test file.
* Improve KinesisVideoRekognitionLambdaExample to use AWS CloudFormation Template to create resources.

### Release 1.0.11 (Mar 2019)
* Bugfix: KinesisVideoRekognitionIntegrationExample broken because the frame process callback is not invoked.

### Release 1.0.10 (Feb 2019)
* Bugfix: Close GetMedia connection to fix the connection leak issue.

### Release 1.0.9 (Jan 2019)
* Added KinesisVideo Rekgonition Lamba example which combines Rekognition output with KVS fragments to draw bounding boxes
for detected faces and ingest into new KVS Stream.
* Added Optional track number parameter in the FrameVisitor to process only frames with that track number.

### Release 1.0.8 (Dec 2018)
* Add close method for derived classes to cleanup resources.
* Add exception type which could be used in downstream frame processing logic.
* Make boolean value thread-safe in ContinuousGetMediaWorker.
* Remove extra exception wrapping in CompositeMkvElementVisitor.
* Declare exception throwing for some methods.
* Enabled stack trace in ContinuousGetMediaWorker when there is an exception.

### Release 1.0.7 (Sep 2018)
* Add flag in KinesisVideoRendererExample and KinesisVideoExample to use the existing stream (and not doing PutMedia again if it exists already).
* Added support to retrieve the information from FragmentMetadata and display in the image panel during rendering.

### Release 1.0.6 (Sep 2018)
* Introduce handling for empty fragment metadata
* Added a new SimpleFrame Visitor to handle video with no tags
* Refactored H264FrameDecoder, so that base class method can be reused by child class

### Release 1.0.5 (May 2018)
* Introduce `GetMediaResponseStreamConsumer` as an abstract class used to consume the output of a GetMedia* call
to Kinesis Video in a streaming fashion. Child classes will use visitors to implement different consumers.
* The `MergedOutputPiper` extends `GetMediaResponseStreamConsumer` to merge consecutive mkv streams in the output of GetMedia
   and pipes the merged stream to the stdin of a child process.
* Add the capability and example to pipe the output of GetMedia calls to GStreamer using `MergedOutputPiper`.

### Release 1.0.4 (April 2018)
* Add example for KinesisVideo Streams integration with Rekognition and draw Bounding Boxes for every sampled frame.
* Fix for stream ending before reporting tags visited.
* Same test data file for parsing and rendering example.
* Known Issues:  In `KinesisVideoRekognitionIntegrationExample`, the decode/renderer sample using JCodec may not be able to decode all mkv files.

### Release 1.0.3 (Februrary 2018)
*  In OutputSegmentMerger, make sure that the lastClusterTimecode is updated for the first fragment.
If timecode is equal to that of a previous cluster, stop merging
* FrameVisitor to process the frames in the output of a GetMedia call.
* CopyVisitor to copy the raw bytes of the stream being parsed to an output stream.
* Add example that shows parsing and rendering Kinesis Video Streams.
* Known Issues:  In `KinesisVideoRendererExample`, the decode/renderer sample using JCodec may not be able to decode all mkv files.
   
### Release 1.0.2 (December 2017)
* Add example that shows integration with Kinesis Video Streams.
* Remove unnecessary import.

### Release 1.0.1 (November 2017)
* Update to include the url for Amazon Kinesis Video Streams in the pom.xml

### Release 1.0.0 (November 2017)
* First release of the Amazon Kinesis Video Parser Library.
* Supports Mkv elements up to version 4. 
* Known issues:
    * EBMLMaxIDLength and EBMLMaxSizeLength are hardcoded as 4 and 8 bytes respectively
    * Unknown EBML elements not specified in `MkvTypeInfos` are not readable by the user using `StreamingMkvReader`.
    * Unknown EBML elements not specified in `MkvTypeInfos` of unknown length lead to an exception.
    * Does not do any CRC validation for any Mkv elements with the `CRC-32` element. 
