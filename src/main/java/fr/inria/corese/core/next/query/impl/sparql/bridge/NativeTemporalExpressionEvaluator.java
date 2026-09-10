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

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

/** Evaluates date-time and timezone expressions. */
final class NativeTemporalExpressionEvaluator {

    private NativeTemporalExpressionEvaluator() {
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
