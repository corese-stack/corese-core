package fr.inria.corese.core.next.query.impl.result;

import fr.inria.corese.core.next.data.api.Statement;
import fr.inria.corese.core.next.query.api.result.GraphQueryResult;

import java.util.Iterator;

/**
 * Basic implementation backed by a pre-built list of statements.
 */
public class CoreseGraphQueryResult implements GraphQueryResult {

    private final Iterator<Statement> iterator;

    public CoreseGraphQueryResult(Iterator<Statement> iterator) {
        this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
        return this.iterator.hasNext();
    }

    @Override
    public Statement next() {
        return this.iterator.next();
    }

    /**
     * No closing needed in this implementation
     */
    @Override
    public void close() {
    }
}
