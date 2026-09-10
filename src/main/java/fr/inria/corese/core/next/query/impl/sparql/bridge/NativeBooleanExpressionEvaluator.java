package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.query.api.exception.QueryTypeErrorException;
import fr.inria.corese.core.next.query.api.exception.UnsupportedQueryFeatureException;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.AndAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.BinaryConstraintAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.BinaryRegexAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.BooleanExpressionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.BooleanNotAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.BoundAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.ContainsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.DifferentAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.EqualsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.ExistsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.GreaterOrEqualThanAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.GreaterThanAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.InAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.IsBlankAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.IsIriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.IsLiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.IsNumericAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.LangMatchesAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.LowerOrEqualThanAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.LowerThanAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.NotExistsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.NotInAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.OrAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.SameTermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.StrEndsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.StrStartsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.TrinaryRegexAst;

import java.util.List;

/** Evaluates boolean expressions using the SPARQL error truth tables. */
final class NativeBooleanExpressionEvaluator {

    private NativeBooleanExpressionEvaluator() {
    }

    static DatatypeValue evaluate(BooleanExpressionAst expression, NativeEvaluationContext context) {
        boolean result = switch (expression) {
            case AndAst binary -> and(binary, context);
            case OrAst binary -> or(binary, context);
            case BooleanNotAst unary -> !context.effectiveBooleanValue(unary.argument());
            case EqualsAst binary -> left(binary, context).equalsWE(right(binary, context));
            case DifferentAst binary -> !left(binary, context).equalsWE(right(binary, context));
            case LowerThanAst binary -> compare(binary, context) < 0;
            case LowerOrEqualThanAst binary -> compare(binary, context) <= 0;
            case GreaterThanAst binary -> compare(binary, context) > 0;
            case GreaterOrEqualThanAst binary -> compare(binary, context) >= 0;
            case SameTermAst binary -> left(binary, context).sameTerm(right(binary, context));
            case BoundAst bound -> bound.argument() instanceof VarAst(var name)
                    && context.variable(name) != null;
            case IsIriAst unary -> context.required(unary.argument()).isIRI();
            case IsBlankAst unary -> context.required(unary.argument()).isBNode();
            case IsLiteralAst unary -> context.required(unary.argument()).isLiteral();
            case IsNumericAst unary -> NativeNumericExpressionEvaluator.isNumeric(context.required(unary.argument()));
            case StrStartsAst binary -> NativeStringExpressionEvaluator.startsWith(binary, context);
            case StrEndsAst binary -> NativeStringExpressionEvaluator.endsWith(binary, context);
            case ContainsAst binary -> NativeStringExpressionEvaluator.contains(binary, context);
            case LangMatchesAst binary -> languageMatches(
                    context.simpleString(binary.getLeftArgument()).getLabel(),
                    context.simpleString(binary.getRightArgument()).getLabel());
            case BinaryRegexAst regex -> NativeStringExpressionEvaluator.regex(regex, context);
            case TrinaryRegexAst regex -> NativeStringExpressionEvaluator.regex(regex, context);
            case InAst(var left, var candidates) -> in(context.required(left), candidates, context);
            case NotInAst(var left, var candidates) -> !in(context.required(left), candidates, context);
            case ExistsAst(var pattern) -> context.exists(pattern);
            case NotExistsAst(var pattern) -> !context.exists(pattern);
            default -> throw unsupported(expression);
        };
        return context.values().createLiteral(result);
    }

    private static boolean and(BinaryConstraintAst expression, NativeEvaluationContext context) {
        BooleanResult left = booleanResult(expression.getLeftArgument(), context);
        if (left.isFalse()) {
            return false;
        }
        BooleanResult right = booleanResult(expression.getRightArgument(), context);
        if (right.isFalse()) {
            return false;
        }
        left.throwFailure();
        right.throwFailure();
        return true;
    }

    private static boolean or(BinaryConstraintAst expression, NativeEvaluationContext context) {
        BooleanResult left = booleanResult(expression.getLeftArgument(), context);
        if (left.isTrue()) {
            return true;
        }
        BooleanResult right = booleanResult(expression.getRightArgument(), context);
        if (right.isTrue()) {
            return true;
        }
        left.throwFailure();
        right.throwFailure();
        return false;
    }

    private static BooleanResult booleanResult(TermAst expression, NativeEvaluationContext context) {
        try {
            return BooleanResult.value(context.effectiveBooleanValue(expression));
        } catch (QueryTypeErrorException failure) {
            return BooleanResult.failure(failure);
        }
    }

    private static boolean in(
            DatatypeValue left,
            List<TermAst> candidates,
            NativeEvaluationContext context) {
        QueryTypeErrorException failure = null;
        for (TermAst candidate : candidates) {
            try {
                DatatypeValue right = context.required(candidate);
                if (left.equalsWE(right)) {
                    return true;
                }
            } catch (QueryTypeErrorException candidateFailure) {
                failure = candidateFailure;
            }
        }
        if (failure != null) {
            throw failure;
        }
        return false;
    }

    private static DatatypeValue left(BinaryConstraintAst expression, NativeEvaluationContext context) {
        return context.required(expression.getLeftArgument());
    }

    private static DatatypeValue right(BinaryConstraintAst expression, NativeEvaluationContext context) {
        return context.required(expression.getRightArgument());
    }

    private static int compare(BinaryConstraintAst expression, NativeEvaluationContext context) {
        return NativeValueComparison.compare(left(expression, context), right(expression, context));
    }

    private static boolean languageMatches(String language, String range) {
        if ("*".equals(range)) {
            return !language.isEmpty();
        }
        String normalizedLanguage = language.toLowerCase(java.util.Locale.ROOT);
        String normalizedRange = range.toLowerCase(java.util.Locale.ROOT);
        return normalizedLanguage.equals(normalizedRange)
                || normalizedLanguage.startsWith(normalizedRange + "-");
    }

    private static UnsupportedQueryFeatureException unsupported(BooleanExpressionAst expression) {
        return new UnsupportedQueryFeatureException(
                "Boolean expression is not supported yet: " + expression.getClass().getSimpleName());
    }

    private record BooleanResult(Boolean value, QueryTypeErrorException failure) {

        static BooleanResult value(boolean value) {
            return new BooleanResult(value, null);
        }

        static BooleanResult failure(QueryTypeErrorException failure) {
            return new BooleanResult(null, failure);
        }

        boolean isTrue() {
            return Boolean.TRUE.equals(value);
        }

        boolean isFalse() {
            return Boolean.FALSE.equals(value);
        }

        void throwFailure() {
            if (failure != null) {
                throw failure;
            }
        }
    }
}
