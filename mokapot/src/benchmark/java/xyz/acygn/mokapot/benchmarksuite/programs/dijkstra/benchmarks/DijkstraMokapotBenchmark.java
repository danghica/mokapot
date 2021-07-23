package xyz.acygn.mokapot.benchmarksuite.programs.dijkstra.benchmarks;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.util.ArrayList;

import xyz.acygn.mokapot.CommunicationAddress;
import xyz.acygn.mokapot.DistributedCommunicator;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkData;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.Utilities;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable.ExampleType;
import xyz.acygn.mokapot.benchmarksuite.programs.dijkstra.DijkstraImpl;

/**
 * @author Kelsey McKenna
 */
public class DijkstraMokapotBenchmark extends DijkstraBenchmark {

	@Override
	public ExampleType getType() {
		return ExampleType.MOKAPOT;
	}

    @Override
    public int getNumberOfRequiredPeers() {
        return 1;
    }

    /**
     * Initialises a DijkstraImpl object remotely.
     */
    @Override
    public void distribute() throws IOException {
        dij = communicator.runRemotely(DijkstraImpl::new, remotes.get(0));
        
        assert Utilities.isStoredRemotely(dij) : "The computation class for Dijkstra was not created remotely";
    }

    @Override
    public void stop() throws IOException, NotBoundException {
        dij = null;
        System.gc();
        communicator.stopCommunication();
    }

    @Override
    public boolean requiresFix(BenchmarkPhase phase) {
        return phase == BenchmarkPhase.WHILE_DISTRIBUTION_TOP;
    }

    @Override
    public void fix(BenchmarkPhase phase) {
        if (phase == BenchmarkPhase.WHILE_DISTRIBUTION_TOP) {
            dij = null;
        }
    }

}
