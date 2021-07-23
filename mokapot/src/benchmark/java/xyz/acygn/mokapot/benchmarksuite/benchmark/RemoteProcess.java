package xyz.acygn.mokapot.benchmarksuite.benchmark;

import java.net.InetAddress;

/**
 * Encapsulates a running process as well as the IP address and TCP port at
 * which it is reachable. RemoteProcess does not place any restrictions on
 * what kind of process it wraps, so code generating RemoteProcess objects
 * must take care to correctly track which RemoteProcess instances refer to
 * mokapot peers, which to rmi peers, and so on.
 *
 * @author Marcello De Bernardi 10/07/2017.
 */
public class RemoteProcess {
    private Process process;
    private InetAddress address;
    private int port;


    /**
     * Creates a new RemoteProcess object to wrap a {@link Process} that can be
     * communicated with over the network, at the given address and port.
     *
     * @param process
     * @param address
     * @param port
     */
    RemoteProcess(Process process, InetAddress address, int port) {
        this.process = process;
        this.address = address;
        this.port = port;
    }


    /**
     * <p>
     * Returns a {@link Process} reference to the running process. This could be
     * on a local machine as well as on a remote machine, so long as whatever procedure
     * that starts the process (locally or remotely) is implemented in such a way
     * that a Process reference to it was obtained and given to this class's
     * constructor.
     * </p>
     * <p>
     * The primary purpose of this method is to enable the benchmarking script to
     * call destroy() on the process once benchmarking is complete.
     * </p>
     *
     * @return reference to the encapsulated process
     */
    Process getProcess() {
        return process;
    }

    /**
     * Returns the internet address at which the process is available. This may be
     * localhost, or it may be a remote address, depending on how the process was
     * created.
     *
     * @return internet address of remote process
     */
    public InetAddress getAddress() {
        return address;
    }

    /**
     * Returns the port on which the process is available. This should be a TCP port,
     * under the assumption that the startup procedure for the peer itself
     * (i.e. {@link xyz.acygn.mokapot.DistributedServer} or
     * {@link xyz.acygn.mokapot.rmiserver.RMIServerImplementation}) binds itself to
     * a TCP port, but this behavior is not guaranteed.
     *
     * @return port number for remote process
     */
    public int getPort() {
        return port;
    }
}
