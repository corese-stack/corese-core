package fr.inria.corese.core.next.data.impl.io.parser.turtle;

import fr.inria.corese.core.next.data.api.term.*;
import fr.inria.corese.core.next.data.api.model.*;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.data.api.io.option.BaseIRIOptions;
import fr.inria.corese.core.next.data.api.vocabulary.RDF;
import fr.inria.corese.core.next.data.api.exception.ParsingException;
import fr.inria.corese.core.next.data.impl.io.parser.support.AbstractTurtleTriGListener;
import fr.inria.corese.core.next.data.impl.io.parser.support.ParserConstants;
import fr.inria.corese.core.next.generated.antlr.TurtleBaseListener;
import fr.inria.corese.core.next.generated.antlr.TurtleParser;

import java.util.List;

/**
 * ANTLR listener for parsing Turtle documents into RDF graphs.
 * Extends {@link AbstractTurtleTriGListener} for common RDF parsing functionality.
 *
 */
public class TurtleListener extends TurtleBaseListener {

    private final AbstractTurtleTriGListener delegate;

    /**
     * Constructs a Turtle listener extracting base URI from options.
     *
     * @param model   RDF model to populate with parsed triples
     * @param factory factory for creating RDF terms
     * @param options I/O options potentially containing base URI
     */
    public TurtleListener(Model model, ValueFactory factory, IOOptions options) {
        String baseURI = null;

        if (options instanceof BaseIRIOptions baseIRIOptions) {
            baseURI = baseIRIOptions.getBaseIRI();
        }

        if (baseURI == null || baseURI.isEmpty()) {
            baseURI = ParserConstants.EMPTY_STRING;
        }

        this.delegate = new TurtleListenerDelegate(model, factory, baseURI);
    }

    /**
     * Handles {@code @prefix} directive by registering a namespace prefix.
     *
     * @param ctx prefix directive context
     */
    @Override
    public void exitPrefixID(TurtleParser.PrefixIDContext ctx) {
        String prefix = ctx.PNAME_NS().getText();
        prefix = prefix.substring(0, prefix.length() - 1);
        String iri = delegate.extractAndUnescapeIRI(ctx.IRIREF().getText());
        delegate.registerPrefix(prefix, iri);
    }

    /**
     * Handles directive by updating the base URI for relative IRI resolution.
     *
     * @param ctx base directive context
     */
    @Override
    public void exitBase(TurtleParser.BaseContext ctx) {
        if (ctx.IRIREF() != null) {
            String newBase = delegate.extractAndUnescapeIRI(ctx.IRIREF().getText());
            delegate.updateBaseURI(newBase);
        }
    }

    /**
     * Handles SPARQL-style {@code BASE} directive by updating the base URI.
     *
     * @param ctx SPARQL base directive context
     */
    @Override
    public void exitSparqlBase(TurtleParser.SparqlBaseContext ctx) {
        String newBase = delegate.extractAndUnescapeIRI(ctx.IRIREF().getText());
        delegate.updateBaseURI(newBase);
    }

    /**
     * Handles SPARQL-style {@code PREFIX} directive by registering a namespace prefix.
     *
     * @param ctx SPARQL prefix directive context
     */
    @Override
    public void exitSparqlPrefix(TurtleParser.SparqlPrefixContext ctx) {
        String prefix = ctx.PNAME_NS().getText();
        prefix = prefix.substring(0, prefix.length() - 1);
        String iri = delegate.extractAndUnescapeIRI(ctx.IRIREF().getText());
        delegate.registerPrefix(prefix, iri);
    }

    /**
     * Handles triple declarations starting with a subject or blank node property list.
     * Processes the subject and its associated predicate-object list.
     *
     * @param ctx triples context
     * @throws ParsingException if the subject is missing or processing fails
     */
    @Override
    public void enterTriples(TurtleParser.TriplesContext ctx) {
        try {
            if (ctx.subject() != null) {
                delegate.currentSubject = extractSubject(ctx.subject());
                if (ctx.predicateObjectList() != null) {
                    processPredicateObjectList(ctx.predicateObjectList());
                }
            } else if (ctx.blankNodePropertyList() != null) {
                delegate.currentSubject = processBlankNodePropertyList(ctx.blankNodePropertyList());
                if (ctx.predicateObjectList() != null) {
                    processPredicateObjectList(ctx.predicateObjectList());
                }
            } else {
                throw new ParsingException("Missing subject in triple.");
            }
        } catch (ParsingException e) {
            throw e;
        } catch (Exception e) {
            throw new ParsingException("Error processing triples: " + e.getMessage(), e);
        }
    }

    /**
     * Processes a predicate-object list, generating triples for each predicate-object pair.
     *
     * @param ctx predicate-object list context
     */
    private void processPredicateObjectList(TurtleParser.PredicateObjectListContext ctx) {
        for (int i = 0; i < ctx.verb().size(); i++) {
            TurtleParser.VerbContext verb = ctx.verb(i);
            TurtleParser.ObjectListContext objectList = ctx.objectList(i);

            delegate.currentPredicate = extractVerb(verb);

            if (objectList != null) {
                for (TurtleParser.Object_Context objectCtx : objectList.object_()) {
                    Value object = extractObject(objectCtx);
                    delegate.safeAddStatement(delegate.currentSubject, delegate.currentPredicate, object);
                }
            }
        }
    }

    /**
     * Extracts an RDF value from an object context.
     *
     * @param ctx object context
     * @return extracted RDF value (IRI, blank node, or literal)
     * @throws ParsingException if the object type is unsupported or IRI cannot be resolved
     */
    private Value extractObject(TurtleParser.Object_Context ctx) {
        if (ctx.iri() != null) {
            String resolvedIRI = delegate.resolveIRI(ctx.iri().getText());
            if (resolvedIRI.isEmpty()) {
                throw new ParsingException("Cannot resolve object IRI: " + ctx.iri().getText());
            }
            return delegate.factory.createIRI(resolvedIRI);
        }

        if (ctx.BlankNode() != null) {
            String blankNodeText = ctx.BlankNode().getText();
            if (blankNodeText.startsWith(ParserConstants.BLANK_NODE_PREFIX)) {
                return delegate.factory.createBNode(blankNodeText.substring(2));
            } else if (blankNodeText.equals(ParserConstants.EMPTY_SQUARE_BRACKET)) {
                return delegate.factory.createBNode();
            } else {
                throw new ParsingException("Unsupported blank node format: " + blankNodeText);
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

        throw new ParsingException("Unsupported object type: " + (ctx.getText() != null ? ctx.getText() : "null"));
    }

    /**
     * Extracts a subject resource from a subject context.
     *
     * @param ctx subject context
     * @return subject resource (IRI, blank node, or collection head)
     * @throws ParsingException if the subject type is unsupported
     */
    private Resource extractSubject(TurtleParser.SubjectContext ctx) {
        if (ctx.iri() != null) {
            return delegate.factory.createIRI(delegate.resolveIRI(ctx.iri().getText()));
        }
        if (ctx.BlankNode() != null) {
            String blankNodeText = ctx.BlankNode().getText();
            if (blankNodeText.startsWith(ParserConstants.BLANK_NODE_PREFIX)) {
                return delegate.factory.createBNode(blankNodeText.substring(2));
            } else if (blankNodeText.equals(ParserConstants.EMPTY_SQUARE_BRACKET)) {
                return delegate.factory.createBNode();
            } else {
                throw new ParsingException("Unsupported blank node format: " + blankNodeText);
            }
        }
        if (ctx.collection() != null) {
            return processCollection(ctx.collection());
        }
        throw new ParsingException("Unsupported subject type: " + ctx.getText());
    }

    /**
     * Processes a blank node property list
     * Creates a fresh blank node and parses its property list.
     * Subject and predicate contexts are saved and restored to handle nested structures.
     *
     * @param ctx blank node property list context
     * @return created blank node resource
     */
    private Resource processBlankNodePropertyList(TurtleParser.BlankNodePropertyListContext ctx) {
        Resource bnode = delegate.factory.createBNode();
        Resource savedSubject = delegate.currentSubject;
        IRI savedPredicate = delegate.currentPredicate;

        try {
            delegate.currentSubject = bnode;
            if (ctx.predicateObjectList() != null) {
                processPredicateObjectList(ctx.predicateObjectList());
            }
        } finally {
            delegate.currentSubject = savedSubject;
            delegate.currentPredicate = savedPredicate;
        }

        return bnode;
    }

    /**
     * Processes an RDF collection
     * Constructs the linked list structure using
     *
     * @param ctx collection context
     * @return head of the list (blank node) or {@code rdf:nil} for empty lists
     */
    private Resource processCollection(TurtleParser.CollectionContext ctx) {
        List<TurtleParser.Object_Context> objects = ctx.object_();

        if (objects.isEmpty()) {
            return delegate.factory.createIRI(RDF.nil.getIRI().stringValue());
        }

        Resource head = delegate.factory.createBNode();
        Resource current = head;
        IRI firstPredicate = delegate.factory.createIRI(RDF.first.getIRI().stringValue());
        IRI restPredicate = delegate.factory.createIRI(RDF.rest.getIRI().stringValue());
        Value nilValue = delegate.factory.createIRI(RDF.nil.getIRI().stringValue());

        for (int i = 0; i < objects.size(); i++) {
            Value object = extractObject(objects.get(i));
            delegate.safeAddStatement(current, firstPredicate, object);

            if (i == objects.size() - 1) {
                delegate.safeAddStatement(current, restPredicate, nilValue);
            } else {
                Resource next = delegate.factory.createBNode();
                delegate.safeAddStatement(current, restPredicate, next);
                current = next;
            }
        }

        return head;
    }

    /**
     * Extracts an RDF literal from a literal context.
     * Handles plain, language-tagged, typed, boolean, and numeric literals.
     *
     * @param ctx literal context
     * @return RDF literal value
     * @throws ParsingException if the literal type is unsupported
     */
    private Literal extractLiteral(TurtleParser.LiteralContext ctx) {
        if (ctx.rdfLiteral() != null) {
            String label = delegate.unescapeString(ctx.rdfLiteral().string().getText());
            if (ctx.rdfLiteral().LANGTAG() != null) {
                return delegate.createLiteral(label, ctx.rdfLiteral().LANGTAG().getText().substring(1), null);
            }
            if (ctx.rdfLiteral().iri() != null) {
                return delegate.createLiteral(label, null, delegate.resolveIRI(ctx.rdfLiteral().iri().getText()));
            }
            return delegate.createLiteral(label, null, null);
        }

        if (ctx.BooleanLiteral() != null) {
            return delegate.createBooleanLiteral(ctx.BooleanLiteral().getText());
        }

        if (ctx.numericLiteral() != null) {
            String numericText = ctx.numericLiteral().getText();
            AbstractTurtleTriGListener.NumericType type;

            if (ctx.numericLiteral().DOUBLE() != null) {
                type = AbstractTurtleTriGListener.NumericType.DOUBLE;
            } else if (ctx.numericLiteral().DECIMAL() != null) {
                type = AbstractTurtleTriGListener.NumericType.DECIMAL;
            } else {
                type = AbstractTurtleTriGListener.NumericType.INTEGER;
            }

            return delegate.createNumericLiteral(numericText, type);
        }

        throw new ParsingException("Unsupported literal: " + ctx.getText());
    }

    /**
     * Extracts a predicate IRI from a verb context.
     * Handles the special case of {@code a} as shorthand for {@code rdf:type}.
     *
     * @param ctx verb context
     * @return predicate IRI
     * @throws ParsingException if the IRI cannot be resolved
     */
    private IRI extractVerb(TurtleParser.VerbContext ctx) {
        String verbText = ctx.getText();
        String resolvedIRI = delegate.resolveIRI(verbText);
        if (resolvedIRI.isEmpty()) {
            throw new ParsingException("Cannot resolve verb to a valid IRI: " + verbText);
        }
        return delegate.factory.createIRI(resolvedIRI);
    }

    /**
     * Delegate extending for Turtle parsing.
     * Provides access to common RDF parsing functionality without additional extensions.
     */
    private static class TurtleListenerDelegate extends AbstractTurtleTriGListener {
        public TurtleListenerDelegate(Model model, ValueFactory factory, String baseURI) {
            super(model, factory, baseURI);
        }
    }
}
