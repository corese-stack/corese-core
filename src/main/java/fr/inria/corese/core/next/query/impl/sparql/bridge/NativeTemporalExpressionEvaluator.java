package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.data.api.literal.XSDDatatype;

import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.vocabulary.XSD;
import fr.inria.corese.core.next.query.api.exception.QueryTypeErrorException;
import fr.inria.corese.core.next.query.api.exception.UnsupportedQueryFeatureException;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.NowAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.TimezoneAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.XsdDateTimeExpressionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.XsdDayTimeDurationExpressionAst;

import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.YearAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.MonthAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.DayAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.HoursAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.MinutesAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.SecondsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.NumericExpressionAst;
import java.math.BigDecimal;
import java.math.BigInteger;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

/** Evaluates date-time and timezone expressions. */
final class NativeTemporalExpressionEvaluator {

    private NativeTemporalExpressionEvaluator() {
    }

    /**
     * Extracts a date-time component as an integer or decimal for seconds.
     *
     * @param expression component extraction expression
     * @param context current evaluation context
     * @return the requested component with its SPARQL numeric datatype
     * @throws QueryTypeErrorException if the operand is not a date-time literal
     * @throws UnsupportedQueryFeatureException if the component is unsupported
     */
    static DatatypeValue evaluateComponent(
            NumericExpressionAst expression, NativeEvaluationContext context) {
        return switch (expression) {
            case YearAst unary -> context.values().createLiteral(
                    BigInteger.valueOf(calendar(unary.argument(), context).getYear()));
            case MonthAst unary -> context.values().createLiteral(
                    BigInteger.valueOf(calendar(unary.argument(), context).getMonth()));
            case DayAst unary -> context.values().createLiteral(
                    BigInteger.valueOf(calendar(unary.argument(), context).getDay()));
            case HoursAst unary -> context.values().createLiteral(
                    BigInteger.valueOf(calendar(unary.argument(), context).getHour()));
            case MinutesAst unary -> context.values().createLiteral(
                    BigInteger.valueOf(calendar(unary.argument(), context).getMinute()));
            case SecondsAst unary -> seconds(calendar(unary.argument(), context), context);
            default -> throw new UnsupportedQueryFeatureException(
                    "Temporal component is not supported: " + expression.getClass().getSimpleName());
        };
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

    static DatatypeValue evaluateDateTime(
            XsdDateTimeExpressionAst expression,
            NativeEvaluationContext context) {
        if (expression instanceof NowAst) {
            return context.values().createLiteral(context.queryEvaluationTime());
        }
        throw new UnsupportedQueryFeatureException(
                "Date-time expression is not supported yet: "
                        + expression.getClass().getSimpleName());
    }

    static DatatypeValue evaluateDuration(
            XsdDayTimeDurationExpressionAst expression,
            NativeEvaluationContext context) {
        if (expression instanceof TimezoneAst timezone) {
            return timezoneDuration(calendar(timezone.argument(), context), context);
        }
        throw new UnsupportedQueryFeatureException(
                "Duration expression is not supported yet: "
                        + expression.getClass().getSimpleName());
    }

    static XMLGregorianCalendar calendar(TermAst expression, NativeEvaluationContext context) {
        DatatypeValue value = context.required(expression);
        if (value instanceof Literal literal
                && literal.getCoreDatatype() == XSDDatatype.DATETIME) {
            return literal.calendarValue();
        }
        throw new QueryTypeErrorException("Date/time function expects a calendar literal");
    }

    static String timezoneLabel(XMLGregorianCalendar calendar) {
        int minutes = calendar.getTimezone();
        if (minutes == DatatypeConstants.FIELD_UNDEFINED) {
            return "";
        }
        if (minutes == 0) {
            return "Z";
        }
        int absoluteMinutes = Math.abs(minutes);
        return "%s%02d:%02d".formatted(
                minutes < 0 ? "-" : "+",
                absoluteMinutes / 60,
                absoluteMinutes % 60);
    }

    private static DatatypeValue timezoneDuration(
            XMLGregorianCalendar calendar,
            NativeEvaluationContext context) {
        int minutes = calendar.getTimezone();
        if (minutes == DatatypeConstants.FIELD_UNDEFINED) {
            throw new QueryTypeErrorException(
                    "TIMEZONE expects a date/time value with a timezone");
        }
        int absoluteMinutes = Math.abs(minutes);
        String sign = minutes < 0 ? "-" : "";
        String hours = absoluteMinutes >= 60 ? absoluteMinutes / 60 + "H" : "";
        String remainder = absoluteMinutes % 60 != 0 ? absoluteMinutes % 60 + "M" : "";
        String lexical = minutes == 0 ? "PT0S" : sign + "PT" + hours + remainder;
        return context.values().createLiteral(lexical, XSD.xsdDayTimeDuration.getIRI());
    }
}
