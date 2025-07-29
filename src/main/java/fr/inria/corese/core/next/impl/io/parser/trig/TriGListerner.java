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
     * Constructor for the TriGListerner.
     *
     * @param model   The RDF model to populate.
     * @param factory The ValueFactory for creating RDF resources.
     * @param options IOOptions for configuration (if any).
     */
    public TriGListerner(Model model, ValueFactory factory, IOOptions options) {
        this.model = model;
        this.baseURI = baseURI != null ? baseURI : "";
        if (options != null && options instanceof RDFParserBaseIRIOptions);
        this.factory = factory;
    }

    @Override
    public void exitPrefixID(TriGParser.PrefixIDContext ctx) {
        String prefix = ctx.PNAME_NS().getText();
        String iri = ctx.IRIREF().getText();
        prefix = prefix.substring(0, prefix.length() - 1);
        iri = iri.substring(1, iri.length() - 1);
        prefixMap.put(prefix, iri);
        model.setNamespace(prefix, iri);
    }

    @Override
    public void exitSparqlBase(TriGParser.SparqlBaseContext ctx) {
        baseURI = ctx.IRIREF().getText().replaceAll("^<|>$", "");
    }

    @Override
    public void enterBlock(TriGParser.BlockContext ctx) {
        currentGraph = ctx.Graph_w() != null && ctx.labelOrSubject() != null
                ? extractLabelOrSubject(ctx.labelOrSubject())
                : null;
    }

    @Override
    public void exitBlock(TriGParser.BlockContext ctx) {
        currentGraph = null;
    }

    @Override
    public void enterTriplesOrGraph(TriGParser.TriplesOrGraphContext ctx) {
        if (ctx.labelOrSubject() != null && ctx.predicateObjectList() != null) {
            currentSubject = extractLabelOrSubject(ctx.labelOrSubject());
            processPredicateObjectList(ctx.predicateObjectList());
        }
    }

    @Override
    public void enterTriples(TriGParser.TriplesContext ctx) {
        currentSubject = extractSubject(ctx.subject());
        processPredicateObjectList(ctx.predicateObjectList());
    }

    /**
     * Processes a PredicateObjectList context, extracting verbs and corresponding object lists,
     * and adding triples to the model for the current subject and graph.
     *
     * @param ctx the predicate-object list context from the parser
     */
    private void processPredicateObjectList(TriGParser.PredicateObjectListContext ctx) {
        List<TriGParser.VerbContext> verbs = ctx.verb();
        List<TriGParser.ObjectListContext> objLists = ctx.objectList();

        for (int i = 0; i < verbs.size(); i++) {
            currentPredicate = extractVerb(verbs.get(i));
            List<TriGParser.ObjectContext> objects = objLists.get(i).object();
            for (TriGParser.ObjectContext objCtx : objects) {
                Value object = extractObject(objCtx);
                model.add(currentSubject, currentPredicate, object, currentGraph);
            }
        }
    }

    /**
     * Extracts an RDF object from the ObjectContext.
     * Supports IRIs, blank nodes, literals, and inline blank node property lists.
     *
     * @param ctx the object context
     * @return the extracted RDF Value
     */
    private Value extractObject(TriGParser.ObjectContext ctx) {
        if (ctx.iri() != null) return factory.createIRI(resolveIRI(ctx.iri().getText()));
        if (ctx.blank() != null) return extractBlank(ctx.blank());
        if (ctx.literal() != null) return extractLiteral(ctx.literal());
        if (ctx.blankNodePropertyList() != null) return processBlankNodePropertyList(ctx.blankNodePropertyList());
        throw new RuntimeException("Unsupported object: " + ctx.getText());
    }

    /**
     * Processes an inline blank node with its property list, returning the blank node as a Resource.
     * Temporarily updates the current subject to the new blank node during processing.
     *
     * @param ctx the blank node property list context
     * @return the new blank node resource
     */
    private Resource processBlankNodePropertyList(TriGParser.BlankNodePropertyListContext ctx) {
        Resource bnode = factory.createBNode();
        Resource savedSubject = currentSubject;
        currentSubject = bnode;
        processPredicateObjectList(ctx.predicateObjectList());
        currentSubject = savedSubject;
        return bnode;
    }

    /**
     * Extracts a subject from a SubjectContext, which can be an IRI or a blank node.
     *
     * @param ctx the subject context
     * @return the extracted subject as a Resource
     */
    private Resource extractSubject(TriGParser.SubjectContext ctx) {
        if (ctx.iri() != null) return factory.createIRI(resolveIRI(ctx.iri().getText()));
        if (ctx.blank() != null) return extractBlank(ctx.blank());
        throw new RuntimeException("Unsupported subject: " + ctx.getText());
    }

    /**
     * Extracts a blank node from a BlankContext, supporting labeled (_:b) and anonymous ([]) forms.
     *
     * @param ctx the blank context
     * @return the blank node as a Resource
     */
    private Resource extractBlank(TriGParser.BlankContext ctx) {
        TriGParser.BlankNodeContext node = ctx.blankNode();
        if (node != null) {
            if (node.BLANK_NODE_LABEL() != null)
                return factory.createBNode(node.BLANK_NODE_LABEL().getText());
            if (node.ANON() != null)
                return factory.createBNode();
        }
        throw new RuntimeException("Unsupported blank node structure: " + ctx.getText());
    }

    /**
     * Extracts a graph label or subject from a LabelOrSubjectContext.
     * Supports IRI and blank node.
     *
     * @param ctx the label or subject context
     * @return the extracted resource
     */
    private Resource extractLabelOrSubject(TriGParser.LabelOrSubjectContext ctx) {
        if (ctx.iri() != null) return factory.createIRI(resolveIRI(ctx.iri().getText()));
        if (ctx.blankNode() != null) return factory.createBNode(ctx.blankNode().getText());
        throw new RuntimeException("Unsupported labelOrSubject: " + ctx.getText());
    }

    /**
     * Extracts a predicate IRI from a VerbContext.
     * Handles the special keyword 'a' as rdf:type.
     *
     * @param ctx the verb context
     * @return the extracted IRI
     */
    private IRI extractVerb(TriGParser.VerbContext ctx) {
        return factory.createIRI(resolveIRI(ctx.getText()));
    }

    /**
     * Extracts a Literal from a LiteralContext, handling typed, language-tagged, boolean, and numeric literals.
     *
     * @param ctx the literal context
     * @return the extracted Literal
     */
    private Literal extractLiteral(TriGParser.LiteralContext ctx) {
        if (ctx.rDFLiteral() != null) {
            String label = stripQuotes(ctx.rDFLiteral().string().getText());
            if (ctx.rDFLiteral().LANGTAG() != null)
                return factory.createLiteral(label, ctx.rDFLiteral().LANGTAG().getText().substring(1));
            if (ctx.rDFLiteral().iri() != null)
                return factory.createLiteral(label, factory.createIRI(resolveIRI(ctx.rDFLiteral().iri().getText())));
            return factory.createLiteral(label);
        }
        if (ctx.BooleanLiteral() != null)
            return factory.createLiteral(ctx.BooleanLiteral().getText(), XSD.BOOLEAN.getIRI());
        if (ctx.numericLiteral() != null) {
            if (ctx.numericLiteral().INTEGER() != null)
                return factory.createLiteral(ctx.numericLiteral().INTEGER().getText(), XSD.INTEGER.getIRI());
            if (ctx.numericLiteral().DECIMAL() != null)
                return factory.createLiteral(ctx.numericLiteral().DECIMAL().getText(), XSD.DECIMAL.getIRI());
            if (ctx.numericLiteral().DOUBLE() != null)
                return factory.createLiteral(ctx.numericLiteral().DOUBLE().getText(), XSD.DOUBLE.getIRI());
        }
        throw new RuntimeException("Unsupported literal: " + ctx.getText());
    }

    /**
     * Resolves an IRI or QName into a full URI string.
     * Handles full IRIs in angle brackets, QNames using prefixes, and special case "a".
     *
     * @param raw the raw string
     * @return the resolved URI string
     */
    private String resolveIRI(String raw) {
        raw = raw.trim();
        if (raw.startsWith("<") && raw.endsWith(">")) return raw.substring(1, raw.length() - 1);
        if (raw.equals("a")) return RDF.type.getIRI().stringValue();
        if (raw.contains(":")) {
            String[] parts = raw.split(":", 2);
            String ns = prefixMap.get(parts[0]);
            if (ns != null) return ns + parts[1];
            throw new IllegalArgumentException("Undeclared prefix: " + parts[0]);
        }
        return baseURI + raw;
    }

    /**
     * Strips surrounding quotes from a string literal, including single, double, and multi-line forms.
     *
     * @param text the quoted string
     * @return the unquoted string
     */
    private String stripQuotes(String text) {
        if (text == null || text.length() < 2) return text;
        if ((text.startsWith("\"") && text.endsWith("\"")) ||
                (text.startsWith("\"\"\"") && text.endsWith("\"\"\"")) ||
                (text.startsWith("'''") && text.endsWith("'''"))) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }
}