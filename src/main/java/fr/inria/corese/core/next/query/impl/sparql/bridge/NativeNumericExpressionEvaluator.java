package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.UnsupportedQueryFeatureException;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.AbsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.AddAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.BinaryConstraintAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.CeilAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.DayAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.DivideAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.FloorAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.HoursAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.MinutesAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.MonthAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.MultiplyAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.NumericExpressionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.RandAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.RoundAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.SecondsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.StrLenAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.SubtractAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.UnaryMinusAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.UnaryPlusAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.YearAst;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

/** Evaluates numeric expressions with SPARQL numeric type promotion. */
final class NativeNumericExpressionEvaluator {

    private NativeNumericExpressionEvaluator() {
    }

    static DatatypeValue evaluate(
            NumericExpressionAst expression,
            NativeEvaluationContext context) {
        return switch (expression) {
            case AddAst binary -> arithmetic(binary, context, Operation.ADD);
            case SubtractAst binary -> arithmetic(binary, context, Operation.SUBTRACT);
            case MultiplyAst binary -> arithmetic(binary, context, Operation.MULTIPLY);
            case DivideAst binary -> arithmetic(binary, context, Operation.DIVIDE);
            case UnaryPlusAst unary -> numericLiteral(context.required(unary.argument())).literal();
            case UnaryMinusAst unary -> unaryMinus(
                    numericLiteral(context.required(unary.argument())), context);
            case AbsAst unary -> absolute(
                    numericLiteral(context.required(unary.argument())), context);
            case CeilAst unary -> rounded(
                    numericLiteral(context.required(unary.argument())), context, Rounding.CEILING);
            case FloorAst unary -> rounded(
                    numericLiteral(context.required(unary.argument())), context, Rounding.FLOOR);
            case RoundAst unary -> rounded(
                    numericLiteral(context.required(unary.argument())), context, Rounding.NEAREST);
            case StrLenAst unary -> stringLength(unary, context);
            case RandAst ignored -> context.values().createLiteral(
                    ThreadLocalRandom.current().nextDouble());
            case YearAst unary -> context.values().createLiteral(calendar(unary.argument(), context).getYear());
            case MonthAst unary -> context.values().createLiteral(calendar(unary.argument(), context).getMonth());
            case DayAst unary -> context.values().createLiteral(calendar(unary.argument(), context).getDay());
            case HoursAst unary -> context.values().createLiteral(calendar(unary.argument(), context).getHour());
            case MinutesAst unary -> context.values().createLiteral(calendar(unary.argument(), context).getMinute());
            case SecondsAst unary -> seconds(calendar(unary.argument(), context), context);
            default -> throw unsupported(expression);
        };
    }

    static double numericDouble(DatatypeValue value) {
        return numericLiteral(value).literal().doubleValue();
    }

    private static DatatypeValue arithmetic(
            BinaryConstraintAst expression,
            NativeEvaluationContext context,
            Operation operation) {
        NumericLiteral left = numericLiteral(context.required(expression.getLeftArgument()));
        NumericLiteral right = numericLiteral(context.required(expression.getRightArgument()));
        NumericKind resultKind = NumericKind.promote(left.kind(), right.kind(), operation);
        return switch (resultKind) {
            case DOUBLE -> context.values().createLiteral(
                    doubleOperation(left.doubleValue(), right.doubleValue(), operation));
            case FLOAT -> context.values().createLiteral((float)
                    doubleOperation(left.doubleValue(), right.doubleValue(), operation));
            case DECIMAL -> context.values().createLiteral(
                    decimalOperation(left.decimalValue(), right.decimalValue(), operation));
            case INTEGER -> context.values().createLiteral(switch (operation) {
                case ADD -> left.literal().integerValue().add(right.literal().integerValue());
                case SUBTRACT -> left.literal().integerValue().subtract(right.literal().integerValue());
                case MULTIPLY -> left.literal().integerValue().multiply(right.literal().integerValue());
                case DIVIDE -> throw new IllegalStateException("Integer division must promote to decimal");
            });
        };
    }

    private static double doubleOperation(double left, double right, Operation operation) {
        return switch (operation) {
            case ADD -> left + right;
            case SUBTRACT -> left - right;
            case MULTIPLY -> left * right;
            case DIVIDE -> left / right;
        };
    }

    private static BigDecimal decimalOperation(
            BigDecimal left,
            BigDecimal right,
            Operation operation) {
        return switch (operation) {
            case ADD -> left.add(right);
            case SUBTRACT -> left.subtract(right);
            case MULTIPLY -> left.multiply(right);
            case DIVIDE -> left.divide(right, MathContext.DECIMAL128);
        };
    }

    private static DatatypeValue unaryMinus(
            NumericLiteral value,
            NativeEvaluationContext context) {
        return switch (value.kind()) {
            case DOUBLE -> context.values().createLiteral(-value.literal().doubleValue());
            case FLOAT -> context.values().createLiteral(-value.literal().floatValue());
            case DECIMAL -> context.values().createLiteral(value.decimalValue().negate());
            case INTEGER -> context.values().createLiteral(value.literal().integerValue().negate());
        };
    }

    private static DatatypeValue absolute(
            NumericLiteral value,
            NativeEvaluationContext context) {
        return switch (value.kind()) {
            case DOUBLE -> context.values().createLiteral(Math.abs(value.literal().doubleValue()));
            case FLOAT -> context.values().createLiteral(Math.abs(value.literal().floatValue()));
            case DECIMAL -> context.values().createLiteral(value.decimalValue().abs());
            case INTEGER -> context.values().createLiteral(value.literal().integerValue().abs());
        };
    }

    private static DatatypeValue rounded(
            NumericLiteral value,
            NativeEvaluationContext context,
            Rounding rounding) {
        return switch (value.kind()) {
            case DOUBLE -> context.values().createLiteral(rounding.apply(value.literal().doubleValue()));
            case FLOAT -> context.values().createLiteral((float) rounding.apply(value.literal().floatValue()));
            case DECIMAL -> context.values().createLiteral(
                    rounding.apply(value.decimalValue()));
            case INTEGER -> value.literal();
        };
    }

    private static DatatypeValue stringLength(StrLenAst expression, NativeEvaluationContext context) {
        String text = context.stringLiteral(expression.argument()).getLabel();
        return context.values().createLiteral(text.codePointCount(0, text.length()));
    }

    private static XMLGregorianCalendar calendar(
            fr.inria.corese.core.next.query.impl.sparql.ast.TermAst expression,
            NativeEvaluationContext context) {
        return NativeTemporalExpressionEvaluator.calendar(expression, context);
    }

    private static DatatypeValue seconds(
            XMLGregorianCalendar calendar,
            NativeEvaluationContext context) {
        BigDecimal seconds = BigDecimal.valueOf(calendar.getSecond());
        if (calendar.getFractionalSecond() != null) {
            seconds = seconds.add(calendar.getFractionalSecond());
        }
        return context.values().createLiteral(seconds);
    }

    private static NumericLiteral numericLiteral(DatatypeValue value) {
        if (!(value instanceof Literal literal) || !literal.isNumber()) {
            throw new QueryEvaluationException("Expected a numeric RDF literal");
        }
        return new NumericLiteral(literal, NumericKind.of(literal));
    }

    private static UnsupportedQueryFeatureException unsupported(NumericExpressionAst expression) {
        return new UnsupportedQueryFeatureException(
                "Numeric expression is not supported yet: " + expression.getClass().getSimpleName());
    }

    private enum Operation {
        ADD,
        SUBTRACT,
        MULTIPLY,
        DIVIDE
    }

    private enum NumericKind {
        INTEGER,
        DECIMAL,
        FLOAT,
        DOUBLE;

        static NumericKind of(Literal literal) {
            if (!(literal.getCoreDatatype() instanceof XSDDatatype datatype)) {
                throw new QueryEvaluationException("Expected an XML Schema numeric datatype");
            }
            return switch (datatype) {
                case DOUBLE -> DOUBLE;
                case FLOAT -> FLOAT;
                case DECIMAL -> DECIMAL;
                default -> INTEGER;
            };
        }

        static NumericKind promote(NumericKind left, NumericKind right, Operation operation) {
            if (left == DOUBLE || right == DOUBLE) {
                return DOUBLE;
            }
            if (left == FLOAT || right == FLOAT) {
                return FLOAT;
            }
            if (left == DECIMAL || right == DECIMAL || operation == Operation.DIVIDE) {
                return DECIMAL;
            }
            return INTEGER;
        }
    }

    private record NumericLiteral(Literal literal, NumericKind kind) {

        BigDecimal decimalValue() {
            return kind == NumericKind.INTEGER
                    ? new BigDecimal(literal.integerValue())
                    : literal.decimalValue();
        }

        double doubleValue() {
            return literal.doubleValue();
        }
    }

    private enum Rounding {
        CEILING {
            @Override
            double apply(double value) {
                return Math.ceil(value);
            }

            @Override
            BigDecimal apply(BigDecimal value) {
                return value.setScale(0, RoundingMode.CEILING);
            }
        },
        FLOOR {
            @Override
            double apply(double value) {
                return Math.floor(value);
            }

            @Override
            BigDecimal apply(BigDecimal value) {
                return value.setScale(0, RoundingMode.FLOOR);
            }
        },
        NEAREST {
            @Override
            double apply(double value) {
                return Math.floor(value + 0.5d);
            }

            @Override
            BigDecimal apply(BigDecimal value) {
                BigDecimal floor = value.setScale(0, RoundingMode.FLOOR);
                return value.subtract(floor).compareTo(HALF) >= 0
                        ? floor.add(BigDecimal.ONE)
                        : floor;
            }
        };

        private static final BigDecimal HALF = new BigDecimal("0.5");

        abstract double apply(double value);

        abstract BigDecimal apply(BigDecimal value);
    }
}
