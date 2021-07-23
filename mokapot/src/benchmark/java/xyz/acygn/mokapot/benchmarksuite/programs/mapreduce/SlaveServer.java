package xyz.acygn.mokapot.benchmarksuite.programs.mapreduce;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

import xyz.acygn.mokapot.markers.NonCopiable;

public interface SlaveServer extends NonCopiable, Remote {

	/**
	 * Adds a WebPage
	 * @param _w The WebPage to be added
	 */
	void addWebPage(WebPage _w) throws RemoteException;

	/**
	 * Gets the number of WebPages stored on the SlaveServer
	 * @return The number of WebPages
	 */
	int getWebPageNumber() throws RemoteException;

	/**
	 * Sets the word to be looked for when searching the WebPages
	 * @param _word The word to be looked for when searching the WebPages
	 */
	void setWord(String _word) throws RemoteException;

	/*
	 * Looks for WebPages that contain last word set by the "setWord" method in the list of WebPages held on the SlaveServer
	 */
	ArrayList<String> run() throws RemoteException;
	
	/*
	 * Returns true when nothing else is happening inside the class.
	 * @return true when nothing else is happening inside the class.
	 */
	boolean ready() throws RemoteException;
}