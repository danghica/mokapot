package xyz.acygn.mokapot.benchmarksuite.programs.bst.mokapot;

import xyz.acygn.mokapot.CommunicationAddress;
import xyz.acygn.mokapot.DistributedCommunicator;
import xyz.acygn.mokapot.benchmarksuite.programs.bst.local.BSTIterator;
import xyz.acygn.mokapot.benchmarksuite.programs.bst.local.BSTNode;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Example class to represent a binary search tree.
 * <p>
 * Note that the tree can contain both local and remote nodes. The root node is
 * always remote and all remote nodes are created using RMI.
 *
 * @author Alexandra Paduraru
 */
public class MokapotBinarySearchTree {
    private BSTNode root;
    private DistributedCommunicator communicator;
    private CommunicationAddress remote;

    private HashSet<BSTNode> remoteNodes = new HashSet();

    /**
     * Create a new binary search tree with a given root node data. Note that
     * the root node is created remotely.
     *
     * @param rootData The root node of the tree, which contains data and left and
     *             right sub-trees.
     */
    public MokapotBinarySearchTree(DistributedCommunicator communicator, int rootData, CommunicationAddress remote) {
        super();

        // start communication
        this.communicator = communicator;
        this.remote = remote;

        this.root = DistributedCommunicator.getCommunicator().runRemotely(() -> new BSTNode(rootData),
                remote);
        remoteNodes.add(root);

    }

    /**
     * Inserts a new node in the correct place in the binary search tree, with
     * the given data value. Note that the node is inserted on the local
     * machine.
     *
     * @param data The data value to be inserted in the binary search tree.
     */
    public void insertLocally(int data) {
        if (root == null) {
            root = new BSTNode(data);
        } else
            root = insertLocally(root, data);
    }

    /**
     * Inserts a new node in the correct place in the binary search tree, with
     * the given data value. Note that the node is created and inserted
     * remotely.
     *
     * @param data The data value to be inserted in the binary search tree.
     */
    public void insertRemotely(int data) {
        if (root == null) {
            root = DistributedCommunicator.getCommunicator().runRemotely(() -> new BSTNode(data), remote);
            remoteNodes.add(root);
        } else
            root = insertRemotely(root, data);
    }

    /**
     * Helper method that returns a new BSTNode created locally, with a given
     * value inserted in the tree which has that node as a root.
     *
     * @param node The root node of the sub-tree in which the data value needs to
     *             be inserted.
     * @param data The data value that needs to be inserted.
     * @return A new tree, with the given data value inserted in a new node.
     */
    private BSTNode insertLocally(BSTNode node, int data) {
        if (node == null) {
            return new BSTNode(data);
        }
        if (data < node.getData())
            node.setLeft(insertLocally(node.getLeft(), data));
        else
            node.setRight(insertLocally(node.getRight(), data));
        return node;
    }

    /**
     * Helper method that returns a new BSTNode created remotely, with a given
     * value inserted in the tree which has that node as a root.
     *
     * @param node The root node of the sub-tree in which the data value needs to
     *             be inserted.
     * @param data The data value that needs to be inserted.
     * @return A new tree, with the given data value inserted in a new node.
     */
    private BSTNode insertRemotely(BSTNode node, int data) {
        if (node == null) {
            BSTNode newNode = DistributedCommunicator.getCommunicator().runRemotely(() -> new BSTNode(data), remote);
            remoteNodes.add(newNode);
            return newNode;
        }
        if (data < node.getData())
            node.setLeft(insertRemotely(node.getLeft(), data));
        else
            node.setRight(insertRemotely(node.getRight(), data));
        return node;
    }

    /**
     * Performs in order traversal on a binary search tree and prints the data
     * in every node.
     *
     * @param root The root node of the desired binary search tree.
     */
    public ArrayList<BSTNode> inOrderTraversal(BSTNode root) {
        BSTIterator it = new BSTIterator(root);
        ArrayList<BSTNode> result = new ArrayList<>();
        while (it.hasNext()) {
            result.add(it.next());
        }

        return result;
    }

    /**
     * Increments the value of all the nodes in the BST.
     */
    public void incrementAllNodes() {
        BSTIterator it = new BSTIterator(root);
        while (it.hasNext()) {
            BSTNode current = it.next();
            current.setData(current.getData() + 1);
        }
    }

    public void deleteAllNodes() {
        /*for(BSTNode node: remoteNodes){
            System.out.println("deleting node " + node);
			node = null;
		}*/


    }


    // Getters and setters below

    /**
     * Retrieve the tree root node.
     *
     * @return The root node.
     */
    public BSTNode getRoot() {
        return root;
    }

}