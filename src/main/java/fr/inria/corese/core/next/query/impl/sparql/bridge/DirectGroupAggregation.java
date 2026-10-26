package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.exception.IncorrectOperationException;
import fr.inria.corese.core.next.query.api.exception.QueryTypeErrorException;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.engine.solution.GroupAggregator;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;
import fr.inria.corese.core.next.query.impl.sparql.ast.AggregateAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.AggregateFunction;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Direct aggregation of simple grouped SELECT projections.
 *
 * <p>Only the first mapping and scalar accumulator state are retained per group.
 * Group keys use the same node order as the engine's ordinary grouping, including
 * unbound keys. A sorted table avoids assuming that arbitrary Node implementations
 * provide hashes consistent with {@link Node#compare(Node)}.
 * This component does not evaluate HAVING, ORDER BY, or emit evaluator callbacks;
 * callers requiring those facilities must use the ordinary aggregation path.</p>
 */
public final class DirectGroupAggregation {

    public static final GroupAggregator INSTANCE = DirectGroupAggregation::tryAggregate;

    static {
        register();
    }

    public static void register() {
        Mappings.setGroupAggregator(INSTANCE);
    }

    private DirectGroupAggregation() {
    }

    /** Returns whether the query can be reduced without evaluating expressions. */
    public static boolean isEligible(Query query) {
        if (!query.isSelect() || (query.isDistinct() && !query.isDistinctRedundantWithGroupBy()) || query.isListGroup()
                || query.getGroupBy().isEmpty()
                || query.getHaving() != null || !query.getOrderBy().isEmpty()) {
            return false;
        }
        if (!query.getGroupBy().stream().allMatch(DirectGroupAggregation::isPlainGroup)) {
            return false;
        }
        return eligibleProjections(query);
    }

    private static boolean isPlainGroup(Exp expression) {
        return expression.getNode() != null && expression.getNode().isVariable()
                && expression.getFilter() == null && !expression.isExpGroupBy();
    }

    private static boolean eligibleProjections(Query query) {
        boolean hasAggregate = false;
        for (Exp projection : query.getSelectFun()) {
            if (projection.isExpGroupBy() || projection.getNode() == null) {
                return false;
            }
            if (projection.isAggregate()) {
                if (simpleAggregate(projection) == null) {
                    return false;
                }
                hasAggregate = true;
            } else if (projection.getFilter() != null) {
                return false;
            }
        }
        return hasAggregate;
    }

    private static AggregateAst simpleAggregate(Exp projection) {
        if (projection.getFilter() == null
                || !(projection.getFilter().getExp() instanceof AstBackedExpr expression)
                || !(expression.sourceAst().orElse(null) instanceof AggregateAst aggregate)
                || aggregate.distinct()) {
            return null;
        }
        boolean supported = switch (aggregate.function()) {
            case COUNT, SUM, AVG, MIN, MAX -> true;
            default -> false;
        };
        boolean simpleArgument = aggregate.expression() instanceof VarAst
                || aggregate.function() == AggregateFunction.COUNT && aggregate.expression() == null;
        return supported && simpleArgument ? aggregate : null;
    }

    /**
     * Replaces input mappings by their aggregated representatives.
     *
     * @return false when ineligible, leaving input mappings untouched; true after
     *         successful reduction, including an empty grouped input
     */
    public static boolean tryAggregate(Query query, Mappings mappings) {
        if (!isEligible(query) || mappings.isFake() || mappings.getGroup() != null) {
            return false;
        }
        List<Projection> projections = projections(query);
        Node[] groupNodes = query.getGroupBy().stream().map(Exp::getNode).toArray(Node[]::new);
        TreeMap<GroupKey, GroupState> groups = new TreeMap<>(GroupKey::compareTo);
        for (Mapping mapping : mappings) {
            GroupKey key = GroupKey.from(mapping, groupNodes);
            groups.computeIfAbsent(key, ignored -> new GroupState(mapping, projections)).add(mapping);
        }
        List<Mapping> representatives = new ArrayList<>(groups.size());
        for (GroupState state : groups.values()) {
            representatives.add(state.finish(projections));
        }
        mappings.getList().clear();
        mappings.getList().addAll(representatives);
        return true;
    }

    private static List<Projection> projections(Query query) {
        List<Projection> projections = new ArrayList<>();
        for (Exp expression : query.getSelectFun()) {
            if (expression.isAggregate()) {
                AggregateAst aggregate = simpleAggregate(expression);
                String variable = aggregate.expression() instanceof VarAst varAst ? varAst.name() : null;
                projections.add(new Projection(expression.getNode(), variable, aggregate.function()));
            }
        }
        return projections;
    }

    private record Projection(Node target, String variable, AggregateFunction function) {
    }

    private static final class GroupKey {
        private final Node[] values;

        private GroupKey(Node[] values) {
            this.values = values;
        }

        static GroupKey from(Mapping mapping, Node[] variables) {
            Node[] values = new Node[variables.length];
            Node[] prepared = mapping.getGroupByNodes();
            for (int i = 0; i < variables.length; i++) {
                values[i] = prepared == null || prepared.length == 0
                        ? mapping.getNode(variables[i]) : prepared[i];
            }
            return new GroupKey(values);
        }

        int compareTo(GroupKey other) {
            for (int i = 0; i < values.length; i++) {
                int comparison = compare(values[i], other.values[i]);
                if (comparison != 0) {
                    return comparison;
                }
            }
            return 0;
        }

        private static int compare(Node left, Node right) {
            if (left == right) {
                return 0;
            }
            if (left == null) {
                return -1;
            }
            return right == null ? 1 : left.compare(right);
        }
    }

    private static final class GroupState {
        private final Mapping representative;
        private final Accumulator[] accumulators;

        GroupState(Mapping mapping, List<Projection> projections) {
            representative = mapping;
            accumulators = projections.stream().map(Accumulator::new)
                    .toArray(Accumulator[]::new);
        }

        void add(Mapping mapping) {
            for (int i = 0; i < accumulators.length; i++) {
                accumulators[i].add(mapping);
            }
        }

        Mapping finish(List<Projection> projections) {
            for (int i = 0; i < accumulators.length; i++) {
                representative.setNode(projections.get(i).target(), accumulators[i].result());
            }
            return representative;
        }
    }

    private static final class Accumulator {
        private final AggregateFunction function;
        private final String variable;
        private final NativeAggregateEvaluator.NumericAccumulator numeric;
        private long count;
        private Node best;
        private boolean error;

        Accumulator(Projection projection) {
            function = projection.function();
            variable = projection.variable();
            numeric = function == AggregateFunction.SUM || function == AggregateFunction.AVG
                    ? new NativeAggregateEvaluator.NumericAccumulator() : null;
        }

        void add(Mapping mapping) {
            Node value = variable == null ? null : mapping.getNode(variable);
            if (function == AggregateFunction.COUNT) {
                if (variable == null || value != null) {
                    count++;
                }
            } else if (value != null && !error) {
                addValue(value);
            }
        }

        private void addValue(Node value) {
            try {
                switch (function) {
                    case SUM, AVG -> numeric.add(value.getDatatypeValue());
                    case MIN, MAX -> updateBest(value);
                    default -> throw new IllegalStateException("Unsupported aggregate: " + function);
                }
            } catch (QueryTypeErrorException | IncorrectOperationException | ArithmeticException
                    | NumberFormatException e) {
                // A type error invalidates this aggregate, not the other projections.
                error = true;
            }
        }

        private void updateBest(Node value) {
            if (best == null) {
                best = value;
                return;
            }
            int comparison = value.compare(best);
            if (function == AggregateFunction.MIN ? comparison < 0 : comparison > 0) {
                best = value;
            }
        }

        Node result() {
            if (error) {
                return null;
            }
            return switch (function) {
                case COUNT -> NodeImpl.forValue(Values.factory().createLiteral(BigInteger.valueOf(count)));
                case SUM -> NodeImpl.forValue(numeric.toSumLiteral(Values.factory()));
                case AVG -> numeric.hasValues()
                        ? NodeImpl.forValue(numeric.toAvgLiteral(Values.factory())) : null;
                case MIN, MAX -> best;
                default -> throw new IllegalStateException("Unsupported aggregate: " + function);
            };
        }
    }
}
