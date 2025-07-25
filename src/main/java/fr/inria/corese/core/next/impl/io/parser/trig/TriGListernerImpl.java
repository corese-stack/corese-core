package fr.inria.corese.core.next.impl.io.parser.trig;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.api.io.parser.RDFParserBaseIRIOptions;
import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.parser.antlr.TriGBaseListener;
import fr.inria.corese.core.next.impl.parser.antlr.TriGParser;
import fr.inria.corese.core.next.api.ValueFactory;
import java.util.HashMap;
import java.util.Map;

public class TriGListernerImpl extends TriGBaseListener {
    private final Model model;
    private String baseURI;
    private final Map<String, String> prefixMap = new HashMap<>();
    private final ValueFactory factory;

    private Resource currentSubject;
    private IRI currentPredicate;

    public TriGListernerImpl(Model model, ValueFactory factory, IOOptions options) {
        this.model = model;
        this.baseURI = baseURI != null ? baseURI : "";
        if (options != null && options instanceof RDFParserBaseIRIOptions);
        this.factory = factory;
    }

    public void exitPrefixID(TriGParser.PrefixIDContext ctx) {
        String prefix = ctx.PNAME_NS().getText();
        String iri = ctx.IRIREF().getText();
        prefix = prefix.substring(0, prefix.length() - 1);
        iri = iri.substring(1, iri.length() - 1);
        prefixMap.put(prefix, iri);
        model.setNamespace(prefix, iri);
    }

    public void exitSparqlBase(TriGParser.SparqlBaseContext ctx) {
        String iri = ctx.IRIREF().getText();
        baseURI = iri.substring(1, iri.length() - 1);
    }

    public void enterTriples(TriGParser.TriplesContext ctx) {
        currentSubject = extractSubject(ctx.subject());
    }

    public void enterVerb(TriGParser.VerbContext ctx) {
        currentPredicate = extractVerb(ctx);
    }

    public void exitObject(TriGParser.ObjectContext ctx) {
        Value object = extractObject(ctx);
        model.add(currentSubject, currentPredicate, object);
    }

    private String resolveIRI(String raw) {
        if (raw.startsWith("<") && raw.endsWith(">")) {
            return raw.substring(1, raw.length() - 1);
        } else if (raw.equals("a")) {
            return RDF.type.getIRI().stringValue();
        } else if (raw.contains(":")) {
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

    private Literal extractLiteral(TriGParser.LiteralContext ctx) {
        String label;
        IRI datatype;
        String lang;

        if (ctx.rDFLiteral() != null) {
            if (ctx.rDFLiteral().iri() != null) {
                datatype = factory.createIRI(resolveIRI(ctx.rDFLiteral().iri().getText()));
                label = ctx.rDFLiteral().string().getText();
                return factory.createLiteral(stripQuotes(label), datatype);
            }
            if (ctx.rDFLiteral().LANGTAG() != null) {
                lang = ctx.rDFLiteral().LANGTAG().getText().substring(1);
                label = ctx.rDFLiteral().string().getText();
                return factory.createLiteral(stripQuotes(label), lang);
            }
            label = ctx.rDFLiteral().string().getText();
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

    private Value extractObject(TriGParser.ObjectContext ctx) {
        if (ctx.iri() != null) {
            return factory.createIRI(resolveIRI(ctx.iri().getText()));
        }
        if (ctx.blank() != null) {
            return factory.createBNode(ctx.blank().getText());
        }
        if (ctx.literal() != null) {
            return extractLiteral(ctx.literal());
        }
        throw new RuntimeException("Unsupported object: " + ctx.getText());
    }

    private Resource extractSubject(TriGParser.SubjectContext ctx) {
        if (ctx.iri() != null) {
            return factory.createIRI(resolveIRI(ctx.iri().getText()));
        }
        if (ctx.blank() != null) {
            return factory.createBNode(ctx.blank().getText());
        }
        throw new RuntimeException("Unsupported subject: " + ctx.getText());
    }

    private IRI extractPredicate(TriGParser.PredicateContext ctx) {
        return factory.createIRI(resolveIRI(ctx.getText()));
    }

    private IRI extractVerb(TriGParser.VerbContext ctx) {
        if (ctx.predicate() != null) {
            return extractPredicate(ctx.predicate());
        } else {
            return factory.createIRI(resolveIRI(ctx.getText()));
        }
    }
}
