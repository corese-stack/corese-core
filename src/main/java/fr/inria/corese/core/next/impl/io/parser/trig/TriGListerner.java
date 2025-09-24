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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Listener for the ANTLR4 generated parser for TriG.
 * This listener traverses the parse tree and builds the RDF model,
 * supporting named graphs. It includes unescaping logic for URIs and literals.
 */
public class TriGListerner extends TriGBaseListener {
    private final Model model;
    private String baseURI;
    private final Map<String, String> prefixMap = new HashMap<>();
    private final ValueFactory factory;

    private Resource currentSubject;
    private IRI currentPredicate;
    private Resource currentGraph;

    /**
     * Constructor for the TriGListerner with a given base URI.
     *
     * @param model   The RDF model to populate.
     * @param factory The ValueFactory for creating RDF resources.
     * @param options IOOptions for configuration (if any).
     * @param baseURI The base URI to use for relative URI resolution.
     */
    public TriGListerner(Model model, ValueFactory factory, IOOptions options, String baseURI) {
        this.model = model;
        this.factory = factory;
        this.baseURI = baseURI;

        if (this.baseURI != null && !this.baseURI.isEmpty()) {
            prefixMap.put(ParserConstants.EMPTY_STRING, this.baseURI);
            model.setNamespace(ParserConstants.EMPTY_STRING, this.baseURI);
        }
    }

    /**
     * Constructor for the TriGListerner that extracts the base URI from options.
     *
     * @param model   The RDF model to populate.
     * @param factory The ValueFactory for creating RDF resources.
     * @param options IOOptions for configuration.
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

        if (this.baseURI != null && !this.baseURI.isEmpty()) {
            prefixMap.put(ParserConstants.EMPTY_STRING, this.baseURI);
            model.setNamespace(ParserConstants.EMPTY_STRING, this.baseURI);
        }
    }

    /**
     * Called when a `BASE` directive is exited.
     * Resolves the new base URI against the current one and updates it.
     *
     * @param ctx The parse tree context for the `BASE` directive.
     */
    @Override
    public void exitBase(TriGParser.BaseContext ctx) {
        String newBase = ctx.IRIREF().getText();

        newBase = newBase.substring(1, newBase.length() - 1);
        newBase = unescapeIRI(newBase);

        try {
            if (this.baseURI != null && !this.baseURI.isEmpty()) {
                URI currentBase = new URI(this.baseURI);
                URI resolved = currentBase.resolve(newBase);
                this.baseURI = resolved.toString();
            } else {
                if (isAbsoluteIRI(newBase)) {
                    this.baseURI = newBase;
                } else {
                    this.baseURI = ParserConstants.DEFAULT_BASE_URI + newBase;
                }
            }
        } catch (URISyntaxException e) {
            if (isAbsoluteIRI(newBase)) {
                this.baseURI = newBase;
            } else {
                this.baseURI = (this.baseURI != null && !this.baseURI.isEmpty())
                        ? this.baseURI + newBase
                        : ParserConstants.DEFAULT_BASE_URI + newBase;
            }
        }

        prefixMap.put(ParserConstants.EMPTY_STRING, this.baseURI);
        model.setNamespace(ParserConstants.EMPTY_STRING, this.baseURI);
    }

    /**
     * Called when a `@prefix` directive is exited.
     * Extracts the prefix and IRI, resolves the IRI, and adds the mapping to the
     * prefix map and model.
     *
     * @param ctx The parse tree context for the `@prefix` directive.
     */
    @Override
    public void exitPrefixID(TriGParser.PrefixIDContext ctx) {
        String prefix = ctx.PNAME_NS().getText();
        String iri = ctx.IRIREF().getText();
        prefix = prefix.substring(0, prefix.length() - 1);
        iri = iri.substring(1, iri.length() - 1);
        iri = unescapeIRI(iri);

        String resolvedIRI;
        try {
            if (baseURI != null && !baseURI.isEmpty()) {
                URI base = new URI(baseURI);
                URI resolved = base.resolve(iri);
                resolvedIRI = resolved.toString();
            } else {
                resolvedIRI = isAbsoluteIRI(iri) ? iri : ParserConstants.DEFAULT_BASE_URI + iri;
            }
        } catch (URISyntaxException e) {
            resolvedIRI = isAbsoluteIRI(iri) ? iri :
                    ((baseURI != null && !baseURI.isEmpty()) ? baseURI + iri : ParserConstants.DEFAULT_BASE_URI + iri);
        }
        prefixMap.put(prefix, resolvedIRI);
        model.setNamespace(prefix, resolvedIRI);
    }

    /**
     * Called when a `SPARQL BASE` directive is exited.
     * Updates the base URI, similar to `exitBase`.
     *
     * @param ctx The parse tree context for the `SPARQL BASE` directive.
     */
    @Override
    public void exitSparqlBase(TriGParser.SparqlBaseContext ctx) {
        String newBase = ctx.IRIREF().getText();
        newBase = newBase.substring(1, newBase.length() - 1);
        newBase = unescapeIRI(newBase);

        try {
            if (this.baseURI != null && !this.baseURI.isEmpty() && !isAbsoluteIRI(newBase)) {
                URI currentBase = new URI(this.baseURI);
                URI resolved = currentBase.resolve(newBase);
                this.baseURI = resolved.toString();
            } else {
                this.baseURI = newBase;
            }
        } catch (URISyntaxException e) {
            this.baseURI = newBase;
        }

        prefixMap.put(ParserConstants.EMPTY_STRING, this.baseURI);
        model.setNamespace(ParserConstants.EMPTY_STRING, this.baseURI);
    }

    /**
     * Called when a `SPARQL PREFIX` directive is exited.
     * Handles prefix mappings, similar to `exitPrefixID`.
     *
     * @param ctx The parse tree context for the `SPARQL PREFIX` directive.
     */
    @Override
    public void exitSparqlPrefix(TriGParser.SparqlPrefixContext ctx) {
        String prefix = ctx.PNAME_NS().getText();
        String iri = ctx.IRIREF().getText();
        prefix = prefix.substring(0, prefix.length() - 1);
        iri = iri.substring(1, iri.length() - 1);
        iri = unescapeIRI(iri);

        String resolvedIRI;
        try {
            if (baseURI != null && !baseURI.isEmpty()) {
                URI base = new URI(baseURI);
                URI resolved = base.resolve(iri);
                resolvedIRI = resolved.toString();
            } else {
                resolvedIRI = isAbsoluteIRI(iri) ? iri : ParserConstants.DEFAULT_BASE_URI + iri;
            }
        } catch (URISyntaxException e) {
            resolvedIRI = isAbsoluteIRI(iri) ? iri :
                    ((baseURI != null && !baseURI.isEmpty()) ? baseURI + iri : ParserConstants.DEFAULT_BASE_URI + iri);
        }
        prefixMap.put(prefix, resolvedIRI);
        model.setNamespace(prefix, resolvedIRI);
    }

    /**
     * Called when a graph block is entered.
     * Sets the `currentGraph` context for all triples within this block.
     *
     * @param ctx The parse tree context for the graph block.
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
     * Called when a graph block is exited.
     * Resets the `currentGraph` to `null`.
     *
     * @param ctx The parse tree context for the graph block.
     */
    @Override
    public void exitBlock(TriGParser.BlockContext ctx) {
        currentGraph = null;
    }

    /**
     * Called when a `triplesOrGraph` statement is entered.
     * Handles subjects that are defined before their predicate-object lists.
     *
     * @param ctx The parse tree context for `triplesOrGraph`.
     */
    @Override
    public void enterTriplesOrGraph(TriGParser.TriplesOrGraphContext ctx) {
        if (ctx.labelOrSubject() != null && ctx.predicateObjectList() != null) {
            currentSubject = extractLabelOrSubject(ctx.labelOrSubject());
            processPredicateObjectList(ctx.predicateObjectList());
        } else if (ctx.labelOrSubject() != null && ctx.predicateObjectList() == null) {
            Resource potentialGraph = extractLabelOrSubject(ctx.labelOrSubject());
            currentGraph = potentialGraph;
        }
    }

    /**
     * Called when `triples` (subject-predicate-object) are entered.
     * Sets the current subject for the triple and processes the object list.
     *
     * @param ctx The parse tree context for `triples`.
     */
    @Override
    public void enterTriples(TriGParser.TriplesContext ctx) {
        Resource savedSubject = currentSubject;

        if (ctx.subject() != null) {
            currentSubject = extractSubject(ctx.subject());
        } else if (ctx.blankNodePropertyList() != null) {
            currentSubject = processBlankNodePropertyList(ctx.blankNodePropertyList());
        }

        if (ctx.predicateObjectList() != null) {
            processPredicateObjectList(ctx.predicateObjectList());
        }

        currentSubject = savedSubject;
    }

    /**
     * Called when `triples2` are entered, which handle blank node property lists
     * or collections as the subject.
     *
     * @param ctx The parse tree context for `triples2`.
     */
    @Override
    public void enterTriples2(TriGParser.Triples2Context ctx) {
        Resource savedSubject = currentSubject;

        if (ctx.blankNodePropertyList() != null) {
            currentSubject = processBlankNodePropertyList(ctx.blankNodePropertyList());

            if (ctx.predicateObjectList() != null) {
                processPredicateObjectList(ctx.predicateObjectList());
            }
        } else if (ctx.collection() != null) {
            currentSubject = processCollection(ctx.collection());

            if (ctx.predicateObjectList() != null) {
                processPredicateObjectList(ctx.predicateObjectList());
            }
        }

        currentSubject = savedSubject;
    }

    /**
     * Processes an RDF collection, converting it into a linked list of blank nodes
     * using `rdf:first` and `rdf:rest`.
     *
     * @param ctx The parse tree context for the collection.
     * @return The head of the newly created blank node linked list.
     */
    private Resource processCollection(TriGParser.CollectionContext ctx) {
        List<TriGParser.ObjectContext> objects = ctx.object();

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
    }


    /**
     * Processes a list of predicates and objects, adding statements to the model.
     *
     * @param ctx The parse tree context for the predicate-object list.
     */
    private void processPredicateObjectList(TriGParser.PredicateObjectListContext ctx) {
        for (int i = 0; i < ctx.verb().size(); i++) {
            TriGParser.VerbContext verb = ctx.verb(i);
            TriGParser.ObjectListContext objectList = ctx.objectList(i);

            currentPredicate = extractVerb(verb);

            for (TriGParser.ObjectContext objectCtx : objectList.object()) {
                Value object = extractObject(objectCtx);
                safeAddStatement(currentSubject, currentPredicate, object);
            }
        }
    }

    /**
     * Safely adds a statement to the model with proper error handling for graph contexts.
     * If adding to a named graph fails, it attempts to add to the default graph as a fallback.
     */
    private void safeAddStatement(Resource subject, IRI predicate, Value object) {

        try {
            model.add(subject, predicate, object, currentGraph);
        } catch (Exception e) {
            if (currentGraph != null) {
                try {
                    model.add(subject, predicate, object, null);
                } catch (Exception e2) {
                    throw new ParsingErrorException("Failed to add statement to model: " + e.getMessage() +
                            " (also failed on default graph: " + e2.getMessage() + ")", e);
                }
            } else {
                throw new ParsingErrorException("Failed to add statement to default graph: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Extracts an object from the parse tree context, determining its type (IRI, blank, literal, etc.).
     *
     * @param ctx The parse tree context for the object.
     * @return The extracted RDF value.
     */
    private Value extractObject(TriGParser.ObjectContext ctx) {
        if (ctx.iri() != null) {
            String resolvedIRI = resolveIRI(ctx.iri().getText());
            if (resolvedIRI.isEmpty()) {
                return factory.createIRI(ParserConstants.EMPTY_STRING);
            }
            return factory.createIRI(resolvedIRI);
        }
        if (ctx.blank() != null) {
            return extractBlank(ctx.blank());
        }
        if (ctx.literal() != null) {
            return extractLiteral(ctx.literal());
        }
        if (ctx.blankNodePropertyList() != null) {
            return processBlankNodePropertyList(ctx.blankNodePropertyList());
        }
        throw new RuntimeException("Unsupported object: " + ctx.getText());
    }

    /**
     * Processes a blank node property list, creating a new blank node and
     * processing its properties.
     *
     * @param ctx The parse tree context for the blank node property list.
     * @return The newly created blank node.
     */
    private Resource processBlankNodePropertyList(TriGParser.BlankNodePropertyListContext ctx) {
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
    }

    /**
     * Extracts a subject from the parse tree, which can be an IRI, a blank node,
     * or a collection.
     *
     * @param ctx The parse tree context for the subject.
     * @return The extracted subject as an RDF resource.
     */
    private Resource extractSubject(TriGParser.SubjectContext ctx) {
        if (ctx.iri() != null) {
            return factory.createIRI(resolveIRI(ctx.iri().getText()));
        }
        if (ctx.blank() != null) {
            TriGParser.BlankNodeContext node = ctx.blank().blankNode();
            if (node != null) {
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
        throw new RuntimeException("Unsupported subject: " + ctx.getText());
    }

    /**
     * Extracts a blank node, which can be a named blank node, an anonymous blank node,
     * or a collection.
     *
     * @param ctx The parse tree context for the blank node.
     * @return The extracted blank node resource.
     */
    private Resource extractBlank(TriGParser.BlankContext ctx) {
        TriGParser.BlankNodeContext node = ctx.blankNode();
        if (node != null) {
            if (node.BLANK_NODE_LABEL() != null)
                return factory.createBNode(node.BLANK_NODE_LABEL().getText().substring(2));
            if (node.ANON() != null)
                return factory.createBNode();
        }

        TriGParser.CollectionContext collection = ctx.collection();
        if (collection != null) {
            return processCollection(collection);
        }

        throw new RuntimeException("Unsupported blank node structure: " + ctx.getText());
    }

    /**
     * Extracts the label or subject for a named graph.
     *
     * @param ctx The parse tree context.
     * @return The extracted graph name as a resource.
     * @throws ParsingErrorException if the format is unsupported.
     */
    private Resource extractLabelOrSubject(TriGParser.LabelOrSubjectContext ctx) {
        if (ctx.iri() != null) {
            String iriText = ctx.iri().getText();
            String resolvedIRI = resolveIRI(iriText);
            return factory.createIRI(resolvedIRI);
        }
        if (ctx.blankNode() != null) {
            if (ctx.blankNode().BLANK_NODE_LABEL() != null) {
                String blankNodeId = ctx.blankNode().BLANK_NODE_LABEL().getText().substring(2);
                return factory.createBNode(blankNodeId);
            }
            if (ctx.blankNode().ANON() != null) {
                return factory.createBNode();
            }
        }
        throw new ParsingErrorException("Unsupported label or subject for a named graph: " + ctx.getText());
    }

    /**
     * Extracts the verb (predicate) for a statement.
     *
     * @param ctx The parse tree context for the verb.
     * @return The extracted verb as an IRI.
     */
    private IRI extractVerb(TriGParser.VerbContext ctx) {
        String verbText = ctx.getText();
        String resolvedIRI = resolveIRI(verbText);
        if (resolvedIRI.isEmpty()) {
            return factory.createIRI(ParserConstants.EMPTY_STRING);
        }
        IRI result = factory.createIRI(resolvedIRI);
        return result;
    }

    /**
     * Extracts a literal value from the parse tree.
     * Handles string literals (with or without language tags/datatypes), booleans, and numerics.
     *
     * @param ctx The parse tree context for the literal.
     * @return The extracted literal value.
     */
    private Literal extractLiteral(TriGParser.LiteralContext ctx) {
        if (ctx.rDFLiteral() != null) {

            String label = unescapeString(ctx.rDFLiteral().string().getText());
            if (ctx.rDFLiteral().LANGTAG() != null)
                return factory.createLiteral(label, ctx.rDFLiteral().LANGTAG().getText().substring(1));
            if (ctx.rDFLiteral().iri() != null)
                return factory.createLiteral(label, factory.createIRI(resolveIRI(ctx.rDFLiteral().iri().getText())));
            return factory.createLiteral(label);
        }
        if (ctx.BooleanLiteral() != null)
            return factory.createLiteral(ctx.BooleanLiteral().getText(), XSD.BOOLEAN.getIRI());
        if (ctx.numericLiteral() != null) {
            String numericText = ctx.numericLiteral().getText();

            boolean isNegative = numericText.startsWith(ParserConstants.MINUS);
            String absoluteValue = isNegative ? numericText.substring(1) : numericText;

            if (ctx.numericLiteral().DOUBLE() != null || absoluteValue.toLowerCase().contains(ParserConstants.E)) {
                return factory.createLiteral(numericText, XSD.DOUBLE.getIRI());
            } else if (ctx.numericLiteral().DECIMAL() != null || absoluteValue.contains(ParserConstants.POINT)) {
                return factory.createLiteral(numericText, XSD.DECIMAL.getIRI());
            } else if (ctx.numericLiteral().INTEGER() != null) {
                return factory.createLiteral(numericText, XSD.INTEGER.getIRI());
            } else {
                if (absoluteValue.contains(ParserConstants.POINT)) {
                    return factory.createLiteral(numericText, XSD.DECIMAL.getIRI());
                } else {
                    return factory.createLiteral(numericText, XSD.INTEGER.getIRI());
                }
            }
        }
        throw new RuntimeException("Unsupported literal: " + ctx.getText());
    }

    /**
     * Resolves a raw IRI string from the TriG document into an absolute IRI.
     * Handles IRIREFs, prefixed names, the 'a' keyword, and bare identifiers.
     *
     * @param raw The raw IRI string.
     * @return The resolved absolute IRI string.
     * @throws ParsingErrorException if the IRI format is invalid or the prefix is undeclared.
     */
    private String resolveIRI(String raw) {
        try {
            raw = raw.trim();

            if (raw.equals(ParserConstants.A)) {
                return RDF.type.getIRI().stringValue();
            }

            if (raw.startsWith(ParserConstants.IRI_START) && raw.endsWith(ParserConstants.IRI_END)) {
                String iri = raw.substring(1, raw.length() - 1);
                iri = unescapeIRI(iri);
                if (isAbsoluteIRI(iri)) {
                    return iri;
                }
                if (this.baseURI != null && this.baseURI.startsWith(ParserConstants.FILE_PROTOCOL)) {
                    String resolved = resolveRelativeIRIForFile(iri, this.baseURI);
                    return resolved;
                } else {
                    String resolved = resolveRelativeIRI(iri);
                    return resolved;
                }
            }

            if (raw.contains(ParserConstants.COLON)) {
                String[] parts = raw.split(ParserConstants.COLON, 2);
                String prefix = parts[0];
                String localName = parts[1];

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

                throw new ParsingErrorException("Undeclared prefix: " + prefix);
            }

            if (raw.matches(ParserConstants.PNAME_NS_PATTERN)) {
                String resolved = resolveRelativeIRI(raw);
                return resolved;
            }

            if (!raw.contains(ParserConstants.SPACE) && !raw.contains(ParserConstants.TAB) && !raw.contains(ParserConstants.LINE_FEED)) {
                String resolved = resolveRelativeIRI(raw);
                return resolved;
            }

            throw new ParsingErrorException("Invalid IRI format: '" + raw + "'");

        } catch (IllegalArgumentException e) {
            throw new ParsingErrorException(e.getMessage(), e);
        }
    }

    private String resolveRelativeIRIForFile(String relativeIRI, String baseURI) {
        try {
            URI baseUri = new URI(baseURI);
            String basePath = baseUri.getPath();

            if (relativeIRI.isEmpty()) {
                return new URI(baseUri.getScheme(), baseUri.getAuthority(),
                        basePath, baseUri.getQuery(), null).toString();
            }

            if (relativeIRI.startsWith(ParserConstants.DOUBLE_SLASH)) {
                return "file:" + relativeIRI;
            }

            if (relativeIRI.startsWith(ParserConstants.SLASH)) {
                return ParserConstants.FILE_PROTOCOL + normalizePathForFileURI(relativeIRI, "file");
            }

            if (relativeIRI.startsWith(ParserConstants.QUERY_MARK)) {
                return new URI(baseUri.getScheme(), baseUri.getAuthority(),
                        basePath, relativeIRI.substring(1), null).toString();
            }

            if (relativeIRI.startsWith(ParserConstants.FRAGMENT)) {
                return new URI(baseUri.getScheme(), baseUri.getAuthority(),
                        basePath, baseUri.getQuery(), relativeIRI.substring(1)).toString();
            }

            if (relativeIRI.startsWith(ParserConstants.SEMICOLON)) {
                String newPath = basePath + relativeIRI;
                return new URI(baseUri.getScheme(), baseUri.getAuthority(),
                        newPath, baseUri.getQuery(), baseUri.getFragment()).toString();
            }

            String baseDir = basePath.contains(ParserConstants.SLASH)
                    ? basePath.substring(0, basePath.lastIndexOf('/') + 1)
                    : ParserConstants.SLASH ;

            String resolvedPath = normalizePathForFileURI(baseDir + relativeIRI, "file");
            return new URI(baseUri.getScheme(), baseUri.getAuthority(),
                    resolvedPath, baseUri.getQuery(), baseUri.getFragment()).toString();

        } catch (URISyntaxException e) {
            return resolveRelativeIRIFallback(relativeIRI, baseURI);
        }
    }

    /**
     * Checks if a given IRI string is an absolute IRI.
     *
     * @param iri The IRI string to check.
     * @return `true` if it's an absolute IRI, otherwise `false`.
     */
    private boolean isAbsoluteIRI(String iri) {
        return iri.contains(ParserConstants.COLON) && !iri.startsWith(ParserConstants.COLON);
    }

    /**
     * Gets the effective base URI, providing a default if none is set.
     *
     * @return The effective base URI string.
     */
    private String getEffectiveBaseURI() {
        String effective = (baseURI != null && !baseURI.isEmpty()) ? baseURI : ParserConstants.RDF_TRG_TEST_SUITE_URI;

        if (effective.startsWith(ParserConstants.FILE_PROTOCOL_SIMPLE) && !effective.startsWith(ParserConstants.FILE_PROTOCOL_TRIPLE_SLASH)) {
            effective = effective.replaceFirst(ParserConstants.FILE_PROTOCOL_SIMPLE, ParserConstants.FILE_PROTOCOL_TRIPLE_SLASH);
        }

        return effective;
    }

    /**
     * Resolves a relative IRI against the base URI with full RFC 3986 compliance.
     * This method handles edge cases that Java's URI.resolve() doesn't handle correctly.
     *
     * @param relativeIRI The relative IRI string.
     * @return The resolved absolute IRI string.
     */
    private String resolveRelativeIRI(String relativeIRI) {
        String effectiveBaseURI = getEffectiveBaseURI();

        if (relativeIRI.isEmpty()) {
            return resolveEmptyReference(effectiveBaseURI);
        }

        if (relativeIRI.startsWith(ParserConstants.QUERY_MARK)) {
            return resolveQueryOnlyReference(relativeIRI, effectiveBaseURI);
        } else if (relativeIRI.startsWith(ParserConstants.FRAGMENT)) {
            return resolveFragmentOnlyReference(relativeIRI, effectiveBaseURI);
        } else if (relativeIRI.startsWith(ParserConstants.DOUBLE_SLASH)) {
            return resolveNetworkPathReference(relativeIRI, effectiveBaseURI);
        } else if (relativeIRI.startsWith(ParserConstants.SEMICOLON)) {
            return resolveSemicolonReference(relativeIRI, effectiveBaseURI);
        }

        if (effectiveBaseURI.startsWith(ParserConstants.FILE_PROTOCOL)) {
            return resolveFileURI(relativeIRI, effectiveBaseURI);
        }

        if (hasDoubleSlashesInPath(effectiveBaseURI)) {
            return resolveRelativeIRIWithDoubleSlashes(relativeIRI, effectiveBaseURI);
        }

        try {
            URI base = new URI(effectiveBaseURI);
            URI resolved = base.resolve(relativeIRI);
            String result = resolved.normalize().toString();
            return normalizeResolvedURI(result);
        } catch (URISyntaxException e) {
            return resolveRelativeIRIFallback(relativeIRI, effectiveBaseURI);
        }
    }

    private boolean hasDoubleSlashesInPath(String baseURI) {
        if (baseURI == null || baseURI.isEmpty()) {
            return false;
        }

        int schemeEnd = baseURI.indexOf(ParserConstants.SCHEME_DELIMITER);
        if (schemeEnd == -1) {
            return false;
        }

        String afterScheme = baseURI.substring(schemeEnd + 3);

        int pathStart = afterScheme.indexOf('/');
        if (pathStart == -1) {
            return false;
        }

        String path = afterScheme.substring(pathStart);

        return path.contains(ParserConstants.DOUBLE_SLASH);
    }


    private String resolveFileURI(String relativeIRI, String baseURI) {
        try {
            URI base = new URI(baseURI);
            URI resolved = base.resolve(relativeIRI);

            String result = resolved.toString();

            if (result.startsWith(ParserConstants.FILE_PROTOCOL_SIMPLE) && !result.startsWith(ParserConstants.FILE_PROTOCOL_TRIPLE_SLASH)) {
                result = result.replaceFirst(ParserConstants.FILE_PROTOCOL_SIMPLE, ParserConstants.FILE_PROTOCOL_TRIPLE_SLASH);
            }

            return result;

        } catch (URISyntaxException e) {
            return resolveFileURIManual(relativeIRI, baseURI);
        }
    }

    private String resolveFileURIManual(String relativeIRI, String baseURI) {
        String basePath = baseURI.substring(7);

        if (relativeIRI.startsWith(ParserConstants.SLASH)) {
            return ParserConstants.FILE_PROTOCOL + relativeIRI;
        } else if (relativeIRI.startsWith(ParserConstants.QUERY_MARK)) {
            int queryIndex = baseURI.indexOf('?');
            String baseWithoutQuery = (queryIndex >= 0) ? baseURI.substring(0, queryIndex) : baseURI;
            return baseWithoutQuery + relativeIRI;
        } else if (relativeIRI.startsWith(ParserConstants.FRAGMENT)) {
            int fragmentIndex = baseURI.indexOf('#');
            String baseWithoutFragment = (fragmentIndex >= 0) ? baseURI.substring(0, fragmentIndex) : baseURI;
            return baseWithoutFragment + relativeIRI;
        } else if (relativeIRI.isEmpty()) {
            int fragmentIndex = baseURI.indexOf('#');
            return (fragmentIndex >= 0) ? baseURI.substring(0, fragmentIndex) : baseURI;
        } else {
            String baseDirectory;
            if (basePath.contains(ParserConstants.SLASH)) {
                int lastSlash = basePath.lastIndexOf('/');
                baseDirectory = basePath.substring(0, lastSlash + 1);
            } else {
                baseDirectory = ParserConstants.SLASH ;
            }
            return ParserConstants.FILE_PROTOCOL + baseDirectory + relativeIRI;
        }
    }

    private String resolveRelativeIRIWithDoubleSlashes(String relativeIRI, String baseURI) {
        try {
            int schemeEnd = baseURI.indexOf(ParserConstants.SCHEME_DELIMITER);
            if (schemeEnd == -1) {
                return resolveRelativeIRIFallback(relativeIRI, baseURI);
            }

            String scheme = baseURI.substring(0, schemeEnd);
            String authorityAndPath = baseURI.substring(schemeEnd + 3);

            int pathStart = authorityAndPath.indexOf('/');
            if (pathStart == -1) {
                return scheme + ParserConstants.SCHEME_DELIMITER + authorityAndPath + ParserConstants.SLASH + relativeIRI;
            }

            String authority = authorityAndPath.substring(0, pathStart);
            String path = authorityAndPath.substring(pathStart);

            String resolvedPath;
            if (relativeIRI.startsWith(ParserConstants.SLASH)) {
                resolvedPath = normalizePath(relativeIRI);
            } else if (relativeIRI.isEmpty() || relativeIRI.equals(ParserConstants.POINT) || relativeIRI.equals("./")) {
                resolvedPath = removeLastSegment(path);
            } else if (relativeIRI.equals(ParserConstants.DOUBLE_DOT) || relativeIRI.equals("../")) {
                resolvedPath = removeLastSegment(removeLastSegment(path));
            } else {
                String basePath = path.endsWith(ParserConstants.SLASH) ? path : removeLastSegment(path) + ParserConstants.SLASH ;
                resolvedPath = normalizePath(basePath + relativeIRI);
            }

            return scheme + ParserConstants.SCHEME_DELIMITER + authority + resolvedPath;

        } catch (Exception e) {
            return resolveRelativeIRIFallback(relativeIRI, baseURI);
        }
    }

    private String removeLastSegment(String path) {
        if (path == null || path.isEmpty() || path.equals(ParserConstants.SLASH)) {
            return ParserConstants.SLASH ;
        }

        int lastSlash = path.lastIndexOf('/');
        if (lastSlash == 0) {
            return ParserConstants.SLASH ;
        }

        return path.substring(0, lastSlash);
    }


    private String resolveNetworkPathReference(String networkPathRef, String baseURI) {
        try {
            URI base = new URI(baseURI);
            String scheme = base.getScheme();

            if ("file".equals(scheme)) {
                return "file:" + networkPathRef;
            }

            return scheme + ParserConstants.COLON + networkPathRef;
        } catch (URISyntaxException e) {
            int schemeEnd = baseURI.indexOf(ParserConstants.SCHEME_DELIMITER);
            if (schemeEnd >= 0) {
                String scheme = baseURI.substring(0, schemeEnd);
                return scheme + ParserConstants.COLON + networkPathRef;
            }
            return networkPathRef;
        }
    }

    /**
     * Resolves a query-only relative reference (e.g., "?y") against a base URI.
     * Per RFC 3986, this should keep the base path and parameters, replacing only the query.
     */
    private String resolveQueryOnlyReference(String queryRef, String baseURI) {
        if (baseURI.startsWith(ParserConstants.FILE_PROTOCOL)) {
            int queryIndex = baseURI.indexOf('?');
            int fragmentIndex = baseURI.indexOf('#');

            String baseWithoutQueryFragment;
            String rest = ParserConstants.EMPTY_STRING ;

            if (queryIndex >= 0) {
                baseWithoutQueryFragment = baseURI.substring(0, queryIndex);
            } else if (fragmentIndex >= 0) {
                baseWithoutQueryFragment = baseURI.substring(0, fragmentIndex);
            } else {
                baseWithoutQueryFragment = baseURI;
            }

            return baseWithoutQueryFragment + queryRef;
        }

        return baseURI.replaceFirst("\\?.*", ParserConstants.EMPTY_STRING).replaceFirst("#.*", ParserConstants.EMPTY_STRING) + queryRef;
    }

    /**
     * Resolves a fragment-only relative reference (e.g., "#s") against a base URI.
     * Per RFC 3986, this should keep everything from the base, replacing only the fragment.
     */
    private String resolveFragmentOnlyReference(String fragmentRef, String baseURI) {
        return baseURI.replaceFirst("#.*", ParserConstants.EMPTY_STRING) + fragmentRef;
    }

    /**
     * Resolves an empty relative reference (ParserConstants.EMPTY_STRING) against a base URI.
     * Per RFC 3986, this should return the base URI without its fragment.
     */
    private String resolveEmptyReference(String baseURI) {
        try {
            URI base = new URI(baseURI);

            URI resolved = new URI(
                    base.getScheme(),
                    base.getAuthority(),
                    base.getPath(),
                    base.getQuery(),
                    null
            );

            return resolved.toString();
        } catch (URISyntaxException e) {
            int fragmentIndex = baseURI.indexOf('#');
            return (fragmentIndex >= 0) ? baseURI.substring(0, fragmentIndex) : baseURI;
        }
    }

    /**
     * Additional normalization for resolved URIs to handle edge cases
     * that Java's URI.normalize() doesn't cover properly.
     *
     * @param resolvedURI The URI string after initial resolution.
     * @return The fully normalized URI string.
     */
    private String normalizeResolvedURI(String resolvedURI) {
        try {
            URI uri = new URI(resolvedURI);
            String path = uri.getPath();

            if (path != null) {
                String normalizedPath = normalizePathForFileURI(path, uri.getScheme());

                if (!normalizedPath.equals(path)) {
                    URI newUri = new URI(uri.getScheme(), uri.getAuthority(),
                            normalizedPath, uri.getQuery(), uri.getFragment());
                    return newUri.toString();
                }
            }

            return resolvedURI;
        } catch (URISyntaxException e) {
            return resolvedURI;
        }
    }

    private String normalizePathForFileURI(String path, String scheme) {
        if (path == null || path.isEmpty()) {
            return path;
        }

        if ("file".equals(scheme)) {
            String[] segments = path.split(ParserConstants.SLASH, -1);
            List<String> normalizedSegments = new ArrayList<>();
            int depth = 0;

            for (String segment : segments) {
                if (ParserConstants.POINT.equals(segment) || segment.isEmpty()) {
                    continue;
                } else if (ParserConstants.DOUBLE_DOT.equals(segment)) {
                    if (depth > 0) {
                        normalizedSegments.remove(normalizedSegments.size() - 1);
                        depth--;
                    }
                } else {
                    normalizedSegments.add(segment);
                    if (!segment.isEmpty()) {
                        depth++;
                    }
                }
            }

            String result = String.join(ParserConstants.SLASH, normalizedSegments);

            if (path.startsWith(ParserConstants.SLASH) && !result.startsWith(ParserConstants.SLASH)) {
                result = ParserConstants.SLASH + result;
            }

            if (result.isEmpty()) {
                result = path.startsWith(ParserConstants.SLASH) ? ParserConstants.SLASH : ParserConstants.EMPTY_STRING ;
            }

            return result;
        } else {
            return normalizePathSegments(path);
        }
    }

    /**
     * Normalizes path segments to remove redundant . and .. components
     * according to RFC 3986 section 5.2.4.
     *
     * @param path The path to normalize.
     * @return The normalized path.
     */
    private String normalizePathSegments(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }

        String[] segments = path.split(ParserConstants.SLASH, -1);
        List<String> normalizedSegments = new ArrayList<>();

        for (String segment : segments) {
            if (ParserConstants.POINT.equals(segment)) {
                continue;
            } else if (ParserConstants.DOUBLE_DOT.equals(segment)) {
                if (!normalizedSegments.isEmpty() &&
                        !normalizedSegments.get(normalizedSegments.size() - 1).isEmpty()) {
                    normalizedSegments.remove(normalizedSegments.size() - 1);
                }
            } else {
                normalizedSegments.add(segment);
            }
        }

        String result = String.join(ParserConstants.SLASH, normalizedSegments);

        if (result.isEmpty() && path.startsWith(ParserConstants.SLASH)) {
            result = ParserConstants.SLASH ;
        }

        return result;
    }

    /**
     * Fallback method for relative IRI resolution when Java's URI class fails.
     * This manually implements RFC 3986 resolution rules.
     *
     * @param relativeIRI      The relative IRI string.
     * @param effectiveBaseURI The base URI to resolve against.
     * @return The resolved absolute IRI string.
     */
    private String resolveRelativeIRIFallback(String relativeIRI, String effectiveBaseURI) {
        switch (relativeIRI) {
            case ParserConstants.EMPTY_STRING:
            case ParserConstants.POINT:
            case "./":
                return removeFragment(effectiveBaseURI);

            case ParserConstants.DOUBLE_DOT:
            case "../":
                return resolveToParent(effectiveBaseURI);

            case ";x":
                return resolveSemicolonReference(relativeIRI, effectiveBaseURI);

            case "?y":
                return removeQueryAndFragment(effectiveBaseURI) + "?y" ;

            case "#s":
                return removeFragment(effectiveBaseURI) + "#s" ;

            default:
                try {
                    URI baseUri = new URI(effectiveBaseURI);
                    return resolveRelativeIRIManual(relativeIRI,
                            baseUri.getScheme(),
                            baseUri.getAuthority(),
                            baseUri.getPath(),
                            baseUri.getQuery(),
                            baseUri.getFragment());
                } catch (URISyntaxException e) {
                    return effectiveBaseURI + relativeIRI;
                }
        }
    }

    private String resolveToParent(String uri) {
        try {
            URI base = new URI(uri);
            String path = base.getPath();

            if (path != null && path.contains(ParserConstants.SLASH)) {
                int lastSlash = path.lastIndexOf('/');
                if (lastSlash > 0) {
                    path = path.substring(0, lastSlash);
                } else {
                    path = ParserConstants.SLASH ;
                }
            } else {
                path = ParserConstants.SLASH ;
            }

            URI resolved = new URI(
                    base.getScheme(),
                    base.getAuthority(),
                    path,
                    base.getQuery(),
                    base.getFragment()
            );

            return resolved.toString();
        } catch (URISyntaxException e) {
            int lastSlash = uri.lastIndexOf('/');
            if (lastSlash > uri.indexOf(ParserConstants.SCHEME_DELIMITER) + 3) {
                return uri.substring(0, lastSlash);
            }
            return uri;
        }
    }

    private String resolveRelativeIRIManual(String relativeIRI, String scheme, String authority,
                                            String path, String query, String fragment) {


        if (relativeIRI.contains(ParserConstants.COLON)) {
            int relativeSchemeEnd = relativeIRI.indexOf(ParserConstants.COLON);
            String potentialScheme = relativeIRI.substring(0, relativeSchemeEnd);

            if (isValidScheme(potentialScheme)) {

                return relativeIRI;
            }
        }


        if (relativeIRI.startsWith(ParserConstants.DOUBLE_SLASH)) {

            return scheme + ParserConstants.COLON + relativeIRI;
        }

        if (relativeIRI.startsWith(ParserConstants.SLASH)) {
            path = normalizePath(relativeIRI);
            query = null;
            fragment = null;
        } else if (relativeIRI.startsWith(ParserConstants.QUERY_MARK)) {
            query = relativeIRI.substring(1);
            fragment = null;
        } else if (relativeIRI.startsWith(ParserConstants.FRAGMENT)) {
            fragment = relativeIRI.substring(1);
        } else if (!relativeIRI.isEmpty()) {
            String basePath = path;
            if (basePath == null || basePath.isEmpty()) {
                basePath = ParserConstants.SLASH ;
            } else if (!basePath.endsWith(ParserConstants.SLASH)) {
                int lastSlash = basePath.lastIndexOf('/');
                if (lastSlash >= 0) {
                    basePath = basePath.substring(0, lastSlash + 1);
                } else {
                    basePath = ParserConstants.SLASH ;
                }
            }
            path = normalizePath(basePath + relativeIRI);
            query = null;
            fragment = null;
        }

        StringBuilder result = new StringBuilder();
        result.append(scheme).append(ParserConstants.SCHEME_DELIMITER).append(authority);

        if (path != null && !path.isEmpty()) {
            result.append(path);
        }

        if (query != null && !query.isEmpty()) {
            result.append(ParserConstants.QUERY_MARK).append(query);
        }

        if (fragment != null && !fragment.isEmpty()) {
            result.append(ParserConstants.FRAGMENT).append(fragment);
        }

        return result.toString();
    }

    private boolean isValidScheme(String potentialScheme) {
        if (potentialScheme.isEmpty()) {
            return false;
        }

        if (!Character.isLetter(potentialScheme.charAt(0))) {
            return false;
        }

        for (int i = 0; i < potentialScheme.length(); i++) {
            char c = potentialScheme.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '+' && c != '-' && c != '.') {
                return false;
            }
        }

        return true;
    }

    private String resolveSemicolonReference(String ref, String baseURI) {
        if (baseURI.startsWith(ParserConstants.FILE_PROTOCOL)) {
            int queryIndex = baseURI.indexOf('?');
            int fragmentIndex = baseURI.indexOf('#');

            String pathPart;
            String rest = ParserConstants.EMPTY_STRING ;

            if (queryIndex >= 0) {
                pathPart = baseURI.substring(0, queryIndex);
                rest = baseURI.substring(queryIndex);
            } else if (fragmentIndex >= 0) {
                pathPart = baseURI.substring(0, fragmentIndex);
                rest = baseURI.substring(fragmentIndex);
            } else {
                pathPart = baseURI;
            }

            return pathPart + ref + rest;
        }

        return baseURI.replaceFirst(";.*", ParserConstants.EMPTY_STRING) + ref;
    }

    private String removeFragment(String uri) {
        int fragmentIndex = uri.indexOf('#');
        return (fragmentIndex >= 0) ? uri.substring(0, fragmentIndex) : uri;
    }

    private String removeQueryAndFragment(String uri) {
        int queryIndex = uri.indexOf('?');
        if (queryIndex >= 0) {
            return uri.substring(0, queryIndex);
        }
        return removeFragment(uri);
    }

    /**
     * Normalizes a URI path by processing `.` (current directory) and `..`
     * (parent directory) segments, as per RFC 3986.
     *
     * @param path The path string to normalize.
     * @return The normalized path.
     */
    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return ParserConstants.EMPTY_STRING ;
        }

        if (path.startsWith(ParserConstants.DOUBLE_SLASH)) {
            String remaining = path.substring(2);
            String normalizedRemaining = normalizeSimplePath(remaining);
            return ParserConstants.DOUBLE_SLASH + normalizedRemaining;
        }

        return normalizeSimplePath(path);
    }

    private String normalizeSimplePath(String path) {
        if (path == null || path.isEmpty()) {
            return ParserConstants.EMPTY_STRING ;
        }

        String[] segments = path.split(ParserConstants.SLASH, -1);
        List<String> result = new ArrayList<>();

        for (String segment : segments) {
            if (segment.equals(ParserConstants.POINT) || segment.isEmpty()) {
                continue;
            } else if (segment.equals(ParserConstants.DOUBLE_DOT)) {
                if (!result.isEmpty() && !result.get(result.size() - 1).equals(ParserConstants.DOUBLE_DOT)) {
                    result.remove(result.size() - 1);
                } else {
                    result.add(ParserConstants.DOUBLE_DOT);
                }
            } else {
                result.add(segment);
            }
        }

        String normalized = String.join(ParserConstants.SLASH, result);

        boolean startsWithSlash = path.startsWith(ParserConstants.SLASH);
        boolean endsWithSlash = path.endsWith(ParserConstants.SLASH);

        if (startsWithSlash && !normalized.startsWith(ParserConstants.SLASH)) {
            normalized = ParserConstants.SLASH + normalized;
        }

        if (endsWithSlash && !normalized.endsWith(ParserConstants.SLASH) && !normalized.isEmpty()) {
            normalized = normalized + ParserConstants.SLASH ;
        }

        return normalized;
    }

    /**
     * Unescapes Unicode escape sequences (`\ u` and `\U`) from an IRI string.
     *
     * @param rawIri The raw IRI string to unescape.
     * @return The unescaped IRI string.
     * @throws IllegalArgumentException if the escape sequence is invalid.
     */
    private String unescapeIRI(String rawIri) {
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
                                    throw new IllegalArgumentException("Surrogates not allowed in IRIREF: \\u" + hex);
                                }

                                sb.appendCodePoint(codePoint);
                                i += len + 1;
                            } catch (NumberFormatException e) {
                                throw new IllegalArgumentException("Invalid hexadecimal value in IRI escape.", e);
                            }
                        } else {
                            throw new IllegalArgumentException("Incomplete Unicode escape in IRI.");
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
     * Unescapes escape sequences (e.g., `\t`, `\n`, `\"`) and Unicode
     * escapes from a string literal.
     *
     * @param text The raw string literal to unescape.
     * @return The unescaped string.
     * @throws IllegalArgumentException if the escape sequence is invalid.
     */
    private String unescapeString(String text) {
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
            if (c == '\\') {
                if (i + 1 < content.length()) {
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
                        case '\"':
                            sb.append('\"');
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
                                        throw new IllegalArgumentException("Invalid Unicode escape sequence: Surrogate code points are not allowed.");
                                    }

                                    sb.appendCodePoint(codePoint);
                                    i += len + 1;
                                } catch (NumberFormatException e) {
                                    throw new IllegalArgumentException("Invalid Unicode escape sequence: Invalid hexadecimal value.", e);
                                }
                            } else {
                                throw new IllegalArgumentException("Incomplete Unicode escape sequence.");
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
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
