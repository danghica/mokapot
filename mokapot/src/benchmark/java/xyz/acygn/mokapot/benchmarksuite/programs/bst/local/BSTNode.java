package xyz.acygn.mokapot.benchmarksuite.programs.bst.local;

/**
 * Class to represent a node in a Binary Search Tree.
 * @author Alexandra Paduraru
 *
 */
public class BSTNode{

	private int data;
	private BSTNode left;
	private BSTNode right;

	/**
	 * Creates a new node with a given root data.
	 * @param data The data value in that node.
	 */
	public BSTNode(int data) {
		super();
		this.data = data;
	}

	//Getters and setters below

	/**
	 * Retrieve the data value of the node.
	 * @return The integer in that node.
	 */
	public int getData() {
		return data;
	}

	/**
	 * Change the data value of the node.
	 * @return The new integer in that node.
	 */
	public void setData(int data) {
		this.data = data;
	}

	/**
	 * Retrieve the left subtree of the node.
	 * @return The left subtree.
	 */
	public BSTNode getLeft() {
		return left;
	}

	/**
	 * Change the left subtree of the node.
	 * @return The new left subtree.
	 */
	public void setLeft(BSTNode left) {
		this.left = left;
	}

	/**
	 * Retrieve the right subtree of the node.
	 * @return The left subtree.
	 */
	public BSTNode getRight() {
		return right;
	}

	/**
	 * Retrieve the right subtree of the node.
	 * @return The new right subtree.
	 */
	public void setRight(BSTNode right) {
		this.right = right;
	}
}