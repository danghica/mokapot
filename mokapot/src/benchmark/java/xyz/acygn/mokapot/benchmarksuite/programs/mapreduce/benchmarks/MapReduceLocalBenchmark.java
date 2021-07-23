package xyz.acygn.mokapot.benchmarksuite.programs.mapreduce.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.ArrayList;

import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.mapreduce.MasterServer;
import xyz.acygn.mokapot.benchmarksuite.programs.mapreduce.SlaveServerImpl;

public class MapReduceLocalBenchmark extends MapReduceBenchmark {

	@Override
	public ExampleType getType() {
		return ExampleType.LOCAL;
	}

	@Override
	public void distribute() throws IOException {
		if (webPages == null) {
			generateWebPages();
		}
		
		masterServer = new MasterServer();
		
		for (int i = 0; i < this.getNumberOfRequiredPeers(); i++) {
			masterServer.addSlaveServer(new SlaveServerImpl());
		}
		
		for (int i = 0; i < webPages.size(); i++) {
			masterServer.addWebPage(webPages.get(i));
		}
	}

}
