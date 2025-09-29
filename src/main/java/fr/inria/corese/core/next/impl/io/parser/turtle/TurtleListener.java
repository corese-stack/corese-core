package fr.inria.corese.core.next.impl.io.parser.turtle;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.api.io.parser.RDFParserBaseIRIOptions;
import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;
import fr.inria.corese.core.next.impl.io.parser.util.ParserConstants;
import fr.inria.corese.core.next.impl.parser.antlr.TurtleBaseListener;
import fr.inria.corese.core.next.impl.parser.antlr.TurtleParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Listener for the ANTLR4 generated parser for Turtle.
 */
public class TurtleListener extends TurtleBaseListener {

    private final Model model;
    private String baseURI;
    private final Map<String, String> prefixMap = new HashMap<>();
    private final ValueFactory factory;

    private Resource currentSubject;
    private IRI currentPredicate;

    /**
     * Constructor for TurtleListener that initializes the model, value factory,
     * and configuration options.
     *
     * @param model   the model to be populated by the parser
     * @param factory the value factory used to create RDF values
     * @param options optional configuration options for the parser
     */
    public TurtleListener(Model model, ValueFactory factory, IOOptions options) {
        this.model = model;
        this.factory = factory;

        this.baseURI = null;

        if (options instanceof RDFParserBaseIRIOptions) {
            RDFParserBaseIRIOptions baseIRIOptions = (RDFParserBaseIRIOptions) options;
            this.baseURI = baseIRIOptions.getBase();
        }

        if (this.baseURI == null || this.baseURI.isEmpty()) {
            this.baseURI = ParserConstants.EMPTY_STRING;
        }

        if (this.baseURI != null && !this.baseURI.isEmpty()) {
            prefixMap.put(ParserConstants.EMPTY_STRING, this.baseURI);
            model.setNamespace(ParserConstants.EMPTY_STRING, this.baseURI);
        }
    }

    /**
     * Constructor for TurtleListener that initializes the model and value
     * factory.
     *
     * @param ctx The parse tree context for the {@code prefixID} rule,
     *            which provides access to the parsed prefix name and IRI reference tokens.
     */
    @Override
    public void exitPrefixID(TurtleParser.PrefixIDContext ctx) {
        String prefix = ctx.PNAME_NS().getText();
        String iri = ctx.IRIREF().getText();
        prefix = prefix.substring(0, prefix.length() - 1);
        iri = iri.substring(1, iri.length() - 1);
        iri = unescapeIRI(iri);

        String resolvedIRI = resolveIRIAgainstBase(iri);

        prefixMap.put(prefix, resolvedIRI);
        model.setNamespace(prefix, resolvedIRI);
    }

    /**
     * Processes an `@base` directive and sets the base URI for relative IRI
     * resolution.
     *
     * @param ctx The parse tree context for the base directive.
     */
    @Override
    public void exitBase(TurtleParser.BaseContext ctx) {
        if (ctx.IRIREF() != null) {
            String newBase = ctx.IRIREF().getText();
            newBase = newBase.substring(1, newBase.length() - 1);
            newBase = unescapeIRI(newBase);

            this.baseURI = resolveIRIAgainstBase(newBase);

            prefixMap.put(ParserConstants.EMPTY_STRING, this.baseURI);
            model.setNamespace(ParserConstants.EMPTY_STRING, this.baseURI);
        }
    }

    /**
     * Processes a SPARQL `BASE` declaration.
     *
     * @param ctx The parse tree context for the SPARQL base declaration.
     */
    @Override
    public void exitSparqlBase(TurtleParser.SparqlBaseContext ctx) {
        String newBase = ctx.IRIREF().getText();
        newBase = newBase.substring(1, newBase.length() - 1);
        newBase = unescapeIRI(newBase);

        this.baseURI = resolveIRIAgainstBase(newBase);

        prefixMap.put(ParserConstants.EMPTY_STRING, this.baseURI);
        model.setNamespace(ParserConstants.EMPTY_STRING, this.baseURI);
    }

    /**
     * Processes a SPARQL `PREFIX` declaration.
     *
     * @param ctx The parse tree context for the SPARQL prefix declaration.
     */
    @Override
    public void exitSparqlPrefix(TurtleParser.SparqlPrefixContext ctx) {
        String prefix = ctx.PNAME_NS().getText();
        String iri = ctx.IRIREF().getText();
        prefix = prefix.substring(0, prefix.length() - 1);
        iri = iri.substring(1, iri.length() - 1);
        iri = unescapeIRI(iri);

        String resolvedIRI = resolveIRIAgainstBase(iri);

        prefixMap.put(prefix, resolvedIRI);
        model.setNamespace(prefix, resolvedIRI);
    }

    /**
     * Enters a `triples` rule, processes the subject, and then calls the
     * predicate-object list processing. This is the main entry point for
     * parsing a complete triple or triple-like structure.
     *
     * @param ctx The parse tree context for the triples.
     */
    @Override
    public void enterTriples(TurtleParser.TriplesContext ctx) {
        try {
            if (ctx.subject() != null) {
                currentSubject = extractSubject(ctx.subject());
                if (ctx.predicateObjectList() != null) {
                    processPredicateObjectList(ctx.predicateObjectList());
                }
            } else if (ctx.blankNodePropertyList() != null) {
                currentSubject = processBlankNodePropertyList(ctx.blankNodePropertyList());

                if (ctx.predicateObjectList() != null) {
                    processPredicateObjectList(ctx.predicateObjectList());
                }
            } else {
                throw new ParsingErrorException("Missing subject in triple.");
            }
        } catch (ParsingErrorException e) {
            throw e;
        } catch (Exception e) {
            throw new ParsingErrorException("Error processing triples: " + e.getMessage(), e);
        }
    }


    /**
     * Adds a subject-predicate-object triple to the model, with exception handling.
     *
     * @param subject   The subject of the triple.
     * @param predicate The predicate of the triple.
     * @param object    The object of the triple.
     */
    private void safeAddStatement(Resource subject, IRI predicate, Value object) {
        try {
            model.add(subject, predicate, object);
        } catch (Exception e) {
            throw new ParsingErrorException("Failed to add statement to model: " + e.getMessage(), e);
        }
    }

    /**
     * Resolves the IRI from a raw string, handling prefixed names and base URIs.
     *
     * @param raw the raw string to resolve
     * @return the resolved IRI as a string
     */
    private String resolveIRI(String raw) throws ParsingErrorException {
        try {
            raw = raw.trim();
            if (raw.equals(ParserConstants.A)) {
                return RDF.type.getIRI().stringValue();
            }

            if (raw.startsWith(ParserConstants.IRI_START) && raw.endsWith(ParserConstants.IRI_END)) {
                String iri = raw.substring(1, raw.length() - 1);
                iri = unescapeIRI(iri);
                return resolveIRIAgainstBase(iri);
            }

            if (raw.equals(ParserConstants.COLON)) {
                if (prefixMap.containsKey(ParserConstants.EMPTY_STRING)) {
                    String ns = prefixMap.get(ParserConstants.EMPTY_STRING);
                    if (ns != null && !ns.isEmpty()) {
                        return ns;
                    }
                }
                if (baseURI != null && !baseURI.isEmpty()) {
                    return baseURI;
                } else {
                    return ParserConstants.DEFAULT_BASE_URI;
                }
            }

            if (raw.contains(ParserConstants.COLON)) {
                String[] parts = raw.split(ParserConstants.COLON, 2);
                String prefix = parts[0];
                String localName = parts[1];

                if (prefix.startsWith(ParserConstants.DOT) || prefix.contains(ParserConstants.SPACE) || prefix.contains(ParserConstants.TAB)) {
                    throw new ParsingErrorException("Invalid prefix in prefixed name: '" + prefix + "' in '" + raw + "'");
                }

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

                throw new ParsingErrorException("Undeclared prefix: " + prefix + " in '" + raw + "'");
            }

            if (raw.startsWith(ParserConstants.COLON) && raw.length() > 1) {
                String localName = raw.substring(1);
                if (prefixMap.containsKey(ParserConstants.EMPTY_STRING)) {
                    String ns = prefixMap.get(ParserConstants.EMPTY_STRING);
                    return ns + localName;
                } else if (baseURI != null && !baseURI.isEmpty()) {
                    return baseURI + localName;
                } else {
                    return ParserConstants.DEFAULT_BASE_URI + localName;
                }
            }

            if (raw.startsWith(ParserConstants.DOT) || raw.contains(ParserConstants.SPACE) || raw.contains(ParserConstants.TAB) || raw.contains(ParserConstants.LINE_FEED)) {
                throw new ParsingErrorException("Invalid IRI format: '" + raw + "'");
            }

            if (raw.matches(ParserConstants.PNAME_NS_PATTERN)) {
                return resolveIRIAgainstBase(raw);
            }

            if (!raw.contains(ParserConstants.SPACE) && !raw.contains(ParserConstants.TAB) && !raw.contains(ParserConstants.LINE_FEED)) {
                return resolveIRIAgainstBase(raw);
            }

            throw new ParsingErrorException("Invalid IRI format: '" + raw + "'");

        } catch (ParsingErrorException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new ParsingErrorException("Invalid IRI: " + raw + " - " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ParsingErrorException("Error resolving IRI: " + raw + " - " + e.getMessage(), e);
        }
    }

    /**
     * Determines if a given string represents an absolute IRI.
     * This method handles both standard absolute IRIs and potential false positives
     * from prefixed names.
     *
     * @param iri The string to check.
     * @return true if the string is an absolute IRI, false otherwise.
     */
    private boolean isAbsoluteIRI(String iri) {
        if (!iri.contains(ParserConstants.COLON)) {
            return false;
        }

        int colonPos = iri.indexOf(':');
        if (colonPos == 0) {
            return false;
        }

        String potentialScheme = iri.substring(0, colonPos);

        if (!potentialScheme.matches(ParserConstants.PNAME_NS_PATTERN)) {
            return false;
        }

        String lowerScheme = potentialScheme.toLowerCase();
        return lowerScheme.equals("http") || lowerScheme.equals("https") ||
                lowerScheme.equals("ftp") || lowerScheme.equals("file") ||
                lowerScheme.equals("urn") || lowerScheme.equals("data") ||
                lowerScheme.equals("mailto") || lowerScheme.equals("tel") ||
                (potentialScheme.length() > 2 && iri.length() > colonPos + 1);
    }

    /**
     * Resolves a potentially relative IRI against the current base URI.
     * The resolution logic follows the rules of RFC 3986 for resolving relative
     * references against a base URI.
     *
     * @param iri The IRI to resolve.
     * @return The fully resolved, absolute IRI string.
     */
    private String resolveIRIAgainstBase(String iri) {
        if (isAbsoluteIRI(iri)) {
            return iri;
        }

        String effectiveBase = getEffectiveBaseURI();
        return resolveReference(effectiveBase, iri);
    }

    /**
     * RFC 3986 Section 5.2.2 - Transform references.
     * This method implements the reference resolution algorithm for an IRI.
     *
     * @param baseUri   The base URI.
     * @param reference The reference IRI.
     * @return The resolved target IRI.
     */
    private String resolveReference(String baseUri, String reference) {
        // Parse the base URI components: [scheme, authority, path, query, fragment]
        String[] base = parseURI(baseUri);
        String[] ref = parseURI(reference);
        String[] target = new String[5];

        // RFC 3986 Section 5.2.2 Algorithm
        if (ref[0] != null) { // ref.scheme
            target[0] = ref[0]; // scheme
            target[1] = ref[1]; // authority
            target[2] = removeDotSegments(ref[2]); // path
            target[3] = ref[3]; // query
        } else {
            if (ref[1] != null) { // ref.authority
                target[1] = ref[1]; // authority
                target[2] = removeDotSegments(ref[2]); // path
                target[3] = ref[3]; // query
            } else {
                if (ref[2] == null || ref[2].isEmpty()) { // ref.path
                    target[2] = base[2]; // path
                    if (ref[3] != null) { // ref.query
                        target[3] = ref[3]; // query
                    } else {
                        target[3] = base[3]; // base.query
                    }
                } else {
                    if (ref[2].startsWith(ParserConstants.SLASH)) {
                        target[2] = removeDotSegments(ref[2]);
                    } else {
                        target[2] = merge(base[2], ref[2]);
                        target[2] = removeDotSegments(target[2]);
                    }
                    target[3] = ref[3]; // query
                }
                target[1] = base[1]; // authority
            }
            target[0] = base[0]; // scheme
        }
        target[4] = ref[4]; // fragment

        return recomposeURI(target);
    }

    /**
     * RFC 3986 Section 5.2.3 - Merge paths.
     *
     * @param basePath The base path.
     * @param path     The path to merge.
     * @return The merged path.
     */
    private String merge(String basePath, String path) {
        if (basePath == null || basePath.isEmpty()) {
            return ParserConstants.SLASH + path;
        }

        int lastSlash = basePath.lastIndexOf('/');
        if (lastSlash >= 0) {
            return basePath.substring(0, lastSlash + 1) + path;
        } else {
            return path;
        }
    }

    /**
     * RFC 3986 Section 5.2.4 - Remove dot segments ('.', '..').
     *
     * @param path The path to clean.
     * @return The path without dot segments.
     */
    private String removeDotSegments(String path) {
        if (path == null) {
            return null;
        }

        StringBuilder input = new StringBuilder(path);
        StringBuilder output = new StringBuilder();

        while (input.length() > 0) {
            String inputStr = input.toString();

            // A: If the input buffer begins with "../" or "./",
            if (inputStr.startsWith("../")) {
                input.delete(0, 3);
            } else if (inputStr.startsWith("./")) {
                input.delete(0, 2);
            }
            // B: If the input buffer begins with "/./" or "/.",
            else if (inputStr.startsWith("/./")) {
                input.replace(0, 3, ParserConstants.SLASH);
            } else if (inputStr.equals("/.")) {
                input.replace(0, 2, ParserConstants.SLASH);
            }
            // C: If the input buffer begins with "/../" or "/..",
            else if (inputStr.startsWith("/../")) {
                input.replace(0, 4, ParserConstants.SLASH);
                removeLastSegment(output);
            } else if (inputStr.equals("/..")) {
                input.replace(0, 3, ParserConstants.SLASH);
                removeLastSegment(output);
            }
            // D: If the input buffer consists only of "." or "..",
            else if (inputStr.equals(".") || inputStr.equals("..")) {
                input.setLength(0);
            }
            // E: Move the first path segment from the input buffer to the end
            else {
                int nextSlash = inputStr.indexOf('/', 1);
                if (nextSlash == -1) {
                    output.append(inputStr);
                    input.setLength(0);
                } else {
                    output.append(inputStr, 0, nextSlash);
                    input.delete(0, nextSlash);
                }
            }
        }

        return output.toString();
    }

    /**
     * Removes the last segment from the output buffer for dot segment removal.
     *
     * @param output The output StringBuilder.
     */
    private void removeLastSegment(StringBuilder output) {
        if (output.length() > 0) {
            int lastSlash = output.lastIndexOf(ParserConstants.SLASH);
            if (lastSlash >= 0) {
                output.setLength(lastSlash);
            } else {
                output.setLength(0);
            }
        }
    }

    /**
     * Parses the URI components and returns them as an array: [scheme, authority, path, query, fragment].
     *
     * @param uri The URI to parse.
     * @return An array of strings containing the URI components.
     */
    private String[] parseURI(String uri) {
        String[] components = new String[5];

        if (uri == null || uri.isEmpty()) {
            return components;
        }

        String remaining = uri;

        int fragmentIndex = remaining.indexOf('#');
        if (fragmentIndex >= 0) {
            components[4] = remaining.substring(fragmentIndex + 1);
            remaining = remaining.substring(0, fragmentIndex);
        }

        int queryIndex = remaining.indexOf('?');
        if (queryIndex >= 0) {
            components[3] = remaining.substring(queryIndex + 1);
            remaining = remaining.substring(0, queryIndex);
        }

        int schemeIndex = remaining.indexOf(':');
        if (schemeIndex > 0 && isValidScheme(remaining.substring(0, schemeIndex))) {
            components[0] = remaining.substring(0, schemeIndex);
            remaining = remaining.substring(schemeIndex + 1);
        }

        if (remaining.startsWith(ParserConstants.DOUBLE_SLASH)) {
            int pathIndex = remaining.indexOf('/', 2);
            if (pathIndex >= 0) {
                components[1] = remaining.substring(2, pathIndex);
                components[2] = remaining.substring(pathIndex);
            } else {
                components[1] = remaining.substring(2);
                components[2] = ParserConstants.EMPTY_STRING;
            }
        } else {
            components[2] = remaining;
        }

        return components;
    }

    /**
     * Checks if a scheme is valid according to RFC 3986.
     *
     * @param scheme The scheme to check.
     * @return true if the scheme is valid, false otherwise.
     */
    private boolean isValidScheme(String scheme) {
        if (scheme == null || scheme.isEmpty()) {
            return false;
        }

        char first = scheme.charAt(0);
        if (!Character.isLetter(first)) {
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
     * Recomposes a URI from the array of components: [scheme, authority, path, query, fragment].
     *
     * @param components The array of URI components.
     * @return The recomposed URI.
     */
    private String recomposeURI(String[] components) {
        StringBuilder result = new StringBuilder();

        if (components[0] != null) { // scheme
            result.append(components[0]).append(':');
        }

        if (components[1] != null) { // authority
            result.append(ParserConstants.DOUBLE_SLASH).append(components[1]);
        }

        if (components[2] != null) { // path
            result.append(components[2]);
        }

        if (components[3] != null) { // query
            result.append('?').append(components[3]);
        }

        if (components[4] != null) { // fragment
            result.append('#').append(components[4]);
        }

        return result.toString();
    }


    /**
     * Gets the effective base URI, using a default value if not set.
     *
     * @return The effective base URI.
     */
    private String getEffectiveBaseURI() {
        String effective = (baseURI != null && !baseURI.isEmpty()) ? baseURI : ParserConstants.DEFAULT_BASE_URI;

        if (effective.startsWith(ParserConstants.FILE_PROTOCOL_SIMPLE) && !effective.startsWith(ParserConstants.FILE_PROTOCOL_TRIPLE_SLASH)) {
            effective = effective.replaceFirst(ParserConstants.FILE_PROTOCOL_SIMPLE, ParserConstants.FILE_PROTOCOL_TRIPLE_SLASH);
        }

        return effective;
    }

    /**
     * Unescapes escape sequences in an IRI.
     *
     * @param rawIri The raw IRI.
     * @return The unescaped IRI.
     * @throws ParsingErrorException if an escape sequence is invalid.
     */
    private String unescapeIRI(String rawIri) throws ParsingErrorException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rawIri.length(); i++) {
            char c = rawIri.charAt(i);
            if (c == '\\') {
                if (i + 1 < rawIri.length()) {
                    char next = rawIri.charAt(i + 1);
                    if (next == 'u' || next == 'U') {
                        int len = (next == 'u') ? 4 : 8;
                        if (i + len + 1 <= rawIri.length()) {
                            try {
                                String hex = rawIri.substring(i + 2, i + 2 + len);
                                int codePoint = Integer.parseInt(hex, 16);

                                if (codePoint >= 0xD800 && codePoint <= 0xDFFF) {
                                    throw new ParsingErrorException("Surrogates not allowed in IRIREF: \\u" + hex);
                                }

                                sb.appendCodePoint(codePoint);
                                i += len + 1;
                            } catch (NumberFormatException e) {
                                throw new ParsingErrorException("Invalid hexadecimal value in IRI escape.", e);
                            }
                        } else {
                            throw new ParsingErrorException("Incomplete Unicode escape in IRI.");
                        }
                    } else {
                        sb.append(next);
                        i++;
                    }
                } else {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Unescapes escape sequences in a string literal.
     *
     * @param text The string to unescape.
     * @return The unescaped string.
     * @throws ParsingErrorException if an escape sequence is invalid.
     */
    private String unescapeString(String text) throws ParsingErrorException {
        if (text == null || text.length() < 2) {
            return text;
        }

        boolean isMultiline = text.startsWith(ParserConstants.TRIPLE_QUOTE) || text.startsWith(ParserConstants.TRIPLE_APOSTROPHE);
        String content;
        if (isMultiline) {
            content = text.substring(3, text.length() - 3);
        } else {
            content = text.substring(1, text.length() - 1);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '\\' && i + 1 < content.length()) {
                char next = content.charAt(i + 1);
                switch (next) {
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
                            try {
                                String hex = content.substring(i + 2, i + 2 + len);
                                int codePoint = Integer.parseInt(hex, 16);

                                if (codePoint >= 0xD800 && codePoint <= 0xDFFF) {
                                    throw new ParsingErrorException("Surrogates not allowed in STRING_LITERAL_QUOTE: \\u" + hex);
                                }

                                if (codePoint > 0x10FFFF) {
                                    throw new ParsingErrorException("Invalid Unicode code point: \\u" + hex);
                                }

                                sb.appendCodePoint(codePoint);
                                i += len + 1;
                            } catch (NumberFormatException e) {
                                throw new ParsingErrorException("Invalid Unicode escape sequence: Invalid hexadecimal value.", e);
                            }
                        } else {
                            throw new ParsingErrorException("Incomplete Unicode escape sequence.");
                        }
                        break;
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
                    default:
                        sb.append(next);
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
     * Extracts a literal from the parser context.
     * Handles different literal types (strings, booleans, numeric).
     *
     * @param ctx The literal context.
     * @return The literal created by the factory.
     * @throws ParsingErrorException if the literal is unsupported or invalid.
     */
    private Literal extractLiteral(TurtleParser.LiteralContext ctx) throws ParsingErrorException {
        try {
            if (ctx.rdfLiteral() != null) {
                String label = unescapeString(ctx.rdfLiteral().string().getText());
                if (ctx.rdfLiteral().LANGTAG() != null) {
                    return factory.createLiteral(label, ctx.rdfLiteral().LANGTAG().getText().substring(1));
                }
                if (ctx.rdfLiteral().iri() != null) {
                    return factory.createLiteral(label, factory.createIRI(resolveIRI(ctx.rdfLiteral().iri().getText())));
                }
                return factory.createLiteral(label);
            }

            if (ctx.BooleanLiteral() != null) {
                return factory.createLiteral(ctx.BooleanLiteral().getText(), XSD.BOOLEAN.getIRI());
            }

            if (ctx.numericLiteral() != null) {
                String numericText = ctx.numericLiteral().getText();

                if (ctx.numericLiteral().DOUBLE() != null) {
                    return factory.createLiteral(numericText, XSD.DOUBLE.getIRI());
                } else if (ctx.numericLiteral().DECIMAL() != null) {
                    return factory.createLiteral(numericText, XSD.DECIMAL.getIRI());
                } else {
                    return factory.createLiteral(numericText, XSD.INTEGER.getIRI());
                }
            }

            throw new ParsingErrorException("Unsupported literal: " + ctx.getText());
        } catch (ParsingErrorException e) {
            throw e;
        } catch (Exception e) {
            throw new ParsingErrorException("Error extracting literal: " + ctx.getText() + " - " + e.getMessage(), e);
        }
    }

    /**
     * Extracts an object from the parser context.
     * Handles different object types (IRI, blank node, literal, etc.).
     *
     * @param ctx The object context.
     * @return The value of the object.
     * @throws ParsingErrorException if the object type is unsupported.
     */
    private Value extractObject(TurtleParser.Object_Context ctx) throws ParsingErrorException {
        try {
            if (ctx.iri() != null) {
                String resolvedIRI = resolveIRI(ctx.iri().getText());
                if (resolvedIRI.isEmpty()) {
                    throw new ParsingErrorException("Cannot resolve object IRI: " + ctx.iri().getText());
                }
                return factory.createIRI(resolvedIRI);
            }

            if (ctx.BlankNode() != null) {
                String blankNodeText = ctx.BlankNode().getText();
                if (blankNodeText.startsWith(ParserConstants.BLANK_NODE_PREFIX)) {
                    return factory.createBNode(blankNodeText.substring(2));
                } else if (blankNodeText.equals(ParserConstants.EMPTY_SQUARE_BRACKET)) {
                    return factory.createBNode();
                } else {
                    throw new ParsingErrorException("Unsupported blank node format: " + blankNodeText);
                }
            }

            if (ctx.literal() != null) {
                return extractLiteral(ctx.literal());
            }

            if (ctx.blankNodePropertyList() != null) {
                return processBlankNodePropertyList(ctx.blankNodePropertyList());
            }

            if (ctx.collection() != null) {
                return processCollection(ctx.collection());
            }

            throw new ParsingErrorException("Unsupported object type: " + (ctx.getText() != null ? ctx.getText() : "null"));
        } catch (ParsingErrorException e) {
            throw e;
        } catch (Exception e) {
            throw new ParsingErrorException("Error extracting object: " + ctx.getText() + " - " + e.getMessage(), e);
        }
    }

    /**
     * Extracts a subject from the parser context.
     * Handles different subject types (IRI, blank node, collection).
     *
     * @param ctx The subject context.
     * @return The subject resource.
     * @throws ParsingErrorException if the subject type is unsupported.
     */
    private Resource extractSubject(TurtleParser.SubjectContext ctx) throws ParsingErrorException {
        try {
            if (ctx.iri() != null) {
                String resolvedIRI = resolveIRI(ctx.iri().getText());
                return factory.createIRI(resolvedIRI);
            }
            if (ctx.BlankNode() != null) {
                String blankNodeText = ctx.BlankNode().getText();
                if (blankNodeText.startsWith(ParserConstants.BLANK_NODE_PREFIX)) {
                    return factory.createBNode(blankNodeText.substring(2));
                } else if (blankNodeText.equals(ParserConstants.EMPTY_SQUARE_BRACKET)) {
                    return factory.createBNode();
                } else {
                    throw new ParsingErrorException("Unsupported blank node format: " + blankNodeText);
                }
            }
            if (ctx.collection() != null) {
                return processCollection(ctx.collection());
            }
            throw new ParsingErrorException("Unsupported subject type: " + ctx.getText());
        } catch (ParsingErrorException e) {
            throw e;
        } catch (Exception e) {
            throw new ParsingErrorException("Error extracting subject: " + ctx.getText() + " - " + e.getMessage(), e);
        }
    }

    /**
     * Processes a blank node with a property list.
     * Creates a new blank node, sets it as the subject,
     * processes the predicate-object list, and then restores the previous subject and predicate.
     *
     * @param ctx The blank node property list context.
     * @return The created blank node.
     * @throws ParsingErrorException if an error occurs during processing.
     */
    private Resource processBlankNodePropertyList(TurtleParser.BlankNodePropertyListContext ctx) throws ParsingErrorException {
        try {
            Resource bnode = factory.createBNode();

            Resource savedSubject = currentSubject;
            IRI savedPredicate = currentPredicate;

            currentSubject = bnode;

            if (ctx.predicateObjectList() != null) {
                processPredicateObjectList(ctx.predicateObjectList());
            }

            currentSubject = savedSubject;
            currentPredicate = savedPredicate;

            return bnode;
        } catch (ParsingErrorException e) {
            throw e;
        } catch (Exception e) {
            throw new ParsingErrorException("Error processing blank node property list: " + e.getMessage(), e);
        }
    }

    /**
     * Processes a collection (RDF list) from the parser context.
     * Converts the collection into a linked list of blank nodes and `rdf:first` and `rdf:rest` triples.
     *
     * @param ctx The collection context.
     * @return The head blank node of the list.
     * @throws ParsingErrorException if an error occurs during processing.
     */
    private Resource processCollection(TurtleParser.CollectionContext ctx) throws ParsingErrorException {
        try {
            List<TurtleParser.Object_Context> objects = ctx.object_();

            if (objects.isEmpty()) {
                return factory.createIRI(RDF.nil.getIRI().stringValue());
            }

            Resource head = factory.createBNode();
            Resource current = head;

            for (int i = 0; i < objects.size(); i++) {
                TurtleParser.Object_Context objectCtx = objects.get(i);
                Value object = extractObject(objectCtx);

                safeAddStatement(current, factory.createIRI(RDF.first.getIRI().stringValue()), object);

                if (i == objects.size() - 1) {
                    safeAddStatement(current, factory.createIRI(RDF.rest.getIRI().stringValue()),
                            factory.createIRI(RDF.nil.getIRI().stringValue()));
                } else {
                    Resource next = factory.createBNode();
                    safeAddStatement(current, factory.createIRI(RDF.rest.getIRI().stringValue()), next);
                    current = next;
                }
            }

            return head;
        } catch (ParsingErrorException e) {
            throw e;
        } catch (Exception e) {
            throw new ParsingErrorException("Error processing collection: " + e.getMessage(), e);
        }
    }

    /**
     * Processes a list of predicates and objects for a given subject.
     * Iterates through the predicate-object pairs and adds triples to the model.
     *
     * @param ctx The predicate-object list context.
     * @throws ParsingErrorException if an error occurs during processing.
     */
    private void processPredicateObjectList(TurtleParser.PredicateObjectListContext ctx) throws ParsingErrorException {
        try {
            for (int i = 0; i < ctx.verb().size(); i++) {
                TurtleParser.VerbContext verb = ctx.verb(i);
                TurtleParser.ObjectListContext objectList = ctx.objectList(i);

                currentPredicate = extractVerb(verb);

                if (objectList != null) {
                    for (TurtleParser.Object_Context objectCtx : objectList.object_()) {
                        Value object = extractObject(objectCtx);
                        safeAddStatement(currentSubject, currentPredicate, object);
                    }
                }
            }
        } catch (ParsingErrorException e) {
            throw e;
        } catch (Exception e) {
            throw new ParsingErrorException("Error processing predicate-object list: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts a verb (predicate) from the parser context and resolves it to an IRI.
     *
     * @param ctx The verb context.
     * @return The IRI of the predicate.
     * @throws ParsingErrorException if the verb cannot be resolved to a valid IRI.
     */
    private IRI extractVerb(TurtleParser.VerbContext ctx) throws ParsingErrorException {
        try {
            String verbText = ctx.getText();
            String resolvedIRI = resolveIRI(verbText);
            if (resolvedIRI.isEmpty()) {
                throw new ParsingErrorException("Cannot resolve verb to a valid IRI: " + verbText);
            }
            return factory.createIRI(resolvedIRI);
        } catch (ParsingErrorException e) {
            throw e;
        } catch (Exception e) {
            throw new ParsingErrorException("Invalid verb: " + ctx.getText() + " - " + e.getMessage(), e);
        }
    }
}
