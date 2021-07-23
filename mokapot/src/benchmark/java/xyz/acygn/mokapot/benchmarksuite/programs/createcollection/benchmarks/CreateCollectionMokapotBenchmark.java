package xyz.acygn.mokapot.benchmarksuite.programs.createcollection.benchmarks;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.HashSet;

import xyz.acygn.mokapot.CommunicationAddress;
import xyz.acygn.mokapot.DistributedCommunicator;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkData;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.Utilities;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable.ExampleType;
import xyz.acygn.mokapot.benchmarksuite.programs.createcollection.ExampleObject;
import xyz.acygn.mokapot.benchmarksuite.programs.createcollection.GeneratorImpl;
import xyz.acygn.mokapot.benchmarksuite.programs.createcollection.ICollection;


import static xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase.WHILE_DISTRIBUTION_TOP;
import static xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase.WHILE_EXECUTION_BOTTOM;

/**
 * @author Kelsey McKenna
 */
public class CreateCollectionMokapotBenchmark extends CreateCollectionBenchmark {
	@Override
	public ExampleType getType() {
		return ExampleType.MOKAPOT;
	}

    @Override
    public int getNumberOfRequiredPeers() {
        return 1;
    }

    @Override
    public void distribute() {
        collections = new HashSet<>();
        generator = communicator.runRemotely(GeneratorImpl::new, remotes.get(0));
        assert Utilities.isStoredRemotely(generator);
    }

    @Override
    public void executeAlgorithm() throws IOException {
        for (int i = 0; i < NUMBER_OF_COLLECTIONS; i++) {
            ICollection<ExampleObject> remotelyGeneratedCollection
                    = generator.generate(SEED, COLLECTION_MAX_SIZE);

            assert Utilities.isStoredRemotely(remotelyGeneratedCollection);
            collections.add(remotelyGeneratedCollection);
        }
    }

    @Override
    public void stop() {
        makeCollectionsEligibleForGarbageCollection();
        collections = null;
        generator = null;
        
        communicator.runRemotely(() -> System.gc(), remotes.get(0));

        System.gc();
        System.runFinalization();
        
        communicator.stopCommunication();
    }

    @Override
    public boolean requiresFix(BenchmarkPhase phase) {
        return phase == WHILE_DISTRIBUTION_TOP || phase == WHILE_EXECUTION_BOTTOM;
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void fix(BenchmarkPhase phase) {
        switch (phase) {
            case WHILE_DISTRIBUTION_TOP:
                generator = null;
                collections = null;
                break;
            case WHILE_EXECUTION_BOTTOM:
                makeCollectionsEligibleForGarbageCollection();
                break;
        }
    }

    private void makeCollectionsEligibleForGarbageCollection() {
        for (ICollection<ExampleObject> collection : collections) {
            for (ExampleObject exampleObject : collection) {
                exampleObject = null;
            }
            try {
                collection.clear();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
            collection = null;
        }
        collections.clear();
        collections = null;
        collections = new HashSet<>();
    }

}
