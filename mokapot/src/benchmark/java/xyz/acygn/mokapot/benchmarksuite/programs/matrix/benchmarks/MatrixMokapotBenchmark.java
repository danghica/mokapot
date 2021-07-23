package xyz.acygn.mokapot.benchmarksuite.programs.matrix.benchmarks;

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
import xyz.acygn.mokapot.benchmarksuite.programs.matrix.MatrixImpl;

public class MatrixMokapotBenchmark extends MatrixBenchmark {
	@Override
	public ExampleType getType() {
		return ExampleType.MOKAPOT;
	}
	
    public int getNumberOfRequiredPeers() {
        return 1;
    }


    public void distribute() {
        this.matrixA = communicator.runRemotely(
                () -> new MatrixImpl(this.createRandomMatrix(size, seed1)), remotes.get(0));
        this.matrixB = communicator.runRemotely(
                () -> new MatrixImpl(this.createRandomMatrix(size, seed2)), remotes.get(0));
        
        assert Utilities.isStoredRemotely(this.matrixA);
        
        assert Utilities.isStoredRemotely(this.matrixB);

    }

    public void stop() {
        this.matrixA = null;
        this.matrixB = null;
        System.gc();
        communicator.runRemotely(System::gc, remotes.get(0));
        System.gc();
        communicator.runRemotely(System::gc, remotes.get(0));
        communicator.stopCommunication();
    }

    public boolean requiresFix(BenchmarkPhase phase) {
        return false;
    }

    public void fix(BenchmarkPhase phase) {
    }
}
