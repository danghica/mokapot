package xyz.acygn.mokapot.benchmarksuite.programs.primes.selftest;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;

import xyz.acygn.mokapot.CommunicationAddress;
import xyz.acygn.mokapot.DistributedCommunicator;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkData;
import xyz.acygn.mokapot.benchmarksuite.programs.primes.Prime;
import xyz.acygn.mokapot.benchmarksuite.programs.primes.PrimeImpl;

/**
 * @author Kelsey McKenna
 */
public class PrimeCheckerTest {

    public void correctLocalAnswer() throws RemoteException {
        final Prime unit = new PrimeImpl();
        correctAnswer(unit);
    }

    public void correctMokapotAnswer() throws IOException {
        DistributedCommunicator communicator;
		try {
			communicator = new DistributedCommunicator(BenchmarkData.keyLocation, BenchmarkData.getPassword().toCharArray());
		} catch (KeyManagementException | KeyStoreException e) {
			throw new RuntimeException(e);
		}

        communicator.startCommunication();

        final CommunicationAddress remoteAddress
                = communicator.lookupAddress(
                        InetAddress.getLoopbackAddress(), 15238);

        final PrimeImpl unit = DistributedCommunicator.getCommunicator()
                .runRemotely(PrimeImpl::new, remoteAddress);

        correctAnswer(unit);

        DistributedCommunicator.getCommunicator().stopCommunication();
    }

    private void correctAnswer(final Prime prime) throws RemoteException {
        assert !(prime.isPrime(0L));
        assert !(prime.isPrime(1L));
        assert (prime.isPrime(2L));
        assert (prime.isPrime(3L));
        assert !(prime.isPrime(4L));
        assert (prime.isPrime(5L));
        assert !(prime.isPrime(6L));
        assert (prime.isPrime(7L));
        assert !(prime.isPrime(8L));
        assert !(prime.isPrime(9L));
    }
}
