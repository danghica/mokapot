package xyz.acygn.mokapot.benchmarksuite.programs.validanagram.benchmarks;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
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
import xyz.acygn.mokapot.benchmarksuite.programs.validanagram.CheckerImpl;

public class ValidAnagramMokapotBenchmark extends ValidAnagramBenchmark {
	@Override
	public ExampleType getType() {
		return ExampleType.MOKAPOT;
	}
	
    public int getNumberOfRequiredPeers() {
        return 1;
    }

    public void distribute() throws IOException {
        super.distribute();
        this.firstChecker = new CheckerImpl();
        this.secondChecker = this.communicator.runRemotely(CheckerImpl::new, this.remotes.get(0));
        
        assert Utilities.isStoredRemotely(secondChecker);

        this.firstChecker.pair(this.secondChecker);
        this.secondChecker.pair(this.firstChecker);
    }

    public void stop() throws RemoteException {
        this.firstChecker.unpair();
        this.secondChecker.unpair();
        this.firstChecker = null;
        this.secondChecker = null;
        this.communicator.stopCommunication();
    }

    public boolean requiresFix(BenchmarkPhase phase) {
        return phase == BenchmarkPhase.WHILE_DISTRIBUTION_TOP;
    }

    public void fix(BenchmarkPhase phase) {
        if (phase == BenchmarkPhase.WHILE_DISTRIBUTION_TOP && this.firstChecker != null && this.secondChecker != null) {
            try {
                this.firstChecker.unpair();
                this.secondChecker.unpair();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }

            this.firstChecker = null;
            this.secondChecker = null;
        }
    }
}
