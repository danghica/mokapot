package xyz.acygn.mokapot.benchmarksuite.programs.student.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;

import xyz.acygn.mokapot.benchmarksuite.programs.BenchmarkProgram;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable;
import xyz.acygn.mokapot.benchmarksuite.programs.student.Module;
import xyz.acygn.mokapot.benchmarksuite.programs.student.Student;
import xyz.acygn.mokapot.benchmarksuite.programs.student.ModuleImpl;
import xyz.acygn.mokapot.benchmarksuite.programs.student.StudentImpl;

/**
 * A benchmark in which a {@link StudentImpl}
 * and two {@link ModuleImpl}s are created remotely
 * and checks to see if the modules are assigned correctly to the student.
 */
public abstract class StudentBenchmark extends BenchmarkProgram {
    Student student;
    Module softwareWorkshop;
    Module functional;
    Module maths;

    public void executeAlgorithm() throws IOException {
        boolean result1 = student.hasModule(this.softwareWorkshop);
        boolean result2 = student.hasModule(this.functional);
        boolean result3 = student.hasModule(this.maths);
    }

    public static void main(String[] args) throws IllegalStateException, IOException, NotBoundException {
        StudentBenchmark thisExample = new StudentRMIBenchmark();
        thisExample.setup(thisExample instanceof StudentMokapotBenchmark ? 15239 : 0);
        thisExample.distribute();
        thisExample.executeAlgorithm();
        thisExample.stop();
    }
}
