package xyz.acygn.mokapot.benchmarksuite.programs.bst.rmi;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import xyz.acygn.mokapot.benchmarksuite.programs.bst.local.BSTNode;
import xyz.acygn.mokapot.rmiserver.Executable;

/**
 * Helper class used to create a {@link BSTNode} object remotely, using RMI. The
 * class implements the Executable interface, which is used to specify the code that
 * is going to run on the server, therefore enabling the remote initialisation
 * of the object.
 * 
 * @author Alexandra Paduraru
 *
 */
public class BSTNodeExecutable implements Executable<RemoteBSTNode>, Serializable {

	private int data;

	/**
	 * Create a new BSTNodeTak object, which can be used to create a
	 * {@link BSTNode} remotely. Note that the node data needs to be set before
	 * calling the execute() method, using the setData(int data) method.
	 */
	public BSTNodeExecutable() {
		super();
	}

	/**
	 * Create a new BSTNodeTak object, with a given data.
	 * @param data The node data.
	 */
	public BSTNodeExecutable(int data) {
		super();
		this.data = data;
	}

	/**
	 * Returns a new {@link BSTNode} object created remotely. Note that the node
	 * data needs to be set before calling this method, using the setData(int
	 * data) method.
	 */
	@Override
	public RemoteBSTNode execute(int port) {
		RemoteBSTNode node = new RemoteBSTNodeImp(data);
		
		try {
			return (RemoteBSTNode) UnicastRemoteObject.exportObject(node, port);
		} catch (RemoteException e) {
			e.printStackTrace();
			return null;
		}
	}	

	// Getters and setters below

	/**
	 * Retrieve the data in the node.
	 * 
	 * @return The node data.
	 */
	public int getData() {
		return data;
	}

	/**
	 * Change the node data.
	 * 
	 * @param data
	 *            The new node data.
	 */
	public void setData(int data) {
		this.data = data;
	}

}