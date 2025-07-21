package fr.inria.corese.core.next.impl.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for IRI.
 *
 * Intended to facilitate string manipulation related to IRI.
 */
public class IRIUtils {

    private static final Pattern IRI_PATTERN = Pattern.compile("^(([\\w\\-]+:\\/\\/([\\w\\-_:]+\\.)*[\\w\\-_:]*)(\\/([\\w\\-\\._\\:]+\\/)*))([\\w\\-\\._\\:]+)?(\\?[\\w\\-_\\:\\?\\=]+)?((\\#)?([\\w\\-_]+))?$");
    private static final Pattern STANDARD_IRI_PATTERN = Pattern.compile("^(([^:/?#\\s]+):)(\\/\\/([^/?#\\s]*))?([^?#\\s]*)(\\?([^#\\s]*))?(#(.*))?");

    /**
     * Prevent instantiation of the utility class.
     */
    private IRIUtils() {
    }

    /**
     * Guesses the namespace of an IRI using a regex pattern.
     * @param iri The IRI string to be processed.
     * @return the guessed namespace of the IRI or an empty string if no match is found.
     */
    public static String guessNamespace(String iri) {
        try {
            Matcher matcher = IRI_PATTERN.matcher(iri);

            if(matcher.matches()) {
                if((matcher.group(8) == null) || (matcher.group(6) == null && matcher.group(9) == null) ) { // If the IRI has no fragment or ends with a slash

                    return matcher.group(1);
                } else {
                    // 1: Domain and path ending with a slash, 6: final path element without slash, 9: final # if there is a fragment
                    return matcher.group(1) + matcher.group(6) + matcher.group(9);
                }
            } else {
                return "";
            }
        } catch (IllegalStateException e) {
            return "";
        }
    }

    /**
     * Guesses the local name of an IRI using a regex pattern.
     * @param iri The IRI string to be processed.
     * @return the guessed local name of the IRI or an empty string if no match is found.
     */
    public static String guessLocalName(String iri) {
        try {
            Matcher matcher = IRI_PATTERN.matcher(iri);

            if(matcher.matches()) {
                if(matcher.group(10) != null){ // If the IRI has a fragment
                    return matcher.group(10);
                } else if(matcher.group(6) != null ) { // If the IRI has no fragment but do not ends with a slash
                    return matcher.group(6);
                } else { // If the URI ends with a slash
                    return "";
                }
            } else {
                return "";
            }
        } catch (IllegalStateException e) {
            return "";
        }
    }

    /**
     * Checks if the given string is a valid IRI using a regex pattern extracted from the W3C standards.
     * @param iriString The string to be checked.
     * @return true if the string is a valid IRI, false otherwise.
     */
    public static boolean isStandardIRI(String iriString) {
            try {
            Matcher matcher = STANDARD_IRI_PATTERN.matcher(iriString);
            return matcher.matches();
        } catch (IllegalStateException e) {
            return false;
        } }
}
