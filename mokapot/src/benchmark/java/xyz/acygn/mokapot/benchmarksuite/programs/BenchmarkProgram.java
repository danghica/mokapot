package xyz.acygn.mokapot.benchmarksuite.programs;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.util.ArrayList;

import xyz.acygn.mokapot.CommunicationAddress;
import xyz.acygn.mokapot.DistributedCommunicator;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkData;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.rmiserver.RMIServer;

public abstract class BenchmarkProgram implements Benchmarkable {
	protected ArrayList<CommunicationAddress> remotes;
	protected DistributedCommunicator communicator;
	protected ArrayList<RMIServer> servers;
	
	
	@Override
	public final void setup(int localPort, RemoteProcess... peers) throws IOException, NotBoundException {
		if (this.getType() == ExampleType.MOKAPOT) {
			remotes = new ArrayList<>();
			try {
			communicator = new DistributedCommunicator(BenchmarkData.keyLocation, BenchmarkData.getPassword().toCharArray());
			} catch (KeyManagementException | KeyStoreException e) {
				throw new RuntimeException(e);
			}
			
			communicator.startCommunication();
			
			//TODO uncomment this if you want to use the debug monitor.
			//communicator.setDebugMonitor(new BenchmarkingDebugMonitor());
			
			for (RemoteProcess peer : peers) {
				remotes.add(communicator.lookupAddress(peer.getAddress(), peer.getPort()));
			}
		} else if (this.getType() == ExampleType.RMI) {
			servers = new ArrayList<RMIServer>();

			if (System.getSecurityManager() == null) {
				System.setSecurityManager(new SecurityManager());
			}

			for (RemoteProcess peer : peers) {
				servers.add((RMIServer) LocateRegistry.getRegistry(peer.getAddress().getHostAddress(), peer.getPort()).lookup("RMIServer"));
			}
		}
	}

	@Override
	public void stop() throws IOException, NotBoundException {

	}
}
