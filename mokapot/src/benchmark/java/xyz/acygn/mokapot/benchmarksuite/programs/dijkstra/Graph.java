package xyz.acygn.mokapot.benchmarksuite.programs.dijkstra;

import java.io.Serializable;
import xyz.acygn.mokapot.markers.NonCopiable;

/**
 * A wrapper for an adjacency matrix.
 * Since it is undesirable for the large array to be
 * copied across the network, this class implements
 * NonCopiable.
 *
 * @author Kelsey McKenna
 */
public class Graph implements NonCopiable, Serializable {

    private int[][] weights;

    public Graph(int[][] weights) {
        this.weights = weights;
    }

    public int[][] getWeights() {
        return weights;
    }

    public void clearWeights() {
        for (int i = 0; i < weights.length; i++) {
            weights[i] = null;
        }
        this.weights = null;
    }

}
