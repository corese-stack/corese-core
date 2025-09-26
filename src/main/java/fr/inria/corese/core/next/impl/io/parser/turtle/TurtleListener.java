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

import java.net.URI;
import java.net.URISyntaxException;
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
     * which provides access to the parsed prefix name and IRI reference tokens.
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

    @Override
    public void enterVerb(TurtleParser.VerbContext ctx) {
        // This is called for each verb, but we process them in processPredicateObjectList
        // Keep this method empty to avoid interference
    }

    @Override
    public void exitObject_(TurtleParser.Object_Context ctx) {
        // This is called for each object, but we process them in processPredicateObjectList
        // Keep this method empty to avoid interference
    }

    /**
     * Adds a statement to the model with robust error handling to catch
     * potential issues during the addition process.
     *
     * @param subject   The subject of the statement.
     * @param predicate The predicate of the statement.
     * @param object    The object of the statement.
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
                        String resolved = ns + localName;
                        return resolved;
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

        try {
            URI base = new URI(effectiveBase);
            URI resolved = base.resolve(iri);

            return resolved.toString();
        } catch (URISyntaxException e) {

            if (iri.isEmpty()) {
                return effectiveBase;
            }

            if (iri.contains(ParserConstants.COLON) && !iri.startsWith(ParserConstants.SLASH) && !iri.startsWith(ParserConstants.QUERY_MARK) && !iri.startsWith(ParserConstants.FRAGMENT)) {

                try {
                    URI base = new URI(effectiveBase);
                    String basePath = base.getPath();


                    String resolvedPath;
                    if (basePath.endsWith(ParserConstants.SLASH)) {
                        resolvedPath = basePath + iri;
                    } else {
                        int lastSlash = basePath.lastIndexOf('/');
                        if (lastSlash >= 0) {
                            resolvedPath = basePath.substring(0, lastSlash + 1) + iri;
                        } else {
                            resolvedPath = ParserConstants.SLASH + iri;
                        }
                    }

                    URI resolved = new URI(base.getScheme(), base.getAuthority(), resolvedPath, null, null);
                    return resolved.toString();
                } catch (URISyntaxException ex) {
                    return effectiveBase + (effectiveBase.endsWith(ParserConstants.SLASH) ? ParserConstants.EMPTY_STRING : ParserConstants.SLASH) + iri;
                }
            }

            if (iri.startsWith(ParserConstants.SLASH)) {
                try {
                    URI base = new URI(effectiveBase);
                    return base.getScheme() + ParserConstants.SCHEME_DELIMITER + base.getAuthority() + iri;
                } catch (URISyntaxException ex) {
                    return effectiveBase + iri;
                }
            }

            if (effectiveBase.endsWith(ParserConstants.SLASH)) {
                return effectiveBase + iri;
            } else {
                return effectiveBase + ParserConstants.SLASH + iri;
            }
        }
    }

    /**
     * Returns the effective base URI,
     *
     * @return The effective base URI string.
     */
    private String getEffectiveBaseURI() {
        String effective = (baseURI != null && !baseURI.isEmpty()) ? baseURI : ParserConstants.DEFAULT_BASE_URI;

        if (effective.startsWith(ParserConstants.FILE_PROTOCOL_SIMPLE) && !effective.startsWith(ParserConstants.FILE_PROTOCOL_TRIPLE_SLASH)) {
            effective = effective.replaceFirst(ParserConstants.FILE_PROTOCOL_SIMPLE, ParserConstants.FILE_PROTOCOL_TRIPLE_SLASH);
        }

        return effective;
    }

    /**
     * Unescapes Unicode escape sequences
     * from an IRI string, as per Turtle specification.
     *
     * @param rawIri The raw IRI string containing escape sequences.
     * @return The unescaped IRI string.
     * @throws ParsingErrorException if the escape sequence is invalid.
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
     * Strips quotes from a string, handling single and triple quotes.
     *
     * @param text the string to strip quotes from
     * @return the stripped string
     */
    private String unescapeString(String text) throws ParsingErrorException {
        if (text == null || text.length() < 2) {
            return text;
        }

        boolean isMultiline = text.startsWith("\"\"\"") || text.startsWith("'''");
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
     * Extracts a literal from the given context, handling different types of
     * literals.
     *
     * @param ctx the context containing the literal
     * @return the extracted Literal object
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
                boolean isNegative = numericText.startsWith(ParserConstants.MINUS);
                String absoluteValue = isNegative ? numericText.substring(1) : numericText;

                if (ctx.numericLiteral().DOUBLE() != null || absoluteValue.toLowerCase().contains(ParserConstants.E)) {
                    return factory.createLiteral(numericText, XSD.DOUBLE.getIRI());
                } else if (ctx.numericLiteral().DECIMAL() != null || absoluteValue.contains(ParserConstants.DOT)) {
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
     * Extracts an RDF object from the given parse tree context.
     * An object can be an IRI, a blank node, a literal, a blank node property list, or a collection.
     *
     * @param ctx The context containing the object.
     * @return The extracted {@link Value} object.
     * @throws ParsingErrorException if the object type is unsupported.
     */
    private Value extractObject(TurtleParser.Object_Context ctx) throws ParsingErrorException {
        try {
            if (ctx.iri() != null) {
                String resolvedIRI = resolveIRI(ctx.iri().getText());
                if (resolvedIRI.isEmpty()) {
                    throw new ParsingErrorException("Cannot resolve object IRI: " + ctx.iri().getText());
                }
                IRI iri = factory.createIRI(resolvedIRI);
                return iri;
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
     * Extracts an RDF subject from the given parse tree context.
     * A subject can be an IRI, a blank node, or a collection.
     *
     * @param ctx The context containing the subject.
     * @return The extracted {@link Resource} object.
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
     * Processes a blank node property list, creating a new blank node and
     * recursively processing its predicate-object list.
     *
     * @param ctx The parse tree context for the blank node property list.
     * @return The blank node created to represent the list.
     * @throws ParsingErrorException if processing fails.
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
     * Processes a collection
     *
     * @param ctx The parse tree context for the collection.
     * @return The blank node that is the head of the list.
     * @throws ParsingErrorException if processing fails.
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
                Value object = extractObject(objects.get(i));

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
     * Extracts the predicate from the given context, which is expected to be an
     * IRI.
     *
     * @param ctx the context containing the predicate
     * @return the extracted IRI object
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
     * Extracts the verb from the given context, which can be a predicate or an IRI.
     *
     * @param ctx the context containing the verb
     * @return the extracted IRI object
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
