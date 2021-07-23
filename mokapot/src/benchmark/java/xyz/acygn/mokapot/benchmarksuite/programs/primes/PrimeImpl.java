package xyz.acygn.mokapot.benchmarksuite.programs.primes;

/**
 * Simple example program which contains a method that checks whether a given number is prime.
 * @author Alexandra Paduraru
 *
 */
public class PrimeImpl implements Prime {

	/**
	 * Checks to see if a given number is prime.
	 * @param n The number to be checked for primality.
	 * @return True if the number is prime and false otherwise.
	 */
    public boolean isPrime(long n) {
        if (n < 2) {
            return false;
        }

        for (int i = 2; i <= Math.sqrt(n); i++) {
            if (n % i == 0) {
                return false;
            }
        }

		return true;
	}

}
