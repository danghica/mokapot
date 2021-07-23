package xyz.acygn.mokapot.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.concurrent.BlockingQueue;

/**
 * An input stream that reads bytes from a blocking queue.
 *
 * @author Alex Smith
 */
public class BlockingQueueInputStream extends InputStream {

    /**
     * The queue from which bytes are read.
     */
    private final BlockingQueue<Byte> queue;

    /**
     * Creates a new input stream that reads bytes from a queue.
     *
     * @param queue The queue from which the bytes are read.
     */
    public BlockingQueueInputStream(BlockingQueue<Byte> queue) {
        this.queue = queue;
    }

    /**
     * Reads one byte from the underlying queue.
     *
     * @return A byte shifted from the front of the backing queue.
     * @throws IOException If the read is interrupted while waiting for new data
     * to enter the queue
     */
    @Override
    public int read() throws IOException {
        try {
            int b = queue.take();
            /* Apparently even bytes are signed in Java. */
            if (b < 0) {
                b += 256;
            }
            return b;
        } catch (InterruptedException ex) {
            throw new InterruptedIOException(
                    "BlockingQueueInputStream.read() interrupted");
        }
    }

    /**
     * Reads a number of bytes from the underlying queue. If the requested
     * number of bytes are available, all of them will be read. Otherwise, as
     * many as can be read without blocking will be read. Exception: if 0 bytes
     * can be read without blocking, the method will block until at least 1 is
     * readable.
     *
     * @param array The array to read into.
     * @param offset The first index of the array to read into.
     * @param length The maximum number of elements to read.
     * @return The number of elements that were read.
     * @throws IOException If the read is interrupted before the first element
     * is read
     */
    @Override
    public int read(byte[] array, int offset, int length) throws IOException {
        array[offset] = (byte) read();
        int ptr = offset + 1;
        while (ptr < offset + length) {
            Byte b = queue.poll();
            if (b == null) {
                return ptr - offset;
            }
            array[ptr] = b;
            ptr++;
        }
        return length;
    }

    /**
     * Returns the current size of the backing queue.
     *
     * @return The number of bytes in the backing queue.
     */
    @Override
    public int available() {
        return queue.size();
    }
}
