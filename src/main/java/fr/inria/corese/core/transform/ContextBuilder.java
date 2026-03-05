package fr.inria.corese.core.transform;

import java.util.HashMap;
import java.util.List;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.triple.parser.Context;

/**
 * Extract a Transformer Context from a profile.ttl graph st:param object
 * st:cal a st:Profile ;
 * st:transform st:calendar ;
 * st:param [ st:arg value ; st:title value ] .
 * 
 * @author Olivier Corby, Wimmics INRIA I3S, 2015
 *
 */
public class ContextBuilder {

    Graph graph;
    Context context;
    HashMap<String, Node> done;

    public ContextBuilder(Graph g) {
        this.graph = g;
        done = new HashMap<String, Node>();
        context = new Context();
    }

    public Context process(Node ctx) {
        // context = new Context();
        context(ctx, false);
        return context;
    }

    public ContextBuilder setContext(Context c) {
        context = c;
        return this;
    }

    void context(Node ctx, boolean exporter) {
        importer(ctx);

        for (Edge ent : graph.getEdgeList(ctx)) {
            String label = ent.getEdgeLabel();
            Node object = ent.getNode(1);

            if (label.equals(Context.STL_EXPORT) && (object.isBlank() || object.getDatatypeValue().isSkolem())) {
                // st:export [ st:lod (<http://dbpedia.org/>) ]
                context(object, true);
            } else if (!label.equals(Context.STL_IMPORT)) {
                if (object.isBlank() || object.getDatatypeValue().isSkolem()) {
                    IDatatype list = list(graph, object);
                    if (list != null) {
                        set(label, list, exporter);
                        continue;
                    }
                }
                set(label, object.getValue(), exporter);
            }
        }
    }

    void set(String name, IDatatype dt, boolean b) {
        if (b) {
            context.export(name, dt);
        } else {
            context.set(name, dt);
        }
    }

    /** 
     *           
     */
    void importer(Node n) {
        for (Edge ent : graph.getEdges(Context.STL_IMPORT, n, 0)) {
            if (ent != null) {
                Node imp = ent.getNode(1);
                if (done(imp)) {
                    continue;
                }
                Edge par = graph.getEdge(Context.STL_PARAM, imp, 0);
                if (par != null) {
                    context(par.getNode(1), false);
                }
            }
        }
    }

    boolean done(Node n) {
        if (done.containsKey(n.getLabel())) {
            return true;
        } else {
            done.put(n.getLabel(), n);
        }
        return false;
    }

    IDatatype list(Graph g, Node object) {
        List<IDatatype> list = g.reclist(object);
        if (list == null) {
            return null;
        }
        IDatatype dt = DatatypeMap.createList(list);
        return dt;
    }

}
