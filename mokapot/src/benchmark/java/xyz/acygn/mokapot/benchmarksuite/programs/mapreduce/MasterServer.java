package xyz.acygn.mokapot.benchmarksuite.programs.mapreduce;

import java.rmi.RemoteException;
import java.util.ArrayList;

import xyz.acygn.mokapot.markers.NonCopiable;

/**
 * A class which controls a number of SlaveServers, and uses them to store and compute results
 *
 */
public class MasterServer implements NonCopiable {
	private ArrayList<SlaveServer> slaves = new ArrayList<SlaveServer>();

	/**
	 * The constructor
	 */
	public MasterServer() {
		slaves = new ArrayList<SlaveServer>();
	}

	/**
	 * Adds a new slave server
	 * @param _newSlave The slave server to be added
	 */
	public synchronized void addSlaveServer(SlaveServer _newSlave) {
		slaves.add(_newSlave);
		try {
			_newSlave.ready();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Empties the list of SlaveServers and deletes all WebPages
	 */
	public synchronized void clear() {
		for (SlaveServer s : slaves) {
			try {
				s.ready();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
			s = null;
		}
		slaves = new ArrayList<>();
	}

	/**
	 * Adds a new WebPage. If no Slave servers had been added, the function does nothing
	 * @param page The WebPage to be added
	 */
	public synchronized void addWebPage(WebPage page) {
		this.ready();
		if (slaves.isEmpty()) {
			// TODO: Do this properly
			System.out.println("Cannot add web page");
			return;
		}

		try {
			this.getLowUsageSlaveServer().ready();
			this.getLowUsageSlaveServer().addWebPage(page);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Returns an ArrayList of Strings, which represents the links of the WebPages that contain a certain word
	 * @param _word
	 * @return
	 */
	public synchronized ArrayList<String> getLinksOfPagesWith(String _word) {
		ArrayList<String> result = new ArrayList<String>();
		for (SlaveServer slave : slaves) {
			try {
				slave.ready();
				slave.setWord(_word);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		for (SlaveServer slave : slaves) {
			ArrayList<String> response;
			try {
				slave.ready();
				response = slave.run();
				slave.ready();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
			result.addAll(response);
		}
		return result;
	}

	/**
	 * Returns the SlaveServer with the lowest amount of WebPages stored, and null if no SlaveServers had been added
	 * @return The SlaveServer with the lowest amount of WebPages stored, and null if no SlaveServers had been added
	 */
	private synchronized SlaveServer getLowUsageSlaveServer() {
		this.ready();
		SlaveServer best = null;
		for (SlaveServer slave : slaves) {
			try {
				if (best == null || slave.getWebPageNumber() < best.getWebPageNumber()) {
					best = slave;
				}
			} catch (RemoteException e) {
				e.printStackTrace();
				new RuntimeException(e);
			}
		}

		return best;
	}
	
	/**
	 * Only returns true. Because all methods are synchronized, the value will only be returned when no other computation is active.
	 */
	public synchronized boolean ready() {
		for (SlaveServer s : slaves) {
			try {
				s.ready();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		return true;
	}
	
}
