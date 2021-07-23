package xyz.acygn.mokapot.benchmarksuite.programs.student.benchmarks;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.util.ArrayList;

import xyz.acygn.mokapot.CommunicationAddress;
import xyz.acygn.mokapot.DistributedCommunicator;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkData;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.Utilities;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable.ExampleType;
import xyz.acygn.mokapot.benchmarksuite.programs.student.ModuleImpl;
import xyz.acygn.mokapot.benchmarksuite.programs.student.StudentImpl;

public class StudentMokapotBenchmark extends StudentBenchmark {
	@Override
	public ExampleType getType() {
		return ExampleType.MOKAPOT;
	}

    public int getNumberOfRequiredPeers() {
        return 1;
    }

    public void distribute() throws IOException {
        this.student = DistributedCommunicator.getCommunicator().runRemotely(StudentImpl::new, this.remotes.get(0));
        this.softwareWorkshop = DistributedCommunicator.getCommunicator()
                .runRemotely(() -> new ModuleImpl("Software Workshop", 1), this.remotes.get(0));
        this.functional = DistributedCommunicator.getCommunicator()
                .runRemotely(() -> new ModuleImpl("Functional Programming", 2), this.remotes.get(0));
        this.maths = DistributedCommunicator.getCommunicator()
                .runRemotely(() -> new ModuleImpl("Mathematical Techniques", 3), this.remotes.get(0));
        this.student.addModule(this.softwareWorkshop);
        this.student.addModule(this.functional);
        
        assert Utilities.isStoredRemotely(this.student);
    }

    public void stop() throws IOException, NotBoundException {
        this.student = null;
        this.softwareWorkshop = this.functional = this.maths = null;
        System.gc();
        this.communicator.runRemotely(System::gc, this.remotes.get(0));
        System.gc();
        this.communicator.runRemotely(System::gc, this.remotes.get(0));
        this.communicator.stopCommunication();
    }

    public boolean requiresFix(BenchmarkPhase phase) {
        return false;
    }

    public void fix(BenchmarkPhase phase) {
    }
}
