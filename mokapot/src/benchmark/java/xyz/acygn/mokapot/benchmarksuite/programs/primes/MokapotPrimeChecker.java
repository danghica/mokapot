package xyz.acygn.mokapot.benchmarksuite.programs.primes;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Scanner;

import xyz.acygn.mokapot.CommunicationAddress;
import xyz.acygn.mokapot.DistributedCommunicator;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkData;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.BenchmarkProgram;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable;
import xyz.acygn.mokapot.benchmarksuite.programs.Utilities;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable.ExampleType;

/**
 * Example program which tests a given number for primality.
 * 
 * Note that it creates a {@link PrimeImpl} object remotely and runs code on it
 * using Mokapot.
 * 
 * @author Alexandra Paduraru
 *
 */
public class MokapotPrimeChecker extends BenchmarkProgram {
	private long n = 999983L;
    private Prime primeChecker;

	@Override
	public ExampleType getType() {
		return ExampleType.MOKAPOT;
	}

	/**
	 * Main method which reads a number, either from the command line or from
	 * the user and checks whether it is prime. A {@link PrimeImpl} object is
	 * created remotely using Mokapot.
	 * 
	 * @param args
	 *            The number to be checked for primality.
	 * @throws NotBoundException 
	 */
	public static void main(String[] args) throws IllegalStateException, IOException, NotBoundException {
		MokapotPrimeChecker thisExample = new MokapotPrimeChecker();

		thisExample.setup(15239);
		thisExample.distribute();
		thisExample.executeAlgorithm();
		thisExample.stop();
	}

	@Override
	public int getNumberOfRequiredPeers() {
		return 1;
	}

	@Override
	public void distribute() throws IOException {
		// initializes a PrimeImpl object
		primeChecker = DistributedCommunicator.getCommunicator().runRemotely(PrimeImpl::new, remotes.get(0));
		assert Utilities.isStoredRemotely(primeChecker);
	}

	@Override
	public void executeAlgorithm() throws IOException {
		// runs the primality check remotely
		primeChecker.isPrime(n);
	}

	@Override
	public void stop() throws IOException, NotBoundException {
		primeChecker = null;

		System.gc();
		communicator.runRemotely(() -> { System.gc(); return true; }, remotes.get(0));
		communicator.stopCommunication();
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
	private void getNumber(String[] args) {
		if (args.length > 0) {
			n = Long.parseLong(args[0]);
		} else {
			System.out.println("Please input a number to be checked for primality: ");
			Scanner reader = new Scanner(System.in);
			n = reader.nextLong();

			reader.close();
		}
	}
}
