package xyz.acygn.mokapot.util;

import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import static xyz.acygn.mokapot.util.ThreadUtils.delayInterruptions;

/**
 * An output stream that writes bytes to a blocking queue.
 *
 * @author Alex Smith
 */
public class BlockingQueueOutputStream extends OutputStream {

    /**
     * The queue to which bytes are written.
     */
    private final BlockingQueue<Byte> queue;

    /**
     * Creates a new output stream that writes bytes to a queue.
     *
     * @param queue The queue to which the bytes are written.
     */
    public BlockingQueueOutputStream(BlockingQueue<Byte> queue) {
        this.queue = queue;
    }

    /**
     * Pushes the given byte onto the backing queue.
     * <p>
     * In a minor deviation from the normal specification for I/O, any
     * interruptions of the current thread will be delayed until after the write
     * has completed. This makes the method safe to use with buffering
     * implementations that don't expect interruptions.
     *
     * @param b The byte to push to the back of the backing queue. (Only the
     * bottom 8 bits of the given integer are used.)
     */
    @Override
    public void write(int b) {
        delayInterruptions(() -> queue.put((byte) (b & 0xff)));
    }
}
