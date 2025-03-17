package fr.inria.corese.core;

import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;

import java.util.Iterator;

public class NodeIterator implements Iterable<Node>, Iterator<Node> {

    Iterable<Edge> ie;
    Iterator<Edge> it;
    int index;

    NodeIterator(Iterable<Edge> it, int n) {
        ie = it;
        index = n;
    }

    public static NodeIterator create(Iterable<Edge> it, int n) {
        return new NodeIterator(it, n);
    }

    @Override
    public Iterator<Node> iterator() {
        it = ie.iterator();
        return this;
    }

    @Override
    public boolean hasNext() {
        return it.hasNext();
    }

    @Override
    public Node next() {
        Edge ent = it.next();
        if (ent == null) return null;
        return ent.getNode(index);
    }

    @Override
    public void remove() {
    }


}
