package xyz.acygn.mokapot.benchmarksuite.programs.matrix.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Random;

import xyz.acygn.mokapot.benchmarksuite.programs.BenchmarkProgram;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable;
import xyz.acygn.mokapot.benchmarksuite.programs.matrix.Matrix;

/**
 * A benchmark in which one matrix is added to another.
 * Both matrices are remote to the system running the benchmark,
 * but they are on the same machine.
 *
 * @author Kelsey McKenna
 */
public abstract class MatrixBenchmark extends BenchmarkProgram {

    static final long seed1 = 12576L;
    static final long seed2 = 14672L;
    static final int size = 100;
    Matrix matrixA;
    Matrix matrixB;

    public MatrixBenchmark() {
    }

    public void executeAlgorithm() throws RemoteException {
        matrixA.add(matrixB);
    }

    int[][] createRandomMatrix(int size, long seed) {
        int[][] a = new int[size][size];
        Random gen = new Random(seed);

        for (int i = 0; i < size; ++i) {
            for (int j = 0; j < size; ++j) {
                a[i][j] = gen.nextInt(10);
            }
        }

        return a;
    }

    public static void main(String[] args) throws IOException, NotBoundException {
        MatrixBenchmark thisExample = new MatrixMokapotBenchmark();
        thisExample.setup(thisExample instanceof MatrixMokapotBenchmark ? 15239 : 0);
        thisExample.distribute();
        thisExample.executeAlgorithm();
        thisExample.stop();
    }

}
