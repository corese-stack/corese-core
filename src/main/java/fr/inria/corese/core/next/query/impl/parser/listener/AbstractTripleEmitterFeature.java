package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared base for features that transform SPARQL triple syntax
 * ({@code triplesSameSubject}) into AST triples.
 *
 * <p>Subclasses decide whether a given {@code triplesSameSubject} must be handled
 * ({@link #shouldHandleTriplesSameSubject}) and what to do with each emitted triple
 * ({@link #emitTriple}). Current implementations: {@link BgpFeature} (WHERE clause)
 * and {@link ConstructQueryFeature} (CONSTRUCT template).
 *
 * <p>Only the grammar alternative {@code varOrTerm propertyListNotEmpty} is supported;
 * {@code triplesNode propertyList} (subject as collection/blank node list) is not handled.
 */
public abstract class AbstractTripleEmitterFeature extends AbstractSparqlFeature {

    protected final SparqlAstBuilder builder;

    protected AbstractTripleEmitterFeature(SparqlAstBuilder builder) {
        super(builder);
        this.builder = builder;
    }

    @Override
    public final void exitTriplesSameSubject(SparqlParser.TriplesSameSubjectContext ctx) {
        if (!shouldHandleTriplesSameSubject(ctx)) {
            return;
        }

        if (ctx.varOrTerm() == null || ctx.propertyListNotEmpty() == null) {
            return;
        }

        TermAst subject = termFromVarOrTerm(ctx.varOrTerm());
        var propertyList = ctx.propertyListNotEmpty();

        for (int verbIndex = 0; verbIndex < propertyList.verb().size(); verbIndex++) {
            TermAst predicate = termFromVerb(propertyList.verb(verbIndex));
            List<TermAst> objects = termListFromObjectList(propertyList.objectList(verbIndex));

            for (TermAst object : objects) {
                emitTriple(subject, predicate, object);
            }
        }
    }

    /**
     * Whether this feature should process the given {@code triplesSameSubject}.
     */
    protected abstract boolean shouldHandleTriplesSameSubject(SparqlParser.TriplesSameSubjectContext ctx);

    /**
     * Emits a triple once it has been converted to AST terms.
     */
    protected abstract void emitTriple(TermAst subject, TermAst predicate, TermAst object);

    // ---- shared term helpers ----

    protected TermAst termFromVerb(SparqlParser.VerbContext ctx) {
        if (ctx.A() != null) return builder.iri("a");
        return termFromVarOrIriRef(ctx.varOrIRIref());
    }

    protected TermAst termFromVarOrTerm(SparqlParser.VarOrTermContext ctx) {
        if (ctx.var_() != null) return builder.var(ctx.var_().getText());
        return termFromGraphTerm(ctx.graphTerm());
    }

    protected TermAst termFromVarOrIriRef(SparqlParser.VarOrIRIrefContext ctx) {
        String tokenText = ctx.getText();
        if (tokenText.startsWith("?") || tokenText.startsWith("$")) {
            return builder.var(tokenText);
        }
        return builder.iri(tokenText);
    }

    protected TermAst termFromGraphTerm(SparqlParser.GraphTermContext ctx) {
        if (ctx.iriRef() != null) return builder.iri(ctx.iriRef().getText());
        if (ctx.rdfLiteral() != null) return termFromRdfLiteral(ctx.rdfLiteral());
        if (ctx.numericLiteral() != null) return builder.literal(ctx.numericLiteral().getText(), null, null);
        if (ctx.booleanLiteral() != null) return builder.literal(ctx.booleanLiteral().getText(), null, null);
        if (ctx.blankNode() != null) return builder.iri(ctx.blankNode().getText());
        if (ctx.NIL() != null) return builder.iri("()");
        return builder.iri(ctx.getText());
    }

    protected List<TermAst> termListFromObjectList(SparqlParser.ObjectListContext ctx) {
        List<TermAst> objectTerms = new ArrayList<>();
        for (var objectContext : ctx.object_()) {
            objectTerms.add(termFromObject(objectContext));
        }
        return objectTerms;
    }

    protected TermAst termFromObject(SparqlParser.Object_Context ctx) {
        return termFromGraphNode(ctx.graphNode());
    }

    protected TermAst termFromGraphNode(SparqlParser.GraphNodeContext ctx) {
        if (ctx.varOrTerm() != null) return termFromVarOrTerm(ctx.varOrTerm());
        if (ctx.triplesNode() != null) return builder.iri(ctx.triplesNode().getText());
        return builder.iri(ctx.getText());
    }

    protected TermAst termFromRdfLiteral(SparqlParser.RdfLiteralContext ctx) {
        String lexical = ctx.string_().getText();
        String lang = null;
        String datatype = null;

        if (ctx.LANGTAG() != null) {
            String langTagText = ctx.LANGTAG().getText();
            lang = langTagText.startsWith("@") ? langTagText.substring(1) : langTagText;
        } else if (ctx.DOUBLE_CARET() != null && ctx.iriRef() != null) {
            datatype = ctx.iriRef().getText();
        }

        return builder.literal(lexical, lang, datatype);
    }
}