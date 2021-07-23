package xyz.acygn.mokapot.benchmarksuite.programs.mapreduce;

import java.util.Random;

/**
 * A class that generates random WebPages (see WebPage.java in the same package)
 *
 */
public class WebPageGenerator {
	Random r;
	
	public WebPageGenerator(int _seed) {
		r = new Random(_seed);
	}
	
	/**
	 * The function that generates a random web page
	 * @param MAX_LINK_SIZE The maximum number of words in each link
	 * @param MAX_CONTENT_SIZE The maximum number of words on the actual page
	 * @param MAX_WORD_LENGTH The maximum length of a word
	 * @return The generated WebPage
	 */
	public WebPage generateRandomWebpage(final int MAX_LINK_SIZE, final int MAX_CONTENT_SIZE, final int MAX_WORD_LENGTH) {
		return new WebPage(generateLink(MAX_LINK_SIZE, MAX_WORD_LENGTH), generateContent(MAX_CONTENT_SIZE, MAX_WORD_LENGTH));
	}

	/**
	 * Generates a random word
	 * @param MAX_WORD_LENGTH The maximum word length
	 * @return The generated word
	 */
	public String generateWord(final int MAX_WORD_LENGTH) {
		final String ALPHABET = "qwertyuiopasdfghjklzxcvbnm";
		int length;
		String word = new String();
		length = r.nextInt(MAX_WORD_LENGTH) + 1;

		for (int i = 0; i < length; i++) {
			word += ALPHABET.charAt(r.nextInt(ALPHABET.length()));
		}

		return word;
	}

	/**
	 * Generates a random link
	 * @param MAX_LINK_SIZE The maximum number of words in the link
	 * @param MAX_WORD_LENGTH The maximum word length in the link
	 * @return The generated link
	 */
	private String generateLink(final int MAX_LINK_SIZE, final int MAX_WORD_LENGTH) {
		String link = new String();

		link += "https://www.";

		int length = r.nextInt(MAX_LINK_SIZE) + 1;

		for (int i = 0; i < length; i++) {
			link += generateWord(MAX_WORD_LENGTH);
		}

		link += ".co.uk";

		return link;
	}

	/**
	 * Generates random content of a WebPage
	 * @param MAX_CONTENT_SIZE The maximum number of words in the generated content
	 * @param MAX_WORD_LENGTH The maximum length of a word
	 * @return The generated content
	 */
	private String generateContent(final int MAX_CONTENT_SIZE, final int MAX_WORD_LENGTH) {
		int length = r.nextInt(MAX_CONTENT_SIZE);
		String content = new String();

		for (int i = 0; i < length; i++) {
			content += generateWord(MAX_WORD_LENGTH) + " ";
		}

		return content;
	}
}
