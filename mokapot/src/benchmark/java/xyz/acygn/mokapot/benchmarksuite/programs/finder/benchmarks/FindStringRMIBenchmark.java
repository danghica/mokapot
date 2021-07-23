package xyz.acygn.mokapot.benchmarksuite.programs.finder.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;

import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable.ExampleType;
import xyz.acygn.mokapot.benchmarksuite.programs.finder.StringDataGeneratorImpl;
import xyz.acygn.mokapot.benchmarksuite.programs.finder.StringFinderImpl;
import xyz.acygn.mokapot.rmiserver.RMIServer;

/**
 * @author Kelsey McKenna
 */
public class FindStringRMIBenchmark extends FindStringBenchmark {
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
        generator = servers.get(0).create(StringDataGeneratorImpl::new);

        words = generator.generateRandomWordList(listSize);
        wordToFind = generator.generateRandomWord(10);

        finder = servers.get(0).create(StringFinderImpl::new);
    }

    @Override
    public void stop() throws IOException, NotBoundException {
        //not needed here
    }

    @Override
    public boolean requiresFix(BenchmarkPhase phase) {
        return false;
    }

    @Override
    public void fix(BenchmarkPhase phase) {
    }

}
