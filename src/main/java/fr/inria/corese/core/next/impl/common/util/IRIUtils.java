package fr.inria.corese.core.next.impl.common.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
    private static final int MAX_IRI_LENGTH = 2048;
    private static final long REGEX_TIMEOUT_MS = 100;


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
        if (!isValidInput(iri)) {
            return "";
        }
        try {
            Matcher matcher = matchWithTimeout(IRI_PATTERN, iri);
            if (matcher == null || !matcher.matches()) {
                return "";
            } else if (matcher.matches()) {
                if (matcher.group("protocol") != null && matcher.group("protocol").equals("_")) {
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
        if (!isValidInput(iri)) {
            return "";
        }
        try {
            Matcher matcher = matchWithTimeout(IRI_PATTERN, iri);
            if (matcher == null || !matcher.matches()) {
                return "";
            } else if (matcher.matches()) {
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
        if (!isValidInput(iriString)) {
            return false;
        }

        try {
            Matcher matcher = matchWithTimeout(STANDARD_IRI_PATTERN, iriString);
            if (matcher != null && matcher.matches()) {
                return isValidURI(iriString);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Executes regex matching with timeout protection.
     */
    private static Matcher matchWithTimeout(Pattern pattern, String input) {
        long startTime = System.nanoTime();

        try {
            Matcher matcher = pattern.matcher(input);

            // Check timeout before and during matching
            if (System.nanoTime() - startTime > TimeUnit.MILLISECONDS.toNanos(REGEX_TIMEOUT_MS)) {
                return null;
            }

            // For very long strings, check timeout periodically
            if (input.length() > 100) {
                // Pre-check timeout
                if (System.nanoTime() - startTime > TimeUnit.MILLISECONDS.toNanos(REGEX_TIMEOUT_MS / 2)) {
                    return null;
                }
            }

            return matcher;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Validates input string for basic security checks.
     */
    private static boolean isValidInput(String input) {
        return input != null &&
                !input.isEmpty() &&
                input.length() <= MAX_IRI_LENGTH &&
                !containsSuspiciousPatterns(input);
    }

    /**
     * Checks for patterns that might cause ReDoS attacks.
     */
    private static boolean containsSuspiciousPatterns(String input) {
        final Set<Character> SUSPICIOUS_CHARS = Set.of('.', '-', '_', ':');
        int consecutiveRepeats = 0;
        char lastChar = 0;

        for (char c : input.toCharArray()) {
            if (c == lastChar && SUSPICIOUS_CHARS.contains(c)) {
                if (++consecutiveRepeats > 10) {
                    return true;
                }
            } else {
                consecutiveRepeats = 0;
            }
            lastChar = c;
        }
        return false;
    }

    /**
     * Additional validation using Java's URI class.
     */
    private static boolean isValidURI(String uriString) {
        try {
            URI uri = new URI(uriString);
            return uri.getScheme() != null && uri.getScheme().length() > 0;
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
