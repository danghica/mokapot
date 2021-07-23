package xyz.acygn.mokapot.benchmarksuite.programs.student.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.student.ModuleImpl;
import xyz.acygn.mokapot.benchmarksuite.programs.student.StudentImpl;

public class StudentLocalBenchmark extends StudentBenchmark {
    public StudentLocalBenchmark() {
    }

    public ExampleType getType() {
        return ExampleType.LOCAL;
    }

    public int getNumberOfRequiredPeers() {
        return 0;
    }

    public void distribute() throws IOException {
        this.student = new StudentImpl();
        this.softwareWorkshop = new ModuleImpl("Software Workshop", 1);
        this.functional = new ModuleImpl("Functional Programming", 2);
        this.maths = new ModuleImpl("Mathematical Techniques", 3);
        this.student.addModule(this.softwareWorkshop);
        this.student.addModule(this.functional);
    }

    public void stop() throws IOException, NotBoundException {
    }

    public boolean requiresFix(BenchmarkPhase phase) {
        return false;
    }

    public void fix(BenchmarkPhase phase) {
    }
}
