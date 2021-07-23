package xyz.acygn.mokapot.benchmarksuite.programs.sort.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.List;

import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.sort.HelperImpl;
import xyz.acygn.mokapot.benchmarksuite.programs.sort.SorterImpl;

public class SortLocalBenchmark extends SortBenchmark {

	@Override
	public ExampleType getType() {
		return ExampleType.LOCAL;
	}

	@Override
	public void distribute() throws IOException {
		if (listToBeSorted == null) {
			listToBeSorted = this.generateListOfRandomNumbers();
		}
		
		s = new SorterImpl<Integer>();
		for (int i = 0; i < this.getNumberOfRequiredPeers(); i++) {
			s.addHelper(new HelperImpl());
		}
		s.addElements((List<Integer>)listToBeSorted.clone());
	}

	@Override
	public void stop() throws IOException, NotBoundException {
		super.stop();
	}

}
