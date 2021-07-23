package xyz.acygn.mokapot.benchmarksuite.programs.sort.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.List;

import xyz.acygn.mokapot.CommunicationAddress;
import xyz.acygn.mokapot.DistributedCommunicator;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkData;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.sort.HelperImpl;
import xyz.acygn.mokapot.benchmarksuite.programs.sort.SorterImpl;

public class SortMokapotBenchmark extends SortBenchmark {
	@Override
	public ExampleType getType() {
		// TODO Auto-generated method stub
		return ExampleType.MOKAPOT;
	}

	@Override
	public void distribute() throws IOException {
		if (listToBeSorted == null) {
			listToBeSorted = this.generateListOfRandomNumbers();
		}
		
		s = new SorterImpl<Integer>();

		for (int i = 0; i < remotes.size(); i++) {
			s.addHelper(DistributedCommunicator.getCommunicator().runRemotely(() -> new HelperImpl<Integer>(),
					remotes.get(i)));
		}
		s.addElements((List<Integer>)listToBeSorted.clone());
	}
	
	@Override
	public void fix(BenchmarkPhase phase) {
		super.fix(phase);
//		
//		for (int i = 0; i < remotes.size(); i++) {
//			communicator.runRemotely(() -> System.gc(),remotes.get(i));
//			communicator.runRemotely(() -> System.runFinalization(),remotes.get(i));
//		}
	}

	@Override
	public void stop() throws IOException, NotBoundException {
		super.stop();
		
		for (int i = 0; i < remotes.size(); i++) {
				communicator.runRemotely(() -> System.gc(),remotes.get(i));
				communicator.runRemotely(() -> System.runFinalization(),remotes.get(i));
		}
		
		communicator.stopCommunication();
	}

}
