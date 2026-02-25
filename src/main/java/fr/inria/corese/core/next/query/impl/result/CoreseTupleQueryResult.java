package fr.inria.corese.core.next.query.impl.result;

import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.next.query.kgram.core.Mapping;
import fr.inria.corese.core.next.query.kgram.core.Mappings;

import java.util.Iterator;
import java.util.List;

/**
 * Wrapper around Mappings
 */
public class CoreseTupleQueryResult implements TupleQueryResult {

    private final Mappings mappings;
    private final Iterator<Mapping> iterator;

    public CoreseTupleQueryResult(Mappings mappings) {
        this.mappings = mappings;
        this.iterator = this.mappings.iterator();
    }

    @Override
    public List<String> getBindingNames() {
        return this.mappings.getSelect().stream().map(Node::getLabel).toList();
    }

    @Override
    public boolean hasNext() {
        return this.iterator.hasNext();
    }

    @Override
    public BindingSet next() {
        return new CoreseBindingSet(this.iterator.next());
    }

    /**
     * Does nothing in this implementation
     */
    @Override
    public void close() {

    }
}
