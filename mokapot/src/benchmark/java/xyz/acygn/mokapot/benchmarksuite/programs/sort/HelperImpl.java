package xyz.acygn.mokapot.benchmarksuite.programs.sort;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HelperImpl<T extends Comparable<T>> implements Helper<T>, Serializable {
	private List<T> elements;

	/**
	 * Default constructor
	 */
	public HelperImpl() {
		elements = new ArrayList<>();
	}

	@Override
	public synchronized void addElements(List<T> _elements) {
		for (int i = 0; i < _elements.size(); i++) {
			elements.add(_elements.get(i));
		}
	}
	
	@Override
	public synchronized List<T> getElements() {
		return elements;
	}

	@Override
	public synchronized T getElement(int i) {
		return elements.get(i);
	}
	
	@Override
	public synchronized void clearElements() {
		elements.clear();
	}

	@Override
	public synchronized Integer getSize() {
		return elements.size();
	}

	@Override
	public synchronized void sort() {
		Collections.sort(elements);
	}

	@Override
	public synchronized void merge(Helper<T> _ds) {
		List<T> localElements = elements;

		List<T> temp;
		List<T> foreignElements;
		try {
			temp = _ds.getElements();
			foreignElements = new ArrayList<T>();
			for (int i = 0; i < _ds.getSize(); i++) {
				foreignElements.add(temp.get(i));
			}
		} catch (RemoteException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		

		elements = new ArrayList<>();
		int localElementsCounter = 0, foreignElementsCounter = 0;

		while (localElementsCounter < localElements.size() && foreignElementsCounter < foreignElements.size()) {
			if (localElements.get(localElementsCounter).compareTo(foreignElements.get(foreignElementsCounter)) <= 0) {
				elements.add(localElements.get(localElementsCounter));
				localElementsCounter++;
			} else {
				elements.add(foreignElements.get(foreignElementsCounter));
				foreignElementsCounter++;
			}
		}

		if (localElementsCounter < localElements.size()) {
			for (int i = localElementsCounter; i < localElements.size(); i++) {
				elements.add(localElements.get(i));
			}
		}

		if (foreignElementsCounter < foreignElements.size()) {
			for (int i = foreignElementsCounter; i < foreignElements.size(); i++) {
				elements.add(foreignElements.get(i));
			}
		}
	}
	
	@Override
	public synchronized boolean ready() {
		return true;
	}
}
