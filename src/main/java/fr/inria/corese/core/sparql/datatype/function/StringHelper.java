package fr.inria.corese.core.sparql.datatype.function;

/**
 * Extended functionalities on String objects like "containsIgnoreCase".
 * 
 * @author Priscille Durville
 *
 */
public class StringHelper {

	public static final String WORD_DELIMS = "[^a-zA-Z_0-9\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u00FF]"; 	
	/**
	 * Compares two Strings, returning true if they are equal ignoring case and the accents.
	 * nulls are handled without exceptions. Two null references are considered equal. Comparison is case and "accent" insensitive.
	 * @param s1 the first String, may be null,
	 * @param s2 the second String, may be null
	 * @return true if the Strings are equal, case and accent insensitive, or both null
	 */
	public static boolean equalsIgnoreCaseAndAccent(String s1, String s2) {
	     if ((s1 != null) && (s2 != null)) {
	            return removeAccent(s1).equalsIgnoreCase(removeAccent(s2));
	     } else return (s1 == null) && (s2 == null);
	}
	

	/**
	 * Remove all accents from a given string.
	 * @param string a string
	 * @return the same string without any accent.
	 */
	public static String removeAccent (String string) {
	   char [] charsData = new char [string.length ()];
	   string.getChars(0, charsData.length, charsData, 0);
	   char c;
	   for (int i = 0; i < charsData.length; i++) {
		   c = charsData[i];
	       switch (c) {
	         case '\u00e0' : 
	         case '\u00e1' :  
	         case '\u00e2' :
	         case '\u00e3' :
	         case '\u00e4' :
	         case '\u00e5' : charsData [i] = 'a';
	                         break;
	         case '\u00c0' :
	         case '\u00c1' :
	         case '\u00c2' :
	         case '\u00c3' :
	         case '\u00c4' :
	         case '\u00c5' : charsData [i] = 'A';
	                         break;
	         case '\u00e7' : charsData [i] = 'c';
	                         break;
	         case '\u00c7' : charsData [i] = 'C';
             				 break;
	         case '\u00e8' :
	         case '\u00e9' :
	         case '\u00ea' :
	         case '\u00eb' : charsData [i] = 'e';
	                         break;
	         case '\u00c8' :
	         case '\u00c9' :
	         case '\u00ca' :
	         case '\u00cb' : charsData [i] = 'E';
	                         break;
	         case '\u00ec' :
	         case '\u00ed' :
	         case '\u00ee' :
	         case '\u00ef' : charsData [i] = 'i';
	                         break;
	         case '\u00cc' :
	         case '\u00cd' :
	         case '\u00ce' :
	         case '\u00cf' : charsData [i] = 'I';
	                         break;
	         case '\u00f0' :
	         case '\u00f2' :
	         case '\u00f3' :
	         case '\u00f4' :
	         case '\u00f5' :
	         case '\u00f6' : charsData [i] = 'o';
	                         break;
	         case '\u00d2' :
	         case '\u00d3' :
	         case '\u00d4' :
	         case '\u00d5' :
	         case '\u00d6' : charsData [i] = 'O';
	                         break;
	         case '\u00f9' :
	         case '\u00fa' :
	         case '\u00fb' :
	         case '\u00fc' : charsData [i] = 'u';
	                         break;
	         case '\u00d9' :
	         case '\u00da' :
	         case '\u00db' :
	         case '\u00dc' : charsData [i] = 'U';
	                         break;
	       }
	   }
	   return new String (charsData);
	}
	
	/**
	 * Returns true if the given string (considered as a word or a group of words) is contained in the source one.
	 * This method is null safe.
	 * Example : containsWord("This is a dog.", "This") => returns true.
	 * Example : containsWord("This is a dog.", "his") => returns false.
	 * @param text the text,
	 * @param word the word to look for into the source text.
	 * @return true if the given word is found in the text. False otherwise.
	 */
	public static boolean containsWord(String text, String word) {
		if ((text == null) || (word == null)) {
			return false;
		}
		if (text.contains(word)) { 
			int index = -1;
			int fromIndex = 0;
			while ((index = text.indexOf(word, fromIndex)) != -1) {
				if ((index == 0) && (text.length() > word.length()+1) && text.substring(0, word.length()+1).matches(word+WORD_DELIMS)) {
					return true;
                } else if ((index == 0) && (text.length() == word.length())) {
                    return true;
				} else if ((index > 0) && (text.length() > index + word.length()+1) && text.substring(index-1, index + word.length()+ 1).matches(WORD_DELIMS+word+WORD_DELIMS)) {
					return true;
				} else if ((index > 0) && (text.length() == (index + word.length() + 1)) && text.substring(index-1).matches(WORD_DELIMS+word+WORD_DELIMS)) {
					return true;
				} else if ((index > 0) && (text.length() == index + word.length()) && text.substring(index-1, index + word.length()).matches(WORD_DELIMS+word)) {
                    return true;
                }
				if (text.length() > index + word.length()) {
					//text = text.substring(index + word.length());
					fromIndex = index + word.length();
				} else {
					text = "";
				}
			}
		} 
		return false;
	}
	
	
	/**
	 * Returns true if the given string (considered as a word) is contained in the source one. The algorithm ignores case and accents.
	 * This method is null safe.
	 * Example : containsWordIgnoreCaseAndAccent("ceci est ma clé.", "cle") => returns true.
	 * Example : containsWordIgnoreCaseAndAccent("Ceci est ma cle.", "eci") => returns false.
	 * @param string1 the text,
	 * @param string2 the word (or group of words) to look for into the source text.
	 * @return true if the given word (or group of words) is found in the text, case and accents have been ignored. False otherwise.
	 */
	public static boolean containsWordIgnoreCaseAndAccent(String string1, String string2) {
		if ((string1 == null) || (string2 == null)) {
			return false;
		}
		if (removeAccent(string1.toLowerCase()).contains(removeAccent(string2.toLowerCase()))) { 
			String text = removeAccent(string1.toLowerCase());
			String word = removeAccent(string2.toLowerCase());
			return containsWord(text, word);
		} 
		return false;
	}
	
	
	/**
	 * Returns the index within the text of the first occurrence of the specified word (or groups of words).
	 * This method is null safe.
	 * @param text the text,
	 * @param word the word (or group of words),
	 * @return the index within the text of the first occurrence of the specified word.
	 */
	public static int indexOfWord(String text, String word) {
		if ((text == null) || (word == null)) {
			return -1;
		}
		if (text.contains(word)) { 
			int index = -1;
			int fromIndex = 0;
			while ((index = text.indexOf(word, fromIndex)) != -1) {
				if ((index == 0) && (text.length() > word.length()+1) && text.substring(0, word.length()+1).matches(word+WORD_DELIMS)) {
					return 0;
				} else if ((index == 0) && (text.length() == word.length())) {
					return 0;
				} else if ((index > 0) && (text.length() > (index+word.length()+1)) && text.substring(index-1, index + word.length()+ 1).matches(WORD_DELIMS+word+WORD_DELIMS)) {
					return index;
				} else if ((index > 0) && (text.length() == (index + word.length() + 1)) && text.substring(index-1).matches(WORD_DELIMS+word+WORD_DELIMS)) {
					return index;
				} else if ((index > 0) && (text.length() == (index+word.length())) && text.substring(index-1, index + word.length()).matches(WORD_DELIMS+word)) {
					return index;
				}
				if (text.length() > index + word.length()) {
					//text = text.substring(index + word.length());
					fromIndex = index + word.length();
				} else {
					text = "";
				}
			}
		}
		return -1;
	}
	
	
	/**
	 * Returns the index within the text of the first occurrence of the specified word (or groups of words). The algorithm ignores case and accents.
	 * This method is null safe.
	 * @param string1 the text,
	 * @param string2 the word (or group of words),
	 * @return the index within the text of the first occurrence of the specified word.
	 */
	public static int indexOfWordIgnoreCaseAndAccent(String string1, String string2) {
		if ((string1 == null) || (string2 == null)) {
			return -1;
		}
		if (removeAccent(string1.toLowerCase()).contains(removeAccent(string2.toLowerCase()))) { 
			String text = removeAccent(string1.toLowerCase());
			String word = removeAccent(string2.toLowerCase());
			return indexOfWord(text, word);
		}
		return -1;
	}
	
}
