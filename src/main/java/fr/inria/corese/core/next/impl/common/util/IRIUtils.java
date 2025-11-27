package fr.inria.corese.core.next.impl.common.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for IRI.
 * <p>
 * Intended to facilitate string manipulation related to IRI.
 */
public class IRIUtils {

    private static final Pattern IRI_PATTERN = Pattern.compile("^(?<namespace>" +
            "(?<protocol>[\\w\\-]+):(?<dblSlashes>\\/\\/)?" +
            "(?<domain>([\\w\\-_:@]+\\.)*[\\w\\-_:]*))" +
            "((?<path>\\/([\\w\\-\\._\\:]+\\/)*)" +
            "(?<finalPath>[\\w\\-\\._\\:]+)?" +
            "(?<query>\\?[\\w\\-_\\:\\?\\=]+)?" +
            "(?<anchor>(\\#))?" +
            "(?<fragment>([\\w\\-_]+))?)?$");
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
     *
     * @param iri The IRI string to be processed.
     * @return the guessed namespace of the IRI or an empty string if no match is found.
     */
    public static String guessNamespace(String iri) {
        if (isInvalidInput(iri)) {
            return "";
        }
        try {
            Matcher matcher = matchWithTimeout(IRI_PATTERN, iri);
            if (matcher == null || !matcher.matches()) {
                if (iri.endsWith("#")) {
                    return iri;
                } else if (iri.contains("#")) {
                    return iri.substring(0, iri.lastIndexOf("#") + 1);
                } else {
                    return iri;
                }
            } else if (matcher.matches()) {
                if (matcher.group("protocol") != null && matcher.group("protocol").equals("_")) {
                    return "";
                }
                StringBuilder namespace = new StringBuilder();
                namespace.append(matcher.group("protocol")).append(":");
                if (matcher.group("dblSlashes") != null) {
                    namespace.append(matcher.group("dblSlashes"));
                }
                namespace.append(matcher.group("domain"));
                if (matcher.group("path") != null) {
                    namespace.append(matcher.group("path"));
                }
                if((matcher.group("fragment") != null || matcher.group("anchor") != null) && matcher.group("finalPath") != null) {
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
     *
     * @param iri The IRI string to be processed.
     * @return the guessed local name of the IRI or an empty string if no match is found.
     */
    public static String guessLocalName(String iri) {
        if (isInvalidInput(iri)) {
            return "";
        }
        try {
            Matcher matcher = matchWithTimeout(IRI_PATTERN, iri);
            if (matcher == null || !matcher.matches()) {
                return iri;
            } else if (matcher.matches()) {
                if (matcher.group("fragment") != null) { // If the IRI has a fragment
                    return matcher.group("fragment");
                } else if (matcher.group("finalPath") != null) { // If the IRI has no fragment but do not ends with a slash
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
        if (isShortIRI(iriString)) {
            return true;
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
     * Checks if this is a short IRI that should be accepted in lenient mode.
     */
    private static boolean isShortIRI(String iri) {
        return iri != null &&

                iri.length() <= 10 &&
                !iri.contains(":") &&
                !iri.contains("/") &&
                !iri.contains(" ") &&
                iri.matches("[a-zA-Z0-9]+");
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
    private static boolean isInvalidInput(String input) {
        return input == null ||
                input.isEmpty() ||
                input.length() > MAX_IRI_LENGTH ||
                containsSuspiciousPatterns(input);
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

    /**
     * Checks if a character is invalid in an IRI according to RFC
     *
     * @param c the character to validate
     * @return true if the character is forbidden in IRIs
     */
    public static boolean isInvalidIRICharacter(char c) {
        if (c >= 0x00 && c <= 0x1F) {
            return true;
        }

        // DEL (U+007F) - NOT ALLOWED
        if (c == 0x7F) {
            return true;
        }

        // High control characters (U+0080-U+009F) - NOT ALLOWED
        if (c >= 0x80 && c <= 0x9F) {
            return true;
        }

        return switch (c) {
            case '<', '>', '{', '}', '\\', '^', '`', '|', '"' -> true;
            default -> false;
        };
    }

    /**
     * Returns a human-readable description of a character for error messages.
     *
     * @param c the character to describe
     * @return human-readable description
     */
    public static String getCharacterDescription(char c) {
        switch (c) {
            case 0x00:
                return "null character";
            case 0x09:
                return "tab";
            case 0x0A:
                return "line feed";
            case 0x0D:
                return "carriage return";
            case 0x20:
                return "space";
            case 0x7F:
                return "delete";
            case '<':
                return "less than";
            case '>':
                return "greater than";
            case '{':
                return "left curly bracket";
            case '}':
                return "right curly bracket";
            case '\\':
                return "backslash";
            case '^':
                return "circumflex";
            case '`':
                return "grave accent";
            case '|':
                return "pipe";
            case '"':
                return "quotation mark";
            default:
                if (c < 0x20) {
                    return "control character";
                } else if (c >= 0x80 && c <= 0x9F) {
                    return "high control character";
                } else {
                    return String.format("character '%c'", c);
                }
        }
    }

    /**
     * Escapes characters in a string for display in error messages.
     *
     * @param iri the IRI to escape for display
     * @return escaped version suitable for error messages
     */
    public static String escapeForDisplay(String iri) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < iri.length(); i++) {
            char c = iri.charAt(i);
            if (c < 0x20 || (c >= 0x7F && c <= 0x9F)) {
                // Display control characters as Unicode escapes
                sb.append(String.format("\\u%04X", (int) c));
            } else if (c > 0x7E) {
                // Display non-ASCII as Unicode escapes for clarity
                sb.append(String.format("\\u%04X", (int) c));
            } else if (c == '<' || c == '>' || c == '{' || c == '}' || c == '\\' || c == '^' || c == '`' || c == '|' || c == '"') {
                // Display reserved characters with backslash escape
                sb.append('\\').append(c);
            } else {
                // Display normal ASCII characters as-is
                sb.append(c);
            }
        }
        return sb.toString();
    }
}