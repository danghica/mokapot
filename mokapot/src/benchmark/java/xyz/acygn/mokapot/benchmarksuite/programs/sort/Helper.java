package xyz.acygn.mokapot.benchmarksuite.programs.sort;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import xyz.acygn.mokapot.markers.NonCopiable;

public interface Helper<T extends Comparable<T>> extends Remote, NonCopiable {
	/**
	 * The function used for receiving new elements
	 *
	 * @param _elements
	 *            The elements
	 */
	public void addElements(List<T> _elements) throws RemoteException;
	
	/**
	 * 
	 * @return All elements
	 */
	public  List<T> getElements() throws RemoteException;
	
	/**
	 * Returns the element in the ith position
	 *
	 * @param i
	 *            The index
	 * @return The element
	 */
	public  T getElement(int i) throws RemoteException;
	
	/**
	 * Empties the list of elements.
	 */
	public  void clearElements() throws RemoteException;

	/**
	 * Returns the number of elements inside the helper
	 *
	 * @return The number of elements inside the helper
	 */
	public Integer getSize() throws RemoteException;

	/**
	 * The function which tells the Helper to sort its elements
	 */
	public void sort() throws RemoteException;

	/**
	 * Merges the contents of local elements and elements from received
	 * DistantSorter
	 *
	 * @param _ds
	 *            The Helper
	 */
	public void merge(Helper<T> _ds) throws RemoteException;
	
	/**
	 * Returns true when nothing is happening.
	 */
	public boolean ready() throws RemoteException;
}
