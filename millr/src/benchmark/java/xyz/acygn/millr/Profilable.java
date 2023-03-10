package xyz.acygn.millr;

/**
 * An application conforming to the rules for profiling using MillrProfiler.
 *
 * @author Marcello De Bernardi
 */
interface Profilable {
    // provides all Profilable objects with a default iteration parameters
    int DEFAULT_ITERATIONS = 200;

    /**
     * Simplified interface to the Profilable's operations. The class
     * {@link MillrProfiler} could simply have called the main() method
     * of profilable classes directly, but this provides a simplified interface
     * allowing to pass an integer (rather than a String array).
     *
     * @param iterations number of iterations for the Profilable's specific tests
     */
    void run(int iterations);
}
