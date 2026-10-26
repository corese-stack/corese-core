package fr.inria.corese.core.next.query.impl.engine.eval;

import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.QueryPlan;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.solution.MappingSet;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;
import fr.inria.corese.core.next.query.impl.engine.solution.Memory;

import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;

import static fr.inria.corese.core.next.query.impl.engine.eval.Eval.STOP;

/**
 * Joins intermediate solutions on the evaluation thread. Cancellation may be
 * requested concurrently and is observed through the volatile stop flag.
 *
 * @author corby
 */
final class EvalJoin {

    // Stack backtracking levels cannot reach this value; no continuation ran.
    private static final int NO_CANDIDATE = Integer.MAX_VALUE;

    private final Eval engine;
    private volatile boolean stop;

    EvalJoin(Eval engine) {
        this.engine = engine;
    }

    void setStop() {
        stop = true;
    }

    QueryPlan getQuery() {
        return engine.getMemory().getQuery();
    }

    /**
     * JOIN(e1, e2) Eval e1, engine e2, generate all joins that are compatible in
     * cartesian product
     */
    int eval(Producer p, Node graphNode, Exp exp, Mappings data, Stack stack, int n) {
        int backtrack = n - 1;
        Memory env = engine.getMemory();
        Mappings map1 = engine.subEval(p, graphNode, graphNode, exp.first(), exp, data);
        if (map1.isEmpty()) {
            engine.getVisitor().join(engine, engine.getGraphNode(graphNode), exp, map1, map1);
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

        MappingSet set1 = new MappingSet(map1Extended);
        Mappings joinMappings = null;
        if (engine.isJoinMappings()) {
            joinMappings = set1.prepareMappingsRest(exp.rest());
        }
        Mappings map2 = engine.subEval(p, graphNode, graphNode, exp.rest(), exp, joinMappings);

        engine.getVisitor().join(engine, engine.getGraphNode(graphNode), exp, map1, map2);

        if (map2.isEmpty()) {
            return backtrack;
        }

        return join(p, graphNode, stack, env, map1, map2, n);
    }


    int join(Producer p, Node graphNode, Stack stack, Memory env, Mappings map1, Mappings map2, int n) {
        Node commonVariable = map1.getCommonNode(map2);
        if (commonVariable == null) {
            return joinWithoutCommonVariable(p, graphNode, stack, env, map1, map2, n);
        } else {
            return joinWithCommonVariable(commonVariable, p, graphNode, stack, env, map1, map2, n);
        }
    }

    /**
     * Sorts the smaller join input by a shared variable and enumerates compatible
     * pairs. Unbound values remain compatible with every value on the other side.
     */
    @SuppressWarnings("java:S107") // The shared variable accompanies the existing evaluation frame.
    int joinWithCommonVariable(Node commonVariable, Producer producer, Node graphNode, Stack stack,
                               Memory environment, Mappings left, Mappings right, int level) {
        if (left.size() > right.size()) {
            Mappings temporary = left;
            left = right;
            right = temporary;
        }
        right.setEval(engine);
        right.sort(commonVariable);
        int backtrack = level - 1;
        for (Mapping candidate : left) {
            if (stop) {
                return STOP;
            }
            Node value = candidate.getNode(commonVariable);
            if (!environment.push(candidate, level)) {
                continue;
            }
            int continuation = value == null
                    ? joinAllCandidates(producer, graphNode, stack, environment, right, level)
                    : joinCandidatesForBinding(value, commonVariable, producer, graphNode, stack, right, level);
            if (continuation != NO_CANDIDATE) {
                backtrack = continuation;
                if (backtrack < level) {
                    return backtrack;
                }
            }
            environment.pop(candidate);
        }
        return backtrack;
    }

    /** Enumerates the Cartesian product when the inputs have no shared variable. */
    int joinWithoutCommonVariable(Producer producer, Node graphNode, Stack stack, Memory environment,
                                  Mappings left, Mappings right, int level) {
        int backtrack = level - 1;
        for (Mapping candidate : left) {
            if (stop) {
                return STOP;
            }
            if (environment.push(candidate, level)) {
                int continuation = joinAllCandidates(producer, graphNode, stack, environment, right, level);
                if (continuation != NO_CANDIDATE) {
                    backtrack = continuation;
                    if (backtrack < level) {
                        return backtrack;
                    }
                }
                environment.pop(candidate);
            }
        }
        return backtrack;
    }

    private int joinAllCandidates(Producer producer, Node graphNode, Stack stack, Memory environment,
                                  Mappings candidates, int level) {
        int backtrack = NO_CANDIDATE;
        for (Mapping candidate : candidates) {
            if (stop) {
                return STOP;
            }
            if (environment.push(candidate, level)) {
                backtrack = evaluateCandidate(producer, graphNode, stack, environment, candidate, level);
                if (backtrack < level) {
                    return backtrack;
                }
            }
        }
        return backtrack;
    }

    private int joinCandidatesForBinding(Node value, Node variable, Producer producer, Node graphNode,
                                          Stack stack, Mappings candidates, int level) {
        int unbound = joinUnboundCandidates(variable, producer, graphNode, stack, candidates, level);
        if (unbound != NO_CANDIDATE && unbound < level) {
            return unbound;
        }
        int bound = joinBoundCandidates(value, variable, producer, graphNode, stack, candidates, level);
        return bound == NO_CANDIDATE ? unbound : bound;
    }

    private int joinUnboundCandidates(Node variable, Producer producer, Node graphNode, Stack stack,
                                      Mappings candidates, int level) {
        Memory environment = engine.getMemory();
        int backtrack = NO_CANDIDATE;
        for (Mapping candidate : candidates) {
            if (stop) {
                return STOP;
            }
            if (candidate.getNode(variable) != null) {
                break;
            }
            if (environment.push(candidate, level)) {
                backtrack = evaluateCandidate(producer, graphNode, stack, environment, candidate, level);
                if (backtrack < level) {
                    return backtrack;
                }
            }
        }
        return backtrack;
    }

    private int joinBoundCandidates(Node value, Node variable, Producer producer, Node graphNode,
                                    Stack stack, Mappings candidates, int level) {
        Memory environment = engine.getMemory();
        int backtrack = NO_CANDIDATE;
        int start = candidates.find(value, variable);
        if (start < 0) {
            return backtrack;
        }
        for (int index = start; index < candidates.size(); index++) {
            Mapping candidate = candidates.get(index);
            Node candidateValue = candidate.getNode(variable);
            if (candidateValue == null || !value.match(candidateValue)) {
                break;
            }
            if (environment.push(candidate, level)) {
                backtrack = evaluateCandidate(producer, graphNode, stack, environment, candidate, level);
                if (backtrack < level) {
                    return backtrack;
                }
            }
        }
        return backtrack;
    }

    private int evaluateCandidate(Producer producer, Node graphNode, Stack stack, Memory environment,
                                  Mapping candidate, int level) {
        int backtrack = engine.eval(producer, graphNode, stack, level + 1);
        environment.pop(candidate);
        return backtrack;
    }

    // use case: join(and(values ?s { <uri> }), service ?s { })
    boolean isFirstWithValuesOnly(Exp exp) {
        Exp fst = exp.first();
        return fst.isBGPAnd() && fst.size() == 1 && fst.get(0).isValues();
    }

}
