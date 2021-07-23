package xyz.acygn.mokapot.benchmarksuite.programs.matrix.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;

import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable.ExampleType;
import xyz.acygn.mokapot.benchmarksuite.programs.matrix.Matrix;
import xyz.acygn.mokapot.benchmarksuite.programs.matrix.MatrixImpl;
import xyz.acygn.mokapot.rmiserver.RMIServer;

public class MatrixRMIBenchmark extends MatrixBenchmark {
	@Override
	public ExampleType getType() {
		return ExampleType.RMI;
	}

    public int getNumberOfRequiredPeers() {
        return 1;
    }

    public void distribute() throws IOException {
        Matrix a = new MatrixImpl(this.createRandomMatrix(size, seed1));
        Matrix b = new MatrixImpl(this.createRandomMatrix(size, seed2));
        this.matrixA = servers.get(0).create(() -> a);
        this.matrixB = servers.get(0).create(() -> b);
    }

    public void stop() {
    }

    public boolean requiresFix(BenchmarkPhase phase) {
        return false;
    }

    public void fix(BenchmarkPhase phase) {
    }
}
