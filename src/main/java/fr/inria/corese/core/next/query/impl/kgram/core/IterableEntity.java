package fr.inria.corese.core.next.query.impl.kgram.core;

import fr.inria.corese.core.next.query.impl.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Node;

import java.util.Iterator;

/**
 * @author Olivier Corby, Wimmics INRIA I3S, 2015
 */
final class IterableEntity implements Iterable<Edge> {

    private final Iterable<?> values;

    IterableEntity(Iterable<?> loop) {
        values = loop;
    }

    @Override
    public Iterator<Edge> iterator() {
        Iterator<?> iterator = values.iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Edge next() {
                return asEdge(iterator.next());
            }
        };
    }

    private static Edge asEdge(Object value) {
        if (value instanceof Edge edge) {
            return edge;
        }
        if (value instanceof Node node && node.getNodeObject() instanceof Edge edge) {
            return edge;
        }
        throw new IllegalStateException("Loop item cannot be converted to a KGRAM edge: " + value);
    }
}
