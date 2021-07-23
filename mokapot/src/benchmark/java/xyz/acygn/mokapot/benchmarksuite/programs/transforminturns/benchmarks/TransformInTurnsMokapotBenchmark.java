package xyz.acygn.mokapot.benchmarksuite.programs.transforminturns.benchmarks;

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
import xyz.acygn.mokapot.benchmarksuite.programs.transforminturns.TransformerImpl;

public class TransformInTurnsMokapotBenchmark extends TransformInTurnsBenchmark {
	@Override
	public ExampleType getType() {
		return ExampleType.MOKAPOT;
	}

    public int getNumberOfRequiredPeers() {
        return 1;
    }

    public void distribute() throws IOException {
        super.distribute();
        this.localTransformer = new TransformerImpl();
        this.remoteTransformer = this.communicator.runRemotely(TransformerImpl::new, this.remotes.get(0));
        
        assert Utilities.isStoredRemotely(this.remoteTransformer);

        this.localTransformer.pair(this.remoteTransformer);
        this.remoteTransformer.pair(this.localTransformer);
    }

    public void stop() throws IOException, NotBoundException {
        this.localTransformer.unpair();
        this.remoteTransformer.unpair();
        this.localTransformer = null;
        this.remoteTransformer = null;
        this.stringToTransform = null;
        this.communicator.stopCommunication();
    }

    public boolean requiresFix(BenchmarkPhase phase) {
        return phase == BenchmarkPhase.WHILE_DISTRIBUTION_TOP;
    }

    public void fix(BenchmarkPhase phase) {
        if (phase == BenchmarkPhase.WHILE_DISTRIBUTION_TOP && this.localTransformer != null && this.remoteTransformer != null) {
            try {
                this.localTransformer.unpair();
                this.remoteTransformer.unpair();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }

            this.stringToTransform = null;
            this.localTransformer = null;
            this.remoteTransformer = null;
        }

    }
}
