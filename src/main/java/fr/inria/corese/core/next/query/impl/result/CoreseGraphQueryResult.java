package fr.inria.corese.core.next.query.impl.result;

import fr.inria.corese.core.next.data.api.Model;
import fr.inria.corese.core.next.data.api.Statement;
import fr.inria.corese.core.next.query.api.result.GraphQueryResult;

import java.util.Iterator;

/**
 * Basic implementation around a Model
 */
public class CoreseGraphQueryResult implements GraphQueryResult {

    private Iterator<Statement> iterator;

    public CoreseGraphQueryResult(Model model) {
        this.iterator = model.iterator();
    }

    public CoreseGraphQueryResult(Iterator<Statement> it) {
        this.iterator = it;
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
