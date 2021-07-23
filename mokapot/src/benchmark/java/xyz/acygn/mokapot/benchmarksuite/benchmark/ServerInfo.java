package xyz.acygn.mokapot.benchmarksuite.benchmark;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Encapsulates the data required to connect to either a Mokapot server, or an RMI server
 *
 */
public class ServerInfo {
	private InetAddress address;
    private int port;
    
    /**
     * Creates a ServerInfo with the given host name and port.
     * @param _hostName The name of the host
     * @param _port The port
     */
    public ServerInfo (String _hostName, int _port) {
    		try {
				address = InetAddress.getByName(_hostName);
			} catch (UnknownHostException e) {
				throw new RuntimeException(e);
			}
    		port = _port;
    }
    
    /**
     * Returns the InetAddress
     * @return The InetAddress
     */
    public InetAddress getAddress() {
    		return address;
    }
    
    /**
     * Returns the port
     * @return The port
     */
    public int getPort() {
    		return port;
    }
}
