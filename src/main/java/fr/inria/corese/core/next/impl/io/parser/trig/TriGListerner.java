package fr.inria.corese.core.next.impl.io.parser.trig;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.api.io.parser.RDFParserBaseIRIOptions;
import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;
import fr.inria.corese.core.next.impl.io.parser.util.ParserConstants;
import fr.inria.corese.core.next.impl.parser.antlr.TriGBaseListener;
import fr.inria.corese.core.next.impl.parser.antlr.TriGParser;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TriG listener with proper IRI resolution, blank nodes and collections
 * according to RFC3986 specification.
 * This class translates TriG parsing events into RDF model operations.
 */
public class TriGListerner extends TriGBaseListener {
    private final Model model;
    private String baseURI;
    private final Map<String, String> prefixMap = new HashMap<>();
    private final ValueFactory factory;
    private boolean insideGraphBlock = false;

    private Resource currentSubject;
    private IRI currentPredicate;
    private Resource currentGraph;

    /**
     * Constructor for the TriG listener.
     * Initializes the model, value factory, and base URI.
     *
     * @param model   The RDF model where statements will be added.
     * @param factory The factory for creating values (IRI, BNode, Literal).
     * @param options I/O options (not directly used for logic but passed).
     * @param baseURI The initial base URI for resolution.
     */
    public TriGListerner(Model model, ValueFactory factory, IOOptions options, String baseURI) {
        this.model = model;
        this.factory = factory;
        this.baseURI = baseURI;

        initializeBasePrefix();
    }

    /**
     * Constructor for the TriG listener.
     * Initializes the model, value factory, and attempts to retrieve the base URI from options.
     *
     * @param model   The RDF model where statements will be added.
     * @param factory The factory for creating values (IRI, BNode, Literal).
     * @param options I/O options, potentially containing the base URI.
     */
    public TriGListerner(Model model, ValueFactory factory, IOOptions options) {
        this.model = model;
        this.factory = factory;

        if (options instanceof RDFParserBaseIRIOptions) {
            RDFParserBaseIRIOptions baseIRIOptions = (RDFParserBaseIRIOptions) options;
            this.baseURI = baseIRIOptions.getBase() != null ? baseIRIOptions.getBase() : ParserConstants.EMPTY_STRING;
        } else {
            this.baseURI = ParserConstants.EMPTY_STRING;
        }

        initializeBasePrefix();
    }

    /**
     * Initializes the base (empty) prefix with the current base URI.
     * Updates both the internal prefix map and the model's namespace mapping.
     */
    private void initializeBasePrefix() {
        if (this.baseURI != null && !this.baseURI.isEmpty()) {
            prefixMap.put(ParserConstants.EMPTY_STRING, this.baseURI);
            model.setNamespace(ParserConstants.EMPTY_STRING, this.baseURI);
        }
    }

    /**
     * Handles the exit of the `@base` rule.
     * Extracts and unescapes the IRI, then updates the base URI.
     *
     * @param ctx The context for the `base` rule.
     */
    @Override
    public void exitBase(TriGParser.BaseContext ctx) {
        String newBase = extractAndUnescapeIRI(ctx.IRIREF().getText());
        updateBaseURI(newBase);
    }

    /**
     * Handles the exit of the `@prefix` declaration rule.
     * Extracts the prefix and the IRI, and registers the association.
     *
     * @param ctx The context for the `prefixID` rule.
     */
    @Override
    public void exitPrefixID(TriGParser.PrefixIDContext ctx) {
        String prefix = ctx.PNAME_NS().getText();
        // Remove trailing ':'
        prefix = prefix.substring(0, prefix.length() - 1);
        String iri = extractAndUnescapeIRI(ctx.IRIREF().getText());
        registerPrefix(prefix, iri);
    }

    /**
     * Handles the exit of the SPARQL `BASE` rule.
     * Extracts and unescapes the IRI, then updates the base URI.
     *
     * @param ctx The context for the `sparqlBase` rule.
     */
    @Override
    public void exitSparqlBase(TriGParser.SparqlBaseContext ctx) {
        String newBase = extractAndUnescapeIRI(ctx.IRIREF().getText());
        updateBaseURI(newBase);
    }

    /**
     * Handles the exit of the SPARQL `PREFIX` declaration rule.
     * Extracts the prefix and the IRI, and registers the association.
     *
     * @param ctx The context for the `sparqlPrefix` rule.
     */
    @Override
    public void exitSparqlPrefix(TriGParser.SparqlPrefixContext ctx) {
        String prefix = ctx.PNAME_NS().getText();
        // Remove trailing ':'
        prefix = prefix.substring(0, prefix.length() - 1);
        String iri = extractAndUnescapeIRI(ctx.IRIREF().getText());
        registerPrefix(prefix, iri);
    }

    /**
     * Handles the entry into a graph block.
     * If it's a named graph (`Graph_w`), sets {@code currentGraph} to the graph label or subject.
     *
     * @param ctx The context for the `block` rule.
     */
    @Override
    public void enterBlock(TriGParser.BlockContext ctx) {
        if (ctx.Graph_w() != null && ctx.labelOrSubject() != null) {
            currentGraph = extractLabelOrSubject(ctx.labelOrSubject());
        } else {
            currentGraph = null;
        }
    }

    /**
     * Handles the exit of a graph block.
     * Resets {@code currentGraph} to null.
     *
     * @param ctx The context for the `block` rule.
     */
    @Override
    public void exitBlock(TriGParser.BlockContext ctx) {
        currentGraph = null;
    }

    /**
     * Handles the entry into a wrapped graph (`{ ... }`).
     * Sets the {@code insideGraphBlock} flag to true.
     *
     * @param ctx The context for the `wrappedGraph` rule.
     */
    @Override
    public void enterWrappedGraph(TriGParser.WrappedGraphContext ctx) {
        insideGraphBlock = true;
    }

    /**
     * Handles the exit of a wrapped graph.
     * Sets the {@code insideGraphBlock} flag to false.
     *
     * @param ctx The context for the `wrappedGraph` rule.
     */
    @Override
    public void exitWrappedGraph(TriGParser.WrappedGraphContext ctx) {
        insideGraphBlock = false;
    }

    /**
     * Handles the entry of a `triplesOrGraph` construction.
     * If a {@code labelOrSubject} is present, it either acts as the subject of the triples
     * (if followed by a {@code predicateObjectList}) or the graph name (if followed by a {@code wrappedGraph}).
     *
     * @param ctx The context for the `triplesOrGraph` rule.
     */
    @Override
    public void enterTriplesOrGraph(TriGParser.TriplesOrGraphContext ctx) {
        if (ctx.labelOrSubject() != null) {
            Resource resource = extractLabelOrSubject(ctx.labelOrSubject());
            if (ctx.predicateObjectList() != null) {
                // Triples with explicit subject
                currentSubject = resource;
                processPredicateObjectList(ctx.predicateObjectList());
            } else if (ctx.wrappedGraph() != null) {
                // Named graph definition
                currentGraph = resource;
            }
        }
    }

    /**
     * Handles the entry of a simple `triples` declaration (starting with a subject or a blank node property list).
     * Sets the {@code currentSubject}, processes the {@code predicateObjectList}, and restores state.
     *
     * @param ctx The context for the `triples` rule.
     */
    @Override
    public void enterTriples(TriGParser.TriplesContext ctx) {
        Resource savedSubject = currentSubject;
        try {
            if (ctx.subject() != null) {
                currentSubject = extractSubject(ctx.subject());
            } else if (ctx.blankNodePropertyList() != null) {
                currentSubject = processBlankNodePropertyList(ctx.blankNodePropertyList());
            }

            if (ctx.predicateObjectList() != null) {
                processPredicateObjectList(ctx.predicateObjectList());
            }
        } finally {
            currentSubject = savedSubject;
        }
    }

    /**
     * Handles the entry of a `triples2` declaration (starting with BNode property list or Collection).
     * Processes the list or collection to establish the subject, then handles the predicate-object list.
     * Includes validation for standalone collections.
     * {@code currentSubject} is restored in the `finally` block.
     *
     * @param ctx The context for the `triples2` rule.
     */
    @Override
    public void enterTriples2(TriGParser.Triples2Context ctx) {
        Resource savedSubject = currentSubject;

        if (ctx.collection() != null && ctx.predicateObjectList() == null) {
            validateStandaloneCollection(ctx);
        }

        try {
            if (ctx.blankNodePropertyList() != null) {
                currentSubject = processBlankNodePropertyList(ctx.blankNodePropertyList());
            } else if (ctx.collection() != null) {
                currentSubject = processCollection(ctx.collection());
            }

            if (ctx.predicateObjectList() != null) {
                processPredicateObjectList(ctx.predicateObjectList());
            }
        } finally {
            currentSubject = savedSubject;
        }
    }

    /**
     * Validates if a collection is used as a standalone statement (without a {@code predicateObjectList}
     * following it) outside of a wrapped graph block.
     * Throws a {@code ParsingErrorException} if the syntax is incorrect.
     *
     * @param ctx The context for the `triples2` rule.
     */
    private void validateStandaloneCollection(TriGParser.Triples2Context ctx) {
        if (ctx.predicateObjectList() == null && !insideGraphBlock) {
            List<TriGParser.ObjectContext> objects = ctx.collection().object();
            if (objects.isEmpty()) {
                throw new ParsingErrorException("Free-standing list of zero-elements outside {} : bad syntax");
            } else {
                throw new ParsingErrorException("Free-standing list outside {} : bad syntax");
            }
        }
    }

    /**
     * Processes a blank node property list (e.g., `[ predicateObjectList ]`).
     * Creates a new blank node, sets it as the current subject, processes the nested
     * predicate-object list, and restores the subject state.
     *
     * @param ctx The context for the `blankNodePropertyList` rule.
     * @return The created blank node resource.
     */
    private Resource processBlankNodePropertyList(TriGParser.BlankNodePropertyListContext ctx) {
        Resource bnode = factory.createBNode();
        Resource savedSubject = currentSubject;
        IRI savedPredicate = currentPredicate;

        try {
            currentSubject = bnode;
            if (ctx.predicateObjectList() != null) {
                processPredicateObjectList(ctx.predicateObjectList());
            }
        } finally {
            currentSubject = savedSubject;
            currentPredicate = savedPredicate;
        }

        return bnode;
    }

    /**
     * Processes an RDF collection (list: `( object* )`).
     * Constructs the RDF list structure using blank nodes, {@code rdf:first}, and {@code rdf:rest},
     * terminated by {@code rdf:nil}.
     *
     * @param ctx The context for the `collection` rule.
     * @return The head resource of the RDF list (a blank node or {@code rdf:nil} if empty).
     */
    private Resource processCollection(TriGParser.CollectionContext ctx) {
        List<TriGParser.ObjectContext> objects = ctx.object();

        if (objects.isEmpty()) {
            // Empty list is rdf:nil
            return factory.createIRI(RDF.nil.getIRI().stringValue());
        }

        Resource head = factory.createBNode();
        Resource current = head;
        Resource savedSubject = currentSubject;
        IRI savedPredicate = currentPredicate;

        try {
            IRI firstPredicate = factory.createIRI(RDF.first.getIRI().stringValue());
            IRI restPredicate = factory.createIRI(RDF.rest.getIRI().stringValue());
            Value nilValue = factory.createIRI(RDF.nil.getIRI().stringValue());

            for (int i = 0; i < objects.size(); i++) {
                Value object = extractObject(objects.get(i));
                safeAddStatement(current, firstPredicate, object);

                if (i == objects.size() - 1) {
                    // Last element, rest is rdf:nil
                    safeAddStatement(current, restPredicate, nilValue);
                } else {
                    // Not the last element, create next blank node for rest
                    Resource next = factory.createBNode();
                    safeAddStatement(current, restPredicate, next);
                    current = next;
                }
            }
        } finally {
            currentSubject = savedSubject;
            currentPredicate = savedPredicate;
        }

        return head;
    }

    /**
     * Processes a predicate-object list (i.e., `verb objectList ; verb objectList ...`).
     * Iterates through each verb/object list pair, sets the current predicate,
     * processes the object list, and adds statements to the model.
     *
     * @param ctx The context for the `predicateObjectList` rule.
     */
    private void processPredicateObjectList(TriGParser.PredicateObjectListContext ctx) {
        for (int i = 0; i < ctx.verb().size(); i++) {
            TriGParser.VerbContext verb = ctx.verb(i);
            TriGParser.ObjectListContext objectList = ctx.objectList(i);

            IRI savedPredicate = currentPredicate;
            try {
                currentPredicate = extractVerb(verb);

                if (objectList != null) {
                    for (TriGParser.ObjectContext objectCtx : objectList.object()) {
                        Value object = extractObject(objectCtx);
                        safeAddStatement(currentSubject, currentPredicate, object);
                    }
                }
            } finally {
                currentPredicate = savedPredicate;
            }
        }
    }

    /**
     * Safely adds an RDF statement (triple or quad) to the model.
     * Includes a fallback mechanism: if adding to the named graph fails and a graph is defined,
     * it attempts to add the statement to the default graph (null graph name).
     * Throws a {@code ParsingErrorException} on persistent failure.
     *
     * @param subject   The subject resource.
     * @param predicate The predicate IRI.
     * @param object    The object value.
     */
    private void safeAddStatement(Resource subject, IRI predicate, Value object) {
        try {
            model.add(subject, predicate, object, currentGraph);
        } catch (Exception e) {
            if (currentGraph != null) {
                try {
                    // Fallback to default graph if adding to named graph fails
                    model.add(subject, predicate, object, null);
                } catch (Exception e2) {
                    throw new ParsingErrorException("Failed to add statement: " + e.getMessage(), e);
                }
            } else {
                throw new ParsingErrorException("Failed to add statement: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Extracts and resolves the object value from its context.
     * Can be an IRI, a blank node, a literal, or a blank node property list.
     *
     * @param ctx The context for the `ObjectContext`.
     * @return The RDF value (IRI, BNode, or Literal).
     */
    private Value extractObject(TriGParser.ObjectContext ctx) {
        if (ctx.iri() != null) {
            return factory.createIRI(resolveIRI(ctx.iri().getText()));
        }
        if (ctx.blank() != null) {
            return extractBlank(ctx.blank());
        }
        if (ctx.literal() != null) {
            return extractLiteral(ctx.literal());
        }
        if (ctx.blankNodePropertyList() != null) {
            // Note: blankNodePropertyList inside object context becomes the object BNode itself
            return processBlankNodePropertyList(ctx.blankNodePropertyList());
        }
        throw new ParsingErrorException("Unsupported object: " + ctx.getText());
    }

    /**
     * Extracts a blank node resource from its context.
     * Can be a labeled blank node (`_:name`), an anonymous blank node (`[]`), or a collection (`(...)`).
     *
     * @param ctx The context for the `BlankContext`.
     * @return The BNode or the {@code rdf:nil} IRI for an empty collection.
     */
    private Resource extractBlank(TriGParser.BlankContext ctx) {
        TriGParser.BlankNodeContext node = ctx.blankNode();
        if (node != null) {
            if (node.BLANK_NODE_LABEL() != null) {
                // Remove the leading '_:'
                return factory.createBNode(node.BLANK_NODE_LABEL().getText().substring(2));
            }
            if (node.ANON() != null) {
                return factory.createBNode();
            }
        }

        TriGParser.CollectionContext collection = ctx.collection();
        if (collection != null) {
            return processCollection(collection);
        }

        throw new ParsingErrorException("Unsupported blank node: " + ctx.getText());
    }

    /**
     * Extracts and resolves the subject resource from its context.
     * The subject can be an IRI or a blank node (including collections).
     *
     * @param ctx The context for the `SubjectContext`.
     * @return The RDF resource (IRI or BNode) to be used as the subject.
     */
    private Resource extractSubject(TriGParser.SubjectContext ctx) {
        if (ctx.iri() != null) {
            return factory.createIRI(resolveIRI(ctx.iri().getText()));
        }
        if (ctx.blank() != null) {
            if (ctx.blank().blankNode() != null) {
                TriGParser.BlankNodeContext node = ctx.blank().blankNode();
                if (node.BLANK_NODE_LABEL() != null) {
                    return factory.createBNode(node.BLANK_NODE_LABEL().getText().substring(2));
                }
                if (node.ANON() != null) {
                    return factory.createBNode();
                }
            } else if (ctx.blank().collection() != null) {
                return processCollection(ctx.blank().collection());
            }
        }
        throw new ParsingErrorException("Unsupported subject: " + ctx.getText());
    }

    /**
     * Extracts and resolves a graph label or subject from its context.
     * The label/subject can be an IRI or a blank node.
     *
     * @param ctx The context for the `LabelOrSubjectContext`.
     * @return The RDF resource (IRI or BNode) for the graph label or subject.
     */
    private Resource extractLabelOrSubject(TriGParser.LabelOrSubjectContext ctx) {
        if (ctx.iri() != null) {
            String iriText = ctx.iri().getText();
            try {
                return factory.createIRI(resolveIRI(iriText));
            } catch (Exception e) {
                throw new ParsingErrorException("Failed to resolve IRI: " + iriText, e);
            }
        }
        if (ctx.blankNode() != null) {
            if (ctx.blankNode().BLANK_NODE_LABEL() != null) {
                return factory.createBNode(ctx.blankNode().BLANK_NODE_LABEL().getText().substring(2));
            }
            if (ctx.blankNode().ANON() != null) {
                return factory.createBNode();
            }
        }
        throw new ParsingErrorException("Unsupported label or subject: " + ctx.getText());
    }

    /**
     * Extracts and resolves the verb (predicate) from its context.
     * Handles the shortcut {@code a} for {@code rdf:type}.
     *
     * @param ctx The context for the `VerbContext`.
     * @return The resolved IRI for the predicate.
     */
    private IRI extractVerb(TriGParser.VerbContext ctx) {
        String verbText = ctx.getText();
        if (verbText.equals(ParserConstants.A)) {
            return factory.createIRI(RDF.type.getIRI().stringValue());
        }
        return factory.createIRI(resolveIRI(verbText));
    }

    /**
     * Extracts an RDF literal from its context.
     * Handles simple, typed, and language-tagged literals, as well as boolean and numeric literals.
     *
     * @param ctx The context for the `LiteralContext`.
     * @return The RDF literal value.
     */
    private Literal extractLiteral(TriGParser.LiteralContext ctx) {
        if (ctx.rDFLiteral() != null) {
            String label = unescapeString(ctx.rDFLiteral().string().getText());
            if (ctx.rDFLiteral().LANGTAG() != null) {
                return factory.createLiteral(label, ctx.rDFLiteral().LANGTAG().getText().substring(1));
            }
            if (ctx.rDFLiteral().iri() != null) {
                return factory.createLiteral(label, factory.createIRI(resolveIRI(ctx.rDFLiteral().iri().getText())));
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
    }

    /**
     * Extracts the text from an IRIREF (surrounded by `<` and `>`) and unescapes Unicode sequences.
     *
     * @param text The raw text of the IRIREF.
     * @return The unescaped IRI.
     */
    private String extractAndUnescapeIRI(String text) {
        String iri = text.substring(1, text.length() - 1);
        return unescapeIRI(iri);
    }

    /**
     * Updates the base URI for relative IRI resolution.
     * Resolves the new base against the old one, updates {@code baseURI}, and registers
     * the empty prefix in the map and the model.
     *
     * @param newBase The new base IRI.
     */
    private void updateBaseURI(String newBase) {
        this.baseURI = resolveIRIAgainstBase(newBase);
        prefixMap.put(ParserConstants.EMPTY_STRING, this.baseURI);
        model.setNamespace(ParserConstants.EMPTY_STRING, this.baseURI);
    }

    /**
     * Registers a prefix with its corresponding namespace URI.
     * The namespace IRI is resolved against the current base URI.
     *
     * @param prefix The prefix.
     * @param iri    The namespace IRI.
     */
    private void registerPrefix(String prefix, String iri) {
        String resolvedIRI = resolveIRIAgainstBase(iri);
        prefixMap.put(prefix, resolvedIRI);
        model.setNamespace(prefix, resolvedIRI);
    }

    /**
     * Resolves a raw IRI (which can be relative, prefixed, or absolute) into an absolute IRI.
     * Applies prefix resolution and relative resolution against {@code baseURI}.
     *
     * @param raw The raw IRI (including QNames, IRI references, or absolute IRIs).
     * @return The resolved absolute IRI.
     * @throws ParsingErrorException If an undeclared prefix is encountered.
     */
    private String resolveIRI(String raw) {
        try {
            raw = raw.trim();

            if (raw.equals(ParserConstants.A)) {
                return RDF.type.getIRI().stringValue();
            }

            if (raw.equals(ParserConstants.COLON)) {
                String ns = prefixMap.get(ParserConstants.EMPTY_STRING);
                return ns != null ? ns : getEffectiveBaseURI();
            }

            // Handle <iri_ref>
            if (raw.startsWith(ParserConstants.IRI_START) && raw.endsWith(ParserConstants.IRI_END)) {
                String iri = raw.substring(1, raw.length() - 1);
                iri = unescapeIRI(iri);
                return iri.isEmpty() ? getEffectiveBaseURI() : resolveIRIAgainstBase(iri);
            }

            // Handle prefixed name (QName)
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
     * Resolves an IRI against the effective base URI using RFC3986 rules.
     *
     * @param iri The relative or absolute IRI to resolve.
     * @return The resolved absolute IRI.
     */
    private String resolveIRIAgainstBase(String iri) {
        String effectiveBase = getEffectiveBaseURI();

        // 1. If R is absolute, the result is R
        if (isAbsoluteIRI(iri)) {
            return iri;
        }

        // 2. If R is empty, the result is the base URI
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

            if (refScheme != null) {
                // 3. R has a scheme component
                targetScheme = refScheme;
                targetAuthority = refAuthority;
                targetPath = removeDotSegments(refPath);
                targetQuery = refQuery;
            } else {
                // 4. R inherits the base scheme
                if (refAuthority != null) {
                    // 4.1 R is net-path
                    targetScheme = baseScheme;
                    targetAuthority = refAuthority;
                    targetPath = removeDotSegments(refPath);
                    targetQuery = refQuery;
                } else {
                    // 4.2 R is abs-path or rel-path
                    targetScheme = baseScheme;
                    targetAuthority = baseAuthority;
                    if (refPath.isEmpty()) {
                        // 4.2.1 R is empty or only query/fragment
                        targetPath = basePath;
                        targetQuery = refQuery != null ? refQuery : baseQuery;
                    } else {
                        // 4.2.2 R is abs-path or rel-path
                        if (refPath.startsWith(ParserConstants.SLASH)) {
                            // 4.2.2.1 R is abs-path
                            targetPath = removeDotSegments(refPath);
                        } else {
                            // 4.2.2.2 R is rel-path
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
     * Reconstructs a URI from its components (scheme, authority, path, query, fragment).
     *
     * @param scheme    The scheme (protocol).
     * @param authority The authority (host, port, user info).
     * @param path      The path.
     * @param query     The query.
     * @param fragment  The fragment.
     * @return The normalized URI string.
     */
    private String buildURI(String scheme, String authority, String path, String query, String fragment) {
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
     * Parses a URI/IRI reference into its five main components:
     * Scheme, Authority, Path, Query, Fragment.
     *
     * @param ref The URI/IRI reference string.
     * @return An array of 5 strings: [scheme, authority, path, query, fragment].
     */
    private String[] parseReference(String ref) {
        String[] parts = new String[5];
        String remaining = ref;

        // 1. Fragment
        int fragmentIndex = remaining.indexOf('#');
        if (fragmentIndex >= 0) {
            parts[4] = remaining.substring(fragmentIndex + 1);
            remaining = remaining.substring(0, fragmentIndex);
        }

        // 2. Query
        int queryIndex = remaining.indexOf('?');
        if (queryIndex >= 0) {
            parts[3] = remaining.substring(queryIndex + 1);
            remaining = remaining.substring(0, queryIndex);
        }

        // 3. Scheme (only if valid, must start with letter and colon follows)
        int colonIndex = remaining.indexOf(':');
        if (colonIndex > 0 && isValidScheme(remaining.substring(0, colonIndex))) {
            parts[0] = remaining.substring(0, colonIndex);
            remaining = remaining.substring(colonIndex + 1);
        }

        // 4. Authority (if starts with //)
        if (remaining.startsWith(ParserConstants.DOUBLE_SLASH)) {
            int authorityEnd = remaining.indexOf('/', 2);
            if (authorityEnd < 0) {
                authorityEnd = remaining.length();
            }
            parts[1] = remaining.substring(2, authorityEnd);
            remaining = remaining.substring(authorityEnd);
        }

        // 5. Path
        parts[2] = remaining;
        return parts;
    }

    /**
     * Merges the base path and the reference path for relative resolution.
     *
     * @param basePath The path of the base URI.
     * @param refPath  The path of the reference IRI.
     * @return The merged path string.
     */
    private String mergePaths(String basePath, String refPath) {
        if (basePath == null || basePath.isEmpty()) {
            return ParserConstants.SLASH + refPath;
        }
        int lastSlash = basePath.lastIndexOf('/');
        return lastSlash >= 0 ? basePath.substring(0, lastSlash + 1) + refPath : refPath;
    }

    /**
     * Removes dot segments (`.` and `..`) from the path according to the RFC3986 algorithm (section 5.2.4).
     *
     * @param path The URI path to be cleaned.
     * @return The path without dot segments.
     */
    private String removeDotSegments(String path) {
        if (path == null || path.isEmpty()) {
            return ParserConstants.EMPTY_STRING;
        }

        String input = path;
        StringBuilder output = new StringBuilder();

        while (!input.isEmpty()) {
            // 1. "." / ".." / "./" / "../"
            if (input.startsWith("../")) {
                input = input.substring(3);
            } else if (input.startsWith("./")) {
                input = input.substring(2);
            }
            // 2. "/./" / "/."
            else if (input.startsWith("/./")) {
                input = ParserConstants.SLASH + input.substring(3);
            } else if (input.equals("/.")) {
                input = ParserConstants.SLASH;
            }
            // 3. "/../" / "/.."
            else if (input.startsWith("/../")) {
                input = ParserConstants.SLASH + input.substring(4);
                removeLastSegment(output);
            } else if (input.equals("/..")) {
                input = ParserConstants.SLASH;
                removeLastSegment(output);
            }
            // 4. "." / ".."
            else if (input.equals(ParserConstants.POINT) || input.equals(ParserConstants.DOUBLE_DOT)) {
                input = ParserConstants.EMPTY_STRING;
            }
            // 5. Normal segment
            else {
                int nextSlash;
                if (input.startsWith(ParserConstants.SLASH)) {
                    nextSlash = input.indexOf(ParserConstants.SLASH, 1);
                    if (nextSlash >= 0) {
                        output.append(input.substring(0, nextSlash));
                        input = input.substring(nextSlash);
                    } else {
                        output.append(input);
                        input = ParserConstants.EMPTY_STRING;
                    }
                } else {
                    nextSlash = input.indexOf(ParserConstants.SLASH);
                    if (nextSlash >= 0) {
                        output.append(input.substring(0, nextSlash));
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
     * Removes the last segment (up to the last slash) of the path in the {@code output} StringBuilder.
     * Used by {@code removeDotSegments} to handle {@code ..}.
     *
     * @param output The StringBuilder containing the path under construction.
     */
    private void removeLastSegment(StringBuilder output) {
        String outputStr = output.toString();
        int lastSlash = outputStr.lastIndexOf(ParserConstants.SLASH);
        output.setLength(lastSlash >= 0 ? lastSlash : 0);
    }

    /**
     * Provides a simple fallback method for relative IRI resolution.
     * Used if RFC3986-based resolution fails (e.g., due to a {@code URISyntaxException}).
     *
     * @param base     The base URI.
     * @param relative The relative IRI.
     * @return The resolved IRI.
     */
    private String performSimpleFallback(String base, String relative) {
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
     * Normalizes the URI string, specifically handling the {@code file:} protocol.
     * Ensures that {@code file:} is correctly represented by {@code file:///}.
     *
     * @param uri The URI string to normalize.
     * @return The normalized URI.
     */
    private String normalizeURI(String uri) {
        if (uri == null) {
            return null;
        }
        // Specific normalization for 'file:' protocol to ensure 'file:///' format
        if (uri.startsWith(ParserConstants.FILE_PROTOCOL_SIMPLE) && !uri.startsWith(ParserConstants.FILE_PROTOCOL_TRIPLE_SLASH)) {
            if (!uri.startsWith(ParserConstants.FILE_PROTOCOL)) {
                uri = uri.replace(ParserConstants.FILE_PROTOCOL_SIMPLE, ParserConstants.FILE_PROTOCOL_TRIPLE_SLASH);
            }
        }
        return uri;
    }

    /**
     * Checks if an IRI string is an absolute IRI (contains a valid scheme followed by a colon).
     *
     * @param iri The IRI string.
     * @return {@code true} if the IRI is absolute, otherwise {@code false}.
     */
    private boolean isAbsoluteIRI(String iri) {
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
     * Checks if an URI scheme string is syntactically valid according to RFC3986.
     *
     * @param scheme The scheme string to validate.
     * @return {@code true} if the scheme is valid, otherwise {@code false}.
     */
    private boolean isValidScheme(String scheme) {
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
     * Retrieves the effective base URI, using a default value if not set.
     *
     * @return The effective and normalized base URI.
     */
    private String getEffectiveBaseURI() {
        String effective = (baseURI != null && !baseURI.isEmpty()) ? baseURI : ParserConstants.DEFAULT_BASE_URI;
        return normalizeURI(effective);
    }

    /**
     * Unescapes Unicode escape sequences within an IRI string.
     *
     * @param rawIri The raw, potentially escaped IRI.
     * @return The IRI with resolved Unicode escapes.
     * @throws IllegalArgumentException If a Unicode sequence is incomplete or a surrogate is found.
     */
    private String unescapeIRI(String rawIri) {
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
     * Unescapes common and Unicode escape sequences in RDF literal strings.
     * Also handles the removal of surrounding single or triple quotes/apostrophes.
     *
     * @param text The raw literal text (including delimiters).
     * @return The unescaped literal string value.
     * @throws IllegalArgumentException If an invalid Unicode sequence is found.
     */
    private String unescapeString(String text) {
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
}
