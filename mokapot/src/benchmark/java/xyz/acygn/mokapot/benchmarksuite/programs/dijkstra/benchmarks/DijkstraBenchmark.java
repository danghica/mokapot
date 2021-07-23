package xyz.acygn.mokapot.benchmarksuite.programs.dijkstra.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.List;

import xyz.acygn.mokapot.benchmarksuite.programs.BenchmarkProgram;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable;
import xyz.acygn.mokapot.benchmarksuite.programs.Utilities;
import xyz.acygn.mokapot.benchmarksuite.programs.dijkstra.Dijkstra;
import xyz.acygn.mokapot.benchmarksuite.programs.dijkstra.Graph;

/**
 * A benchmark in which a remote graph object is created and then Dijkstra's
 * algorithm is run remotely to find shortest distances from the start node to
 * every other node.
 *
 * @author Kelsey McKenna
 */
public abstract class DijkstraBenchmark extends BenchmarkProgram {

    static final int numNodes = 100;
    Dijkstra dij;
    Graph graph;

    @Override
    public void executeAlgorithm() throws IOException {
        graph = dij.generateRandomGraph(numNodes);
        assert !(this instanceof DijkstraMokapotBenchmark) || Utilities.isStoredRemotely(graph);

        final int startNode = 0;

        List<Integer> dist = dij.shortestDist(graph, startNode);
        assert !(this instanceof DijkstraMokapotBenchmark) || Utilities.isStoredRemotely(dist);

        graph.clearWeights();
        graph = null;
        dist.clear();
        dist = null;
    }

    public static void main(String[] args) throws IOException, NotBoundException {
        DijkstraBenchmark benchmark = new DijkstraRMIBenchmark();

        benchmark.setup(benchmark instanceof DijkstraMokapotBenchmark ? 15239 : 0);
        benchmark.distribute();
        benchmark.executeAlgorithm();
        benchmark.stop();
    }

}
