package fr.inria.corese.core.edge;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Node;

/**
 * Graph Edge for the defaultGraph
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2016
 */
public class EdgeDefault extends EdgeTriple {

    EdgeDefault(Node predicate, Node subject, Node object) {
        super(predicate, subject, object);
    }

    public static EdgeDefault create(Node source, Node subject, Node predicate, Node object) {
        return new EdgeDefault(predicate, subject, object);
    }

    @Override
    public Node getGraph() {
        return subject.getTripleStore().getNode(Graph.DEFAULT_INDEX);
    }

}
