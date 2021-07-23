package xyz.acygn.mokapot.benchmarksuite.programs.finder.benchmarks;

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
import xyz.acygn.mokapot.benchmarksuite.programs.finder.StringDataGeneratorImpl;
import xyz.acygn.mokapot.benchmarksuite.programs.finder.StringFinderImpl;

/**
 * @author Kelsey McKenna
 */
public class FindStringMokapotBenchmark extends FindStringBenchmark {
	@Override
	public ExampleType getType() {
		return ExampleType.MOKAPOT;
	}

    @Override
    public int getNumberOfRequiredPeers() {
        return 1;
    }

    @Override
    public void distribute() throws IOException {
        generator = DistributedCommunicator.getCommunicator().runRemotely(StringDataGeneratorImpl::new, remotes.get(0));
        assert Utilities.isStoredRemotely(generator);

        words = generator.generateRandomWordList(listSize);
        assert Utilities.isStoredRemotely(words);
        wordToFind = generator.generateRandomWord(10);

        finder = DistributedCommunicator.getCommunicator().runRemotely(StringFinderImpl::new, remotes.get(0));
        assert Utilities.isStoredRemotely(finder);
    }

    @Override
    public void stop() throws IOException, NotBoundException {
        finder = null;
        generator = null;
        words = null;

        System.gc();
        communicator.runRemotely(System::gc, remotes.get(0));
        System.gc();
        communicator.runRemotely(System::gc, remotes.get(0));

        communicator.stopCommunication();
    }

    @Override
    public boolean requiresFix(BenchmarkPhase phase) {
        return false;
    }

    @Override
    public void fix(BenchmarkPhase phase) {
    }

}
