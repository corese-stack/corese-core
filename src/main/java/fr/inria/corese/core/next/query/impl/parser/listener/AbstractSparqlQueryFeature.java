package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.query.impl.parser.SparqlQueryAstBuilder;

public abstract class AbstractSparqlQueryFeature extends AbstractSparqlFeature implements QueryFeature {

    protected AbstractSparqlQueryFeature(SparqlAstBuilder builder) {
        super(builder);
    }

    @Override
    public SparqlQueryAstBuilder queryBuilder() {
        return (SparqlQueryAstBuilder) builder();
    }
}
