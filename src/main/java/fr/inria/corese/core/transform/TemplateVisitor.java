package fr.inria.corese.core.transform;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.sparql.api.TransformVisitor;

/**
 * Visitor that can be associated to a transformation
 * use case:
 * filter(st:visit(st:exp, ?x, true))
 * @author Olivier Corby - INRIA I3S -2015
 */
public interface TemplateVisitor extends TransformVisitor {
           
    void setGraph(Graph g);
             
    Graph visitedGraph();
        
}
