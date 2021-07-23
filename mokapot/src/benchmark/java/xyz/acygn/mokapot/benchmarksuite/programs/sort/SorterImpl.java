package xyz.acygn.mokapot.benchmarksuite.programs.sort;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import xyz.acygn.mokapot.markers.NonCopiable;

/**
 * The class used to sort a list of elements
 *
 */
public class SorterImpl<T extends Comparable<T>> implements NonCopiable {
	ArrayList<Helper<T>> helpers;
	List<T> elements;

	/**
	 * The constructor.
	 *
	 * @param elements
	 */
	public SorterImpl() {
		helpers = new ArrayList<>();
		elements = new ArrayList<>();
	}

	/**
	 * Adds new elements to the list of elements
	 *
	 * @param _elements
	 *            The elements to be added
	 */
	public synchronized void addElements(List<T> _elements) {
		elements.addAll(_elements);
		this.addElementsToHelpers();
	}

	/**
	 * Clears the list of elements.
	 */
	public synchronized void clearElements() {
		elements.clear();
		this.clearElementsInHelpers();
	}
	
	/**
	 * Clears all the elements inside all helpers
	 */
	private synchronized void clearElementsInHelpers() {
		for (Helper<T> h : helpers) {
			try {
			h.clearElements();
			} catch (RemoteException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}
	
	/**
	 * Returns the list of elements
	 * @return
	 */
	public synchronized List<T> getElements() {
		return elements;
	}

	/**
	 * Adds a new helper.
	 *
	 * @param h
	 */
	public synchronized void addHelper(Helper<T> h) {
		helpers.add(h);
	}

	/**
	 * Clears all helpers
	 */
	public synchronized void clearHelpers() {
		helpers.clear();
	}
	
	/**
	 * Only returns true when there is no other method running.
	 */
	public synchronized boolean ready() {
		return true;
	}

	/**
	 * Sorts the list of elements. If no helpers are present, the Collections.sort
	 * method is used instead.
	 */
	public synchronized void sort() {
		if (elements == null) {
			return;
		}
		
		if (helpers.isEmpty()) {
			Collections.sort(elements);
			return;
		}

		sortHelpers();

		/*
		 * merge helpers
		 */
		List<Helper<T>> newHelpers = new ArrayList<Helper<T>>();
		for (int i = 0; i < helpers.size(); i++) {
			newHelpers.add(helpers.get(i));
		}

		mergeHelpers(newHelpers);

		getResultAndClearHelpers(newHelpers.get(0));
	}

	/**
	 * Transforms the given list of helpers into a list of 1 helper, which contains the result.
	 * @param newHelpers The list of helpers to be merged
	 */
	private synchronized void mergeHelpers(List<Helper<T>> newHelpers) {
		while (newHelpers.size() > 1) {
			for (int i = 0; i < newHelpers.size(); i += 2) {
				if ((i + 1) < newHelpers.size()) {
					try {
					newHelpers.get(i).merge(newHelpers.get(i + 1));
					} catch (RemoteException e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}
					newHelpers.remove(i + 1);
					i--;
				}
			}
			//Making sure all operations are done
			for (int i = 0; i < newHelpers.size(); i++) {
				try {
					newHelpers.get(i).ready();
					} catch (RemoteException e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}
			}
		}
	}

	/**
	 * Replaces the SorterImpl's list of elements with the sorted list of elements inside a Helper, and then clears the contents of all helpers contained in SorterImpl
	 * @param resultOwner The Helper which contains the result 
	 */
	private synchronized void getResultAndClearHelpers(Helper<T> resultOwner) {
		List<T> result = new ArrayList<>();

		try {
			for (int i = 0; i < resultOwner.getSize(); i++) {
				result.add((T) resultOwner.getElement(i));
			}

			for (int i = 0; i < helpers.size(); i++) {
				helpers.get(i).clearElements();
			}
			
			elements = result;
			return;
		} catch (RemoteException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	/**
	 * Tells each helper to sort its contents.
	 */
	private synchronized void sortHelpers() {
		for (int j = 0; j < helpers.size(); j++) {
			try {
			helpers.get(j).sort();
			} catch (RemoteException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}
	
	/**
	 * Adds the correct number of elements to each helper
	 */
	public synchronized void addElementsToHelpers() {
		if (helpers.size() == 0) {
			return;
		}
		int elementsPerHelper = elements.size() / helpers.size();

		/*
		 * Distribute data between helpers.
		 */
		for (int j = 0; j < helpers.size(); j++) {
			List<T> temp = new ArrayList<T>();
			for (int i = j * elementsPerHelper; i < (j + 1) * elementsPerHelper; i++) {
				temp.add(elements.get(i));
			}

			if (j == helpers.size() - 1) {
				for (int i = (j + 1) * elementsPerHelper; i < elements.size(); i++) {
					temp.add(elements.get(i));
				}
			}
			
			try {
			helpers.get(j).addElements(temp);
			} catch (RemoteException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}
}
