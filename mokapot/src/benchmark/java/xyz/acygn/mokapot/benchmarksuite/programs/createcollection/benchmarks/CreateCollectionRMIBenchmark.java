package xyz.acygn.mokapot.benchmarksuite.programs.createcollection.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.HashSet;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable.ExampleType;
import xyz.acygn.mokapot.benchmarksuite.programs.createcollection.GeneratorImpl;
import xyz.acygn.mokapot.rmiserver.RMIServer;

/**
 * @author Kelsey McKenna
 */
public class CreateCollectionRMIBenchmark extends CreateCollectionBenchmark {

	@Override
	public ExampleType getType() {
		return ExampleType.RMI;
	}

    @Override
    public int getNumberOfRequiredPeers() {
        return 1;
    }

    @Override
    public void distribute() throws RemoteException {
        collections = new HashSet<>();
        generator = servers.get(0).create(GeneratorImpl::new);
    }

    @Override
    public void executeAlgorithm() throws RemoteException {
        for (int i = 0; i < NUMBER_OF_COLLECTIONS; i++) {
            collections.add(generator.generate(SEED, COLLECTION_MAX_SIZE));
        }
    }

    @Override
    public void stop() throws IOException, NotBoundException {
        // no termination procedure required
    }

    @Override
    public boolean requiresFix(BenchmarkPhase phase) {
        // no fixing required
        return false;
    }

    @Override
    public void fix(BenchmarkPhase phase) {
        // no fixing required
    }

}
