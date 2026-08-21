package fr.inria.corese.core.next.query.impl.sparql.parser.listener;

import fr.inria.corese.core.next.generated.antlr.SparqlParserBaseListener;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlAstBuilder;

/**
 * Base class for SPARQL parsing features implemented as ANTLR listeners.
 *
 * @see SparqlParserBaseListener
 */
public abstract class AbstractSparqlAstListener extends SparqlParserBaseListener {

    private final SparqlAstBuilder builder;

    protected AbstractSparqlAstListener(SparqlAstBuilder builder) {
        this.builder = builder;
    }

    protected SparqlAstBuilder builder() {
        return this.builder;
    }


}
