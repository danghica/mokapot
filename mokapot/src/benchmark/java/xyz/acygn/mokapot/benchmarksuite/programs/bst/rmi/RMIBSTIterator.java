package xyz.acygn.mokapot.benchmarksuite.programs.bst.rmi;

import java.rmi.RemoteException;
import java.util.Stack;

import xyz.acygn.mokapot.benchmarksuite.programs.bst.local.BSTIterator;

/**
 * Distributed version of the {@link BSTIterator} class, which uses Binary
 * Search Trees consisting of {@link RMIBSTNode}.
 * 
 * @author Alexandra Paduraru
 *
 */
public class RMIBSTIterator {

	private Stack<RMIBSTNode> stack;

	/**
	 * Return a new {@link RMIBSTIterator} of the tree with a given root node.
	 * 
	 * @param root
	 *            The BST root node.
	 * @throws RemoteException
	 */
	public RMIBSTIterator(RMIBSTNode root) throws RemoteException {
		stack = new Stack<RMIBSTNode>();
		while (root != null) {
			stack.push(root);
			root = root.getLeft();
		}
	}

	/**
	 * Checks whether the BST has any remaining nodes.
	 * 
	 * @return True if the BST has more nodes and false otherwise.
	 */
	public boolean hasNext() {
		return !stack.isEmpty();
	}

	/**
	 * Returns the next node in the BST.
	 * 
	 * @return The next node.
	 * @throws RemoteException
	 */
	public RMIBSTNode next() throws RemoteException {
		RMIBSTNode node = stack.pop();
		RMIBSTNode result = node;
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