package xyz.acygn.mokapot.benchmarksuite.programs.mapreduce;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;

import xyz.acygn.mokapot.markers.NonCopiable;

/**
 * A class which imitates an Internet web page, formed of a link and content made up of a number of words
 */
public class WebPage implements Serializable, NonCopiable {
	private String link;
	private String content;
	private HashMap<String, Boolean> words;

	/**
	 * A constructor which takes 2 Strings as input: one that represents a link, and one that represents content
	 * @param _link The String that represents the link
	 * @param _content The String that represents the content
	 */
	public WebPage(String _link, String _content) {
		link = _link;
		content = _content.trim().replaceAll("\n", "").replaceAll(" +", " ");
		words = processContent(content.trim().split("\\W+"));
	}

	/**
	 * Returns all words contained in the content as an ArrayList of Strings
	 * @param content The content
	 * @return The ArrayList of Strings
	 */
	private static HashMap<String, Boolean> processContent(String[] content) {
		HashMap<String, Boolean> result = new HashMap<String,Boolean>();
		for (String element : content) {
			result.put(element, true);
		}
		return result;
	}

	/**
	 * Returns the WebPage's link
	 * @return The link
	 */
	public String getLink() {
		return link;
	}

	/**
	 * Returns all words contained in the WebPage
	 * @return The words
	 */
	public HashMap<String, Boolean> getWords() {
		return words;
	}

	/**
	 * A function which tells whether a word is contained into a web page
	 * @param word The word
	 * @return true if the word is present on the web page, false otherwise
	 */
	public boolean hasWord(String word) {
		if (words.get(word) == null) {
			return false;
		}
		return words.get(word);
	}
	
	/**
	 * Clones the WebPage.
	 */
	public WebPage clone() {
		return (new WebPage(this.link,this.content));
	}
}
