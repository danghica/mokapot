package xyz.acygn.millr.testclasses;

import xyz.acygn.millr.mokapotsemantics.StateTracker;
import xyz.acygn.millr.mokapotsemantics.TrackableSampleProgram;

import java.net.URLClassLoader;
import java.util.Random;

/**
 * Local implementation of the matrix transformation program interface.
 *
 * @author Marcello De Bernardi
 */
public class MatrixTransformationLocal extends TrackableSampleProgram {

    // program state has to be stored in instance variables for state
    // tracker to work
    private Random localRng;
    private Random remoteRng;
    private Object[][] matrix;
    private Object[][] remoteMatrix;


    /**
     * Constructor. Registers components of program state that are to be tracked
     * using the state tracker.
     */
    public MatrixTransformationLocal() throws Exception {
        super((URLClassLoader) MatrixTransformationLocal.class.getClassLoader(), false);
        stateTracker.register(this);
    }


    @Override
    public StateTracker run()  throws Exception{
        try {
            int matrixSize = 100;

            localRng = new Random(1);
            remoteRng = new Random(1);
        System.out.println("a");
            matrix = new Object[matrixSize][1];
            System.out.println("b");
            remoteMatrix = new Object[matrixSize][1];
        System.out.println("c");

           stateTracker.collectAll();


            // initialize matrix
            for (int row = 0; row < matrix.length; row++) {
                for (int col = 0; col < matrix[row].length; col++) {
                    matrix[row][col] = localRng.nextInt();
                }
            }
            stateTracker.collectAll();

            // initialize remoteMatrix
            for (int row = 0; row < matrix.length; row++) {
                for (int col = 0; col < matrix[row].length; col++) {
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
