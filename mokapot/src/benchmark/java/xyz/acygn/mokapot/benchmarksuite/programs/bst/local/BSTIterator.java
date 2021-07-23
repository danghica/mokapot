package xyz.acygn.mokapot.benchmarksuite.programs.bst.local;

import java.util.Stack;

/**
 * Iterator for a {@link BinarySearchTree}.
 *
 * @author Alexandra Paduraru
 *
 */
public class BSTIterator {
	private Stack<BSTNode> stack;

	/**
	 * Creates a new {@link BSTIterator} object with a given root node.
	 *
	 * @param root
	 *            The Bnary Search Tree root.
	 */
	public BSTIterator(BSTNode root) {
		stack = new Stack<BSTNode>();
		while (root != null) {
			stack.push(root);
			root = root.getLeft();
		}
	}

	/**
	 * Checks whether there are remaining nodes in the Binary Search Tree.
	 *
	 * @return True if there are remaining nodes in the tree and false otherwise.
	 */
	public boolean hasNext() {
		return !stack.isEmpty();
	}

	/**
	 * Returns the next node in the Binary Search Tree.
	 * @return The next node in the tree.
	 */
	public BSTNode next() {
		BSTNode node = stack.pop();
		BSTNode result = node;
		if (node.getRight() != null) {
			node = node.getRight();
			while (node != null) {
				stack.push(node);
				node = node.getLeft();
			}
		}
		return result;
	}
}