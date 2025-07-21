package fr.inria.corese.core.kgram.core;

import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.api.query.Environment;
import fr.inria.corese.core.kgram.api.query.Producer;

import static fr.inria.corese.core.kgram.core.Eval.STOP;

/**
 * @author corby
 */
public class EvalOptional {

    Eval eval;
    private boolean stop = false;

    EvalOptional(Eval e) {
        eval = e;
    }

    /**
     * @return the stop
     */
    public boolean isStop() {
        return stop;
    }

    /**
     * @param stop the stop to set
     */
    public void setStop(boolean stop) {
        this.stop = stop;
    }

    /**
     * A optional B Filter F map1 = eval(A) ; map2 = eval(values Vb {
     * distinct(map1/Vb) } B) Vb = variables in-subscope in B, ie in-scope
     * except in right arg of an optional in B for m1 in map1: for m2 in map2:
     * if m1.compatible(m2): merge = m1.merge(m2) if eval(F(merge)) result +=
     * merge ...
     */
    int eval(Producer p, Node graphNode, Exp exp, Mappings data, Stack stack, int n) throws SparqlException {
        int backtrack = n - 1;
        Memory env = eval.getMemory();
        Node queryNode = null;

        Mappings map1 = eval.subEval(p, graphNode, queryNode, exp.first(), exp, data);
        if (isStop()) {
            return STOP;
        }
        if (map1.isEmpty()) {
            return backtrack;
        }

        Mappings map2 = null;
        Exp rest = exp.rest();
        MappingSet set1 = new MappingSet(getQuery(), map1);
        Mappings map = set1.prepareMappingsRest(rest);

        if (map != null && exp.getInscopeFilter() != null) {
            /*
             * Use case: BGP1 optional { filter(exp) BGP2} all var in
             * exp.getVariables() are inscope(BGP1, BGP2) and are bound in
             * Mappings map => select Mapping m in map where filter (exp)
             * succeed and skip Mapping m otherwise
             *
             */

            Mappings nmap = filter(graphNode, exp, env, p, map);
            map = nmap;
        }

        /*
         * Push bindings from map1 into rest when there is at least one variable
         * in-subscope of rest that is always bound in map1 ?x p ?y optional {
         * ?y q ?z } -> values ?y { y1 yn } {?x p ?y optional { ?y q ?z }}
         * optional { ?z r ?t } -> if ?z is not bound in every map1, generate no
         * values
         *
         * set1.getJoinMappings() = Mappings to be considered by rest Either
         * pushed into rest or stored in Memory to be used in eval(rest) eg when
         * rest=service.
         */
        if (map != null && map.isEmpty()) {
            // Every Mapping fail filter, rest() will always fail: skip optional rest()
            map2 = Mappings.create(env.getQuery());
        } else {
            map2 = eval.subEval(p, graphNode, queryNode, rest, exp, map);
        }

        eval.getVisitor().optional(eval, eval.getGraphNode(graphNode), exp, map1, map2);

        MappingSet set = new MappingSet(getQuery(), exp, set1,
                new MappingSet(getQuery(), map2));
        set.start();

        for (Mapping m1 : map1) {
            if (isStop()) {
                return STOP;
            }
            boolean success = false;
            int nbsuc = 0;

            for (Mapping m2 : set.getCandidateMappings(m1)) {
                if (isStop()) {
                    return STOP;
                }
                Mapping merge = m1.merge(m2);
                if (merge != null) {
                    success = filter(env, p, queryNode, graphNode, merge, exp);
                    if (success) {
                        nbsuc++;
                        if (env.push(merge, n)) {
                            backtrack = eval.eval(p, graphNode, stack, n + 1);
                            env.pop(merge);
                            if (backtrack < n) {
                                return backtrack;
                            }
                        }
                    }
                }
            }

            if ((nbsuc == 0) && (env.push(m1, n))) {
                backtrack = eval.eval(p, graphNode, stack, n + 1);
                env.pop(m1);
                if (backtrack < n) {
                    return backtrack;
                }
            }
        }

        return backtrack;
    }

    /**
     * Remove Mapping that fail in-scope filter Use case: BGP1 optional {
     * filter(var) BGP2 } var memberOf in-scope(BGP1, BGP2) Mappings map = eval
     * (BGP1) skip binding Mapping m when it fail filter(var) Remove such
     * Mapping m from map before binding BGP2 with map
     */
    Mappings filter(Node gNode, Exp exp, Environment memory, Producer p, Mappings map) throws SparqlException {
        for (int i = 0; i < map.size(); ) {
            Mapping m = map.get(i);
            m.setQuery(memory.getQuery());
            m.setMap(memory.getMap());
            m.setBind(memory.getBind());
            m.setEval(eval);
            boolean suc = true;

            for (Exp ft : exp.getInscopeFilter()) {
                boolean b = eval.test(gNode, ft.getFilter(), m, p);
                if (!b) {
                    suc = false;
                    break;
                }
            }

            if (suc) {
                i++;
            } else {
                map.remove(i);
            }
        }
        return map;
    }

    /**
     *
     */
    boolean filter(Environment memory, Producer p, Node queryNode, Node gNode, Mapping map, Exp exp) throws SparqlException {
        if (exp.isPostpone()) {
            // A optional B
            // filters of B must be evaluated now
            for (Exp f : exp.getPostpone()) {
                map.setQuery(memory.getQuery());
                map.setMap(memory.getMap());
                map.setBind(memory.getBind());
                map.setEval(eval);
                boolean b = eval.test(gNode, f.getFilter(), map, p);
                if (eval.hasFilter) {
                    b = eval.getVisitor().filter(eval, gNode, f.getFilter().getExp(), b);
                }
                if (!b) {
                    return false;
                }
            }
        }
        return true;
    }

    Query getQuery() {
        return eval.getMemory().getQuery();
    }
}
