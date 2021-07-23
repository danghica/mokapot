package xyz.acygn.mokapot.benchmarksuite.programs.bst.rmi;

import java.rmi.RemoteException;

/**
 * @author Alexandra Paduraru
 *
 */
public interface RMIBSTNode{
	
	public RMIBSTNode getLeft() throws RemoteException;
	public RMIBSTNode getRight() throws RemoteException;
	public int getData()throws RemoteException;
	
	public void setLeft(RMIBSTNode left) throws RemoteException;
	public void setRight(RMIBSTNode right) throws RemoteException;
	public void setData(int data) throws RemoteException;

}