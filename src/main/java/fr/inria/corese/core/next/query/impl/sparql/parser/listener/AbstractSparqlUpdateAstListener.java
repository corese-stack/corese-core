package fr.inria.corese.core.next.query.impl.sparql.parser.listener;

import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlUpdateAstBuilder;

public abstract class AbstractSparqlUpdateAstListener  extends AbstractSparqlAstListener implements UpdateAstListener{
    protected AbstractSparqlUpdateAstListener(SparqlAstBuilder builder) {
        super(builder);
    }

    @Override
    public SparqlUpdateAstBuilder updateBuilder() {
        return (SparqlUpdateAstBuilder) builder();
    }
}
