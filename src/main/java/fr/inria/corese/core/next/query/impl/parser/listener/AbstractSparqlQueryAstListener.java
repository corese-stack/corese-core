package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.query.impl.parser.SparqlQueryAstBuilder;

public abstract class AbstractSparqlQueryAstListener extends AbstractSparqlAstListener implements QueryAstListener {

    protected AbstractSparqlQueryAstListener(SparqlAstBuilder builder) {
        super(builder);
    }

    @Override
    public SparqlQueryAstBuilder queryBuilder() {
        return (SparqlQueryAstBuilder) builder();
    }
}
