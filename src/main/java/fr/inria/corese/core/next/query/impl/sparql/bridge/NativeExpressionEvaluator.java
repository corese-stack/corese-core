package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.UnsupportedQueryFeatureException;
import fr.inria.corese.core.next.query.impl.kgram.api.query.Environment;
import fr.inria.corese.core.next.query.impl.kgram.api.query.Evaluator;
import fr.inria.corese.core.next.query.impl.kgram.api.query.Producer;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.BnodeAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.BooleanExpressionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.CoalesceAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.FunctionCallAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.IfAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.IriExpressionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.LiteralExpressionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.NumericExpressionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.XsdDateTimeExpressionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.XsdDayTimeDurationExpressionAst;

import java.util.List;
import java.util.Objects;

/** Dispatches Corese-next AST expressions to small native evaluators. */
final class NativeExpressionEvaluator {

    private NativeExpressionEvaluator() {
    }

    static DatatypeValue evaluate(
            TermAst expression,
            Evaluator evaluator,
            Environment environment,
            Producer producer,
            WhereCompiler whereCompiler) {
        Objects.requireNonNull(expression, "expression");
        NativeEvaluationContext context = new NativeEvaluationContext(
                evaluator, environment, producer, whereCompiler);
        return context.evaluate(expression);
    }

    static DatatypeValue evaluateExpression(
            TermAst expression,
            NativeEvaluationContext context) {
        return switch (expression) {
            case VarAst(var name) -> context.variable(name);
            case IriAst ignored -> context.constant(expression);
            case LiteralAst ignored -> context.constant(expression);
            case BooleanExpressionAst booleanExpression ->
                    NativeBooleanExpressionEvaluator.evaluate(booleanExpression, context);
            case NumericExpressionAst numericExpression ->
                    NativeNumericExpressionEvaluator.evaluate(numericExpression, context);
            case IriExpressionAst iriExpression ->
                    NativeIriExpressionEvaluator.evaluate(iriExpression, context);
            case XsdDateTimeExpressionAst dateTimeExpression ->
                    NativeTemporalExpressionEvaluator.evaluateDateTime(dateTimeExpression, context);
            case XsdDayTimeDurationExpressionAst durationExpression ->
                    NativeTemporalExpressionEvaluator.evaluateDuration(durationExpression, context);
            case LiteralExpressionAst literalExpression ->
                    NativeStringExpressionEvaluator.evaluate(literalExpression, context);
            case CoalesceAst coalesce -> coalesce(coalesce.arguments(), context);
            case IfAst(var condition, var thenExpr, var elseExpr) -> context.evaluate(
                    context.effectiveBooleanValue(condition) ? thenExpr : elseExpr);
            case BnodeAst bnode -> bnode.getLabel() == null
                    ? context.values().createBNode()
                    : context.values().createBNode(context.required(bnode.getLabel()).stringValue());
            case FunctionCallAst function -> throw new UnsupportedQueryFeatureException(
                    "Extension function evaluation is not supported yet: " + function.getName());
            default -> throw new UnsupportedQueryFeatureException(
                    "Expression is not supported yet by the native evaluator: "
                            + expression.getClass().getSimpleName());
        };
    }

    private static DatatypeValue coalesce(
            List<TermAst> arguments,
            NativeEvaluationContext context) {
        QueryEvaluationException lastFailure = null;
        for (TermAst argument : arguments) {
            try {
                DatatypeValue value = context.evaluate(argument);
                if (value != null) {
                    return value;
                }
            } catch (QueryEvaluationException failure) {
                lastFailure = failure;
            }
        }
        throw new QueryEvaluationException(
                "COALESCE has no bound, successfully evaluated argument",
                lastFailure);
    }
}
