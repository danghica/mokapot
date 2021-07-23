package xyz.acygn.mokapot.benchmarksuite.programs.transforminturns.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable.ExampleType;
import xyz.acygn.mokapot.benchmarksuite.programs.transforminturns.Transformer;
import xyz.acygn.mokapot.benchmarksuite.programs.transforminturns.TransformerImpl;
import xyz.acygn.mokapot.rmiserver.RMIServer;

public class TransformInTurnsRMIBenchmark extends TransformInTurnsBenchmark {
	@Override
	public ExampleType getType() {
		return ExampleType.RMI;
	}

    public int getNumberOfRequiredPeers() {
        return 1;
    }

    public void distribute() throws IOException {
        super.distribute();
        this.localTransformer = new TransformerImpl();
        this.remoteTransformer = this.servers.get(0).create(TransformerImpl::new);
        this.localTransformer.pair(this.remoteTransformer);
        this.remoteTransformer.pair((Transformer) UnicastRemoteObject.exportObject(this.localTransformer, 15939));
    }

    public void stop() throws IOException, NotBoundException {
    }

    public boolean requiresFix(BenchmarkPhase phase) {
        return false;
    }

    public void fix(BenchmarkPhase phase) {
    }
}
