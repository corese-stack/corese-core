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
import java.util.*;

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
            return factory.createIRI(resolveIRI(ctx.iri().getText()));
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
        return factory.createIRI(resolveIRI(ctx.getText()));
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

            boolean isNegative = numericText.startsWith("-");
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
                String resolved = resolveRelativeIRI(iri);
                return resolved;
            }

            if (raw.contains(ParserConstants.COLON)) {
                String[] parts = raw.split(ParserConstants.COLON, 2);
                String prefix = parts[0];
                String localName = parts[1];
                localName = unescapeIRI(localName);

                if (prefix.isEmpty()) {
                    String defaultNS = prefixMap.get(ParserConstants.EMPTY_STRING);
                    if (defaultNS != null) {
                        String resolved = defaultNS + localName;
                        return resolved;
                    } else {
                        throw new ParsingErrorException("No default namespace defined for empty prefix");
                    }
                }

                String ns = prefixMap.get(prefix);
                if (ns != null) {
                    String resolved = ns + localName;
                    return resolved;
                }
                throw new ParsingErrorException("Undeclared prefix: " + prefix);
            }

            // Handle bare identifiers as relative IRIs (common in TriG for graph names)
            if (raw.matches("^[A-Za-z_][A-Za-z0-9_-]*$")) {
                String resolved = resolveRelativeIRI(raw);
                return resolved;
            }

            // For any other pattern that looks like it could be a relative IRI
            if (!raw.contains(ParserConstants.SPACE) && !raw.contains(ParserConstants.TAB) && !raw.contains(ParserConstants.LINE_FEED)) {
                String resolved = resolveRelativeIRI(raw);
                return resolved;
            }

            // If it doesn't match any valid pattern, throw an error
            throw new ParsingErrorException("Invalid IRI format: '" + raw + "'. Must be an IRIREF (<...>), a prefixed name (prefix:local), a bare identifier, or the keyword 'a'.");

        } catch (IllegalArgumentException e) {
            throw new ParsingErrorException(e.getMessage(), e);
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
        if (baseURI != null && !baseURI.isEmpty()) {
            return baseURI;
        }
        return ParserConstants.RDF_TRG_TEST_SUITE_URI;
    }

    /**
     * Manually resolves a relative IRI against the base URI, implementing RFC 3986.
     * This handles paths, queries, and fragments, ensuring correct resolution.
     *
     * @param relativeIRI The relative IRI string.
     * @return The resolved absolute IRI string.
     */
    private String resolveRelativeIRI(String relativeIRI) {
        String effectiveBaseURI = getEffectiveBaseURI();

        try {
            URI base = new URI(effectiveBaseURI);

            String relativePath;
            String relativeQuery = null;
            String relativeFragment = null;

            String remaining = relativeIRI;
            int fragmentIndex = remaining.indexOf('#');
            if (fragmentIndex >= 0) {
                if (fragmentIndex < remaining.length() - 1) {
                    relativeFragment = remaining.substring(fragmentIndex + 1);
                } else {
                    relativeFragment = ParserConstants.EMPTY_STRING;
                }
                remaining = remaining.substring(0, fragmentIndex);
            }

            int queryIndex = remaining.indexOf('?');
            if (queryIndex >= 0) {
                if (queryIndex < remaining.length() - 1) {
                    relativeQuery = remaining.substring(queryIndex + 1);
                } else {
                    relativeQuery = ParserConstants.EMPTY_STRING;
                }
                remaining = remaining.substring(0, queryIndex);
            }

            relativePath = remaining;

            String targetScheme = base.getScheme();
            String targetAuthority = base.getAuthority();
            String targetPath;
            String targetQuery;
            String targetFragment = relativeFragment;

            if (relativeIRI.startsWith(ParserConstants.DOUBLE_SLASH)) {
                String authorityAndPath = relativeIRI.substring(2);
                int pathStart = authorityAndPath.indexOf('/');
                if (pathStart >= 0) {
                    targetAuthority = authorityAndPath.substring(0, pathStart);
                    String pathAndRest = authorityAndPath.substring(pathStart);

                    fragmentIndex = pathAndRest.indexOf('#');
                    if (fragmentIndex >= 0) {
                        targetFragment = fragmentIndex < pathAndRest.length() - 1 ?
                                pathAndRest.substring(fragmentIndex + 1) : ParserConstants.EMPTY_STRING;
                        pathAndRest = pathAndRest.substring(0, fragmentIndex);
                    }

                    queryIndex = pathAndRest.indexOf('?');
                    if (queryIndex >= 0) {
                        targetQuery = queryIndex < pathAndRest.length() - 1 ?
                                pathAndRest.substring(queryIndex + 1) : ParserConstants.EMPTY_STRING;
                        pathAndRest = pathAndRest.substring(0, queryIndex);
                    } else {
                        targetQuery = null;
                    }

                    targetPath = normalizePath(pathAndRest);
                } else {
                    targetAuthority = authorityAndPath;
                    targetPath = ParserConstants.EMPTY_STRING;
                    targetQuery = relativeQuery;
                }
            } else if (relativePath.startsWith(ParserConstants.SLASH)) {
                targetPath = normalizePath(relativePath);
                targetQuery = relativeQuery;
            } else if (!relativePath.isEmpty()) {
                String basePath = base.getPath();
                if (basePath == null || basePath.isEmpty()) {
                    basePath = ParserConstants.SLASH;
                } else if (!basePath.endsWith(ParserConstants.SLASH)) {
                    int lastSlash = basePath.lastIndexOf('/');
                    if (lastSlash != -1) {
                        basePath = basePath.substring(0, lastSlash + 1);
                    } else {
                        basePath = ParserConstants.SLASH;
                    }
                }
                targetPath = normalizePath(basePath + relativePath);
                targetQuery = relativeQuery;
            } else {
                targetPath = base.getPath();
                if (relativeQuery != null) {
                    targetQuery = relativeQuery;
                } else {
                    targetQuery = base.getQuery();
                }
            }

            boolean relativeEndsWithSlash = relativeIRI.endsWith(ParserConstants.SLASH);
            boolean relativeIsDirectoryOnly = relativePath.matches("^(\\.\\./)*\\.\\./?$") ||
                    relativePath.equals("./") || relativePath.equals("../");
            boolean relativeEndsWithDotSlash = relativePath.endsWith("/.");

            boolean baseWasDirectory = effectiveBaseURI.endsWith(ParserConstants.SLASH);
            boolean relativeIsDirectoryRef = relativePath.isEmpty() || relativePath.equals(ParserConstants.POINT) || relativePath.equals(ParserConstants.DOUBLE_DOT);

            if ((relativeEndsWithSlash || relativeIsDirectoryOnly || relativeEndsWithDotSlash ||
                    (baseWasDirectory && relativeIsDirectoryRef)) &&
                    !targetPath.endsWith(ParserConstants.SLASH) && !targetPath.isEmpty()) {
                targetPath += ParserConstants.SLASH;
            }

            URI result = new URI(targetScheme, targetAuthority, targetPath, targetQuery, targetFragment);
            String resolved = result.toString();
            return resolved;

        } catch (URISyntaxException e) {
            throw new RuntimeException("URI syntax error during resolution: Base: " + effectiveBaseURI + ", Relative: " + relativeIRI, e);
        }
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
            return ParserConstants.EMPTY_STRING;
        }

        boolean hasLeadingSlash = path.startsWith(ParserConstants.SLASH);
        boolean hasTrailingSlash = path.endsWith(ParserConstants.SLASH);
        String cleanedPath = hasLeadingSlash ? path.substring(1) : path;

        if (hasTrailingSlash && cleanedPath.length() > 0) {
            cleanedPath = cleanedPath.substring(0, cleanedPath.length() - 1);
        }

        List<String> segments = new ArrayList<>();
        if (!cleanedPath.isEmpty()) {
            segments.addAll(Arrays.asList(cleanedPath.split(ParserConstants.SLASH)));
        }

        List<String> normalizedSegments = new ArrayList<>();

        for (String segment : segments) {
            if (segment.isEmpty() || segment.equals(ParserConstants.POINT)) {
                continue;
            } else if (segment.equals(ParserConstants.DOUBLE_DOT)) {
                if (!normalizedSegments.isEmpty()) {
                    normalizedSegments.remove(normalizedSegments.size() - 1);
                }
            } else {
                normalizedSegments.add(segment);
            }
        }

        String normalizedPath = String.join(ParserConstants.SLASH, normalizedSegments);

        if (hasLeadingSlash) {
            normalizedPath = ParserConstants.SLASH + normalizedPath;
        }

        if (hasTrailingSlash && !normalizedPath.endsWith(ParserConstants.SLASH)) {
            normalizedPath += ParserConstants.SLASH;
        }

        if (normalizedPath.isEmpty() && hasLeadingSlash) {
            return ParserConstants.SLASH;
        }

        return normalizedPath;
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
