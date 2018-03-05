package fr.inria.edelweiss.kgraph.core.edge;

import fr.inria.edelweiss.kgram.api.core.Node;

/**
 * Graph Edge for internal storage
 *
 * @author Olivier Corby, Wimmics, INRIA I3S, 2014
 *
 */
public  class EdgeInternal extends EdgeBinary {
    protected Node graph;

    public EdgeInternal() {
    }

  
    EdgeInternal(Node graph, Node subject, Node object) {
        this.graph = graph;
        this.subject = subject;
        this.object = object;
    }  
    
    public static EdgeInternal create(Node graph, Node subject, Node predicate, Node object){
        return new EdgeInternal(graph, subject, object);
    }
    
     @Override
    public Node getGraph(){
        return graph;
    }
     
    @Override
     public void setGraph(Node g){
         graph = g;
     }
    
}

