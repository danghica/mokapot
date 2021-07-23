package xyz.acygn.mokapot.benchmarksuite.programs.validanagram.benchmarks;

import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.validanagram.CheckerImpl;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable.ExampleType;
import xyz.acygn.mokapot.benchmarksuite.programs.validanagram.Checker;
import xyz.acygn.mokapot.rmiserver.RMIServer;

public class ValidAnagramRMIBenchmark extends ValidAnagramBenchmark {
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
        super.distribute();
        this.firstChecker = new CheckerImpl();
        this.secondChecker = this.servers.get(0).create(CheckerImpl::new);
        this.firstChecker.pair(this.secondChecker);
        this.secondChecker.pair((Checker) UnicastRemoteObject.exportObject(this.firstChecker, 15939));
    }

    @Override
    public void stop() throws RemoteException {
        this.fix(BenchmarkPhase.POST_EXECUTION);
    }

    @Override
    public boolean requiresFix(BenchmarkPhase phase) {
        return phase == BenchmarkPhase.WHILE_EXECUTION_BOTTOM;
    }

    @Override
    public void fix(BenchmarkPhase phase) {
        switch (phase) {
            case WHILE_EXECUTION_BOTTOM:
            case POST_EXECUTION:
                try {
                    if (this.secondChecker != null) {
                        UnicastRemoteObject.unexportObject(this.secondChecker, true);
                    }
                } catch (NoSuchObjectException e) {
                }
            default:
        }
    }
}
