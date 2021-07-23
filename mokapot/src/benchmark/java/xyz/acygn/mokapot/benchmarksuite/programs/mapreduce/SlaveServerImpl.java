package xyz.acygn.mokapot.benchmarksuite.programs.mapreduce;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * A class which is used to store WebPages and look for WebPages which contain a given word
 *
 */
public class SlaveServerImpl implements SlaveServer, Serializable {
	private ArrayList<WebPage> webPages;
	private ArrayList<String> response;
	private String word;

	/**
	 * The constructor
	 */
	public SlaveServerImpl() {
		webPages = new ArrayList<>();
		word = "";
		response = null;
	}
	
	@Override
	public synchronized void addWebPage(WebPage _w) {
		webPages.add(_w.clone());
	}
	
	@Override
	public synchronized int getWebPageNumber() {
		return webPages.size();
	}
	
	@Override
	public synchronized void setWord(String _word) {
		word = _word;
	}

	@Override
	public synchronized ArrayList<String> run() {
		if (word == null || word == "") {
			response = new ArrayList<String>();
			return new ArrayList<String>();
		}
		
		ArrayList<String> validLinks;

		validLinks = new ArrayList<>();
		for (WebPage currentPage : webPages) {
			if (currentPage.hasWord(word)) {
				validLinks.add(currentPage.getLink());
			}
		}

		response = validLinks;
		
		return response;
	}

	@Override
	public synchronized boolean ready() throws RemoteException {
		return true;
	}
}
