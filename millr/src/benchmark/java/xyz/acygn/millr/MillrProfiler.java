package xyz.acygn.millr;

/**
 * A quality-of-life-improvement class for running all the profileable benchmark
 * classes. Each of the different classes that are of interest for profiling, such
 * as {@link ArrayInitialization} and {@link ArraySorting}, each contain code for
 * carrying out a particular type of operation that is affected by millr. The main
 * idea is to profile the execution of that code directly, as well as to profile the
 * execution of the milled version of the code. All the classes containing
 * profilable code follow three principles.
 *
 * The first is to allow the user to easily run any single one of the classes in
 * isolation, which entails providing a main() method.
 *
 * The second is to allow all of the classes to be executed sequentially (in a batch).
 * This is the purpose of {@link MillrProfiler}, which contains code for executing all
 * the profilable classes; the profilable classes all implement the interface
 * {@link Profilable}, not out of necessity but for convenience (provides a simplified
 * interface to each class's main() method as well as some default settings).
 *
 * Finally, the profilable code of each of the profilable classes is split into inner
 * classes implementing the different implementations. This is to ensure that the
 * different operations to profile are split into different .class files, which helps
 * in case millr for some reason breaks while milling any particular operation. Thus
 * this helps pinpoint any bugs in millr.
 *
 * @author Marcello De Bernardi
 */
class MillrProfiler {
    private static final int PROFILER_STARTUP_WAIT_TIME = 10000;
    private static final int OPERATION_ITERATIONS = 5000;


    /**
     * Run all the profilable applications.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        sleep();

        Profilable arrayInitializations = new ArrayInitialization();
        Profilable arraySorting = new ArraySorting();
        Profilable wrapperInitializations = new WrapperInitializations();
        Profilable customWrapperInitializations = new CustomWrapperInitializations();

        arrayInitializations.run(OPERATION_ITERATIONS);
        arraySorting.run(OPERATION_ITERATIONS);
        wrapperInitializations.run(OPERATION_ITERATIONS);
        customWrapperInitializations.run(OPERATION_ITERATIONS);
    }


    /**
     * Put the thread to sleep for a pre-determined amount of time.
     */
    private static void sleep() {
        try {
            System.out.println("Sleeping to wait for profiler ...");
            Thread.sleep(PROFILER_STARTUP_WAIT_TIME);
            System.out.println("Starting ...");
        }
        catch (InterruptedException e) {
            System.err.println("Interrupted while waiting for profiler");
            System.exit(1);
        }
    }
}
