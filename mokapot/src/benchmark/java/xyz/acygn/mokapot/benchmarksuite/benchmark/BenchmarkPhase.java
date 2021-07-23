package xyz.acygn.mokapot.benchmarksuite.benchmark;

import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable;

/**
 * <p>
 * Represents the current phase in the benchmarking process. The runner script
 * can use this enum to inform a {@link Benchmarkable} of the current
 * state of the benchmark.
 * </p><p>
 * The purpose of this enum is to aid in the process of "fixing" the {@link Benchmarkable}
 * example programs as they are being benchmarked. Several of the example programs
 * break when parts of them are executed in a loop; for example, exporting RMI
 * {@link java.rmi.Remote} objects multiple times results in an error, yet we want the
 * export to happen within the benchmarked section of code, at every iteration of the
 * benchmarking loop. Thus the solution is to unexport the object after each iteration,
 * so that at the next iteration it may be exported again without errors.
 * </p><p>
 * There are differences in structure within the example programs due to which the
 * "fixup" procedure cannot be generalized. The parts of the program that break when
 * looped vary, and the code to perform the required fixes cannot be embedded in the
 * methods whose performance we wish to benchmark. This enum provides a way for the
 * benchmarking script to communicate to the example program its current state, based
 * on which the example program can decide whether a fix is required. The benchmarking
 * script can then issue a call to the example program's fixing method, which has an
 * implementation specific to the example program in question.
 * </p>
 * @author Marcello De Bernardi 23/06/2017.
 */
public enum BenchmarkPhase {
    PRE_DISTRIBUTION,               // after setup() but before entering benchmarkable loop
    WHILE_DISTRIBUTION_TOP,         // at top of distribution loop
    WHILE_DISTRIBUTION_BOTTOM,      // at bottom of distribution loop
    POST_DISTRIBUTION,              // after distribution loop, before execution loop
    WHILE_EXECUTION_TOP,            // at top of execution loop
    WHILE_EXECUTION_BOTTOM,         // at bottom of execution loop
    POST_EXECUTION                  // after execution loop, before stop()
}
