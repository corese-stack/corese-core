package fr.inria.corese.core.next.query.impl.sparql.parser.listener;

import fr.inria.corese.core.next.generated.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlQueryAstBuilder;

/**
 * Sparql ASK query as a feature
 * The listener will call {@code enterAskQuery()} and {@code exitAskQuery()}
 */
public class AskQueryAstListener extends AbstractSparqlAstListener implements QueryAstListener {

    public AskQueryAstListener(SparqlQueryAstBuilder builder) {
        super(builder);
    }

    public SparqlQueryAstBuilder queryBuilder() {
        return (SparqlQueryAstBuilder) builder();
    }

    @Override
    public void enterAskQuery(SparqlParser.AskQueryContext ctx) {
        queryBuilder().enterAskQuery();
    }

    @Override
    public void exitAskQuery(SparqlParser.AskQueryContext ctx) {
        queryBuilder().exitAskQuery();
    }


}
