package xyz.acygn.mokapot;

import java.util.concurrent.ExecutorService;
import static java.util.concurrent.Executors.newCachedThreadPool;
import xyz.acygn.mokapot.util.ResettableThreadLocal;

/**
 * A Thread-like object that executes using a thread pool, rather than as a
 * thread of its own. This also keeps track of the communicator associated with
 * the thread, in situations where we're on a secondary communicator.
 *
 * @author Alex Smith
 */
class PooledThread {

    /**
     * The thread pool used to run pooled threads.
     */
    private static ExecutorService threadPool = newCachedThreadPool();

    /**
     * Causes all threads currently in the thread pool to no longer be reused.
     * They will be terminated immediately (if dormant), or upon execution
     * ceasing (if not dormant). This method is useful when the system is about
     * to shut down, as it will prevent threads in the thread pool that are
     * waiting to be reused from blocking system shutdown.
     * <p>
     * Further use of <code>PooledThread</code>s is possible, but they will be
     * run on an entirely different set of Java threads (i.e. "rotating" the
     * thread pool to use a different set of threads).
     * <p>
     * The caller will need appropriate security access to shut down threads.
     */
    public static synchronized void rotateThreadPool() {
        threadPool.shutdown();
        threadPool = newCachedThreadPool();
    }

    /**
     * The code that this "thread" runs.
     */
    private final Runnable code;

    /**
     * The distributed communicator associated with the thread tree leading from
     * each thread. If this is <code>null</code>, it means that the thread was
     * not started from within a distributed communicator, but rather came from
     * "outside" the system (and thus counts as part of the main communicator).
     */
    private final static InheritableThreadLocal<DistributedCommunicator> threadTreeCommunicator
            = new InheritableThreadLocal<>();

    /**
     * Returns the distributed communicator associated with the current thread.
     * If the current thread is the extension of a thread that was initially
     * created remotely, or else was a thread created by such a thread, this
     * will return the communicator in question. (Note that the communicator
     * need not still be active, in the case that the thread was created
     * indirectly.) If the thread was created by the Java runtime (e.g. to run
     * <code>main</code>, or by a thread created in such a manner (including
     * indirectly), returns <code>null</code>.
     *
     * @return A distributed communicator, or <code>null</code>.
     */
    static DistributedCommunicator getCommunicator() {
        return threadTreeCommunicator.get();
    }

    /**
     * Creates a new thread pool task for which the code to run is specified by
     * overriding the <code>run()</code> method. Because it requires overriding
     * a method, this constructor should only be called by a derived class and
     * not directly.
     */
    protected PooledThread() {
        code = () -> {
            throw new IllegalArgumentException(
                    "The run() method should be overridden");
        };
    }

    /**
     * Creates a new thread pool task that mimics a thread, given the code to
     * run.
     *
     * @param code The code that's run by this "thread".
     */
    PooledThread(Runnable code) {
        this.code = code;
    }

    /**
     * Starts running this thread pool task, on an existing but dormant thread
     * if one is available, or creating a new one if none are available. Any
     * <code>ResettableThreadLocal</code> storage objects will be told to
     * consider the used thread to be a new thread, thus causing it to act as
     * though it were new even if it was actually recycled.
     * <p>
     * Each individual task should only be run once. Create a duplicate task if
     * you wish to run a task again.
     * <p>
     * Note that the communicator will not be held alive by the thread; the
     * caller will need to do that if necessary.
     *
     * @param madeBy The distributed communicator that created this thread pool
     * task (and will be reported via <code>getCommunicator()</code>.
     */
    public void start(DistributedCommunicator madeBy) {
        synchronized (PooledThread.class) {
            threadPool.execute(() -> {
                threadTreeCommunicator.set(madeBy);
                try {
                    this.run();
                } finally {
                    threadTreeCommunicator.set(null);
                    ResettableThreadLocal.reset();
                }
            });
        }
    }

    /**
     * Performs the tasks of this thread pool task. By default, this runs the
     * tasks specified in the constructor, but it can be overridden to specify
     * alternate tasks to run (just like with <code>Thread</code>).
     */
    protected void run() {
        code.run();
    }

    /**
     * Returns a description of this thread.
     *
     * @return A description of the thread
     */
    @Override
    public String toString() {
        return "pooled thread '" + Thread.currentThread().getName()
                + "' on communicator for address " + getCommunicator().getMyAddress();
    }
}
