package xyz.acygn.mokapot.benchmarksuite.programs.matrix;

import java.io.Serializable;
import java.rmi.RemoteException;

/**
 * @author Kelsey McKenna
 */
public class MatrixImpl implements Matrix, Serializable {

    private int[][] elements;

    public MatrixImpl(int[][] elements) {
        this.elements = elements;
    }

    public int[][] getElements() {
        return elements;
    }

    @Override
    public void clear() throws RemoteException {
        elements = null;
    }

    @Override
    public int size() {
        return elements.length;
    }

    @Override
    public void add(Matrix other) throws RemoteException {
        final int size = elements.length;
        assert size == other.size();

        int[][] otherElements = other.getElements();

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                elements[i][j] += otherElements[i][j];
            }
        }
    }

    @Override
    public String prettyString() throws RemoteException {
        return toString();
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();

        for (int i = 0; i < elements.length; ++i) {
            for (int j = 0; j < elements.length; ++j) {
                result.append(elements[i][j]);

                if (j < elements.length - 1) {
                    result.append(", ");
                }
            }

            if (i < elements.length - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

}
