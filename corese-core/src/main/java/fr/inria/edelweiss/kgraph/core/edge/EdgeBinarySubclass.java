package fr.inria.edelweiss.kgraph.core.edge;

import fr.inria.edelweiss.kgram.api.core.Node;
import fr.inria.edelweiss.kgraph.core.Graph;

/**
 * Edge entailed by a Rule
 * index and provenance
 * 
 * @author Olivier Corby, Wimmics INRIA I3S, 2016
 *
 */
public class EdgeBinarySubclass extends EdgeBinary {

    EdgeBinarySubclass(Node subject, Node object) {
        super(subject, object);
    }
    
    public static EdgeBinarySubclass create(Node source, Node subject, Node predicate, Node object){
        return new EdgeBinarySubclass(subject, object);
    }

    @Override
    public Node getEdgeNode() {
        return subject.getTripleStore().getNode(Graph.SUBCLASS_INDEX);
    }
}
