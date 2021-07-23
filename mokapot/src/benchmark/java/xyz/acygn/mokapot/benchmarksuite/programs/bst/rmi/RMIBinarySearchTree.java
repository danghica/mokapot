package xyz.acygn.mokapot.benchmarksuite.programs.bst.rmi;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;

import xyz.acygn.mokapot.rmiserver.Executable;
import xyz.acygn.mokapot.rmiserver.RMIServer;

/**
 * Example class to represent a binary search tree.
 * 
 * Note that the tree can contain both local and remote nodes, implemented using
 * RMI. The root node is always remote.
 * 
 * @author Alexandra Paduraru
 *
 */
public class RMIBinarySearchTree {
	private RMIBSTNode root;
	private Executable<RemoteBSTNode> createNodes;
	private static RMIServer server;

	/**
	 * Create a new binary search tree with a given root node data. Note that
	 * the root node is created remotely.
	 * 
	 * @param root
	 *            The root node of the tree, which contains data and left and
	 *            right sub-trees.
	 */
	public RMIBinarySearchTree(int rootData, RMIServer _server) {
		super();
		server = _server;
		createNodes = new BSTNodeExecutable(rootData);
		try {
			this.root = server.execute(createNodes);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Inserts a new node in the correct place in the binary search tree, with
	 * the given data value. Note that the node is inserted on the local
	 * machine.
	 * 
	 * @param data
	 *            The data value to be inserted in the binary search tree.
	 * @throws RemoteException
	 */
	public void insertLocally(int data) throws RemoteException {
		if (root == null)
			root = new LocalBSTNodeImp(data);
		else
			root = insertLocally(root, data);
	}

	/**
	 * Inserts a new node in the correct place in the binary search tree, with
	 * the given data value. Note that the node is created and inserted
	 * remotely.
	 * 
	 * @param data
	 *            The data value to be inserted in the binary search tree.
	 * @throws RemoteException
	 */
	public void insertRemotely(int data) throws RemoteException {
		if (root == null) {
			((BSTNodeExecutable) createNodes).setData(data);
			root = server.execute(createNodes);
		} else
			root = insertRemotely(root, data);
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
	 * @throws RemoteException
	 */
	private RMIBSTNode insertLocally(RMIBSTNode node, int data) throws RemoteException {
		if (node == null)
			return new LocalBSTNodeImp(data);
		if (data < node.getData())
			node.setLeft(insertLocally(node.getLeft(), data));
		else
			node.setRight(insertLocally(node.getRight(), data));
		return node;
	}

	/**
	 * Helper method that returns a new RMIBSTNode created remotely, with a
	 * given value inserted in the tree which has that node as a root.
	 * 
	 * @param node
	 *            The root node of the sub-tree in which the data value needs to
	 *            be inserted.
	 * @param data
	 *            The data value that needs to be inserted.
	 * @return A new tree, with the given data value inserted in a new node.
	 * @throws RemoteException
	 */
	private RMIBSTNode insertRemotely(RMIBSTNode node, int data) throws RemoteException {
		if (node == null) {
			((BSTNodeExecutable) createNodes).setData(data);
			return server.execute(createNodes);
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
	 * @param root
	 *            The root node of the desired binary search tree.
	 * @throws RemoteException
	 */
	public ArrayList<RMIBSTNode> inOrderTraversal(RMIBSTNode root) throws RemoteException {
		RMIBSTIterator it = new RMIBSTIterator(root);
		ArrayList<RMIBSTNode> result = new ArrayList<>();
		while (it.hasNext()) {
			result.add(it.next());
		}

		return result;
	}

	/**
	 * The syntactically equivalent version of the Mokapot and local
	 * incrementAllNodes() method. However, this does not perform as expected on
	 * the RMI implementation, due to copying arguments, instead of referencing
	 * them.
	 * 
	 * @throws RemoteException
	 */
	public void naiveIncrementAllNodes() throws RemoteException {
		RMIBSTIterator it = new RMIBSTIterator(root);
		while (it.hasNext()) {
			RMIBSTNode current = it.next();
			current.setData(current.getData() + 1);

			System.out.println("incremented " + (current instanceof RemoteBSTNode ? " remote " : " local") + " node "
					+ (current.getData() - 1));
		}
	}

	/**
	 * Increments all nodes of the Binary Search Tree.
	 * @throws RemoteException
	 */
	public void incrementAllNodes() throws RemoteException {
		RMIBSTIterator it = new RMIBSTIterator(root);
		RMIBinarySearchTree newBST = new RMIBinarySearchTree(root.getData() + 1, server);
		while (it.hasNext()) {
			RMIBSTNode current = it.next();

			if (current == root)
				continue;

			if (current instanceof RemoteBSTNodeImp)
				newBST.insertRemotely(current.getData() + 1);
			else
				newBST.insertLocally(current.getData() + 1);
		}
		root = newBST.getRoot();
	}

	// Getters and setters below
	
	/**
	 * Retrieve the tree root node.
	 * 
	 * @return The root node.
	 */
	public RMIBSTNode getRoot() {
		return root;
	}
}