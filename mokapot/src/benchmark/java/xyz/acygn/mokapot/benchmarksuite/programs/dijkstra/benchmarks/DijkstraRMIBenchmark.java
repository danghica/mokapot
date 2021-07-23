package xyz.acygn.mokapot.benchmarksuite.programs.dijkstra.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;

import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable.ExampleType;
import xyz.acygn.mokapot.benchmarksuite.programs.dijkstra.DijkstraImpl;
import xyz.acygn.mokapot.rmiserver.RMIServer;

/**
 * @author Kelsey McKenna
 */
public class DijkstraRMIBenchmark extends DijkstraBenchmark {
	@Override
	public ExampleType getType() {
		return ExampleType.RMI;
	}
	
    @Override
    public int getNumberOfRequiredPeers() {
        return 1;
    }

    @Override
    public void distribute() throws IOException {
        dij = servers.get(0).create(DijkstraImpl::new);
    }

    @Override
    public void stop() throws IOException, NotBoundException {
        // Nothing to do
    }

    @Override
    public boolean requiresFix(BenchmarkPhase phase) {
        return false;
    }

    @Override
    public void fix(BenchmarkPhase phase) {
        // Nothing to do
    }

}
