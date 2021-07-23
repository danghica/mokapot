package xyz.acygn.mokapot.benchmarksuite.programs.bst.rmi;

/**
 * Class to represent a remote node in a Binary Search Tree implemented using
 * RMI.
 *
 * @author Alexandra Paduraru
 *
 */
public class RemoteBSTNodeImp implements RemoteBSTNode {

    private int data;
    private RMIBSTNode left;
    private RMIBSTNode right;

    /**
     * Creates a new node with a given root data.
     *
     * @param data The data value in that node.
     */
    public RemoteBSTNodeImp(int data) {
        super();
        this.data = data;
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
