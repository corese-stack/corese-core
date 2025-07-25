package fr.inria.corese.core.next.impl.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for IRI.
 *
 * Intended to facilitate string manipulation related to IRI.
 */
public class IRIUtils {

    private static final Pattern IRI_PATTERN = Pattern.compile("^(?<namespace>(?<protocol>[\\w\\-]+):(?<dblSlashes>\\/\\/)?(?<domain>([\\w\\-_:@]+\\.)*[\\w\\-_:]*))((?<path>\\/([\\w\\-\\._\\:]+\\/)*)(?<finalPath>[\\w\\-\\._\\:]+)?(?<query>\\?[\\w\\-_\\:\\?\\=]+)?(\\#)?(?<fragment>([\\w\\-_]+))?)?$");
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
                if(matcher.group("protocol") != null && matcher.group("protocol").equals("_")) {
                    return "";
                }
                StringBuilder namespace = new StringBuilder();
                namespace.append(matcher.group("protocol")).append(":");
                if(matcher.group("dblSlashes") != null) {
                    namespace.append(matcher.group("dblSlashes"));
                }
                namespace.append(matcher.group("domain"));
                if(matcher.group("path") != null) {
                    namespace.append(matcher.group("path"));
                }
                if(matcher.group("fragment") != null && matcher.group("finalPath") != null) {
                    namespace.append(matcher.group("finalPath")).append("#");
                }
                return namespace.toString();
            } else {
                throw new IllegalStateException("No namespace found for the given IRI: " + iri + ".");
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
                if(matcher.group("fragment") != null){ // If the IRI has a fragment
                    return matcher.group("fragment");
                } else if(matcher.group("finalPath") != null ) { // If the IRI has no fragment but do not ends with a slash
                    return matcher.group("finalPath");
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
