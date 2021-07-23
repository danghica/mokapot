package xyz.acygn.mokapot.benchmarksuite.programs.dijkstra;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import xyz.acygn.mokapot.markers.NonCopiable;

/**
 * @author Kelsey McKenna
 */
public interface Dijkstra extends Remote, NonCopiable {


    Graph generateRandomGraph(int size) throws RemoteException;

    /**
     * Performs Dijkstra's algorithm and returns the shortest distances from the start node to every other node.
     *
     * @param graph The graph, containing an adjacency matrix for the weights.
     * @param s     The start node.
     * @return The array of shortest distances from the start node to every other node in the graph.
     */
    List<Integer> shortestDist(Graph graph, int s) throws RemoteException;

}
