package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.query.api.exception.QueryTypeErrorException;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.spi.Environment;
import fr.inria.corese.core.next.query.impl.sparql.ast.AggregateAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Evaluates SPARQL 1.1 aggregate functions over grouped or whole-query solution mappings.
 */
final class NativeAggregateEvaluator {

    private NativeAggregateEvaluator() {
    }

    static DatatypeValue evaluate(
            AggregateAst aggregate,
            NativeEvaluationContext context) {
        Objects.requireNonNull(aggregate, "aggregate");
        Objects.requireNonNull(context, "context");

        Iterable<Mapping> mappings = context.environment() != null
                ? context.environment().getAggregate()
                : List.of();
        if (mappings == null) {
            mappings = List.of();
        }

        Environment prevEnv = context.environment();
        try {
            return switch (aggregate.function()) {
                case COUNT -> evaluateCount(aggregate, mappings, context);
                case SUM -> evaluateSum(aggregate, mappings, context);
                case AVG -> evaluateAvg(aggregate, mappings, context);
                case MIN -> evaluateMinMax(aggregate, mappings, context, true);
                case MAX -> evaluateMinMax(aggregate, mappings, context, false);
                case SAMPLE -> evaluateSample(aggregate, mappings, context);
                case GROUP_CONCAT -> evaluateGroupConcat(aggregate, mappings, context);
            };
        } finally {
            context.setEnvironment(prevEnv);
        }
    }

    private static DatatypeValue evaluateCount(
            AggregateAst aggregate,
            Iterable<Mapping> mappings,
            NativeEvaluationContext context) {
        TermAst expression = aggregate.expression();
        if (expression == null) {
            return evaluateCountWildcard(aggregate.distinct(), mappings, context);
        }

        long count = 0;
        Set<DatatypeValue> seen = aggregate.distinct() ? new LinkedHashSet<>() : null;
        for (Mapping m : mappings) {
            context.setEnvironment(m);
            try {
                DatatypeValue val = context.evaluate(expression);
                if (val != null && (seen == null || seen.add(val))) {
                    count++;
                }
            } catch (QueryTypeErrorException ignored) {
                // SPARQL 1.1: evaluation errors in COUNT are ignored
            }
        }
        return context.values().createLiteral(BigInteger.valueOf(count));
    }

    private static DatatypeValue evaluateCountWildcard(
            boolean distinct,
            Iterable<Mapping> mappings,
            NativeEvaluationContext context) {
        long count = 0;
        if (distinct) {
            Set<Mapping> distinctMappings = new LinkedHashSet<>();
            for (Mapping m : mappings) {
                if (distinctMappings.add(m)) {
                    count++;
                }
            }
        } else if (mappings instanceof Collection<?> col) {
            count = col.size();
        } else {
            Iterator<Mapping> it = mappings.iterator();
            while (it.hasNext()) {
                it.next();
                count++;
            }
        }
        return context.values().createLiteral(BigInteger.valueOf(count));
    }

    private static DatatypeValue evaluateSum(
            AggregateAst aggregate,
            Iterable<Mapping> mappings,
            NativeEvaluationContext context) {
        NumericAccumulator accumulator = new NumericAccumulator();
        Set<DatatypeValue> seen = aggregate.distinct() ? new LinkedHashSet<>() : null;

        for (Mapping m : mappings) {
            context.setEnvironment(m);
            DatatypeValue val = context.evaluate(aggregate.expression());
            if (val == null || (seen != null && !seen.add(val))) {
                continue;
            }
            accumulator.add(val);
        }

        if (!accumulator.hasValues()) {
            return context.values().createLiteral(BigInteger.ZERO);
        }
        return accumulator.toSumLiteral(context);
    }

    private static DatatypeValue evaluateAvg(
            AggregateAst aggregate,
            Iterable<Mapping> mappings,
            NativeEvaluationContext context) {
        boolean hasMappings = false;
        NumericAccumulator accumulator = new NumericAccumulator();
        Set<DatatypeValue> seen = aggregate.distinct() ? new LinkedHashSet<>() : null;

        for (Mapping m : mappings) {
            hasMappings = true;
            context.setEnvironment(m);
            DatatypeValue val = context.evaluate(aggregate.expression());
            if (val == null || (seen != null && !seen.add(val))) {
                continue;
            }
            accumulator.add(val);
        }

        if (!hasMappings) {
            throw new QueryTypeErrorException("AVG is undefined for empty group");
        }
        if (!accumulator.hasValues()) {
            throw new QueryTypeErrorException("AVG is undefined when all values are unbound");
        }

        return accumulator.toAvgLiteral(context);
    }

    private static DatatypeValue evaluateMinMax(
            AggregateAst aggregate,
            Iterable<Mapping> mappings,
            NativeEvaluationContext context,
            boolean isMin) {
        boolean hasMappings = false;
        DatatypeValue best = null;

        for (Mapping m : mappings) {
            hasMappings = true;
            context.setEnvironment(m);
            DatatypeValue val = context.evaluate(aggregate.expression());
            best = updateBestMinMax(best, val, isMin);
        }

        if (!hasMappings) {
            throw new QueryTypeErrorException("MIN/MAX is undefined for empty group");
        }
        if (best == null) {
            throw new QueryTypeErrorException("MIN/MAX is undefined when all values are unbound");
        }
        return best;
    }

    private static DatatypeValue updateBestMinMax(DatatypeValue best, DatatypeValue candidate, boolean isMin) {
        if (candidate == null) {
            return best;
        }
        if (best == null || isBetterMinMax(candidate, best, isMin)) {
            return candidate;
        }
        return best;
    }

    private static boolean isBetterMinMax(DatatypeValue candidate, DatatypeValue current, boolean isMin) {
        int cmp = compareMinMax(candidate, current);
        return isMin ? cmp < 0 : cmp > 0;
    }

    private static int compareMinMax(DatatypeValue left, DatatypeValue right) {
        int rankLeft = termRank(left);
        int rankRight = termRank(right);
        if (rankLeft != rankRight) {
            return Integer.compare(rankLeft, rankRight);
        }
        if (left.isBNode()) {
            return left.getLabel().compareTo(right.getLabel());
        }
        if (left.isIRI()) {
            return left.stringValue().compareTo(right.stringValue());
        }
        return NativeValueComparison.compare(left, right);
    }

    private static int termRank(DatatypeValue val) {
        if (val.isBNode()) {
            return 0;
        }
        if (val.isIRI()) {
            return 1;
        }
        if (val instanceof Literal) {
            return 2;
        }
        return 3;
    }

    private static DatatypeValue evaluateSample(
            AggregateAst aggregate,
            Iterable<Mapping> mappings,
            NativeEvaluationContext context) {
        boolean hasMappings = false;
        for (Mapping m : mappings) {
            hasMappings = true;
            context.setEnvironment(m);
            try {
                DatatypeValue val = context.evaluate(aggregate.expression());
                if (val != null) {
                    return val;
                }
            } catch (QueryTypeErrorException ignored) {
                // Continue searching for a bound value in the group
            }
        }
        if (!hasMappings) {
            throw new QueryTypeErrorException("SAMPLE is undefined for empty group");
        }
        throw new QueryTypeErrorException("SAMPLE is undefined when all values are unbound");
    }

    private static DatatypeValue evaluateGroupConcat(
            AggregateAst aggregate,
            Iterable<Mapping> mappings,
            NativeEvaluationContext context) {
        String separator = unquoteSeparator(aggregate.groupConcatSeparator(), context);
        List<String> parts = new ArrayList<>();
        Set<String> seen = aggregate.distinct() ? new LinkedHashSet<>() : null;

        for (Mapping m : mappings) {
            context.setEnvironment(m);
            DatatypeValue val = context.evaluate(aggregate.expression());
            if (val != null) {
                String s = val.stringValue();
                if (seen == null || seen.add(s)) {
                    parts.add(s);
                }
            }
        }

        return context.values().createLiteral(String.join(separator, parts));
    }

    private static String unquoteSeparator(String rawSeparator, NativeEvaluationContext context) {
        if (rawSeparator == null) {
            return " ";
        }
        return context.termResolver().unquoteLexical(rawSeparator);
    }

    /** Helper class accumulating numeric values across solution mappings. */
    private static final class NumericAccumulator {
        private BigInteger intSum = BigInteger.ZERO;
        private BigDecimal decSum = BigDecimal.ZERO;
        private double doubleSum = 0.0;
        private NativeNumericExpressionEvaluator.NumericKind currentKind =
                NativeNumericExpressionEvaluator.NumericKind.INTEGER;
        private long count = 0;

        void add(DatatypeValue val) {
            NativeNumericExpressionEvaluator.NumericLiteral num =
                    NativeNumericExpressionEvaluator.numericLiteral(val);
            count++;
            NativeNumericExpressionEvaluator.NumericKind previousKind = currentKind;
            currentKind = NativeNumericExpressionEvaluator.NumericKind.promote(
                    currentKind, num.kind(), NativeNumericExpressionEvaluator.Operation.ADD);

            if (currentKind != previousKind) {
                promoteSum(previousKind, currentKind);
            }

            switch (currentKind) {
                case DOUBLE, FLOAT -> doubleSum += num.doubleValue();
                case DECIMAL -> decSum = decSum.add(num.decimalValue());
                case INTEGER -> intSum = intSum.add(num.literal().integerValue());
            }
        }

        private void promoteSum(
                NativeNumericExpressionEvaluator.NumericKind previousKind,
                NativeNumericExpressionEvaluator.NumericKind targetKind) {
            if (previousKind == NativeNumericExpressionEvaluator.NumericKind.INTEGER
                    && targetKind == NativeNumericExpressionEvaluator.NumericKind.DECIMAL) {
                decSum = new BigDecimal(intSum);
                intSum = BigInteger.ZERO;
            } else if (previousKind == NativeNumericExpressionEvaluator.NumericKind.INTEGER) {
                doubleSum = intSum.doubleValue();
                intSum = BigInteger.ZERO;
            } else if (previousKind == NativeNumericExpressionEvaluator.NumericKind.DECIMAL) {
                doubleSum = decSum.doubleValue();
                decSum = BigDecimal.ZERO;
            }
        }

        boolean hasValues() {
            return count > 0;
        }

        DatatypeValue toSumLiteral(NativeEvaluationContext context) {
            return switch (currentKind) {
                case DOUBLE -> context.values().createLiteral(doubleSum);
                case FLOAT -> context.values().createLiteral((float) doubleSum);
                case DECIMAL -> context.values().createLiteral(decSum);
                case INTEGER -> context.values().createLiteral(intSum);
            };
        }

        DatatypeValue toAvgLiteral(NativeEvaluationContext context) {
            return switch (currentKind) {
                case DOUBLE -> context.values().createLiteral(doubleSum / count);
                case FLOAT -> context.values().createLiteral((float) (doubleSum / count));
                case DECIMAL -> context.values().createLiteral(
                        decSum.divide(BigDecimal.valueOf(count), MathContext.DECIMAL128));
                case INTEGER -> context.values().createLiteral(
                        new BigDecimal(intSum).divide(BigDecimal.valueOf(count), MathContext.DECIMAL128));
            };
        }
    }
}
