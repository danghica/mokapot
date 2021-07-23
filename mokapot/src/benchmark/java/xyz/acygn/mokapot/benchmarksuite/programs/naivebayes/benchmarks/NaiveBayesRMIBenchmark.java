package xyz.acygn.mokapot.benchmarksuite.programs.naivebayes.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;

import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable.ExampleType;
import xyz.acygn.mokapot.benchmarksuite.programs.naivebayes.GeneratorImpl;
import xyz.acygn.mokapot.benchmarksuite.programs.naivebayes.LearnerImpl;
import xyz.acygn.mokapot.rmiserver.RMIServer;

public class NaiveBayesRMIBenchmark extends NaiveBayesBenchmark {
	@Override
	public ExampleType getType() {
		return ExampleType.RMI;
	}

    public int getNumberOfRequiredPeers() {
        return 1;
    }

    public void distribute() throws RemoteException {
        LearnerImpl localLearner = new LearnerImpl(new GeneratorImpl().generate(seed, sampleSize, numberOfFeatures));
        this.learner = servers.get(0).create(() -> localLearner);
    }

    public void stop() throws IOException, NotBoundException {
    }

    public boolean requiresFix(BenchmarkPhase phase) {
        return false;
    }

    public void fix(BenchmarkPhase phase) {
    }
}
