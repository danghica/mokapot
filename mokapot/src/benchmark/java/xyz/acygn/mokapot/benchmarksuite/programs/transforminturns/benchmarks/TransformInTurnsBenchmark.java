package xyz.acygn.mokapot.benchmarksuite.programs.transforminturns.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.Random;

import xyz.acygn.mokapot.benchmarksuite.programs.BenchmarkProgram;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable;
import xyz.acygn.mokapot.benchmarksuite.programs.transforminturns.Transformer;

/**
 * A benchmark in which a pair of {@link Transformer}s transform
 * a {@link String}. The local transformer transforms the {@link String},
 * then forwards the transformed {@link String} onto its partner.
 * This benchmark has a high number of network calls and minimum computation.
 *
 * @author Kelsey McKenna
 */
public abstract class TransformInTurnsBenchmark extends BenchmarkProgram {
    private static final int STRING_SIZE = 200;
    private static final int NUMBER_OF_TRANSFORMS = 100;
    String stringToTransform;
    Transformer localTransformer;
    Transformer remoteTransformer;

    public void distribute() throws IOException {
        this.stringToTransform = generateString(STRING_SIZE);
    }

    public void executeAlgorithm() throws IOException {
        String result = localTransformer.transform(stringToTransform, NUMBER_OF_TRANSFORMS);
    }

    private static String generateString(int length) {
        Random rng = new Random();
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < length; ++i) {
            builder.append((char) (rng.nextInt(26) + 97));
        }

        return builder.toString();
    }

    public static void main(String[] args) throws IllegalStateException, IOException, NotBoundException {
        TransformInTurnsBenchmark thisExample = new TransformInTurnsRMIBenchmark();
        thisExample.setup(thisExample instanceof TransformInTurnsMokapotBenchmark ? 15239 : 0);
        thisExample.distribute();
        thisExample.executeAlgorithm();
        thisExample.stop();
    }
}
