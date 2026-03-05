package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;

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
        this.builder().exitFilter();
    }
}
