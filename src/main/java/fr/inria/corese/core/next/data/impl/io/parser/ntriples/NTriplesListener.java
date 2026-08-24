package fr.inria.corese.core.next.data.impl.io.parser.ntriples;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.data.api.exception.ParsingException;
import fr.inria.corese.core.next.data.impl.io.parser.support.AbstractNTriplesNQuadsListener;
import fr.inria.corese.core.next.generated.antlr.NTriplesBaseListener;
import fr.inria.corese.core.next.generated.antlr.NTriplesParser;

/**
 * Listener for the ANTLR4 generated parser for N-Triples.
 * This listener traverses the parse tree and builds the RDF model.
 * It includes unescaping logic for URIs and literals.
 */
public class NTriplesListener extends NTriplesBaseListener {

    private final AbstractNTriplesNQuadsListener abstractNTriplesQuadsListener;
    private final Model model;
    private final ValueFactory factory;

    private Resource currentSubject;
    private IRI currentPredicate;

    /**
     * Constructs an N-Triples listener.
     *
     * @param model   The RDF model to populate.
     * @param factory The ValueFactory for creating RDF resources.
     * @param options IOOptions for configuration (if any).
     */
    public NTriplesListener(Model model, ValueFactory factory, IOOptions options) {
        this.model = model;
        this.factory = factory;
        this.abstractNTriplesQuadsListener = new AbstractNTriplesNQuadsListener(model, factory, options) {};
    }

    @Override
    public void enterTriple(NTriplesParser.TripleContext ctx) {
        currentSubject = extractSubject(ctx.subject());
        currentPredicate = extractPredicate(ctx.predicate());
    }

    @Override
    public void exitTriple(NTriplesParser.TripleContext ctx) {
        Value object = extractObject(ctx.object());
        model.add(currentSubject, currentPredicate, object);
        currentSubject = null;
        currentPredicate = null;
    }

    /**
     * Extracts a resource (IRI or Blank Node) from the subject context.
     */
    public Resource extractSubject(NTriplesParser.SubjectContext ctx) {
        if (ctx.IRIREF() != null) {
            String iri = abstractNTriplesQuadsListener.unescapeUri(abstractNTriplesQuadsListener.stripAngles(ctx.IRIREF().getText()));
            return factory.createIRI(iri);
        }
        if (ctx.BLANK_NODE_LABEL() != null) {
            String label = abstractNTriplesQuadsListener.extractBlankNodeLabel(ctx.BLANK_NODE_LABEL().getText());
            abstractNTriplesQuadsListener.validateBlankNodeLabel(label);
            return factory.createBNode(label);
        }
        throw new ParsingException("Unsupported N-Triples subject: " + ctx.getText());
    }

    /**
     * Extracts a predicate (IRI) from the predicate context.
     */
    public IRI extractPredicate(NTriplesParser.PredicateContext ctx) {
        if (ctx.IRIREF() != null) {
            String iri = abstractNTriplesQuadsListener.unescapeUri(abstractNTriplesQuadsListener.stripAngles(ctx.IRIREF().getText()));
            return factory.createIRI(iri);
        }
        throw new ParsingException("Unsupported N-Triples predicate: " + ctx.getText());
    }

    /**
     * Extracts a value (IRI, Blank Node, or Literal) from the object context.
     */
    public Value extractObject(NTriplesParser.ObjectContext ctx) {
        if (ctx.IRIREF() != null) {
            String iri = abstractNTriplesQuadsListener.unescapeUri(abstractNTriplesQuadsListener.stripAngles(ctx.IRIREF().getText()));
            return factory.createIRI(iri);
        }
        if (ctx.BLANK_NODE_LABEL() != null) {
            String label = abstractNTriplesQuadsListener.extractBlankNodeLabel(ctx.BLANK_NODE_LABEL().getText());
            abstractNTriplesQuadsListener.validateBlankNodeLabel(label);
            return factory.createBNode(label);
        }
        if (ctx.literal() != null) {
            return extractLiteral(ctx.literal());
        }
        throw new ParsingException("Unsupported N-Triples object: " + ctx.getText());
    }

    /**
     * Extracts and unescapes a literal from the ANTLR context.
     */
    protected Literal extractLiteral(NTriplesParser.LiteralContext ctx) {
        String rawText = ctx.STRING_LITERAL_QUOTE().getText();
        String label = abstractNTriplesQuadsListener.unescapeLiteral(rawText);

        IRI datatype = null;
        String langTag = null;

        if (ctx.IRIREF() != null) {
            String iri = abstractNTriplesQuadsListener.unescapeUri(abstractNTriplesQuadsListener.stripAngles(ctx.IRIREF().getText()));
            datatype = factory.createIRI(iri);
        } else if (ctx.LANGTAG() != null) {
            langTag = ctx.LANGTAG().getText().substring(1);
        }

        return abstractNTriplesQuadsListener.createLiteral(label, datatype, langTag);
    }

    /**
     * Validates a blank node label according to RDF N-Triples specification.
     * Blank node labels must not be empty and must not contain a colon.
     *
     * @param label The blank node label (without the "_:" prefix)
     * @throws ParsingException if the label is invalid
     * @deprecated Use helper.validateBlankNodeLabel instead
     */
    @Deprecated(since = "4.5.0", forRemoval = false)
    protected void validateBlankNodeLabel(String label) throws ParsingException {
        abstractNTriplesQuadsListener.validateBlankNodeLabel(label);
    }

    /**
     * Unescapes common N-Triples literal escape sequences.
     *
     * @param literalText The raw literal string from ANTLR (including quotes)
     * @return The unescaped literal string without surrounding quotes
     * @deprecated Use helper.unescapeLiteral instead
     */
    @Deprecated(since = "4.5.0", forRemoval = false)
    public String unescapeLiteral(String literalText) {
        return abstractNTriplesQuadsListener.unescapeLiteral(literalText);
    }

    /**
     * Unescapes common N-Triples URI escape sequences.
     *
     * @param uri The escaped URI string
     * @return The unescaped URI string
     * @deprecated Use helper.unescapeUri instead
     */
    @Deprecated(since = "4.5.0", forRemoval = false)
    protected String unescapeUri(String uri) {
        return abstractNTriplesQuadsListener.unescapeUri(uri);
    }
}

