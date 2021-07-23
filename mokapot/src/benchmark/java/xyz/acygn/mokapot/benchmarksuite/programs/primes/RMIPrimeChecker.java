package xyz.acygn.mokapot.benchmarksuite.programs.primes;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Scanner;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.BenchmarkProgram;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable.ExampleType;
import xyz.acygn.mokapot.rmiserver.RMIServer;

/**
 * Example program which repeatedly tests a given number for primality.
 * 
 * Note that it creates a {@link PrimeImpl} object remotely and runs code on
 * it using RMI.
 * 
 * @author Alexandra Paduraru
 *
 */
public class RMIPrimeChecker extends BenchmarkProgram {
	private Prime primeChecker;
	private static long n = 999983L;

	@Override
	public ExampleType getType() {
		return ExampleType.RMI;
	}

	/**
	 * Main method which reads a number, either from the command line or from
	 * the user and checks whether it is prime. A {@link PrimeImpl} object is
	 * created remotely using RMI.
	 * 
	 * @param args The number to be checked for primality.
	 * @throws NotBoundException 
	 * @throws IOException 
	 */
	public static void main(String args[]) throws NotBoundException, IOException {
		RMIPrimeChecker program = new RMIPrimeChecker();

		program.setup(0);
		program.distribute();
		program.getNumber(args);
		program.executeAlgorithm();
		program.stop();
	}

	@Override
	public int getNumberOfRequiredPeers() {
		return 1;
	}

	@Override
	public void distribute() throws IOException {
		primeChecker = servers.get(0).create(PrimeImpl::new);
	}

	@Override
	public void executeAlgorithm() throws IOException {
		primeChecker.isPrime(n);
	}

	@Override
	public void stop() throws IOException, NotBoundException {
		//not needed here
	}

	@Override
	public boolean requiresFix(BenchmarkPhase phase) {
		return false;
	}

	@Override
	public void fix(BenchmarkPhase phase) {
	}


	/**
	 * Read a number from the user or use the command line argument if available.
	 * @param args
	 * @throws IOException
	 */
	@SuppressWarnings("Duplicates")
	private void getNumber(String[] args) {
		if (args.length > 0) {
			n = Long.parseLong(args[0]);
		} else {
			System.out.println("Please input a number to be checked for primalityt: ");
			Scanner reader = new Scanner(System.in);
			n = reader.nextLong();

			reader.close();
		}
	}
}
