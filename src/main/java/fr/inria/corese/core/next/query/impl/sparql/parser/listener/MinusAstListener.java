package fr.inria.corese.core.next.query.impl.sparql.parser.listener;

import fr.inria.corese.core.next.generated.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlAstBuilder;

/**
 * SPARQL 1.1 parser feature that handles the {@code MINUS} graph pattern.
 */
public class MinusAstListener extends AbstractSparqlAstListener {

    public MinusAstListener(SparqlAstBuilder builder) {
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
