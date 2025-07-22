package fr.inria.corese.core.next.impl.io.parser.turtle;

import java.util.HashMap;
import java.util.Map;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Literal;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Value;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.api.io.parser.RDFParserBaseIRIOptions;
import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.parser.antlr.TurtleBaseListener;
import fr.inria.corese.core.next.impl.parser.antlr.TurtleParser;

/**
 * Listener for the ANTLR4 generated parser for Turtle.
 */
public class TurtleListenerImpl extends TurtleBaseListener {

    private final Model model;
    private String baseURI;
    private final Map<String, String> prefixMap = new HashMap<>();
    private final ValueFactory factory;

    private Resource currentSubject;
    private IRI currentPredicate;

    /**
     * Constructor for TurtleListenerImpl that initializes the model, value factory,
     * and configuration options.
     *
     * @param model   the model to be populated by the parser
     * @param factory the value factory used to create RDF values
     * @param options optional configuration options for the parser
     */
    public TurtleListenerImpl(Model model, ValueFactory factory, IOOptions options) {
        this.model = model;
        this.baseURI = "";
        if (options != null && options instanceof RDFParserBaseIRIOptions) {
            this.baseURI = ((RDFParserBaseIRIOptions) options).getBase();
        }
        this.factory = factory;
    }

    /**
     * Constructor for TurtleListenerImpl that initializes the model and value
     * factory.
     *
     * @param ctx The parse tree context for the {@code prefixID} rule,
     * which provides access to the parsed prefix name and IRI reference tokens.
     */
    public void exitPrefixID(TurtleParser.PrefixIDContext ctx) {
        String prefix = ctx.PNAME_NS().getText();
        String iri = ctx.IRIREF().getText();
        prefix = prefix.substring(0, prefix.length() - 1);
        iri = iri.substring(1, iri.length() - 1);
        prefixMap.put(prefix, iri);

        model.setNamespace(prefix, iri);
    }

    @Override
    public void exitSparqlBase(TurtleParser.SparqlBaseContext ctx) {
        String iri = ctx.IRIREF().getText();
        baseURI = iri.substring(1, iri.length() - 1);
    }

    @Override
    public void enterTriples(TurtleParser.TriplesContext ctx) {
        currentSubject = extractSubject(ctx.subject());
    }

    @Override
    public void enterVerb(TurtleParser.VerbContext ctx) {
        currentPredicate = extractVerb(ctx);
    }

    @Override
    public void exitObject_(TurtleParser.Object_Context ctx) {
        Value object = extractObject(ctx);
        model.add(currentSubject, currentPredicate, object);
    }

    /**
     * Resolves the IRI from a raw string, handling prefixed names and base URIs.
     *
     * @param raw the raw string to resolve
     * @return the resolved IRI as a string
     */
    private String resolveIRI(String raw) {
        if (raw.startsWith("<") && raw.endsWith(">")) {
            return raw.substring(1, raw.length() - 1);
        } else if (raw.equals("a")) {
            return RDF.type.getIRI().stringValue();
        } else if (raw.contains(":")) {
            // Prefixed name (e.g., ex:predicate)
            String[] parts = raw.split(":", 2);
            String ns = prefixMap.get(parts[0]);
            if (ns != null) {
                return ns + parts[1];
            } else {
                throw new IllegalArgumentException("Prefix not declared: " + parts[0]);
            }
        } else {
            return baseURI + raw;
        }
    }

    /**
     * Strips quotes from a string, handling single and triple quotes.
     *
     * @param text the string to strip quotes from
     * @return the stripped string
     */
    private String stripQuotes(String text) {
        if (text == null || text.length() < 2)
            return text;
        if ((text.startsWith("\"") && text.endsWith("\"")) ||
                (text.startsWith("'''") && text.endsWith("'''")) ||
                (text.startsWith("\"\"\"") && text.endsWith("\"\"\""))) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }

    /**
     * Extracts a literal from the given context, handling different types of
     * literals.
     *
     * @param ctx the context containing the literal
     * @return the extracted Literal object
     */
    private Literal extractLiteral(TurtleParser.LiteralContext ctx) {
        String label;
        IRI datatype;
        String lang;

        if (ctx.rdfLiteral() != null) {
            if (ctx.rdfLiteral().iri() != null) {
                datatype = factory.createIRI(resolveIRI(ctx.rdfLiteral().iri().getText()));
                label = ctx.rdfLiteral().string().getText();
                return factory.createLiteral(stripQuotes(label), datatype);
            }
            if (ctx.rdfLiteral().LANGTAG() != null) {
                lang = ctx.rdfLiteral().LANGTAG().getText().substring(1);
                label = ctx.rdfLiteral().string().getText();
                return factory.createLiteral(stripQuotes(label), lang);
            }
            label = ctx.rdfLiteral().string().getText();
            return factory.createLiteral(stripQuotes(label));
        }

        if (ctx.BooleanLiteral() != null) {
            label = ctx.BooleanLiteral().getText();
            datatype = XSD.BOOLEAN.getIRI();
            return factory.createLiteral(label, datatype);
        }
        if (ctx.numericLiteral() != null) {
            if (ctx.numericLiteral().DECIMAL() != null) {
                label = ctx.numericLiteral().DECIMAL().getText();
                datatype = XSD.DECIMAL.getIRI();
                return factory.createLiteral(label, datatype);
            }
            if (ctx.numericLiteral().DOUBLE() != null) {
                label = ctx.numericLiteral().DOUBLE().getText();
                datatype = XSD.DOUBLE.getIRI();
                return factory.createLiteral(label, datatype);
            }
            if (ctx.numericLiteral().INTEGER() != null) {
                label = ctx.numericLiteral().INTEGER().getText();
                datatype = XSD.INTEGER.getIRI();
                return factory.createLiteral(label, datatype);
            }
        }
        throw new IllegalArgumentException("Unsupported literal type: " + ctx.getText());
    }

    /**
     * Extracts the object from the given context, which can be an IRI, blank node,
     * or literal.
     *
     * @param ctx the context containing the object
     * @return the extracted Value object
     */
    private Value extractObject(TurtleParser.Object_Context ctx) {
        if (ctx.iri() != null) {
            return factory.createIRI(resolveIRI(ctx.iri().getText()));
        }
        if (ctx.BlankNode() != null) {
            return factory.createBNode(ctx.BlankNode().getText());
        }
        if (ctx.literal() != null) {
            return extractLiteral(ctx.literal());
        }
        throw new RuntimeException("Unsupported object: " + ctx.getText());
    }

    /**
     * Extracts the subject from the given context, which can be an IRI or blank
     * node.
     *
     * @param ctx the context containing the subject
     * @return the extracted Resource object
     */
    private Resource extractSubject(TurtleParser.SubjectContext ctx) {
        if (ctx.iri() != null) {
            return factory.createIRI(resolveIRI(ctx.iri().getText()));
        }
        if (ctx.BlankNode() != null) {
            return factory.createBNode(ctx.BlankNode().getText());
        }
        throw new RuntimeException("Unsupported subject: " + ctx.getText());
    }

    /**
     * Extracts the predicate from the given context, which is expected to be an
     * IRI.
     *
     * @param ctx the context containing the predicate
     * @return the extracted IRI object
     */
    private IRI extractPredicate(TurtleParser.PredicateContext ctx) {
        return factory.createIRI(resolveIRI(ctx.getText()));
    }

    /**
     * Extracts the verb from the given context, which can be a predicate or an IRI.
     *
     * @param ctx the context containing the verb
     * @return the extracted IRI object
     */
    private IRI extractVerb(TurtleParser.VerbContext ctx) {
        if (ctx.predicate() != null) {
            return extractPredicate(ctx.predicate());
        } else
            return factory.createIRI(resolveIRI(ctx.getText()));
    }
}