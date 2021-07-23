package xyz.acygn.mokapot.benchmarksuite.programs.mapreduce.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;

import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable.ExampleType;
import xyz.acygn.mokapot.benchmarksuite.programs.mapreduce.MasterServer;
import xyz.acygn.mokapot.benchmarksuite.programs.mapreduce.SlaveServer;
import xyz.acygn.mokapot.benchmarksuite.programs.mapreduce.SlaveServerImpl;
import xyz.acygn.mokapot.rmiserver.RMIServer;

public class MapReduceRMIBenchmark extends MapReduceBenchmark {
	@Override
	public ExampleType getType() {
		return ExampleType.RMI;
	}

	@Override
	public void distribute() throws IOException {
		if (webPages == null) {
			generateWebPages();
		}
		
		masterServer = new MasterServer();
		
		for (RMIServer rMIServer : servers) {
			SlaveServer s = rMIServer.create(SlaveServerImpl::new);
			masterServer.addSlaveServer(s);
		}
		
		for (int i = 0; i < webPages.size(); i++) {
			masterServer.addWebPage(webPages.get(i));
		}
		
	}

}
