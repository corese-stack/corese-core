package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;

/**
 * SPARQL 1.1 parser feature that handles the {@code MINUS} graph pattern.
 */
public class MinusFeature extends AbstractSparqlFeature {

    public MinusFeature(SparqlAstBuilder builder) {
        super(builder);
    }

    @Override
    public void enterMinusGraphPattern(SparqlParser.MinusGraphPatternContext ctx) {
        builder().enterMinus();
    }

    @Override
    public void exitMinusGraphPattern(SparqlParser.MinusGraphPatternContext ctx) {
        builder().exitMinus();
    }
}
