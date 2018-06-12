package fr.inria.edelweiss.kgtool.util;

import fr.inria.edelweiss.kgraph.core.Graph;
import fr.inria.edelweiss.kgtool.load.Load;
import fr.inria.edelweiss.kgtool.load.LoadException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2014
 *
 */
public class GraphUtil {
    
    Graph gs;
    
    public GraphUtil(Graph g){
        gs = g;
    }
    
    public Graph shoot(){
        Graph g = Graph.create();
        try {
            Load ld = Load.create(g);
            ld.loadString(gs.toString(), Load.TURTLE_FORMAT);
        } catch (LoadException ex) {
            LogManager.getLogger(GraphUtil.class.getName()).log(Level.ERROR, "", ex);
        }
        return g;
    }   
    
    public Graph shoot(String name){
        Graph g = shoot();
        gs.setNamedGraph(name, g);
        return g;
    }

}
