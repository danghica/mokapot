package xyz.acygn.mokapot.benchmarksuite.programs.mapreduce.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.util.ArrayList;

import xyz.acygn.mokapot.CommunicationAddress;
import xyz.acygn.mokapot.DistributedCommunicator;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkData;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.BenchmarkingDebugMonitor;
import xyz.acygn.mokapot.benchmarksuite.programs.mapreduce.MasterServer;
import xyz.acygn.mokapot.benchmarksuite.programs.mapreduce.SlaveServer;
import xyz.acygn.mokapot.benchmarksuite.programs.mapreduce.SlaveServerImpl;

public class MapReduceMokapotBenchmark extends MapReduceBenchmark {
	@Override
	public ExampleType getType() {
		return ExampleType.MOKAPOT;
	}

	@Override
	public void distribute() throws IOException {
		if (webPages == null) {
			generateWebPages();
		}
		
		masterServer = new MasterServer();
		
		for (int i = 0; i < remotes.size(); i++) {
			masterServer.addSlaveServer(communicator.runRemotely(() -> new SlaveServerImpl(), remotes.get(i)));
		}
		
		for (int i = 0; i < webPages.size(); i++) {
			masterServer.addWebPage(webPages.get(i));
		}
		
	}
	
	@Override
	public void fix(BenchmarkPhase phase) {
		if (masterServer != null) {
			masterServer.ready();
		}
		System.gc();
		for (int i = 0; i < remotes.size(); i++) {
			communicator.runRemotely(() -> System.gc(),remotes.get(i));
			communicator.runRemotely(() -> System.runFinalization(),remotes.get(i));
		}
	}
	
	@Override
	public void executeAlgorithm() throws IOException {
		super.executeAlgorithm();
	}

	@Override
	public void stop() throws IOException, NotBoundException {
		super.stop();
		
		this.fix(null);
		masterServer = null;
		this.fix(null);
		communicator.stopCommunication();
		return;
	}

}
