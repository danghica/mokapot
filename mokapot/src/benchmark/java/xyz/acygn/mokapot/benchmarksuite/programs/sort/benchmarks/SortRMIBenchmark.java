package xyz.acygn.mokapot.benchmarksuite.programs.sort.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.List;

import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.sort.Helper;
import xyz.acygn.mokapot.benchmarksuite.programs.sort.HelperImpl;
import xyz.acygn.mokapot.benchmarksuite.programs.sort.SorterImpl;
import xyz.acygn.mokapot.rmiserver.RMIServer;

public class SortRMIBenchmark extends SortBenchmark {
	@Override
	public ExampleType getType() {
		return ExampleType.RMI;
	}

	@Override
	public void distribute() throws IOException {
		if (listToBeSorted == null) {
			listToBeSorted = this.generateListOfRandomNumbers();
		}
		
		s = new SorterImpl<Integer>();
		for (RMIServer server : servers) {
			Helper<Integer> h = server.create(HelperImpl::new);
			s.addHelper(h);
		}
		s.addElements((List<Integer>)listToBeSorted.clone());
	}

	@Override
	public void stop() throws IOException, NotBoundException {
		super.stop();
	}

}
