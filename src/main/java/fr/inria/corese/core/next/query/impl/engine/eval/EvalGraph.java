package fr.inria.corese.core.next.query.impl.engine.eval;

import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;
import fr.inria.corese.core.next.query.impl.engine.solution.Memory;

import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.spi.Matcher;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;

import java.util.List;

/**
 * @author corby
 */
public class EvalGraph {

    Eval engine;
    boolean stop = false;

    EvalGraph(Eval e) {
        engine = e;
    }

    void setStop() {
        stop = true;
    }

    /**
     * gNode is possible named graphURI stemming from evaluation context
     * It is not the named graph at stake here
     */
    int eval(Producer p, Node gNode, Exp exp, Mappings data, Stack stack, int n) throws SparqlException {
        int backtrack = n - 1;
        Node graphNode = exp.getGraphName();
        Node graph = engine.getNode(p, graphNode);
        Mappings res;

        if (graph == null) {
            res = graphNodes(p, exp, data);
        } else {
            res = graph(p, graph, exp, data);
        }

        if (res == null) {
            return backtrack;
        }

        Memory env = engine.getMemory();

        for (Mapping m : res) {
            if (stop) {
                return Eval.STOP;
            }

            if (!pushGraphMapping(env, graphNode, m, n)) {
                continue;
            }

            backtrack = engine.eval(p, gNode, stack, n + 1);
            env.pop(graphNode);
            env.pop(m);

            if (backtrack < n) {
                return backtrack;
            }
        }

        return backtrack;
    }

    private boolean pushGraphMapping(Memory environment, Node graphNode, Mapping mapping, int index) {
        Node boundGraph = graphNode.isVariable() ? mapping.getNode(graphNode) : null;
        if (boundGraph != null && !boundGraph.equals(mapping.getNamedGraph())) {
            return false;
        }
        if (!environment.push(mapping, index)) {
            return false;
        }
        if (environment.push(graphNode, mapping.getNamedGraph())) {
            return true;
        }
        environment.pop(mapping);
        return false;
    }

    /**
     * Iterate named graph pattern evaluation on named graph list
     * named graph list may come from Mappings map  from previous statement
     * OR from the "from named" clause OR from dataset named graph list
     */
    private Mappings graphNodes(Producer p, Exp exp, Mappings map) throws SparqlException {
        Memory env = engine.getMemory();
        Query qq = engine.getQuery();
        Matcher mm = engine.getMatcher();
        Node name = exp.getGraphName();
        Mappings res = null;
        Iterable<Node> graphNodes = null;

        if (map != null && map.inScope(name)) {
            // named graph list may come from evaluation context
            List<Node> list = map.aggregate(name);
            if (!list.isEmpty()) {
                graphNodes = list;
            }
        }
        if (graphNodes == null) {
            // from named clause OR dataset named graph list
            graphNodes = p.getGraphNodes(name, qq.getFrom(name), env);
        }

        for (Node graph : graphNodes) {
            if (mm.match(name, graph, env)) {
                Mappings m = graph(p, graph, exp, map);
                if (res == null) {
                    res = m;
                } else {
                    res.add(m);
                }
                env.pop(name);
            }
        }
        return res;
    }

    /**
     * Node graph: graph URI or Node graph pointer or Node path pointer
     * Exp exp: graph name { BGP }
     */
    private Mappings graph(Producer p, Node graph, Exp exp, Mappings map) throws SparqlException {
        boolean external = false;
        Producer np = p;
        if (graph != null && p.isProducer(graph)) {
            // Named graph pattern in GraphStore
            np = p.getProducer(graph, engine.getMemory());
            np.setGraphNode(graph);  // the new gNode
            external = true;
        }

        Exp body = exp.rest();
        Mappings res;
        Node varNode = null;
        Node target = null;

        if (!external) {
            target = graph;
        }

        if (engine.isFederate(exp)) {
            res = engine.subEval(np, target, varNode, body, exp, map, null, false, external);
        } else {
            Exp ee = body;
            Mappings data = null;

            if (graph != null && graph.getPath() == null) {
                // not a path pointer
                if (engine.isParameterGraphMappings()) {
                    // engine graph body with parameter map
                    // pro: if body is optional, engine it with parameter map
                    data = map;
                } else {
                    // engine graph body with values(map)
                    ee = body.complete(map);
                }
            }

            res = engine.subEval(np, target, varNode, ee, exp, data, null, false, external);
        }
        res.setNamedGraph(graph);

        engine.getVisitor().graph(engine, graph, exp, res);
        return res;
    }


}
