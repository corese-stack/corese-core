package fr.inria.corese.core.next.impl.io.parser.common;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;
import fr.inria.corese.core.next.impl.io.parser.util.ParserConstants;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for RDF parsers (Turtle, TriG) providing common functionality.
 * Implements IRI resolution according to RFC 3986, Unicode escape handling,
 * prefix management, and RDF term creation.
 *
 */
public abstract class AbstractTurtleTriGListener {

    public final Model model;
    public final ValueFactory factory;
    public final Map<String, String> prefixMap = new HashMap<>();
    public String baseURI;

    public Resource currentSubject;
    public IRI currentPredicate;

    /**
     * Constructs a parser listener with the specified model, factory and base URI.
     *
     * @param model   RDF model to populate with parsed statements
     * @param factory factory for creating RDF terms (IRIs, blank nodes, literals)
     * @param baseURI base URI for resolving relative IRI references
     */
    public AbstractTurtleTriGListener(Model model, ValueFactory factory, String baseURI) {
        this.model = model;
        this.factory = factory;
        this.baseURI = baseURI;
        initializeBasePrefix();
    }

    /**
     * Registers the base URI as the empty prefix namespace.
     */
    public void initializeBasePrefix() {
        if (this.baseURI != null && !this.baseURI.isEmpty()) {
            prefixMap.put(ParserConstants.EMPTY_STRING, this.baseURI);
            model.setNamespace(ParserConstants.EMPTY_STRING, this.baseURI);
        }
    }

    /**
     * Extracts IRI from angle brackets and processes Unicode escape sequences.
     *
     * @param text raw IRI text including angle brackets
     * @return unescaped IRI string
     */
    public String extractAndUnescapeIRI(String text) {
        String iri = text.substring(1, text.length() - 1);
        return unescapeIRI(iri);
    }

    /**
     * Updates the base URI and registers it in prefix mappings.
     *
     * @param newBase new base URI to set
     */
    public void updateBaseURI(String newBase) {
        this.baseURI = resolveIRIAgainstBase(newBase);
        prefixMap.put(ParserConstants.EMPTY_STRING, this.baseURI);
        model.setNamespace(ParserConstants.EMPTY_STRING, this.baseURI);
    }

    /**
     * Registers a namespace prefix with its corresponding IRI.
     *
     * @param prefix namespace prefix
     * @param iri    namespace IRI
     */
    public void registerPrefix(String prefix, String iri) {
        String resolvedIRI = resolveIRIAgainstBase(iri);
        prefixMap.put(prefix, resolvedIRI);
        model.setNamespace(prefix, resolvedIRI);
    }

    /**
     * Resolves an IRI reference to absolute form.
     * Handles prefixed names (QNames), relative IRIs, and absolute IRIs.
     *
     * @param raw raw IRI string
     * @return resolved absolute IRI
     * @throws ParsingErrorException if the IRI cannot be resolved
     */
    public String resolveIRI(String raw) {
        try {
            raw = raw.trim();

            if (raw.equals(ParserConstants.A)) {
                return RDF.type.getIRI().stringValue();
            }

            if (raw.equals(ParserConstants.COLON)) {
                String ns = prefixMap.get(ParserConstants.EMPTY_STRING);
                return ns != null ? ns : getEffectiveBaseURI();
            }

            if (raw.startsWith(ParserConstants.IRI_START) && raw.endsWith(ParserConstants.IRI_END)) {
                String iri = raw.substring(1, raw.length() - 1);
                iri = unescapeIRI(iri);
                return iri.isEmpty() ? getEffectiveBaseURI() : resolveIRIAgainstBase(iri);
            }

            if (raw.contains(ParserConstants.COLON)) {
                String[] parts = raw.split(ParserConstants.COLON, 2);
                String prefix = parts[0];
                String localName = parts[1];

                if (prefixMap.containsKey(prefix)) {
                    localName = unescapeIRI(localName);
                    String ns = prefixMap.get(prefix);
                    if (ns != null) {
                        return ns + localName;
                    }
                }

                if (isAbsoluteIRI(raw)) {
                    return raw;
                }

                throw new ParsingErrorException("Undeclared prefix: " + prefix);
            }

            return resolveIRIAgainstBase(raw);

        } catch (IllegalArgumentException e) {
            throw new ParsingErrorException(e.getMessage(), e);
        }
    }

    /**
     * Resolves a relative IRI reference against the base URI using RFC 3986 algorithm.
     *
     * @param iri IRI reference to resolve
     * @return resolved absolute IRI
     */
    public String resolveIRIAgainstBase(String iri) {
        String effectiveBase = getEffectiveBaseURI();

        if (isAbsoluteIRI(iri)) {
            return iri;
        }

        if (iri.isEmpty()) {
            return effectiveBase;
        }

        try {
            URI baseUri = new URI(effectiveBase);
            String baseScheme = baseUri.getScheme();
            String baseAuthority = baseUri.getAuthority();
            String basePath = baseUri.getPath();
            String baseQuery = baseUri.getQuery();

            String[] refParts = parseReference(iri);
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
            return performSimpleFallback(effectiveBase, iri);
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
    public String buildURI(String scheme, String authority, String path, String query, String fragment) {
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
            result.append(ParserConstants.FRAGMENT).append(fragment);
        }
        return normalizeURI(result.toString());
    }

    /**
     * Parses a URI reference into its five components.
     *
     * @param ref URI reference to parse
     * @return array containing [scheme, authority, path, query, fragment]
     */
    public String[] parseReference(String ref) {
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
    public String mergePaths(String basePath, String refPath) {
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
    public String removeDotSegments(String path) {
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
    public void removeLastSegment(StringBuilder output) {
        String outputStr = output.toString();
        int lastSlash = outputStr.lastIndexOf(ParserConstants.SLASH);
        output.setLength(lastSlash >= 0 ? lastSlash : 0);
    }

    /**
     * Provides a fallback resolution mechanism when RFC 3986 parsing fails.
     *
     * @param base     base URI
     * @param relative relative IRI reference
     * @return resolved IRI using simple concatenation rules
     */
    public String performSimpleFallback(String base, String relative) {
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
    public String normalizeURI(String uri) {
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
     * Determines whether an IRI is absolute (contains a valid scheme).
     *
     * @param iri IRI to check
     * @return true if the IRI is absolute, false otherwise
     */
    public boolean isAbsoluteIRI(String iri) {
        if (iri == null || iri.isEmpty()) {
            return false;
        }
        int colonIndex = iri.indexOf(':');
        if (colonIndex == -1 || colonIndex == 0) {
            return false;
        }
        return isValidScheme(iri.substring(0, colonIndex));
    }

    /**
     * Validates a URI scheme according to RFC 3986.
     * A valid scheme must start with a letter and contain only letters, digits, '+', '-', or '.'.
     *
     * @param scheme scheme to validate
     * @return true if the scheme is valid, false otherwise
     */
    public boolean isValidScheme(String scheme) {
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

    /**
     * Returns the effective base URI, using a default if none is set.
     *
     * @return effective base URI
     */
    public String getEffectiveBaseURI() {
        String effective = (baseURI != null && !baseURI.isEmpty()) ? baseURI : ParserConstants.DEFAULT_BASE_URI;
        return normalizeURI(effective);
    }

    /**
     * Processes Unicode escape sequences in IRIs.
     *
     * @param rawIri IRI string potentially containing escape sequences
     * @return unescaped IRI string
     * @throws IllegalArgumentException if escape sequences are malformed or contain surrogates
     */
    public String unescapeIRI(String rawIri) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rawIri.length(); i++) {
            char c = rawIri.charAt(i);
            if (c == '\\' && i + 1 < rawIri.length()) {
                char next = rawIri.charAt(i + 1);
                if (next == 'u' || next == 'U') {
                    int len = (next == 'u') ? 4 : 8;
                    if (i + len + 1 <= rawIri.length()) {
                        String hex = rawIri.substring(i + 2, i + 2 + len);
                        int codePoint = Integer.parseInt(hex, 16);
                        if (codePoint >= 0xD800 && codePoint <= 0xDFFF) {
                            throw new IllegalArgumentException("Surrogates not allowed: \\u" + hex);
                        }
                        sb.appendCodePoint(codePoint);
                        i += len + 1;
                    } else {
                        throw new IllegalArgumentException("Incomplete Unicode escape");
                    }
                } else {
                    sb.append(next);
                    i++;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Processes escape sequences in RDF string literals.
     * Handles standard escapes (\t, \n, \r, etc.) and Unicode escapes.
     *
     * @param text literal string including delimiters (quotes or triple-quotes)
     * @return unescaped string content
     * @throws IllegalArgumentException if escape sequences are malformed
     */
    public String unescapeString(String text) {
        if (text == null || text.length() < 2) {
            return text;
        }

        boolean isMultiline = text.startsWith(ParserConstants.TRIPLE_QUOTE) || text.startsWith(ParserConstants.TRIPLE_APOSTROPHE);
        String content = isMultiline ? text.substring(3, text.length() - 3) : text.substring(1, text.length() - 1);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '\\' && i + 1 < content.length()) {
                char next = content.charAt(i + 1);
                switch (next) {
                    case 't':
                        sb.append('\t');
                        i++;
                        break;
                    case 'n':
                        sb.append('\n');
                        i++;
                        break;
                    case 'r':
                        sb.append('\r');
                        i++;
                        break;
                    case 'b':
                        sb.append('\b');
                        i++;
                        break;
                    case 'f':
                        sb.append('\f');
                        i++;
                        break;
                    case '"':
                        sb.append('"');
                        i++;
                        break;
                    case '\'':
                        sb.append('\'');
                        i++;
                        break;
                    case '\\':
                        sb.append('\\');
                        i++;
                        break;
                    case 'u':
                    case 'U':
                        int len = (next == 'u') ? 4 : 8;
                        if (i + len + 1 <= content.length()) {
                            String hex = content.substring(i + 2, i + 2 + len);
                            int codePoint = Integer.parseInt(hex, 16);
                            if (codePoint >= 0xD800 && codePoint <= 0xDFFF) {
                                throw new IllegalArgumentException("Invalid Unicode escape: Surrogate code points not allowed");
                            }
                            sb.appendCodePoint(codePoint);
                            i += len + 1;
                        } else {
                            throw new IllegalArgumentException("Incomplete Unicode escape sequence");
                        }
                        break;
                    default:
                        sb.append(c).append(next);
                        i++;
                        break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Adds an RDF statement to the model with exception handling.
     * Subclasses may override to support named graphs or other extensions.
     *
     * @param subject   statement subject
     * @param predicate statement predicate
     * @param object    statement object
     * @throws ParsingErrorException if the statement cannot be added
     */
    public void safeAddStatement(Resource subject, IRI predicate, Value object) {
        try {
            model.add(subject, predicate, object);
        } catch (Exception e) {
            throw new ParsingErrorException("Failed to add statement: " + e.getMessage(), e);
        }
    }

    /**
     * Creates an RDF literal with optional language tag or datatype.
     *
     * @param label       literal lexical value
     * @param langTag     language tag (may be null)
     * @param datatypeIRI datatype IRI (may be null)
     * @return RDF literal
     */
    public Literal createLiteral(String label, String langTag, String datatypeIRI) {
        if (langTag != null) {
            return factory.createLiteral(label, langTag);
        }
        if (datatypeIRI != null) {
            return factory.createLiteral(label, factory.createIRI(datatypeIRI));
        }
        return factory.createLiteral(label);
    }

    /**
     * Creates a boolean literal.
     *
     * @param text boolean value as string ("true" or "false")
     * @return boolean literal with xsd:boolean datatype
     */
    public Literal createBooleanLiteral(String text) {
        return factory.createLiteral(text, XSD.BOOLEAN.getIRI());
    }

    /**
     * Creates a numeric literal with appropriate XSD datatype.
     *
     * @param text numeric value as string
     * @param type numeric type (INTEGER, DECIMAL, or DOUBLE)
     * @return numeric literal with corresponding XSD datatype
     */
    public Literal createNumericLiteral(String text, NumericType type) {
        switch (type) {
            case DOUBLE:
                return factory.createLiteral(text, XSD.DOUBLE.getIRI());
            case DECIMAL:
                return factory.createLiteral(text, XSD.DECIMAL.getIRI());
            case INTEGER:
            default:
                return factory.createLiteral(text, XSD.INTEGER.getIRI());
        }
    }

    /**
     * Enumeration of numeric literal types corresponding to XSD datatypes.
     */
    public enum NumericType {
        /**
         * XSD integer type
         */
        INTEGER,
        /**
         * XSD decimal type
         */
        DECIMAL,
        /**
         * XSD double type
         */
        DOUBLE
    }
}