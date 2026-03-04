package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;

/**
 * Sparql ASK query as a feature
 * The listener will call {@code enterAskQuery()} and {@code exitAskQuery()}
 */
public class AskQueryFeature extends AbstractSparqlFeature {

    private final SparqlAstBuilder builder;

    public AskQueryFeature(SparqlAstBuilder builder) {
        this.builder = builder;
    }

    @Override
    public void enterAskQuery(SparqlParser.AskQueryContext ctx) {
        builder.enterAskQuery();
    }

    @Override
    public void exitAskQuery(SparqlParser.AskQueryContext ctx) {
        builder.exitAskQuery();
    }


}
