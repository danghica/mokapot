package xyz.acygn.millr.testclasses;

import xyz.acygn.millr.mokapotsemantics.IsMokapotVersion;
import xyz.acygn.millr.mokapotsemantics.StateTracker;
import xyz.acygn.millr.mokapotsemantics.TrackableSampleProgram;
import xyz.acygn.millr.util.ObjectArrayWrapper;
import xyz.acygn.mokapot.CommunicationAddress;
import xyz.acygn.mokapot.DistributedCommunicator;
import xyz.acygn.mokapot.skeletons.ProxyOrWrapper;
import xyz.acygn.millr.util.intArrayWrapper;

import javax.swing.plaf.DimensionUIResource;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URLClassLoader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.util.Random;

/**
 * Mokapot version of the matrix transformation program interface.
 *
 * @author Marcello De Bernardi
 */
public class MatrixTransformationMokapot extends TrackableSampleProgram implements IsMokapotVersion {
    private DistributedCommunicator communicator;
    private CommunicationAddress remote;
    // program state, has to be implemented using instance variables
    // because only instance variables can be registered with state tracker
    private Random localRng;
    private Random remoteRng;
    private int[][] matrix;
    private int[][] remoteMatrix;


    /**
     * Constructor. Registers components of program state that need to be tracked,
     * and performs mokapot setup.
     *
     * @throws IOException in a number of mokapot-related cases
     */
    public MatrixTransformationMokapot(DistributedCommunicator distributedCommunicator, CommunicationAddress communicationAddress) throws NoSuchFieldException, NoSuchMethodException, IOException, ClassNotFoundException {
        super((URLClassLoader) MatrixTransformationMokapot.class.getClassLoader(), true);
        // setup state tracking
        stateTracker.register(this);
        this.communicator = distributedCommunicator;
        this.remote =communicationAddress;


    }


    @Override
    public StateTracker run() throws Exception{
        try {
            int matrixSize = 100;
            System.out.println("a");
            localRng = new Random(1);
            System.out.println("b");
            remoteRng = (Random) communicator.runRemotely(() -> new Random(1), remote);
            System.out.println("c");

            matrix = new int[matrixSize][1];
            System.out.println(matrix.length);
            System.out.println("d");

            remoteMatrix= communicator.runRemotely(()-> { return new int[matrixSize][1];}, remote);
            System.out.println(remoteMatrix.length);
            stateTracker.collectAll();


            // initialize matrix
            for (int row = 0; row < matrix.length; row++) {
                for (int col = 0; col < matrix[row].length; col++) {
                    matrix[row][col] = localRng.nextInt();
                }
            }
            stateTracker.collectAll();

            // initialize remoteMatrix
            for (int row = 0; row < remoteMatrix.length; row++) {
                for (int col = 0; col < remoteMatrix[row].length; col++) {
                    remoteMatrix[row][col] = remoteRng.nextInt();
                }
            }
            stateTracker.collectAll();

            return stateTracker;
        }
        catch (IllegalAccessException e) {
            // this should not be happening
            e.printStackTrace();
            return null;
        }
    }
}