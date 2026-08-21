package fr.inria.corese.core.next.query.impl.sparql.parser.listener;

import fr.inria.corese.core.next.generated.antlr.SparqlParser;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;

/**
 * SPARQL {@code FILTER} feature
 */
public class FilterAstListener extends AbstractSparqlAstListener {

    public FilterAstListener(SparqlAstBuilder builder) {
        super(builder);
    }

    @Override
    public void exitFilter_(SparqlParser.Filter_Context ctx) {
        FilterAst filter;

        if(ctx.constraint() != null) {
            filter = new FilterAst(this.builder().termFromConstraint(ctx.constraint()));
        } else {
            throw new QueryEvaluationException("Empty filter " + ctx.getText() + ", no createFunCall found");
        }

        this.builder().addFilter(filter);
    }

    @Override
    public void enterExistsFunc(SparqlParser.ExistsFuncContext ctx) {
        builder().enterExistsFunc();
    }

    @Override
    public void enterNotExistsFunc(SparqlParser.NotExistsFuncContext ctx) {
        builder().enterNotExistsFunc();
    }
}
