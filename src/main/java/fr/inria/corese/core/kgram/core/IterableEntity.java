package fr.inria.corese.core.kgram.core;

import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;

import java.util.Iterator;

/**
 * @author Olivier Corby, Wimmics INRIA I3S, 2015
 */
class IterableEntity implements Iterable<Edge>, Iterator<Edge> {

    Iterable<Edge> loop;
    Iterator<Edge> it;

    IterableEntity(Iterable<Edge> loop) {
        this.loop = loop;
        it = loop.iterator();
    }

    @Override
    public Iterator<Edge> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return it.hasNext();
    }

    @Override
    public Edge next() {
        Edge obj = it.next();
        if (obj instanceof Node) {
            Node n = (Node) obj;
            return (Edge) n.getNodeObject();
        }

        return obj;


    }

    @Override
    public void remove() {
    }

}
