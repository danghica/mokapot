package xyz.acygn.mokapot.benchmarksuite.programs.dijkstra;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Utility class that implements DijkstraImpl's algorithm.
 * @author Alexandra Paduraru
 *
 */
public class DijkstraImpl implements Dijkstra {

	public static final long seed = 1547;


    @Override
    public List<Integer> shortestDist(Graph graph, int s) {
        final int[][] weights = graph.getWeights();

        int n = weights.length;
        int[] d = new int[n];
        boolean[] tight = new boolean[n];

		for (int i = 0; i < n; i++) {
			d[i] = Integer.MAX_VALUE;
			tight[i] = false;
		}

		d[s] = 0;

		for (int node = 0; node < n - 1; node++) {
			int u = findMin(tight, d);
			tight[u] = true;

			for (int j = 0; j < n; j++)
                if (weights[u][j] != Integer.MAX_VALUE && d[u] != Integer.MAX_VALUE && d[u] + weights[u][j] < d[j])
                    d[j] = d[u] + weights[u][j];
        }

        final List<Integer> result = new ArrayList<>();
        for (int i : d) {
            result.add(i);
        }
        return result;
    }

	/**
	 * Helper method that finds the next node to be marked as tight in Dijkstr's algorithm.
	 * @param tight The array specifying whether each graph node is tight or not.
	 * @param dist The array of shorted distances computed up to a given point in the iterations of the algorithm.
	 * @return The next node with the shorted distance to be marked as tight.
	 */
	private int findMin(boolean[] tight, int[] dist) {
		int min = Integer.MAX_VALUE;
        int minIndex = -1;

		for (int i = 0; i < dist.length; i++)
            if (!tight[i] && dist[i] <= min) {
                min = dist[i];
                minIndex = i;
            }

        return minIndex;
    }

	/**
	 * Creates a random graph of a given size.
	 * @param size The size of the graph.
	 * @return The adjacency matrix of the graph containg the weights of each edge.
	 */
    public Graph generateRandomGraph(int size) {
        int[][] weights = new int[size][size];
        Random gen = new Random(seed);

		// initialise the matrix with infinity, as there will be nodes with no
		// edge connecting them
		for (int i = 0; i < size; i++)
			for (int j = 0; j < size; j++)
				weights[i][j] = Integer.MAX_VALUE;

		//choose the maximum number of edges reaching a node
		int maxIncomingNodes = 10;

		//go through all nodes
		for (int node = 0; node < size; node++) {
			int incomingNodes = 0;
			//choose randomly the exact number of nodes are reaching this node
			int noNodes = gen.nextInt(maxIncomingNodes);
			
			//add edges reaching that node, up to maxIncoming nodes
			while (incomingNodes < noNodes) {
				
				//choose a neighbour node
				int u = gen.nextInt(size);
				//connect them, with a random weight
				weights[u][node] = gen.nextInt(1000);
				incomingNodes++;
			}
		}

        return new Graph(weights);
    }
}
