package fr.inria.corese.core.next.query.impl.sparql.execution;

import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Node;
import fr.inria.corese.core.next.query.impl.kgram.core.Mapping;
import fr.inria.corese.core.next.query.impl.kgram.core.Mappings;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Adapts a KGRAM {@link Mappings} result to the {@link TupleQueryResult} API.
 */
public final class CoreseTupleQueryResult implements TupleQueryResult {

    private final Mappings mappings;
    private final Iterator<Mapping> iterator;
    private boolean closed;

    public CoreseTupleQueryResult(Mappings mappings) {
        this.mappings = Objects.requireNonNull(mappings, "mappings");
        this.iterator = this.mappings.iterator();
    }

    @Override
    public List<String> getBindingNames() {
        return this.mappings.getSelect().stream().map(Node::getLabel).toList();
    }

    @Override
    public boolean hasNext() {
        checkOpen();
        return this.iterator.hasNext();
    }

    @Override
    public BindingSet next() {
        checkOpen();
        return new CoreseBindingSet(this.iterator.next());
    }

    @Override
    public void close() {
        closed = true;
    }

    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException("This tuple query result is closed.");
        }
    }
}
