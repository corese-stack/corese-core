/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.inria.corese.core.query;

import fr.inria.corese.core.api.Loader;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import fr.inria.corese.core.kgram.api.query.Graphable;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.load.LoadException;
import fr.inria.corese.core.util.SPINProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Olivier Corby, Wimmics INRIA I3S, 2014
 */
public class RDFizer {
    private static Logger logger = LoggerFactory.getLogger(RDFizer.class);

    public boolean isGraphAble(Object obj) {
        if (obj instanceof Graphable) {
            return true;
        }
        return false;
    }

    public Graph getGraph(Object obj) {
        if (obj instanceof Graphable) {
            return getGraph((Graphable) obj);
        }
        return null;
    }

    Graph getGraph(Graphable gg) {
        Graph g = (Graph) gg.getGraph();
        if (g != null) {
            return g;
        }
        String rdf = gg.toGraph();
        try {
            g = getGraph(rdf);
            gg.setGraph(g);
        } catch (EngineException ex) {

        }
        return g;
    }

    Graph getGraph(String rdf) throws EngineException {
        Graph g = Graph.create();
        Load ld = Load.create(g);
        ld.setEvent(false);
        try {
            ld.loadString(rdf, Loader.format.TURTLE_FORMAT);
        } catch (LoadException ex) {
            logger.error("", ex);
        }
        g.prepare();
        return g;
    }


    Graph getGraph(Query q) {
        try {
            Graph g = (Graph) q.getGraph();
            if (g == null) {
                ASTQuery ast = q.getAST();
                SPINProcess sp = SPINProcess.create();
                g = sp.toSpinGraph(ast);
                q.setGraph(g);
            }
            g.prepare();
            return g;
        } catch (EngineException ex) {
        }
        return null;
    }

}
