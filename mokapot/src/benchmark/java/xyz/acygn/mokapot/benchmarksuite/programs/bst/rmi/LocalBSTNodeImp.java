package xyz.acygn.mokapot.benchmarksuite.programs.bst.rmi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Class to represent a local node in a Binary Search Tree implemented using
 * RMI.
 *
 * @author Alexandra Paduraru
 *
 */
public class LocalBSTNodeImp implements LocalBSTNode {

    private static final long serialVersionUID = 1L;
    private transient int data;
    private RMIBSTNode left;
    private RMIBSTNode right;

    /**
     * Creates a new local node with a given root data.
     *
     * @param data The data value in that node.
     */
    public LocalBSTNodeImp(int data) {
        super();
        this.data = data;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(this.data);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.data = (int) in.readObject();
    }

    //Getters and setters below
    /**
     * Retrieve the data value of the node.
     *
     * @return The integer in that node.
     */
    public int getData() {
        return data;
    }

    /**
     * Change the data value of the node.
     *
     * @return The new integer in that node.
     */
    public void setData(int data) {
        this.data = data;
    }

    /**
     * Retrieve the left subtree of the node.
     *
     * @return The left subtree.
     */
    public RMIBSTNode getLeft() {
        return left;
    }

    /**
     * Change the left subtree of the node.
     *
     * @return The new left subtree.
     */
    public void setLeft(RMIBSTNode left) {
        this.left = left;
    }

    /**
     * Retrieve the right subtree of the node.
     *
     * @return The left subtree.
     */
    public RMIBSTNode getRight() {
        return right;
    }

    /**
     * Retrieve the right subtree of the node.
     *
     * @return The new right subtree.
     */
    public void setRight(RMIBSTNode right) {
        this.right = right;
    }
}
