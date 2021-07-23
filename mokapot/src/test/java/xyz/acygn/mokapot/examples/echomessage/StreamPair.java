package xyz.acygn.mokapot.examples.echomessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A pair of streams (an input stream, and an output stream), plus a listener on
 * the input stream.
 * <p>
 * When information appears on the input stream, it will be sent to any
 * listeners specified by the <code>addListener</code> message. Information can
 * be sent to the output stream via requesting it via the getter method.
 * <p>
 * The input stream will block until at least one listener is added. From that
 * point onwards, incoming data will be sent to the listener as soon as it
 * arrives.
 * <p>
 * The stream pair also supports exit listeners, which will trigger once when
 * the stream pair is closed (which happens when input reaches EOF, or there's
 * an exception trying to do input or output).
 * <p>
 * You can also close a stream pair by interrupting it.
 *
 * @author Alex Smith
 */
public class StreamPair extends Thread {

    /**
     * The input stream half of this stream pair.
     */
    private final InputStream inputStream;

    /**
     * The output stream half of this stream pair.
     */
    private final PrintStream outputStream;

    /**
     * The listeners currently listening to the input half of this stream pair.
     */
    private final Set<Consumer<String>> inputListeners = new HashSet<>();

    /**
     * The listeners currently listening for this stream pair being closed. (The
     * stream pair becomes closed if the input half reaches EOF or if there's an
     * exception communicating to either half.)
     */
    private final Set<Consumer<IOException>> exitListeners = new HashSet<>();

    /**
     * Creates a stream pair from a specified pair of streams.
     *
     * @param inputStream The input stream to use.
     * @param outputStream The output stream to use. This must be a
     * <code>PrintStream</code>.
     */
    public StreamPair(InputStream inputStream, PrintStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    /**
     * Creates a stream pair for the standard input and output streams.
     */
    public StreamPair() {
        this(System.in, System.out);
    }

    /**
     * Adds a new input listener. Whenever input is available, all input
     * listeners will be sent that input. This is not retroactive: as soon as
     * any input listeners are established, any input will be sent to all the
     * input listeners available at the time that input occurred. However, as a
     * special case, input before the first input listener is established will
     * be sent to that listener.
     *
     * @param inputListener The input listener to add.
     * @throws IllegalStateException If this stream pair has already closed
     */
    public synchronized void addInputListener(Consumer<String> inputListener)
            throws IllegalStateException {
        inputListeners.add(inputListener);

        switch (this.getState()) {
            case NEW:
                this.start();
                break;
            case TERMINATED:
                throw new IllegalStateException(
                        "Adding an input listener to a closed stream pair");
            default:
                break;
        }
    }

    /**
     * Adds a new exit listener to this stream pair, which will be called when
     * it is closed. It will be given the exception that caused the stream pair
     * to close as an argument.
     * <p>
     * To guarantee that the exit listener runs, establish all exit listeners
     * before you establish any input listeners.
     *
     * @param exitListener The exit listener to add.
     * @throws IllegalStateException If the stream pair has already closed
     */
    public synchronized void addExitListener(Consumer<IOException> exitListener)
            throws IllegalStateException {
        exitListeners.add(exitListener);

        if (this.getState().equals(Thread.State.TERMINATED)) {
            throw new IllegalStateException(
                    "Adding an exit listener to a closed stream pair");
        }
    }

    /**
     * Implements the listening behaviour of this stream pair. This is
     * automatically run in a thread of its own when the first input listener is
     * added, and should not be run manually. It terminates when the stream pair
     * is closed.
     */
    @Override
    public void run() {
        BufferedReader reader
                = new BufferedReader(new InputStreamReader(inputStream));
        try {
            while (true) {
                String line = reader.readLine();
                synchronized (this) {
                    inputListeners.stream().forEach((inputListener)
                            -> inputListener.accept(line));
                }
            }
        } catch (IOException ex) {
            exitListeners.stream().forEach((exitListener)
                    -> exitListener.accept(ex));
        }
    }

    /**
     * Returns the output stream for this stream pair. This can be used to send
     * messages via the stream pair.
     *
     * @return This stream pair's output stream.
     */
    public PrintStream getOutputStream() {
        return outputStream;
    }
}
