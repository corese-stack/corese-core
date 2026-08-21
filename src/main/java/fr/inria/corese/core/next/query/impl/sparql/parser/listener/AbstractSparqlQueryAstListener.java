package fr.inria.corese.core.next.query.impl.sparql.parser.listener;

import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlQueryAstBuilder;

public abstract class AbstractSparqlQueryAstListener extends AbstractSparqlAstListener implements QueryAstListener {

    protected AbstractSparqlQueryAstListener(SparqlAstBuilder builder) {
        super(builder);
    }

    @Override
    public SparqlQueryAstBuilder queryBuilder() {
        return (SparqlQueryAstBuilder) builder();
    }
}
