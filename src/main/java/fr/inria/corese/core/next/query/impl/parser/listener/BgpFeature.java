package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * SPARQL 1.0 feature: build triple patterns, BGPs and OPTIONAL for the WHERE clause.
 *
 * <p>Grammar hooks:
 * <ul>
 *   <li>GroupGraphPattern: {@code { ... } } → builder.enterGroup() / exitGroup()</li>
 *   <li>TriplesBlock: BGP boundary → builder.enterBgp() / exitBgp()</li>
 *   <li>TriplesSameSubject (when not inside CONSTRUCT template): {@link SparqlAstBuilder} term helpers
 *       → builder.addTriple(subject, predicate, object)</li>
 *   <li>OptionalGraphPattern: {@code OPTIONAL { ... } } → builder.enterOptional() / exitOptional()</li>
 * </ul>
 */
public class BgpFeature extends AbstractSparqlFeature {

    public BgpFeature(SparqlAstBuilder builder) {
        super(builder);
    }

    @Override
    public void enterWhereClause(SparqlParser.WhereClauseContext ctx) {
        builder().enterWhereClause();
    }

    @Override
    public void enterGroupGraphPattern(SparqlParser.GroupGraphPatternContext ctx) {
        builder().enterGroup();
    }

    @Override
    public void exitGroupGraphPattern(SparqlParser.GroupGraphPatternContext ctx) {
        builder().exitGroup();
    }

    @Override
    public void enterTriplesBlock(SparqlParser.TriplesBlockContext ctx) {
        builder().enterBgp();
    }

    @Override
    public void exitTriplesBlock(SparqlParser.TriplesBlockContext ctx) {
        builder().exitBgp();
    }

    @Override
    public void enterOptionalGraphPattern(SparqlParser.OptionalGraphPatternContext ctx) {
        builder().enterOptional();
    }

    @Override
    public void exitOptionalGraphPattern(SparqlParser.OptionalGraphPatternContext ctx) {
        builder().exitOptional();
    }

    /**
     * Triples in the WHERE clause only (ignore triples inside the CONSTRUCT template).
     */
    @Override
    public void exitTriplesSameSubject(SparqlParser.TriplesSameSubjectContext ctx) {
        if (ctx.getParent() instanceof SparqlParser.ConstructTriplesContext) {
            return;
        }
        if (!builder().hasCurrentBgp()) {
            return;
        }
        if (ctx.varOrTerm() == null || ctx.propertyListNotEmpty() == null) {
            return;
        }
        SparqlAstBuilder b = builder();
        TermAst subject = b.termFromVarOrTerm(ctx.varOrTerm());
        var propertyList = ctx.propertyListNotEmpty();
        for (int verbIndex = 0; verbIndex < propertyList.verb().size(); verbIndex++) {
            TermAst predicate = b.termFromVerb(propertyList.verb(verbIndex));
            List<TermAst> objects = b.termListFromObjectList(propertyList.objectList(verbIndex));
            for (TermAst object : objects) {
                b.addTriple(subject, predicate, object);
            }
        }
    }

    @Override
    public void exitTriplesSameSubjectPath(SparqlParser.TriplesSameSubjectPathContext ctx) {
        SparqlAstBuilder b = builder();
        if (ctx.varOrTerm() != null) {
            var propertyList = ctx.propertyListPathNotEmpty();
            if (propertyList == null) {
                return;
            }
            b.addTriplesFromPropertyListPath(b.termFromVarOrTerm(ctx.varOrTerm()), propertyList);
            return;
        }
        if (ctx.triplesNodePath() == null) {
            return;
        }
        TermAst subject = b.subjectFromTriplesNodePath(ctx.triplesNodePath());
        SparqlParser.PropertyListPathContext propertyListPath = ctx.propertyListPath();
        if (propertyListPath != null && propertyListPath.propertyListPathNotEmpty() != null) {
            b.addTriplesFromPropertyListPath(subject, propertyListPath.propertyListPathNotEmpty());
        }
    }
}
