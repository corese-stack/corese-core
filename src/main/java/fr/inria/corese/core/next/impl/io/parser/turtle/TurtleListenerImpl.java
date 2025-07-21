package fr.inria.corese.core.next.impl.io.parser.turtle;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.api.io.parser.RDFParserBaseIRIOptions;
import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.parser.antlr.TurtleBaseListener;
import fr.inria.corese.core.next.impl.parser.antlr.TurtleParser;
import fr.inria.corese.core.next.impl.temp.ModelNamespace;

import java.util.HashMap;
import java.util.Map;

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

    public TurtleListenerImpl(Model model, ValueFactory factory, IOOptions options) {
        this.model = model;
        this.baseURI =  "";
        if(options != null && options instanceof RDFParserBaseIRIOptions) {
            this.baseURI = ((RDFParserBaseIRIOptions) options).getBase();
        }
        this.factory = factory;
    }

    public void exitPrefixID(TurtleParser.PrefixIDContext ctx) {
        String prefix = ctx.PNAME_NS().getText();
        String iri = ctx.IRIREF().getText();
        prefix = prefix.substring(0, prefix.length() - 1);
        iri = iri.substring(1, iri.length() - 1);
        prefixMap.put(prefix, iri);

        Namespace ns = new ModelNamespace(prefix, iri);
        model.setNamespace(prefix, iri);
    }

    public void exitSparqlBase(TurtleParser.SparqlBaseContext ctx) {
        String iri = ctx.IRIREF().getText();
        baseURI = iri.substring(1, iri.length() - 1);
    }

    public void enterTriples(TurtleParser.TriplesContext ctx) {
        currentSubject = extractSubject(ctx.subject());
     }

    public void enterVerb(TurtleParser.VerbContext ctx) {
        currentPredicate = extractVerb(ctx);
    }

    public void exitObject_(TurtleParser.Object_Context ctx) {
        Value object = extractObject(ctx);
        model.add(currentSubject, currentPredicate, object);
    }

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

    private String stripQuotes(String text) {
        if (text == null || text.length() < 2) return text;
        if ((text.startsWith("\"") && text.endsWith("\"")) ||
                (text.startsWith("'''") && text.endsWith("'''")) ||
                (text.startsWith("\"\"\"") && text.endsWith("\"\"\""))) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }

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

    private Resource extractSubject(TurtleParser.SubjectContext ctx) {
        if (ctx.iri() != null) {
            return factory.createIRI(resolveIRI(ctx.iri().getText()));
        }
        if (ctx.BlankNode() != null) {
            return factory.createBNode(ctx.BlankNode().getText());
        }
        throw new RuntimeException("Unsupported subject: " + ctx.getText());
    }

    private IRI extractPredicate(TurtleParser.PredicateContext ctx) {
        return factory.createIRI(resolveIRI(ctx.getText()));
    }

    private IRI extractVerb(TurtleParser.VerbContext ctx) {
        if (ctx.predicate() != null) {
            return extractPredicate(ctx.predicate());
        }
        else return factory.createIRI(resolveIRI(ctx.getText()));
    }
}