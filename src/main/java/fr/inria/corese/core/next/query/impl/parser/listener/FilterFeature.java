package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;

import java.util.List;

/**
 * SPARQL {@code FILTER} feature
 */
public class FilterFeature extends AbstractSparqlFeature {

    public FilterFeature(SparqlAstBuilder builder) {
        super(builder);
    }

    @Override
    public void enterFilter_(SparqlParser.Filter_Context ctx) {
    }

    @Override
    public void exitFilter_(SparqlParser.Filter_Context ctx) {
        FilterAst filter;

        if(ctx.constraint() != null) {
            filter = new FilterAst(this.expressionFromConstraint(ctx.constraint()));
        } else {
            throw new QueryEvaluationException("Empty filter " + ctx.getText() + ", no createFunCall found");
        }

        this.builder().exitFilter(filter);
    }
}
