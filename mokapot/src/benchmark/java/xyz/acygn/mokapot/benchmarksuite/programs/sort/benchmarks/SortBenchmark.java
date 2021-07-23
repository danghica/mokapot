package xyz.acygn.mokapot.benchmarksuite.programs.sort.benchmarks;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.Random;

import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.BenchmarkProgram;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable;
import xyz.acygn.mokapot.benchmarksuite.programs.sort.SorterImpl;

/**
 * A benchmark where a distributed sorting algorithm is used to sort an array of
 * Integers.
 */
public abstract class SortBenchmark extends BenchmarkProgram {
	SorterImpl<Integer> s;
	//You can modify the length.
	final int LIST_LENGTH = 50;
	final int SEED = 997;
	ArrayList<Integer> listToBeSorted;
	
	/*The benchmarking program will only give as many servers as it has.
	 * This benchmark can use any number of servers.
	 * (non-Javadoc)
	 * @see xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable#getNumberOfRequiredPeers()
	 */
	@Override
	public int getNumberOfRequiredPeers() {
		return 5;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void executeAlgorithm() throws IOException {
		s.sort();
		s.ready();
	}

	@Override
	public boolean requiresFix(BenchmarkPhase phase) {
		if (phase == BenchmarkPhase.WHILE_EXECUTION_TOP) {
			return true;
		}
		return false;
	}

	@Override
	public void fix(BenchmarkPhase phase) {
		try {
			this.distribute();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.gc();
	}
	
	@Override
	public void stop() throws IOException, NotBoundException {
		if (s.getElements().size() != LIST_LENGTH) {
			System.out.println(s.getElements().size());
			System.out.println("There's something wrong with the sort benchmark.");
		}
		
		for (int i = 1; i < s.getElements().size(); i++) {
			if (s.getElements().get(i) < s.getElements().get(i - 1)) {
				System.out.println("There's something wrong with the sort benchmark.");
			}
		}
		
		s.clearElements();
		s = null;
		System.gc();
	}

	protected ArrayList<Integer> generateListOfRandomNumbers() {
		Random r = new Random(SEED);
		ArrayList<Integer> result = new ArrayList<>();

		for (int i = 0; i < LIST_LENGTH; i++) {
			result.add(r.nextInt());
		}

		return result;
	}
	
	public static void main(String[] args) throws IOException, NotBoundException {
        SortMokapotBenchmark program = new SortMokapotBenchmark();
        program.setup(15237);
        program.distribute();
        program.executeAlgorithm();
        System.out.println(program.s.getElements());
        program.stop();
    }
}
