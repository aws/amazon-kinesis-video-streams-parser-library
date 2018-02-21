# Kinesis Video Stream Consumer

## Build
If you use eclipse, I can build jar file as follows.
1. Right-click **com.amazonaws.kinesisvideo.parser.examples.KinesisVideoConsumerMain.java**.
1. Select **Export**, **Java**, **Runnable JAR file**.
1. Choose appropriate **Launch configuration** and **Export destination**.
1. Select **Extract required libraries into generated JAR** as **Library handling**.
1. Push **Finish**.

Copy `src/test/resources/log4j2.properties` to your project root.

## Usage
You can run consumer,
```bash
$ java -Dlog4j.configurationFile=log4j2.properties -jar KinesisVideoConsumerMain.jar
```
