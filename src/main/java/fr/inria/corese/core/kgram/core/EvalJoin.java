package fr.inria.corese.core.kgram.core;

import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.api.query.Producer;

import static fr.inria.corese.core.kgram.core.Eval.STOP;

/**
 * @author corby
 */
public class EvalJoin {
    public static boolean SORT_OVERLOAD = true;

    Eval eval;
    boolean stop = false;

    EvalJoin(Eval eval) {
        this.eval = eval;
    }

    void setStop(boolean b) {
        stop = b;
    }

    Query getQuery() {
        return eval.getMemory().getQuery();
    }

    /**
     * JOIN(e1, e2) Eval e1, eval e2, generate all joins that are compatible in
     * cartesian product
     */
    int eval(Producer p, Node graphNode, Exp exp, Mappings data, Stack stack, int n) throws SparqlException {
        int backtrack = n - 1;
        Memory env = eval.getMemory();
        Mappings map1 = eval.subEval(p, graphNode, graphNode, exp.first(), exp, data);
        if (map1.size() == 0) {
            eval.getVisitor().join(eval, eval.getGraphNode(graphNode), exp, map1, map1);
            return backtrack;
        }
        Mappings map1Extended = map1;
        if (data != null && isFirstWithValuesOnly(exp)) {
            // use case: join(values ?s { <uri> }, service ?s { })
            // where values specify endpoint URL for service and 
            // previous data contains relevant bindings for service in rest
            // let's give a chance to pass relevant data to rest (service)
            // although there is values in between
            map1Extended = map1.join(data);
        }

        if (stop) {
            return STOP;
        }

        MappingSet set1 = new MappingSet(getQuery(), map1Extended);
        Mappings joinMappings = null;
        if (eval.isJoinMappings()) {
            joinMappings = set1.prepareMappingsRest(exp.rest());
        }
        Mappings map2 = eval.subEval(p, graphNode, graphNode, exp.rest(), exp, joinMappings);

        eval.getVisitor().join(eval, eval.getGraphNode(graphNode), exp, map1, map2);

        if (map2.size() == 0) {
            return backtrack;
        }

        return join(p, graphNode, stack, env, map1, map2, n);
    }


    int join(Producer p, Node graphNode, Stack stack, Memory env, Mappings map1, Mappings map2, int n) throws SparqlException {
        Node commonVariable = map1.getCommonNode(map2);
        if (commonVariable == null) {
            return joinWithoutCommonVariable(p, graphNode, stack, env, map1, map2, n);
        } else {
            return joinWithCommonVariable(commonVariable, p, graphNode, stack, env, map1, map2, n);
        }
    }

    /**
     * Try to find common variable var in both mappings
     * if any: sort second map wrt var
     * iterate first map, find occurrence of same value of var by dichotomy in second map
     * map1 and map2 share commonVariable
     * sort map2 on commonVariable
     * enumerate map1
     * retrieve the index of value of commonVariable in map2 by dichotomy
     */
    int joinWithCommonVariable(Node commonVariable, Producer p, Node graphNode, Stack stack, Memory env, Mappings map1, Mappings map2, int n) throws SparqlException {
        int backtrack = n - 1;
        if (map1.size() > map2.size()) {
            Mappings tmp = map1;
            map1 = map2;
            map2 = tmp;
        }
        if (SORT_OVERLOAD) {
            // setEval enable node comparison overload by Visitor compare() for extended datatypes
            map2.setEval(eval);
        }
        map2.sort(commonVariable);

        for (Mapping m1 : map1) {
            if (stop) {
                return STOP;
            }

            Node n1 = m1.getNode(commonVariable);
            if (env.push(m1, n)) {

                if (n1 == null) {
                    // enumerate all map2
                    for (Mapping m2 : map2) {
                        if (stop) {
                            return STOP;
                        }
                        if (env.push(m2, n)) {
                            backtrack = eval.eval(p, graphNode, stack, n + 1);
                            env.pop(m2);
                            if (backtrack < n) {
                                return backtrack;
                            }
                        }
                    }
                } else {
                    // first, try : n2 == null
                    for (Mapping m2 : map2) {
                        if (stop) {
                            return STOP;
                        }
                        Node n2 = m2.getNode(commonVariable);
                        if (n2 != null) {
                            break;
                        }
                        if (env.push(m2, n)) {
                            backtrack = eval.eval(p, graphNode, stack, n + 1);
                            env.pop(m2);
                            if (backtrack < n) {
                                return backtrack;
                            }
                        }
                    }

                    // second, try : n2 != null
                    int nn = map2.find(n1, commonVariable);
                    if (nn >= 0 && nn < map2.size()) {

                        for (int i = nn; i < map2.size(); i++) {
                            // get value of var in map2
                            Mapping m2 = map2.get(i);
                            Node n2 = m2.getNode(commonVariable);

                            if (n2 == null || !n1.match(n2)) {
                                // map2 is sorted, if n1 != n2 we can exit the loop
                                break;
                            } else if (env.push(m2, n)) {
                                backtrack = eval.eval(p, graphNode, stack, n + 1);
                                env.pop(m2);
                                if (backtrack < n) {
                                    return backtrack;
                                }
                            }
                        }
                    }
                }
                env.pop(m1);
            }
        }
        return backtrack;
    }

    /**
     * No variable in common: cartesian product of mappings
     */
    int joinWithoutCommonVariable(Producer p, Node graphNode, Stack stack, Memory env, Mappings map1, Mappings map2, int n) throws SparqlException {
        int backtrack = n - 1;
        for (Mapping m1 : map1) {
            if (stop) {
                return STOP;
            }
            if (env.push(m1, n)) {

                for (Mapping m2 : map2) {
                    if (stop) {
                        return STOP;
                    }
                    if (env.push(m2, n)) {
                        backtrack = eval.eval(p, graphNode, stack, n + 1);
                        env.pop(m2);
                        if (backtrack < n) {
                            return backtrack;
                        }
                    }
                }

                env.pop(m1);
            }
        }
        return backtrack;
    }


    // use case: join(and(values ?s { <uri> }), service ?s { })
    boolean isFirstWithValuesOnly(Exp exp) {
        Exp fst = exp.first();
        return fst.isBGPAnd() && fst.size() == 1 && fst.get(0).isValues();
    }

}
