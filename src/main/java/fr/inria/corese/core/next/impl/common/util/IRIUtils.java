package fr.inria.corese.core.next.impl.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for IRI.
 *
 * Intended to facilitate string manipulation related to IRI.
 */
public class IRIUtils {

    private static final Logger logger = LoggerFactory.getLogger(IRIUtils.class);

    private IRIUtils() {
        // Test class
    }

    /**
     * Guesses the namespace of an IRI using a regex pattern.
     * @param iri
     * @return the guessed namespace of the IRI or an empty string if no match is found.
     */
    public static String guessNamespace(String iri) {
        try {
            Pattern pattern = Pattern.compile("^(([\\w\\-]+:\\/\\/([\\w\\-_:]+\\.)*[\\w\\-_:]*)(\\/([\\w\\-\\._\\:]+\\/)*))([\\w\\-\\._\\:]+)(\\?[\\w\\-_\\:\\?\\=]+)?((\\#)?([\\w\\-_]+))?$");
            Matcher matcher = pattern.matcher(iri);

            if(matcher.matches()) {
                if(matcher.group(8) == null ) { // If the IRI has no fragment)
                    return matcher.group(1);
                } else {
                    return matcher.group(1) + matcher.group(6) + matcher.group(9);
                }
            } else {
                return "";
            }
        } catch (IllegalStateException e) {
            return "";
        }
    }

    public static String guessLocalName(String iri) {
        try {
            Pattern pattern = Pattern.compile("^(([\\w\\-]+:\\/\\/([\\w\\-_:]+\\.)*[\\w\\-_:]*)(\\/([\\w\\-\\._\\:]+\\/)*))([\\w\\-\\._\\:]+)(\\?[\\w\\-_\\:\\?\\=]+)?((\\#)?([\\w\\-_]+))?$");
            Matcher matcher = pattern.matcher(iri);

            if(matcher.matches()) {
                if(matcher.group(8) == null ) { // If the IRI has no fragment)
                    return matcher.group(6);
                } else {
                    return matcher.group(10);
                }
            } else {
                return "";
            }
        } catch (IllegalStateException e) {
            return "";
        }
    }

    public static boolean isStandardIRI(String iriString) {
            try {
            Pattern pattern = Pattern.compile("^(([^:/?#\\s]+):)(\\/\\/([^/?#\\s]*))?([^?#\\s]*)(\\?([^#\\s]*))?(#(.*))?");
            Matcher matcher = pattern.matcher(iriString);
            return matcher.matches();
        } catch (IllegalStateException e) {
            return false;
        } }
}
