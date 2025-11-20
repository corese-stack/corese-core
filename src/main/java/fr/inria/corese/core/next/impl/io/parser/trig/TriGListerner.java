package fr.inria.corese.core.next.impl.io.parser.trig;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.api.io.common.BaseIRIOptions;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;
import fr.inria.corese.core.next.impl.io.parser.common.AbstractTurtleTriGListener;
import fr.inria.corese.core.next.impl.io.parser.util.ParserConstants;
import fr.inria.corese.core.next.impl.parser.antlr.TriGBaseListener;
import fr.inria.corese.core.next.impl.parser.antlr.TriGParser;

import java.util.List;

/**
 * ANTLR listener for parsing TriG documents into RDF datasets.
 * and adds support for named graphs as specified in the TriG format.
 *

 */
public class TriGListerner extends TriGBaseListener {

    private final TriGListenerDelegate delegate;
    private boolean insideGraphBlock = false;

    /**
     * Constructs a TriG listener with explicit base URI.
     *
     * @param model   RDF model to populate with parsed quads
     * @param factory factory for creating RDF terms
     * @param options I/O options (unused in this constructor)
     * @param baseURI base URI for resolving relative references
     */
    public TriGListerner(Model model, ValueFactory factory, IOOptions options, String baseURI) {
        this.delegate = new TriGListenerDelegate(model, factory, baseURI);
    }

    /**
     * Constructs a TriG listener extracting base URI from options.
     *
     * @param model   RDF model to populate with parsed quads
     * @param factory factory for creating RDF terms
     * @param options I/O options potentially containing base URI
     */
    public TriGListerner(Model model, ValueFactory factory, IOOptions options) {
        String baseURI;
        if (options instanceof BaseIRIOptions baseIRIOptions) {
            baseURI = baseIRIOptions.getBaseIRI() != null ? baseIRIOptions.getBaseIRI() : ParserConstants.EMPTY_STRING;
        } else {
            baseURI = ParserConstants.EMPTY_STRING;
        }
        this.delegate = new TriGListenerDelegate(model, factory, baseURI);
    }

    /**
     * Handles directive by updating the base URI for relative IRI resolution.
     *
     * @param ctx base directive context
     */
    @Override
    public void exitBase(TriGParser.BaseContext ctx) {
        String newBase = delegate.extractAndUnescapeIRI(ctx.IRIREF().getText());
        delegate.updateBaseURI(newBase);
    }

    /**
     * Handles {@code @prefix} directive by registering a namespace prefix.
     *
     * @param ctx prefix directive context
     */
    @Override
    public void exitPrefixID(TriGParser.PrefixIDContext ctx) {
        String prefix = ctx.PNAME_NS().getText();
        prefix = prefix.substring(0, prefix.length() - 1);
        String iri = delegate.extractAndUnescapeIRI(ctx.IRIREF().getText());
        delegate.registerPrefix(prefix, iri);
    }

    /**
     * Handles SPARQL-style {@code BASE} directive by updating the base URI.
     *
     * @param ctx SPARQL base directive context
     */
    @Override
    public void exitSparqlBase(TriGParser.SparqlBaseContext ctx) {
        String newBase = delegate.extractAndUnescapeIRI(ctx.IRIREF().getText());
        delegate.updateBaseURI(newBase);
    }

    /**
     * Handles SPARQL-style {@code PREFIX} directive by registering a namespace prefix.
     *
     * @param ctx SPARQL prefix directive context
     */
    @Override
    public void exitSparqlPrefix(TriGParser.SparqlPrefixContext ctx) {
        String prefix = ctx.PNAME_NS().getText();
        prefix = prefix.substring(0, prefix.length() - 1);
        String iri = delegate.extractAndUnescapeIRI(ctx.IRIREF().getText());
        delegate.registerPrefix(prefix, iri);
    }

    /**
     * Handles entry into a graph block, setting the current graph context.
     * If a graph label is present, it becomes the target graph for subsequent statements.
     *
     * @param ctx block context
     */
    @Override
    public void enterBlock(TriGParser.BlockContext ctx) {
        if (ctx.Graph_w() != null && ctx.labelOrSubject() != null) {
            delegate.setCurrentGraph(extractLabelOrSubject(ctx.labelOrSubject()));
        } else {
            delegate.setCurrentGraph(null);
        }
    }

    /**
     * Handles exit from a graph block, clearing the current graph context.
     *
     * @param ctx block context
     */
    @Override
    public void exitBlock(TriGParser.BlockContext ctx) {
        delegate.setCurrentGraph(null);
    }

    /**
     * Marks entry into a wrapped graph block
     * Used for validating standalone collection syntax.
     *
     * @param ctx wrapped graph context
     */
    @Override
    public void enterWrappedGraph(TriGParser.WrappedGraphContext ctx) {
        insideGraphBlock = true;
    }

    /**
     * Marks exit from a wrapped graph block.
     *
     * @param ctx wrapped graph context
     */
    @Override
    public void exitWrappedGraph(TriGParser.WrappedGraphContext ctx) {
        insideGraphBlock = false;
    }

    /**
     * Handles triples or graph declarations (disambiguated by following syntax).
     * Either processes triples with an explicit subject or sets up a named graph.
     *
     * @param ctx triples or graph context
     */
    @Override
    public void enterTriplesOrGraph(TriGParser.TriplesOrGraphContext ctx) {
        if (ctx.labelOrSubject() != null) {
            Resource resource = extractLabelOrSubject(ctx.labelOrSubject());
            if (ctx.predicateObjectList() != null) {
                delegate.setCurrentSubject(resource);
                processPredicateObjectList(ctx.predicateObjectList());
            } else if (ctx.wrappedGraph() != null) {
                delegate.setCurrentGraph(resource);
            }
        }
    }

    /**
     * Handles standard triple declarations starting with a subject or blank node property list.
     * Subject context is saved and restored to handle nested structures.
     *
     * @param ctx triples context
     */
    @Override
    public void enterTriples(TriGParser.TriplesContext ctx) {
        Resource savedSubject = delegate.getCurrentSubject();
        try {
            if (ctx.subject() != null) {
                delegate.setCurrentSubject(extractSubject(ctx.subject()));
            } else if (ctx.blankNodePropertyList() != null) {
                delegate.setCurrentSubject(processBlankNodePropertyList(ctx.blankNodePropertyList()));
            }

            if (ctx.predicateObjectList() != null) {
                processPredicateObjectList(ctx.predicateObjectList());
            }
        } finally {
            delegate.setCurrentSubject(savedSubject);
        }
    }

    /**
     * Handles alternative triple forms starting with blank node property lists or collections.
     * Validates that standalone collections only appear within graph blocks.
     * Subject context is saved and restored to handle nested structures.
     *
     * @param ctx triples2 context
     */
    @Override
    public void enterTriples2(TriGParser.Triples2Context ctx) {
        Resource savedSubject = delegate.getCurrentSubject();

        if (ctx.collection() != null && ctx.predicateObjectList() == null) {
            validateStandaloneCollection(ctx);
        }

        try {
            if (ctx.blankNodePropertyList() != null) {
                delegate.setCurrentSubject(processBlankNodePropertyList(ctx.blankNodePropertyList()));
            } else if (ctx.collection() != null) {
                delegate.setCurrentSubject(processCollection(ctx.collection()));
            }

            if (ctx.predicateObjectList() != null) {
                processPredicateObjectList(ctx.predicateObjectList());
            }
        } finally {
            delegate.setCurrentSubject(savedSubject);
        }
    }

    /**
     * Validates that standalone collections only appear within graph blocks.
     * TriG syntax requires collections without predicates to be inside braces.
     *
     * @param ctx triples2 context containing a potential standalone collection
     * @throws ParsingErrorException if a standalone collection appears outside graph blocks
     */
    private void validateStandaloneCollection(TriGParser.Triples2Context ctx) {
        if (!insideGraphBlock) {
            List<TriGParser.ObjectContext> objects = ctx.collection().object();
            if (objects.isEmpty()) {
                throw new ParsingErrorException("Free-standing list of zero-elements outside {} : bad syntax");
            } else {
                throw new ParsingErrorException("Free-standing list outside {} : bad syntax");
            }
        }
    }

    /**
     * Processes a blank node property list
     * Creates a fresh blank node and parses its property list.
     *
     * @param ctx blank node property list context
     * @return created blank node resource
     */
    private Resource processBlankNodePropertyList(TriGParser.BlankNodePropertyListContext ctx) {
        Resource bnode = delegate.createBNode();
        Resource savedSubject = delegate.getCurrentSubject();
        IRI savedPredicate = delegate.getCurrentPredicate();

        try {
            delegate.setCurrentSubject(bnode);
            if (ctx.predicateObjectList() != null) {
                processPredicateObjectList(ctx.predicateObjectList());
            }
        } finally {
            delegate.setCurrentSubject(savedSubject);
            delegate.setCurrentPredicate(savedPredicate);
        }

        return bnode;
    }

    /**
     * Processes an RDF collection
     * Constructs the linked list structure
     *
     * @param ctx collection context
     * @return head of the list (blank node) or {@code rdf:nil} for empty lists
     */
    private Resource processCollection(TriGParser.CollectionContext ctx) {
        List<TriGParser.ObjectContext> objects = ctx.object();

        if (objects.isEmpty()) {
            return delegate.createIRI(RDF.nil.getIRI().stringValue());
        }

        Resource head = delegate.createBNode();
        Resource current = head;
        Resource savedSubject = delegate.getCurrentSubject();
        IRI savedPredicate = delegate.getCurrentPredicate();

        try {
            IRI firstPredicate = delegate.createIRI(RDF.first.getIRI().stringValue());
            IRI restPredicate = delegate.createIRI(RDF.rest.getIRI().stringValue());
            Value nilValue = delegate.createIRI(RDF.nil.getIRI().stringValue());

            for (int i = 0; i < objects.size(); i++) {
                Value object = extractObject(objects.get(i));
                delegate.addStatement(current, firstPredicate, object);

                if (i == objects.size() - 1) {
                    delegate.addStatement(current, restPredicate, nilValue);
                } else {
                    Resource next = delegate.createBNode();
                    delegate.addStatement(current, restPredicate, next);
                    current = next;
                }
            }
        } finally {
            delegate.setCurrentSubject(savedSubject);
            delegate.setCurrentPredicate(savedPredicate);
        }

        return head;
    }

    /**
     * Processes a predicate-object list, generating statements for each predicate-object pair.
     *
     * @param ctx predicate-object list context
     */
    private void processPredicateObjectList(TriGParser.PredicateObjectListContext ctx) {
        for (int i = 0; i < ctx.verb().size(); i++) {
            TriGParser.VerbContext verb = ctx.verb(i);
            TriGParser.ObjectListContext objectList = ctx.objectList(i);

            IRI savedPredicate = delegate.getCurrentPredicate();
            try {
                delegate.setCurrentPredicate(extractVerb(verb));

                if (objectList != null) {
                    for (TriGParser.ObjectContext objectCtx : objectList.object()) {
                        Value object = extractObject(objectCtx);
                        delegate.addStatement(object);
                    }
                }
            } finally {
                delegate.setCurrentPredicate(savedPredicate);
            }
        }
    }

    /**
     * Extracts an RDF value from an object context.
     *
     * @param ctx object context
     * @return extracted RDF value (IRI, blank node, or literal)
     * @throws ParsingErrorException if the object type is unsupported
     */
    private Value extractObject(TriGParser.ObjectContext ctx) {
        if (ctx.iri() != null) {
            return delegate.createIRI(delegate.resolveIRI(ctx.iri().getText()));
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
        throw new ParsingErrorException("Unsupported object: " + ctx.getText());
    }

    /**
     * Extracts a blank node resource from a blank node context.
     *
     * @param ctx blank node context
     * @return blank node resource or collection head
     * @throws ParsingErrorException if the blank node format is unsupported
     */
    private Resource extractBlank(TriGParser.BlankContext ctx) {
        TriGParser.BlankNodeContext node = ctx.blankNode();
        if (node != null) {
            if (node.BLANK_NODE_LABEL() != null) {
                return delegate.createBNode(node.BLANK_NODE_LABEL().getText().substring(2));
            }
            if (node.ANON() != null) {
                return delegate.createBNode();
            }
        }

        TriGParser.CollectionContext collection = ctx.collection();
        if (collection != null) {
            return processCollection(collection);
        }

        throw new ParsingErrorException("Unsupported blank node: " + ctx.getText());
    }

    /**
     * Extracts a subject resource from a subject context.
     *
     * @param ctx subject context
     * @return subject resource (IRI, blank node, or collection head)
     * @throws ParsingErrorException if the subject type is unsupported
     */
    private Resource extractSubject(TriGParser.SubjectContext ctx) {
        if (ctx.iri() != null) {
            return delegate.createIRI(delegate.resolveIRI(ctx.iri().getText()));
        }
        if (ctx.blank() != null) {
            if (ctx.blank().blankNode() != null) {
                TriGParser.BlankNodeContext node = ctx.blank().blankNode();
                if (node.BLANK_NODE_LABEL() != null) {
                    return delegate.createBNode(node.BLANK_NODE_LABEL().getText().substring(2));
                }
                if (node.ANON() != null) {
                    return delegate.createBNode();
                }
            } else if (ctx.blank().collection() != null) {
                return processCollection(ctx.blank().collection());
            }
        }
        throw new ParsingErrorException("Unsupported subject: " + ctx.getText());
    }

    /**
     * Extracts a graph label or subject resource.
     *
     * @param ctx label or subject context
     * @return resource serving as graph name or statement subject
     * @throws ParsingErrorException if IRI resolution fails or type is unsupported
     */
    private Resource extractLabelOrSubject(TriGParser.LabelOrSubjectContext ctx) {
        if (ctx.iri() != null) {
            String iriText = ctx.iri().getText();
            try {
                return delegate.createIRI(delegate.resolveIRI(iriText));
            } catch (Exception e) {
                throw new ParsingErrorException("Failed to resolve IRI: " + iriText, e);
            }
        }
        if (ctx.blankNode() != null) {
            if (ctx.blankNode().BLANK_NODE_LABEL() != null) {
                return delegate.createBNode(ctx.blankNode().BLANK_NODE_LABEL().getText().substring(2));
            }
            if (ctx.blankNode().ANON() != null) {
                return delegate.createBNode();
            }
        }
        throw new ParsingErrorException("Unsupported label or subject: " + ctx.getText());
    }

    /**
     * Extracts a predicate IRI from a verb context.
     * Handles the special case of {@code a} as shorthand for {@code rdf:type}.
     *
     * @param ctx verb context
     * @return predicate IRI
     */
    private IRI extractVerb(TriGParser.VerbContext ctx) {
        String verbText = ctx.getText();
        if (verbText.equals(ParserConstants.RDF_TYPE_SHORTCUT)) {
            return delegate.createIRI(RDF.type.getIRI().stringValue());
        }
        return delegate.createIRI(delegate.resolveIRI(verbText));
    }

    /**
     * Extracts an RDF literal from a literal context.
     * Handles plain, language-tagged, typed, boolean, and numeric literals.
     *
     * @param ctx literal context
     * @return RDF literal value
     * @throws ParsingErrorException if the literal type is unsupported
     */
    private Literal extractLiteral(TriGParser.LiteralContext ctx) {
        if (ctx.rDFLiteral() != null) {
            String label = delegate.unescapeString(ctx.rDFLiteral().string().getText());
            if (ctx.rDFLiteral().LANGTAG() != null) {
                return delegate.createLiteral(label, ctx.rDFLiteral().LANGTAG().getText().substring(1), null);
            }
            if (ctx.rDFLiteral().iri() != null) {
                return delegate.createLiteral(label, null, delegate.resolveIRI(ctx.rDFLiteral().iri().getText()));
            }
            return delegate.createLiteral(label, null, null);
        }
        if (ctx.BooleanLiteral() != null) {
            return delegate.createBooleanLiteral(ctx.BooleanLiteral().getText());
        }
        if (ctx.numericLiteral() != null) {
            String numericText = ctx.numericLiteral().getText();

            if (ctx.numericLiteral().DOUBLE() != null) {
                return delegate.createNumericLiteral(numericText, AbstractTurtleTriGListener.NumericType.DOUBLE);
            } else if (ctx.numericLiteral().DECIMAL() != null) {
                return delegate.createNumericLiteral(numericText, AbstractTurtleTriGListener.NumericType.DECIMAL);
            } else {
                return delegate.createNumericLiteral(numericText, AbstractTurtleTriGListener.NumericType.INTEGER);
            }
        }
        throw new ParsingErrorException("Unsupported literal: " + ctx.getText());
    }

    /**
     * Delegate extending {@link AbstractTurtleTriGListener} with TriG-specific named graph support.
     * Overrides statement addition to handle quads (subject, predicate, object, graph).
     */
    private static class TriGListenerDelegate extends AbstractTurtleTriGListener {
        private Resource currentGraph;

        public TriGListenerDelegate(Model model, ValueFactory factory, String baseURI) {
            super(model, factory, baseURI);
        }

        public Resource getCurrentSubject() {
            return currentSubject;
        }

        public void setCurrentSubject(Resource subject) {
            this.currentSubject = subject;
        }

        public IRI getCurrentPredicate() {
            return currentPredicate;
        }

        public void setCurrentPredicate(IRI predicate) {
            this.currentPredicate = predicate;
        }


        public void setCurrentGraph(Resource graph) {
            this.currentGraph = graph;
        }

        public void addStatement(Value object) {
            safeAddStatement(currentSubject, currentPredicate, object);
        }

        public void addStatement(Resource subject, IRI predicate, Value object) {
            safeAddStatement(subject, predicate, object);
        }

        public Resource createBNode() {
            return factory.createBNode();
        }

        public Resource createBNode(String id) {
            return factory.createBNode(id);
        }

        public IRI createIRI(String iri) {
            return factory.createIRI(iri);
        }

        /**
         * Adds a quad to the model with fallback to default graph on failure.
         *
         * @param subject   statement subject
         * @param predicate statement predicate
         * @param object    statement object
         * @throws ParsingErrorException if statement cannot be added to any graph
         */
        @Override
        public void safeAddStatement(Resource subject, IRI predicate, Value object) {
            try {
                model.add(subject, predicate, object, currentGraph);
            } catch (Exception e) {
                if (currentGraph != null) {
                    try {
                        model.add(subject, predicate, object, null);
                    } catch (Exception e2) {
                        throw new ParsingErrorException("Failed to add statement: " + e.getMessage(), e);
                    }
                } else {
                    throw new ParsingErrorException("Failed to add statement: " + e.getMessage(), e);
                }
            }
        }
    }
}