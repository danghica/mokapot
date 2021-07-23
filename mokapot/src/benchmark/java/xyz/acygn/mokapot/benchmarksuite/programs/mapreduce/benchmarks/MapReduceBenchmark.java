package xyz.acygn.mokapot.benchmarksuite.programs.mapreduce.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.BenchmarkProgram;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable;
import xyz.acygn.mokapot.benchmarksuite.programs.mapreduce.MasterServer;
import xyz.acygn.mokapot.benchmarksuite.programs.mapreduce.WebPage;
import xyz.acygn.mokapot.benchmarksuite.programs.mapreduce.WebPageGenerator;

public abstract class MapReduceBenchmark extends BenchmarkProgram {
	final int SEED = 1998;
	final int MAX_LINK_SIZE = 3;
	final int MAX_CONTENT_SIZE = 15;
	final int MAX_WORD_LENGTH = 5;
	final int NUMBER_OF_PAGES = 10;
	final int NUMBER_OF_QUERIES = 100;
	ArrayList<WebPage> webPages;
	WebPageGenerator generator;
	MasterServer masterServer;
	
	/*The benchmarking program will only give as many servers as it has.
	 * This benchmark can use any number of servers.
	 */ 
	@Override
	public int getNumberOfRequiredPeers() {
		return 5;
	}
	
	protected void generateWebPages() {
		webPages = new ArrayList<WebPage>();
		generator = new WebPageGenerator(SEED);
		for (int i = 0; i < NUMBER_OF_PAGES; i++) {
			webPages.add(generator.generateRandomWebpage(MAX_LINK_SIZE, MAX_CONTENT_SIZE, MAX_WORD_LENGTH));
		}
	}

	@Override
	public void executeAlgorithm() throws IOException {
		for (int i = 0; i < NUMBER_OF_QUERIES; i++) {
			masterServer.getLinksOfPagesWith(generator.generateWord(MAX_WORD_LENGTH));
		}
	}

	@Override
	public boolean requiresFix(BenchmarkPhase phase) {
		if (phase == BenchmarkPhase.POST_DISTRIBUTION) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void fix(BenchmarkPhase phase) {
		masterServer.ready();
		System.gc();
	}
	
	@Override
	public void stop() throws IOException, NotBoundException {
		masterServer.clear();
		masterServer.ready();
	}
}
