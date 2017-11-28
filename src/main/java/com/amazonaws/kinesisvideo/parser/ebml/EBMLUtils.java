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

import org.apache.commons.lang3.Validate;

import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * Class used to parse EBML Ids and Sizes that make up the EBML element's meta data.
 */
public class EBMLUtils {

    /**
     * Max length for a EBML ID
     */
    public static final int EBML_ID_MAX_BYTES = 4;
    public static final int EBML_SIZE_MAX_BYTES = 8;

    private static final int BYTE_MASK = 0xFF;

    /** Default constructor to make checkstyle happy */
    private EBMLUtils() {

    }


    /**
     * constant for byte with first bit set.
     */
    private static final int BYTE_WITH_FIRST_BIT_SET = 0b10000000;

    static void readId(final TrackingReplayableIdAndSizeByteSource source, IdConsumer resultAcceptor) {
        if (!isEnoughBytes(source, 1)) {
            return;
        }
        final int firstByte = readByte(source);

        if (firstByte == -1) {
            resultAcceptor.accept(firstByte, 1);
        }
        Validate.isTrue(firstByte >= 0, "EBML Id has negative firstByte" + firstByte);

        final int numAdditionalBytes = getNumLeadingZeros(firstByte);
        if (!isEnoughBytes(source, numAdditionalBytes)) {
            return;
        }

        Validate.isTrue(numAdditionalBytes <= (EBML_ID_MAX_BYTES - 1),
                "Trying to decode an EBML ID and it wants " + numAdditionalBytes
                        + " more bytes, but IDs max out at 4 bytes. firstByte was " + firstByte);

        final int rest = (int) readEbmlValueNumber(source, numAdditionalBytes);

        resultAcceptor.accept(firstByte << (numAdditionalBytes * Byte.SIZE) | rest, numAdditionalBytes + 1);
    }

    /**
     * Read a variable-size integer that encodes its own length.
     * Used to read the size of an ebml element.
     * <p>
     * 2.1.  Variable size integer
     * <p>
     * For both element ID and size descriptor EBML uses a variable size
     * integer, coded according to a schema similar to that of UTF-8
     * [UTF-8] encoding. The variable size integer begins with zero or
     * more zero bits to define the width of the integer. Zero zeroes
     * means a width of one byte, one zero a width of two bytes etc. The
     * zeroes are followed by a marker of one set bit and then follows the
     * actual integer data. The integer data consists of alignment data
     * and tail data. The alignment data together with the width
     * descriptor and the marker makes up one ore more complete bytes. The
     * tail data is as many bytes as there were zeroes in the width
     * descriptor, i.e. width-1.
     * <p>
     * VINT           = VINT_WIDTH VINT_MARKER VINT_DATA
     * VINT_WIDTH     = *%b0
     * VINT_MARKER    = %b1
     * VINT_DATA      = VINT_ALIGNMENT VINT_TAIL
     * VINT_ALIGNMENT = *BIT
     * VINT_TAIL      = *BYTE
     * <p>
     * An alternate way of expressing this is the following definition,
     * where the width is the number of levels of expansion.
     * <p>
     * VINT = ( %b0 VINT 7BIT ) / ( %b1 7BIT )
     * <p>
     * Some examples of the encoding of integers of width 1 to 4. The x:es
     * represent bits where the actual integer value would be stored.
     * <p>
     * Width  Size  Representation
     * 1    2^7   1xxx xxxx
     * 2    2^14  01xx xxxx  xxxx xxxx
     * 3    2^21  001x xxxx  xxxx xxxx  xxxx xxxx
     * 4    2^28  0001 xxxx  xxxx xxxx  xxxx xxxx  xxxx xxxx
     *
     * @param source buffer containing chunks of data
     * @param resultAcceptor the callback called when the size of an ebml element is identified.
     * @see "http://www.matroska.org/technical/specs/rfc/index.html"
     */
    private static void readEbmlInt(final TrackingReplayableIdAndSizeByteSource source, SizeConsumer resultAcceptor) {
        if (!isEnoughBytes(source, 1)) {
            return;
        }
        final int firstByte = readByte(source);
        Validate.isTrue(firstByte >= 0, "EBML Int has negative firstByte" + firstByte);

        final int size = getNumLeadingZeros(firstByte);
        if (!isEnoughBytes(source, size)) {
            return;
        }

        // Read the rest of the bytes
        final long rest = readEbmlValueNumber(source, size);

        // Slap the first byte's value onto the front (with the first one-bit unset)
        resultAcceptor.accept((firstByte & ~((byte) BYTE_WITH_FIRST_BIT_SET >> size)) << (size * Byte.SIZE) | rest,
                size + 1);
    }

    /**
     * Read an EBML integer value of varying length from the provided buffer.
     * @param byteBuffer The buffer to read from.
     * @return The integer value.
     * @see "http://www.matroska.org/technical/specs/rfc/index.html"
     */
    public static long readEbmlInt(final ByteBuffer byteBuffer) {
        final int firstByte =  byteBuffer.get() & BYTE_MASK;
        Validate.isTrue(firstByte >= 0, "EBML Int has negative firstByte" + firstByte);

        final int size = getNumLeadingZeros(firstByte);
        // Read the rest of the bytes
        final long rest = readUnsignedIntegerSevenBytesOrLess(byteBuffer, size);

        // Slap the first byte's value onto the front (with the first one-bit unset)
        return ((firstByte & ~((byte) BYTE_WITH_FIRST_BIT_SET >> size)) << (size * Byte.SIZE) | rest);
    }


    /**
     * An alias for readEbmlInt that makes it clear we're reading a data size value.
     *
     * @return long value
     */
    static void readSize(final TrackingReplayableIdAndSizeByteSource source, SizeConsumer resultAcceptor) {
        readEbmlInt(source, resultAcceptor);
    }


    private static int readByte(final TrackingReplayableIdAndSizeByteSource source) {
        return source.readByte() & BYTE_MASK;
    }

    private static boolean isEnoughBytes(final TrackingReplayableIdAndSizeByteSource source, final int len) {
        return source.checkAndReadIntoReplayBuffer(len);
    }

    /**
     * Gets the number of leading zero bits in the specified integer as if it were a byte (to avoid a cast).
     * <p>
     * This is the "count leading zeroes" problem: http://en.wikipedia.org/wiki/Find_first_set
     * <p>
     * Intel processors actually have this as a built-in instruction but we
     * can't access that from the JVM.
     *
     * @param b byte for which we need to find the number of leading zeros.
     *          This is typed as an int but should only have the lower 8 bits set.A
     * @return number of leading zeros in the byte.
     */
    private static int getNumLeadingZeros(final int b) {
        return Integer.numberOfLeadingZeros(b) - (Integer.SIZE - Byte.SIZE);
    }

    /**
     * Read a variable-length data payload as a number, given its size.
     * <p>
     * EBML uses big endian/network order byte order, i.e. most
     * significant bit first. All of the tokens above are byte aligned.
     * <p>
     * Besides having an element list as data payload an element can have
     * its data typed with any of seven predefined data types. The actual
     * type information isn't stored in EBML but is inferred from the
     * document type definition through the element ID. The defined data
     * types are signed integer, unsigned integer, float, ASCII string,
     * UTF-8 string, date and binary data.
     * <p>
     * VALUE = INT / UINT / FLOAT / STRING / DATE / BINARY
     * <p>
     * INT = *8BYTE
     * <p>
     * Signed integer, represented in two's complement notation, sizes
     * from 0-8 bytes. A zero byte integer represents the integer value 0.
     *
     * @param source buffer containing chunks of data
     * @param size Size of the integer in bytes
     * @return long value
     */
    private static long readEbmlValueNumber(final TrackingReplayableIdAndSizeByteSource source, final long size) {
        Validate.inclusiveBetween(0L,
                (long) EBML_SIZE_MAX_BYTES,
                size,
                "Asked for a numeric value of invalid size " + size);

        Validate.isTrue(isEnoughBytes(source, (int) size));

        long value = 0;
        for (int i = 0; i < size; i++) {
            // readByte(buffer) returns a value from 0-255 as an int, already masked with 0xFF
            final int result = readByte(source);
            value = (value << Byte.SIZE) | result;
        }

        return value;
    }

    /**
     * A specialized method used to read a variable length unsigned integer of size 7 bytes or less.
     * @param byteBuffer The byteBuffer to read from.
     * @param size The size of bytes.
     * @return The long containing the integer value.
     */
    public static long readUnsignedIntegerSevenBytesOrLess(final ByteBuffer byteBuffer, long size) {
        Validate.inclusiveBetween(0L,
                (long) EBML_SIZE_MAX_BYTES - 1,
                size,
                "Asked for a numeric value of invalid size " + size);

        Validate.isTrue(byteBuffer.remaining() >= size);
        long value = 0;
        for (int i = 0; i < size; i++) {
            final int result = byteBuffer.get() & 0xFF;
            value = (value << Byte.SIZE) | result;
        }

        return value;
    }

    public static long readDataSignedInteger(final ByteBuffer byteBuffer, long size) {
        Validate.inclusiveBetween(0L,
                (long) EBML_SIZE_MAX_BYTES,
                size,
                "Asked for a numeric value of invalid size " + size);

        Validate.isTrue(byteBuffer.remaining() >= size);
        long value = 0;
        for (int i = 0; i < size; i++) {
            final int result = byteBuffer.get() & 0xFF;
            if (i == 0) {
                boolean positive = (result & 0x80) == 0;
                if (!positive) {
                    value = -1;
                }
            }
            value = (value << Byte.SIZE) | result;
        }

        return value;
    }



    public static BigInteger readDataUnsignedInteger(final ByteBuffer byteBuffer, long size) {
        Validate.inclusiveBetween(0L,
                (long) EBML_SIZE_MAX_BYTES,
                size,
                "Asked for a numeric value of invalid size " + size);

        Validate.isTrue(byteBuffer.remaining() >= size);
        byte [] byteArray = new byte[(int)size];
        byteBuffer.get(byteArray);
        return new BigInteger(1, byteArray);
    }

    @FunctionalInterface
    interface IdConsumer {
        void accept(int val, long idNumBytes);
    }

    @FunctionalInterface
    interface SizeConsumer {
        void accept(long val, long sizeNumBytes);
    }
}
