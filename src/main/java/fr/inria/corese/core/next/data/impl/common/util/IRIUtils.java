package fr.inria.corese.core.next.data.impl.common.util;

import fr.inria.corese.core.next.data.impl.io.parser.util.ParserConstants;

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

    // Example 1 : http://webisa.webdatacommons.org/data/sparql?query=q#line1
    private static final Pattern IRI_PATTERN = Pattern.compile("^(?<rootnamespace>" + // http://webisa.webdatacommons.org
                "(?<protocol>[\\w\\-]+):" + // http:
                "(?<dblSlashes>\\/\\/)?" + // //
                "(?<domain>([\\w\\[\\]\\-_:@]+\\.)*[\\w\\-_:]*)" + // webisa.webdatacommons.org
            ")" +
            "(?<path>\\/([\\w\\-\\._\\:]+\\/)*)*" + // /data/
            "(?<finalPath>[\\w&<>;\\-\\._\\:\\\"\\\']+)?" + // sparql
            "(?<query>\\?[\\w\\-\\\"\\\'_\\:\\?\\=]+)?" + // ?query=q
            "(?<anchor>\\#)?" + // #
            "(?<fragment>[\\w\\-_]+)?" + // line1
            "$"
    );
    private static final Pattern ABSOLUTE_IRI_PATTERN = Pattern.compile("^(?<root>(" +
            "?<protocol>[\\w\\-]+)" +
            ":(\\/\\/)?" +
            "(?<path>([\\S\\-\\._\\:]+[\\/\\.\\:\\@\\-])?)+" +
            "(?<query>\\?[\\S\\-\\\"\\'_\\:\\?\\=]+)?)" +
            "(?<fragment>[\\S\\-_]+)?" +
            "$");
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
                // This is a blank node
                if (matcher.group("protocol") != null && matcher.group("protocol").equals("_")) {
                    return "";
                }

                StringBuilder namespace = new StringBuilder();
                namespace.append(matcher.group("rootnamespace"));
                if (matcher.group("path") != null) {
                    namespace.append(matcher.group("path"));
                }
                if(matcher.group("anchor") != null) {
                    if(matcher.group("finalPath") != null) {
                        namespace.append(matcher.group("finalPath"));
                    }
                    namespace.append(matcher.group("anchor"));
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
     * Detects if an IRI is absolute according to the REGEX given in the recommendation RFC3987
     * @param iri any uri (expecting to be the content between < and >
     * @return true if it is compliant with RFC3987. May accept the prefixed for of uri, as there is no way to
     * distinguish a prefix from a protocol
     */
    public static boolean isAbsoluteIRI(String iri) {
        Matcher matcher = matchWithTimeout(ABSOLUTE_IRI_PATTERN, iri);
        if (matcher == null || !matcher.matches()) {
            return false;
        }
        return matcher.matches();
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

    public static String resolveIRIAgainstBase(String baseIri, String relativePath) {

        if (relativePath.isEmpty()) {
            return baseIri;
        }

        try {
            URI baseUri = new URI(baseIri);
            String baseScheme = baseUri.getScheme();
            String baseAuthority = baseUri.getAuthority();
            String basePath = baseUri.getPath();
            String baseQuery = baseUri.getQuery();

            String[] refParts = parseReference(relativePath);
            String refScheme = refParts[0];
            String refAuthority = refParts[1];
            String refPath = refParts[2];
            String refQuery = refParts[3];
            String refFragment = refParts[4];

            String targetScheme, targetAuthority, targetPath, targetQuery, targetFragment;

            // RFC 3986 Section 5.2.2 - Reference Resolution Algorithm
            if (refScheme != null) {
                targetScheme = refScheme;
                targetAuthority = refAuthority;
                targetPath = removeDotSegments(refPath);
                targetQuery = refQuery;
            } else {
                if (refAuthority != null) {
                    targetScheme = baseScheme;
                    targetAuthority = refAuthority;
                    targetPath = removeDotSegments(refPath);
                    targetQuery = refQuery;
                } else {
                    targetScheme = baseScheme;
                    targetAuthority = baseAuthority;
                    if (refPath.isEmpty()) {
                        targetPath = basePath;
                        targetQuery = refQuery != null ? refQuery : baseQuery;
                    } else {
                        if (refPath.startsWith(ParserConstants.SLASH)) {
                            targetPath = removeDotSegments(refPath);
                        } else {
                            targetPath = removeDotSegments(mergePaths(basePath, refPath));
                        }
                        targetQuery = refQuery;
                    }
                }
            }
            targetFragment = refFragment;

            return buildURI(targetScheme, targetAuthority, targetPath, targetQuery, targetFragment);

        } catch (URISyntaxException e) {
            return performSimpleFallback(baseIri, relativePath);
        }
    }

    /**
     * Constructs a URI from its components.
     *
     * @param scheme    URI scheme (e.g., "http", "file")
     * @param authority authority component (host, port, userinfo)
     * @param path      path component
     * @param query     query component
     * @param fragment  fragment identifier
     * @return normalized URI string
     */
    private static  String buildURI(String scheme, String authority, String path, String query, String fragment) {
        StringBuilder result = new StringBuilder();
        if (scheme != null) {
            result.append(scheme).append(ParserConstants.COLON);
        }
        if (authority != null) {
            result.append(ParserConstants.DOUBLE_SLASH).append(authority);
        }
        if (path != null) {
            result.append(path);
        }
        if (query != null) {
            result.append(ParserConstants.QUERY_MARK).append(query);
        }
        if (fragment != null) {
            result.append(ParserConstants.HASH).append(fragment);
        }
        return normalizeURI(result.toString());
    }

    /**
     * Parses a URI reference into its five components.
     *
     * @param ref URI reference to parse
     * @return array containing [scheme, authority, path, query, fragment]
     */
    private static String[] parseReference(String ref) {
        String[] parts = new String[5];
        String remaining = ref;

        int fragmentIndex = remaining.indexOf('#');
        if (fragmentIndex >= 0) {
            parts[4] = remaining.substring(fragmentIndex + 1);
            remaining = remaining.substring(0, fragmentIndex);
        }

        int queryIndex = remaining.indexOf('?');
        if (queryIndex >= 0) {
            parts[3] = remaining.substring(queryIndex + 1);
            remaining = remaining.substring(0, queryIndex);
        }

        int colonIndex = remaining.indexOf(':');
        if (colonIndex > 0 && isValidScheme(remaining.substring(0, colonIndex))) {
            parts[0] = remaining.substring(0, colonIndex);
            remaining = remaining.substring(colonIndex + 1);
        }

        if (remaining.startsWith(ParserConstants.DOUBLE_SLASH)) {
            int authorityEnd = remaining.indexOf('/', 2);
            if (authorityEnd < 0) {
                authorityEnd = remaining.length();
            }
            parts[1] = remaining.substring(2, authorityEnd);
            remaining = remaining.substring(authorityEnd);
        }

        parts[2] = remaining;
        return parts;
    }

    /**
     * Merges a base path with a relative path.
     *
     * @param basePath base path from base URI
     * @param refPath  relative path from reference
     * @return merged path
     */
    private static String mergePaths(String basePath, String refPath) {
        if (basePath == null || basePath.isEmpty()) {
            return ParserConstants.SLASH + refPath;
        }
        int lastSlash = basePath.lastIndexOf('/');
        return lastSlash >= 0 ? basePath.substring(0, lastSlash + 1) + refPath : refPath;
    }

    /**
     * Removes dot segments from a path (RFC 3986 Section 5.2.4).
     * Processes ".." and "." segments according to the normalization algorithm.
     *
     * @param path path to normalize
     * @return normalized path without dot segments
     */
    private static String removeDotSegments(String path) {
        if (path == null || path.isEmpty()) {
            return ParserConstants.EMPTY_STRING;
        }

        String input = path;
        StringBuilder output = new StringBuilder();

        while (!input.isEmpty()) {
            if (input.startsWith(ParserConstants.DOUBLE_DOT + ParserConstants.SLASH)) {
                input = input.substring(3);
            } else if (input.startsWith(ParserConstants.DOT + ParserConstants.SLASH)) {
                input = input.substring(2);
            } else if (input.startsWith(ParserConstants.SLASH + ParserConstants.DOT + ParserConstants.SLASH)) {
                input = ParserConstants.SLASH + input.substring(3);
            } else if (input.equals(ParserConstants.SLASH + ParserConstants.DOT)) {
                input = ParserConstants.SLASH;
            } else if (input.startsWith(ParserConstants.SLASH + ParserConstants.DOUBLE_DOT + ParserConstants.SLASH)) {
                input = ParserConstants.SLASH + input.substring(4);
                removeLastSegment(output);
            } else if (input.equals(ParserConstants.SLASH + ParserConstants.DOUBLE_DOT)) {
                input = ParserConstants.SLASH;
                removeLastSegment(output);
            } else if (input.equals(ParserConstants.POINT) || input.equals(ParserConstants.DOUBLE_DOT)) {
                input = ParserConstants.EMPTY_STRING;
            } else {
                int nextSlash;
                if (input.startsWith(ParserConstants.SLASH)) {
                    nextSlash = input.indexOf(ParserConstants.SLASH, 1);
                    if (nextSlash >= 0) {
                        output.append(input, 0, nextSlash);
                        input = input.substring(nextSlash);
                    } else {
                        output.append(input);
                        input = ParserConstants.EMPTY_STRING;
                    }
                } else {
                    nextSlash = input.indexOf(ParserConstants.SLASH);
                    if (nextSlash >= 0) {
                        output.append(input, 0, nextSlash);
                        input = input.substring(nextSlash);
                    } else {
                        output.append(input);
                        input = ParserConstants.EMPTY_STRING;
                    }
                }
            }
        }

        return output.toString();
    }

    /**
     * Removes the last path segment from the output buffer.
     * Used during dot segment removal when processing ".." segments.
     *
     * @param output string builder containing the path being constructed
     */
    private static void removeLastSegment(StringBuilder output) {
        String outputStr = output.toString();
        int lastSlash = outputStr.lastIndexOf(ParserConstants.SLASH);
        output.setLength(Math.max(lastSlash, 0));
    }

    /**
     * Provides a fallback resolution mechanism when RFC 3986 parsing fails.
     *
     * @param base     base URI
     * @param relative relative IRI reference
     * @return resolved IRI using simple concatenation rules
     */
    private static String performSimpleFallback(String base, String relative) {
        if (relative.isEmpty()) {
            return base;
        }
        if (base.endsWith(ParserConstants.SLASH)) {
            return base + relative;
        }
        int lastSlash = base.lastIndexOf('/');
        return lastSlash >= 0 ? base.substring(0, lastSlash + 1) + relative : base + ParserConstants.SLASH + relative;
    }

    /**
     * Normalizes URI strings, ensuring proper format for file:// URIs.
     *
     * @param uri URI to normalize
     * @return normalized URI string
     */
    public static String normalizeURI(String uri) {
        if (uri == null) {
            return null;
        }
        if (uri.startsWith(ParserConstants.FILE_PROTOCOL_SIMPLE) && !uri.startsWith(ParserConstants.FILE_PROTOCOL_TRIPLE_SLASH)) {
            if (!uri.startsWith(ParserConstants.FILE_PROTOCOL)) {
                uri = uri.replace(ParserConstants.FILE_PROTOCOL_SIMPLE, ParserConstants.FILE_PROTOCOL_TRIPLE_SLASH);
            }
        }
        return uri;
    }

    /**
     * Validates a URI scheme according to RFC 3986.
     * A valid scheme must start with a letter and contain only letters, digits, '+', '-', or '.'.
     *
     * @param scheme scheme to validate
     * @return true if the scheme is valid, false otherwise
     */
    public static boolean isValidScheme(String scheme) {
        if (scheme == null || scheme.isEmpty() || !Character.isLetter(scheme.charAt(0))) {
            return false;
        }
        for (int i = 1; i < scheme.length(); i++) {
            char c = scheme.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '+' && c != '-' && c != '.') {
                return false;
            }
        }
        return true;
    }

}