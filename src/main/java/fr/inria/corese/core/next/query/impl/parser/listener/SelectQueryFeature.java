package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;

/**
 * Sparql SELECT query as a feature
 * The listener will call {@code enterSelectQuery()} and {@code exitSelectQuery()}
 */
public class SelectQueryFeature extends AbstractSparqlFeature {

    private final SparqlAstBuilder builder;

    public SelectQueryFeature(SparqlAstBuilder builder) {
        this.builder = builder;
    }

    @Override
    public void enterSelectQuery(SparqlParser.SelectQueryContext ctx) {
        builder.enterSelectQuery();
    }

    @Override
    public void exitSelectQuery(SparqlParser.SelectQueryContext ctx) {
        builder.exitSelectQuery();
    }
}
