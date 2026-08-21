package fr.inria.corese.core.next.data.impl.io.parser.nquads;

import fr.inria.corese.core.next.data.api.term.*;
import fr.inria.corese.core.next.data.api.model.*;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.data.api.exception.ParsingException;
import fr.inria.corese.core.next.data.impl.io.parser.support.AbstractNTriplesNQuadsListener;
import fr.inria.corese.core.next.generated.antlr.NQuadsBaseListener;
import fr.inria.corese.core.next.generated.antlr.NQuadsParser;

/**
 * Listener for the ANTLR4 generated parser for N-Quads.
 * This listener traverses the parse tree and builds the RDF model,
 * supporting named graphs. It includes unescaping logic for URIs and literals.
 */
public class NQuadsListener extends NQuadsBaseListener {

    private final AbstractNTriplesNQuadsListener abstractNTriplesQuadsListener;
    private final Model model;
    private final ValueFactory factory;

    private Resource currentSubject;
    private IRI currentPredicate;
    private Resource currentGraph;

    /**
     * Constructs an N-Quads listener.
     *
     * @param model   RDF model to populate
     * @param factory ValueFactory for creating RDF resources
     * @param options IO configuration options
     */
    public NQuadsListener(Model model, ValueFactory factory, IOOptions options) {
        this.model = model;
        this.factory = factory;
        this.abstractNTriplesQuadsListener = new AbstractNTriplesNQuadsListener(model, factory, options) {
        };
    }

    @Override
    public void enterStatement(NQuadsParser.StatementContext ctx) {

        checkForInvalidDirectives(ctx);


        currentSubject = extractSubject(ctx.subject());
        currentPredicate = extractPredicate(ctx.predicate());
        currentGraph = (ctx.graphLabel() != null) ? extractGraph(ctx.graphLabel()) : null;
    }

    /**
     * Checks for invalid directives in N-Quads format.
     * N-Quads does not support @base or @prefix directives.
     */
    private void checkForInvalidDirectives(NQuadsParser.StatementContext ctx) {
        String text = ctx.getText();
        if (text != null && (text.contains("@base") || text.contains("@prefix"))) {
            throw new ParsingException(
                    "Directives (@base, @prefix) are not allowed in N-Quads format");
        }
    }
    @Override
    public void exitStatement(NQuadsParser.StatementContext ctx) {
        Value object = extractObject(ctx.object());

        if (currentGraph != null) {
            model.add(currentSubject, currentPredicate, object, currentGraph);
        } else {
            model.add(currentSubject, currentPredicate, object);
        }

        currentSubject = null;
        currentPredicate = null;
        currentGraph = null;
    }

    /**
     * Extracts a resource (IRI or Blank Node) from the subject context.
     */
    protected Resource extractSubject(NQuadsParser.SubjectContext ctx) {
        if (ctx.IRIREF() != null) {
            String iri = abstractNTriplesQuadsListener.unescapeUri(stripAngles(ctx.IRIREF().getText()));
            return factory.createIRI(iri);
        }
        if (ctx.BLANK_NODE_LABEL() != null) {
            String label = ctx.BLANK_NODE_LABEL().getText().substring(2);
            validateBlankNodeLabel(label);
            return factory.createBNode(label);
        }
        throw new ParsingException("Unsupported N-Quads subject: " + ctx.getText());
    }

    /**
     * Extracts a predicate (IRI) from the predicate context.
     */
    protected IRI extractPredicate(NQuadsParser.PredicateContext ctx) {
        if (ctx.IRIREF() != null) {
            String iri = abstractNTriplesQuadsListener.unescapeUri(stripAngles(ctx.IRIREF().getText()));
            return factory.createIRI(iri);
        }
        throw new ParsingException("Unsupported N-Quads predicate: " + ctx.getText());
    }

    /**
     * Extracts a value (IRI, Blank Node, or Literal) from the object context.
     */
    protected Value extractObject(NQuadsParser.ObjectContext ctx) {
        if (ctx.IRIREF() != null) {
            String iri = abstractNTriplesQuadsListener.unescapeUri(stripAngles(ctx.IRIREF().getText()));
            return factory.createIRI(iri);
        }
        if (ctx.BLANK_NODE_LABEL() != null) {
            String label = ctx.BLANK_NODE_LABEL().getText().substring(2);
            validateBlankNodeLabel(label);
            return factory.createBNode(label);
        }
        if (ctx.literal() != null) {
            return extractLiteral(ctx.literal());
        }
        throw new ParsingException("Unsupported N-Quads object: " + ctx.getText());
    }

    /**
     * Extracts a graph (IRI or Blank Node) from the graph label context.
     */
    protected Resource extractGraph(NQuadsParser.GraphLabelContext ctx) {
        if (ctx.IRIREF() != null) {
            String iri = abstractNTriplesQuadsListener.unescapeUri(stripAngles(ctx.IRIREF().getText()));
            return factory.createIRI(iri);
        }
        if (ctx.BLANK_NODE_LABEL() != null) {
            String label = ctx.BLANK_NODE_LABEL().getText().substring(2);
            validateBlankNodeLabel(label);
            return factory.createBNode(label);
        }
        throw new ParsingException("Unsupported N-Quads graph: " + ctx.getText());
    }

    /**
     * Extracts and unescapes a literal from the ANTLR context.
     */
    protected Literal extractLiteral(NQuadsParser.LiteralContext ctx) {
        String rawText = ctx.STRING_LITERAL_QUOTE() != null
                ? ctx.STRING_LITERAL_QUOTE().getText()
                : null;

        if (rawText == null) {
            throw new ParsingException("Missing literal token: " + ctx.getText());
        }

        String label = abstractNTriplesQuadsListener.unescapeLiteral(rawText);

        IRI datatype = null;
        String langTag = null;

        if (ctx.IRIREF() != null) {
            String iri = abstractNTriplesQuadsListener.unescapeUri(stripAngles(ctx.IRIREF().getText()));
            datatype = factory.createIRI(iri);
        } else if (ctx.LANGTAG() != null) {
            langTag = ctx.LANGTAG().getText().substring(1);
        }

        return abstractNTriplesQuadsListener.createLiteral(label, datatype, langTag);
    }

    /**
     * Strips angle brackets from an IRI reference.
     */
    private String stripAngles(String iriRef) {
        return abstractNTriplesQuadsListener.stripAngles(iriRef);
    }

    /**
     * Validates a blank node label according to RDF 1.1 N-Quads specification.
     * Blank node labels must match PN_LOCAL rules, which means they cannot be empty,
     * and cannot contain colons. They *can* start with a digit.
     * @param label The blank node label string (without the "_:" prefix).
     * @throws ParsingException if the blank node label is invalid.
     */
    protected void validateBlankNodeLabel(String label) {
        abstractNTriplesQuadsListener.validateBlankNodeLabel(label);

        if (!label.matches("^[A-Za-z_0-9][A-Za-z0-9_\\-\\.]*$")) {
            throw new ParsingException("Invalid blank node label syntax: " + label);
        }
    }

    /**
     * Unescapes common N-Quads literal escape sequences.
     *
     * @param literalText The raw literal string from ANTLR (including quotes)
     * @return The unescaped literal string without surrounding quotes
     * @deprecated Use helper.unescapeLiteral instead
     */
    @Deprecated
    protected String unescapeLiteral(String literalText) {
        return abstractNTriplesQuadsListener.unescapeLiteral(literalText);
    }

    /**
     * Unescapes common N-Quads URI escape sequences.
     *
     * @param uri The escaped URI string
     * @return The unescaped URI string
     * @deprecated Use helper.unescapeUri instead
     */
    @Deprecated
    protected String unescapeUri(String uri) {
        return abstractNTriplesQuadsListener.unescapeUri(uri);
    }
}
