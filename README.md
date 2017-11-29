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

## Release Notes
### Release 1.0.0 (November 2017)
* First release of the Amazon Kinesis Video Parser Library.
* Supports Mkv elements up to version 4. 
* Known issues:
    * EBMLMaxIDLength and EBMLMaxSizeLength are hardcoded as 4 and 8 bytes respectively
    * Unknown EBML elements not specified in `MkvTypeInfos` are not readable by the user using `StreamingMkvReader`.
    * Unknown EBML elements not specified in `MkvTypeInfos` of unknown length lead to an exception.
    * Does not do any CRC validation for any Mkv elements with the `CRC-32` element. 

### Release 1.0.1 (November 2017)
* Update to include the url for Amazon Kinesis Video Streams in the pom.xml
