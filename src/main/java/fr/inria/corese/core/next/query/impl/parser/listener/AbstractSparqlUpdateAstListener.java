package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.query.impl.parser.SparqlUpdateAstBuilder;

public abstract class AbstractSparqlUpdateAstListener  extends AbstractSparqlAstListener implements UpdateAstListener{
    protected AbstractSparqlUpdateAstListener(SparqlAstBuilder builder) {
        super(builder);
    }

    @Override
    public SparqlUpdateAstBuilder updateBuilder() {
        return (SparqlUpdateAstBuilder) builder();
    }
}
