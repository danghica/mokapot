package xyz.acygn.mokapot.benchmarksuite.programs.naivebayes.benchmarks;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Set;

import xyz.acygn.mokapot.CommunicationAddress;
import xyz.acygn.mokapot.DistributedCommunicator;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkData;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.Utilities;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable.ExampleType;
import xyz.acygn.mokapot.benchmarksuite.programs.naivebayes.Email;
import xyz.acygn.mokapot.benchmarksuite.programs.naivebayes.GeneratorImpl;
import xyz.acygn.mokapot.benchmarksuite.programs.naivebayes.LearnerImpl;

public class NaiveBayesMokapotBenchmark extends NaiveBayesBenchmark {
	@Override
	public ExampleType getType() {
		return ExampleType.MOKAPOT;
	}

    public int getNumberOfRequiredPeers() {
        return 1;
    }

    public void distribute() throws RemoteException {
        this.learner = communicator.runRemotely(() -> {
            Set<Email> trainingData = new GeneratorImpl().generate(seed, sampleSize, numberOfFeatures);
            return new LearnerImpl(trainingData);
        }, remotes.get(0));
        
        assert Utilities.isStoredRemotely(this.learner);
    }

    public void stop() throws IOException, NotBoundException {
        learner = null;

        this.fix(BenchmarkPhase.POST_EXECUTION);
        communicator.stopCommunication();
    }

    public boolean requiresFix(BenchmarkPhase phase) {
        return phase == BenchmarkPhase.WHILE_EXECUTION_TOP;
    }

    @Override
    public void fix(BenchmarkPhase phase) {
        switch (phase) {
            case WHILE_EXECUTION_TOP:
            case POST_EXECUTION:
                this.garbageCollect();
                break;
        }
    }

    private void garbageCollect() {
        System.gc();
        communicator.runRemotely(System::gc, remotes.get(0));
        System.gc();
        communicator.runRemotely(System::gc, remotes.get(0));
    }
}
