package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * SPARQL {@code FILTER} feature
 */
public class FilterFeature extends AbstractSparqlFeature {

    private static final Logger logger = LoggerFactory.getLogger(FilterFeature.class);

    public FilterFeature(SparqlAstBuilder builder) {
        super(builder);
    }

    @Override
    public void exitFilter_(SparqlParser.Filter_Context ctx) {
        logger.debug("exit {}", ctx);
        FilterAst filter;

        if(ctx.constraint() != null) {
            filter = new FilterAst(this.builder().termFromConstraint(ctx.constraint()));
        } else {
            throw new QueryEvaluationException("Empty filter " + ctx.getText() + ", no createFunCall found");
        }

        this.builder().addFilter(filter);
    }
}
