package xyz.acygn.mokapot.benchmarksuite.programs.uglynumber.benchmarks;

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
import xyz.acygn.mokapot.benchmarksuite.programs.uglynumber.CheckerImpl;

public class UglyNumberMokapotBenchmark extends UglyNumberBenchmark {
	@Override
	public ExampleType getType() {
		return ExampleType.MOKAPOT;
	}

    public int getNumberOfRequiredPeers() {
        return 1;
    }

    public void distribute() {
        this.checker = this.communicator.runRemotely(CheckerImpl::new, this.remotes.get(0));
        assert Utilities.isStoredRemotely(checker);
    }

    public void stop() {
        this.checker = null;
        this.communicator.stopCommunication();
    }

    public boolean requiresFix(BenchmarkPhase phase) {
        return false;
    }

    public void fix(BenchmarkPhase phase) {
    }
}
