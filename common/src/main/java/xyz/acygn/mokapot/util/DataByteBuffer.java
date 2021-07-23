package xyz.acygn.mokapot.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;

/**
 * A <code>ByteBuffer</code> wrapped to implement (most of) the
 * <code>DataInput</code> and <code>DataOutput</code> interfaces. The buffer can
 * be written once, and then read any number of times, with
 * <code>resetForRead()</code> called in between the write and read, and in
 * between each read. The buffer's maximum length must be specified in advance;
 * its actual length will become fixed only after writing is complete (and be
 * equal to the number of bytes written).
 *
 * @author Alex Smith
 */
public class DataByteBuffer implements DataInput, DataOutput {

    /**
     * The subset of the object description that serialises down to primitives.
     * This also includes the names of classes, in situations where those have
     * to be serialised. This contains, effectively, the copiable portion of the
     * object (except for copiable references to noncopiable objects).
     */
    private ByteBuffer byteBuffer;

    /**
     * Constructs a new data byte buffer with the given maximum length.
     *
     * @param length The maximum length of the new buffer.
     */
    public DataByteBuffer(int length) {
        byteBuffer = ByteBuffer.allocate(length);
    }

    /**
     * Constructs a new data byte buffer backed by the given byte array. The
     * cursor will start at the start of the array (thus, if it's read-only,
     * it's full, and if it's read-write, it's empty).
     *
     * @param storage The byte array that backs the data byte buffer.
     * @param readOnly Whether the byte array is read-only.
     */
    public DataByteBuffer(byte[] storage, boolean readOnly) {
        byteBuffer = ByteBuffer.wrap(storage);
        if (readOnly) {
            byteBuffer = byteBuffer.asReadOnlyBuffer();
        }
    }

    /**
     * Constructs a new read-only data byte buffer as a copy of the given data
     * byte buffer.
     *
     * @param buffer The buffer to copy.
     */
    public DataByteBuffer(DataByteBuffer buffer) {
        byteBuffer = buffer.byteBuffer.duplicate();
        resetForReadInternal();
    }

    /**
     * Writes a single byte to the copiable portion of this description.
     *
     * @param b The byte to write.
     * @throws IOException If the description is full or read-only
     */
    @Override
    public void write(int b) throws IOException {
        try {
            byteBuffer.put((byte) b);
        } catch (BufferOverflowException | ReadOnlyBufferException e) {
            throw new IOException(e);
        }
    }

    /**
     * Writes a sequence of bytes to the copiable portion of this description.
     *
     * @param bytes The bytes to write.
     * @throws IOException If the description is full or read-only
     */
    @Override
    public void write(byte[] bytes) throws IOException {
        try {
            byteBuffer.put(bytes);
        } catch (BufferOverflowException | ReadOnlyBufferException e) {
            throw new IOException(e);
        }
    }

    /**
     * Writes a sequence of bytes to the copiable portion of this description,
     * specified as a slice of an array.
     *
     * @param array The array containing the bytes.
     * @param offset The index of the first byte to write.
     * @param length The number of bytes to write.
     * @throws IOException If the description is full or read-only
     */
    @Override
    public void write(byte[] array, int offset, int length) throws IOException {
        try {
            byteBuffer.put(array, offset, length);
        } catch (BufferOverflowException | ReadOnlyBufferException e) {
            throw new IOException(e);
        }
    }

    /**
     * Writes a boolean to the copiable portion of this description.
     *
     * @param b The boolean to write.
     * @throws IOException If the description is full or read-only
     */
    @Override
    public void writeBoolean(boolean b) throws IOException {
        try {
            byteBuffer.put(b ? (byte) 1 : (byte) 0);
        } catch (BufferOverflowException | ReadOnlyBufferException e) {
            throw new IOException(e);
        }
    }

    /**
     * Writes a byte to the copiable portion of this description. Identical to
     * <code>write</code>, and provided to conform with the
     * <code>ObjectOutput</code> interface.
     *
     * @param b The byte to write.
     * @throws IOException If the description is full or read-only
     */
    @Override
    public void writeByte(int b) throws IOException {
        write(b);
    }

    /**
     * Writes a char to the copiable portion of this description.
     *
     * @param c The char to write.
     * @throws IOException If the description is full or read-only
     */
    @Override
    public void writeChar(int c) throws IOException {
        try {
            byteBuffer.putChar((char) c);
        } catch (BufferOverflowException | ReadOnlyBufferException e) {
            throw new IOException(e);
        }
    }

    /**
     * Writes a short to the copiable portion of this description.
     *
     * @param s The short to write.
     * @throws IOException If the description is full or read-only
     */
    @Override
    public void writeShort(int s) throws IOException {
        try {
            byteBuffer.putShort((short) s);
        } catch (BufferOverflowException | ReadOnlyBufferException e) {
            throw new IOException(e);
        }
    }

    /**
     * Writes an int to the copiable portion of this description.
     *
     * @param i The int to write.
     * @throws IOException If the description is full or read-only
     */
    @Override
    public void writeInt(int i) throws IOException {
        try {
            byteBuffer.putInt(i);
        } catch (BufferOverflowException | ReadOnlyBufferException e) {
            throw new IOException(e);
        }
    }

    /**
     * Writes a float to the copiable portion of this description.
     *
     * @param f The float to write.
     * @throws IOException If the description is full or read-only
     */
    @Override
    public void writeFloat(float f) throws IOException {
        try {
            byteBuffer.putFloat(f);
        } catch (BufferOverflowException | ReadOnlyBufferException e) {
            throw new IOException(e);
        }
    }

    /**
     * Writes a long to the copiable portion of this description.
     *
     * @param l The long to write.
     * @throws IOException If the description is full or read-only
     */
    @Override
    public void writeLong(long l) throws IOException {
        try {
            byteBuffer.putLong(l);
        } catch (BufferOverflowException | ReadOnlyBufferException e) {
            throw new IOException(e);
        }
    }

    /**
     * Writes a double to the copiable portion of this description.
     *
     * @param d The double to write.
     * @throws IOException If the description is full or read-only
     */
    @Override
    public void writeDouble(double d) throws IOException {
        try {
            byteBuffer.putDouble(d);
        } catch (BufferOverflowException | ReadOnlyBufferException e) {
            throw new IOException(e);
        }
    }

    /**
     * Reads a number of bytes from the copiable portion of the description.
     *
     * @param into An array to read the bytes into.
     * @throws IOException If the description is write-only or the end of the
     * description has been reached
     */
    @Override
    public void readFully(byte[] into) throws IOException {
        try {
            byteBuffer.get(into);
        } catch (BufferUnderflowException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Reads a number of bytes from the copiable portion of the description.
     *
     * @param into An array to read the bytes into.
     * @param offset The first index in the array to read into.
     * @param length The number of bytes to read.
     * @throws IOException If the description is write-only or the end of the
     * description has been reached
     */
    @Override
    public void readFully(byte[] into, int offset, int length) throws IOException {
        try {
            byteBuffer.get(into, offset, length);
        } catch (BufferUnderflowException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Skips over (that is, reads without returning) a given number of bytes in
     * the copiable portion of the description.
     *
     * @param n The number of bytes to skip.
     * @return <code>n</code>.
     * @throws IOException If the description is write-only or the end of the
     * description has been reached
     * @throws IllegalArgumentException If n is negative
     */
    @Override
    public int skipBytes(int n) throws IOException {
        if (n < 0) {
            throw new IllegalArgumentException("cannot skip backwards");
        }
        if (n > byteBuffer.remaining()) {
            throw new IOException("attempt to skip past end of description");
        }
        int position = byteBuffer.position();
        byteBuffer.position(n + position);
        return n;
    }

    /**
     * Reads a boolean from the copiable portion of the description.
     *
     * @return The read boolean.
     * @throws IOException If the description appears to be corrupted, or the
     * end of the description was reached, or the description is write-only
     */
    @Override
    public boolean readBoolean() throws IOException {
        try {
            switch (byteBuffer.get()) {
                case 0:
                    return false;
                case 1:
                    return true;
                default:
                    throw new IOException("boolean was neither true nor false");
            }
        } catch (BufferUnderflowException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Reads a byte from the copiable portion of the description.
     *
     * @return The read byte.
     * @throws IOException If the end of the description was reached, or the
     * description is write-only
     */
    @Override
    public byte readByte() throws IOException {
        try {
            return byteBuffer.get();
        } catch (BufferUnderflowException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Reads a short from the copiable portion of the description.
     *
     * @return The read short.
     * @throws IOException If the end of the description was reached, or the
     * description is write-only
     */
    @Override
    public short readShort() throws IOException {
        try {
            return byteBuffer.getShort();
        } catch (BufferUnderflowException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Reads a char from the copiable portion of the description.
     *
     * @return The read char.
     * @throws IOException If the end of the description was reached, or the
     * description is write-only
     */
    @Override
    public char readChar() throws IOException {
        try {
            return byteBuffer.getChar();
        } catch (BufferUnderflowException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Reads an int from the copiable portion of the description.
     *
     * @return The read int.
     * @throws IOException If the end of the description was reached, or the
     * description is write-only
     */
    @Override
    public int readInt() throws IOException {
        try {
            return byteBuffer.getInt();
        } catch (BufferUnderflowException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Reads a long from the copiable portion of the description.
     *
     * @return The read long.
     * @throws IOException If the end of the description was reached, or the
     * description is write-only
     */
    @Override
    public long readLong() throws IOException {
        try {
            return byteBuffer.getLong();
        } catch (BufferUnderflowException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Reads a float from the copiable portion of the description.
     *
     * @return The read float.
     * @throws IOException If the end of the description was reached, or the
     * description is write-only
     */
    @Override
    public float readFloat() throws IOException {
        try {
            return byteBuffer.getFloat();
        } catch (BufferUnderflowException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Reads a double from the copiable portion of the description.
     *
     * @return The read double.
     * @throws IOException If the end of the description was reached, or the
     * description is write-only
     */
    @Override
    public double readDouble() throws IOException {
        try {
            return byteBuffer.getDouble();
        } catch (BufferUnderflowException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Reads a byte from the copiable portion of the description, then adds 256
     * if the resulting value is negative.
     *
     * @return The read byte.
     * @throws IOException If the end of the description was reached, or the
     * description is write-only
     */
    @Override
    public int readUnsignedByte() throws IOException {
        int rv = readByte();
        if (rv < 0) {
            rv += 256;
        }
        return rv;
    }

    /**
     * Reads a short from the copiable portion of the description, then adds
     * 65536 if the resulting value is negative.
     *
     * @return The read short.
     * @throws IOException If the end of the description was reached, or the
     * description is write-only
     */
    @Override
    public int readUnsignedShort() throws IOException {
        int rv = readShort();
        if (rv < 0) {
            rv += 65536;
        }
        return rv;
    }

    /**
     * Reads a number of bytes from the description, outputting them as a
     * <code>ByteBuffer</code>. This might potentially share its backing storage
     * with the description itself, so is more efficient than using a byte array
     * in cases where it's possible to operate on buffers directly.
     *
     * @param length The number of bytes to read.
     * @return A buffer holding the read bytes.
     * @throws java.io.IOException If there is no capacity in the buffer
     */
    public ByteBuffer readByteSlice(int length) throws IOException {
        try {
            ByteBuffer slicedPortion = byteBuffer.slice();
            slicedPortion.limit(length);
            byteBuffer.position(byteBuffer.position() + length);
            return slicedPortion;
        } catch (BufferUnderflowException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Not implemented.
     *
     * @param s Ignored.
     * @throws UnsupportedOperationException Always
     */
    @Override
    public void writeBytes(String s) throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "Please encode strings written into buffers yourself");
    }

    /**
     * Not implemented.
     *
     * @param s Ignored.
     * @throws UnsupportedOperationException Always
     */
    @Override
    public void writeChars(String s) throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "Please encode strings written into buffers yourself");
    }

    /**
     * Not implemented.
     *
     * @param s Ignored.
     * @throws UnsupportedOperationException Always
     */
    @Override
    public void writeUTF(String s) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Please encode strings written into buffers yourself");
    }

    /**
     * Not implemented.
     *
     * @return Never returns.
     * @throws UnsupportedOperationException Always
     */
    @Override
    public String readLine() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Please decode strings read from buffers yourself");
    }

    /**
     * Not implemented.
     *
     * @return Never returns.
     * @throws UnsupportedOperationException Always
     */
    @Override
    public String readUTF() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Please decode strings read from buffers yourself");
    }

    /**
     * Makes this buffer readable and read-only, and resets the read pointer.
     * Used once writing to the buffer is complete, or once reading the buffer
     * is complete (to allow it to be read again).
     */
    public void resetForRead() {
        resetForReadInternal();
    }

    /**
     * Non-overridable version of <code>resetForRead</code>. Used to allow the
     * method to be called safely from a constructor, without potentially
     * invoking unwanted behaviour from a derived class.
     */
    private void resetForReadInternal() {
        if (byteBuffer.isReadOnly()) {
            byteBuffer.position(0);
        } else {
            byteBuffer.flip();
            byteBuffer = byteBuffer.asReadOnlyBuffer();
        }
    }

    /**
     * Returns the number of bytes that have been written into this buffer. That
     * is, its maximum length if it's been made read-only, or the number of
     * bytes written so far if it's still writable.
     *
     * @return The number of bytes that have been written into the buffer.
     */
    public int getWrittenLength() {
        if (byteBuffer.isReadOnly()) {
            return byteBuffer.limit();
        } else {
            return byteBuffer.position();
        }
    }

    /**
     * Checks to see whether this buffer is rewound. That is, that the buffer
     * has been placed into read-only mode, and the read cursor is currently at
     * the start of its data (indicating that none of it was read since the
     * previous reset).
     *
     * @return <code>true</code> if and only if the buffer is rewound
     */
    public boolean isRewound() {
        return byteBuffer.isReadOnly() && byteBuffer.position() == 0;
    }

    /**
     * Checks to see whether this buffer is in read-only mode.
     *
     * @return <code>true</code> if the buffer is currently read-only.
     */
    public boolean isReadOnly() {
        return byteBuffer.isReadOnly();
    }

    /**
     * An array of hexadecimal characters. Used to hex-dump the bytes that make
     * up the buffer.
     */
    protected static final char[] HEX_CHARS = new char[]{
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    /**
     * Produces a string representation of the buffer. This contains its
     * contents in a human-readable form, and an indication of whether it's
     * still writable or whether it's now read-only.
     * <p>
     * The string representation is optimised for a mix of binary data and
     * length-prefixed strings; in particular, sequences 3 or more bytes long
     * that appear to be ASCII will be output as strings rather than in
     * hexadecimal, and ASCII characters immediately before a string will not be
     * included as part of the string if they appear to be part of a length
     * prefix for that string.
     *
     * @return A string representation of the buffer.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int byteCount;
        if (byteBuffer.isReadOnly()) {
            sb.append("[read-only buffer:");
            byteCount = byteBuffer.limit();
        } else {
            sb.append("[writable buffer:");
            byteCount = byteBuffer.position();
        }

        /* stringLength is the number of characters that might be part of a
           string that we've just seen, or a negative number to state that the
           next few characters are <i>not</i> part of a string */
        int stringLength = 0;
        final int MIN_STRING_LENGTH = 3;
        StringBuilder stringSoFar = null;
        int i = 0;
        while (i <= byteCount) {
            int b = (i == byteCount ? 999 : byteBuffer.get(i));

            if (b >= 32 && b <= 126 && stringLength >= 0) {
                if (stringSoFar == null) {
                    stringSoFar = new StringBuilder(" \"");
                }
                stringSoFar.append((char) b);
                stringLength++;
            } else {
                if (stringLength > 0 && stringLength < MIN_STRING_LENGTH) {
                    i -= stringLength;
                    stringLength = -stringLength;
                    stringSoFar = null;
                    continue;
                } else if (stringLength >= MIN_STRING_LENGTH
                        && stringSoFar != null) {
                    /* Is this a length-prefixed string? If so, we probably
                       shouldn't include the characters of the length as part of
                       it. */
                    if (i >= stringLength + 3
                            && byteBuffer.getInt(i - stringLength - 3)
                            == stringLength - 1) {
                        i -= stringLength;
                        stringLength = -1;
                        stringSoFar = null;
                        continue;
                    }

                    sb.append(stringSoFar);
                    sb.append('"');
                    stringLength = 0;
                    stringSoFar = null;
                } else if (stringLength < 0) {
                    stringLength++;
                }

                if (b < 0) {
                    b += 256;
                }
                if (b != 999) {
                    sb.append(' ');
                    sb.append(HEX_CHARS[b / 16]);
                    sb.append(HEX_CHARS[b % 16]);
                }
            }
            i++;
        }

        sb.append(']');
        return sb.toString();
    }
}
