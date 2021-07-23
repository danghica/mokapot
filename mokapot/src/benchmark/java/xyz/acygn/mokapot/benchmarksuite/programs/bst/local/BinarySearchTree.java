package xyz.acygn.mokapot.benchmarksuite.programs.bst.local;

import java.util.ArrayList;

/**
 * Class to represent a local Binary Search Tree.
 * 
 * @author Alexandra Paduraru
 *
 */
public class BinarySearchTree {

	private BSTNode root;

	/**
	 * Create a new binary search tree with a given root node data. Note that
	 * the root node is created locally.
	 * 
	 * @param root
	 *            The root node of the tree, which contains data and left and
	 *            right sub-trees.
	 */
	public BinarySearchTree(int rootData) {
		super();
		this.root = new BSTNode(rootData);
	}

	/**
	 * Inserts a new node in the correct place in the binary search tree, with
	 * the given data value. Note that the node is inserted on the local
	 * machine.
	 * 
	 * @param data
	 *            The data value to be inserted in the binary search tree.
	 */
	public void insert(int data) {
		if (root == null)
			root = new BSTNode(data);
		else
			root = insertLocally(root, data);
	}

	/**
	 * Helper method that returns a new BSTNode created locally, with a given
	 * value inserted in the tree which has that node as a root.
	 * 
	 * @param node
	 *            The root node of the sub-tree in which the data value needs to
	 *            be inserted.
	 * @param data
	 *            The data value that needs to be inserted.
	 * @return A new tree, with the given data value inserted in a new node.
	 */
	private BSTNode insertLocally(BSTNode node, int data) {
		if (node == null)
			return new BSTNode(data);
		if (data < node.getData())
			node.setLeft(insertLocally(node.getLeft(), data));
		else
			node.setRight(insertLocally(node.getRight(), data));
		return node;
	}

	/**
	 * Performs in order traversal on a binary search tree and returns an ArrayList of all {@link BSTNode}s.
	 * 
	 * @param root
	 *            The root node of the desired binary search tree.
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
	 * Increments the data in all the nodes of the Binary Search Tree.
	 */
	public void incrementAllNodes() {
		BSTIterator it = new BSTIterator(root);
		while (it.hasNext()) {
			BSTNode current = it.next();
			current.setData(current.getData() + 1);
		}
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