package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;

public class SelectQueryFeature extends AbstractSparqlFeature {

    private final SparqlAstBuilder builder;

    public SelectQueryFeature(SparqlAstBuilder builder) {
        this.builder = builder;
    }

    @Override
    public void enterSelectQuery(SparqlParser.SelectQueryContext ctx) {
        builder.enterSelectQuery();
    }
}
