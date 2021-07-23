package xyz.acygn.mokapot.benchmarksuite.programs.student.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;

import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable.ExampleType;
import xyz.acygn.mokapot.benchmarksuite.programs.student.ModuleImpl;
import xyz.acygn.mokapot.benchmarksuite.programs.student.StudentImpl;
import xyz.acygn.mokapot.rmiserver.RMIServer;

public class StudentRMIBenchmark extends StudentBenchmark {
	@Override
	public ExampleType getType() {
		return ExampleType.RMI;
	}

    public int getNumberOfRequiredPeers() {
        return 1;
    }

    public void distribute() throws IOException {
        this.student = this.servers.get(0).create(StudentImpl::new);
        this.softwareWorkshop = this.servers.get(0).create(() -> new ModuleImpl("Software Workshop", 1));
        this.functional = this.servers.get(0).create(() -> new ModuleImpl("Functional Programming", 2));
        this.maths = this.servers.get(0).create(() -> new ModuleImpl("Mathematical Techniques", 3));
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
