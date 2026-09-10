package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.query.api.exception.QueryTypeErrorException;

import javax.xml.datatype.DatatypeConstants;

/** Implements SPARQL relational comparison without RDF ordering tie-breakers. */
final class NativeValueComparison {

    private NativeValueComparison() {
    }

    static int compare(DatatypeValue left, DatatypeValue right) {
        if (left.isNumber() && right.isNumber()
                && left instanceof Literal leftLiteral
                && right instanceof Literal rightLiteral) {
            return compareNumbers(leftLiteral, rightLiteral);
        }
        if (left instanceof Literal leftLiteral && right instanceof Literal rightLiteral) {
            return compareLiterals(leftLiteral, rightLiteral);
        }
        throw incomparable();
    }

    private static int compareNumbers(Literal left, Literal right) {
        if (isFloatingPoint(left) || isFloatingPoint(right)) {
            return compareFloatingPoint(left.doubleValue(), right.doubleValue());
        }
        return left.decimalValue().compareTo(right.decimalValue());
    }

    private static int compareLiterals(Literal left, Literal right) {
        if (left.getCoreDatatype() == XSDDatatype.STRING
                && right.getCoreDatatype() == XSDDatatype.STRING) {
            return left.getLabel().compareTo(right.getLabel());
        }
        if (isComparableCalendar(left, right)) {
            int comparison = left.calendarValue().compare(right.calendarValue());
            if (comparison != DatatypeConstants.INDETERMINATE) {
                return comparison;
            }
        }
        throw incomparable();
    }

    private static int compareFloatingPoint(double left, double right) {
        if (Double.isNaN(left) || Double.isNaN(right)) {
            throw new QueryTypeErrorException("NaN is not order-comparable");
        }
        if (left < right) {
            return -1;
        }
        return left > right ? 1 : 0;
    }

    private static boolean isFloatingPoint(Literal literal) {
        return literal.getCoreDatatype() == XSDDatatype.FLOAT
                || literal.getCoreDatatype() == XSDDatatype.DOUBLE;
    }

    private static boolean isComparableCalendar(Literal left, Literal right) {
        if (left.getCoreDatatype() != right.getCoreDatatype()) {
            return false;
        }
        return left.getCoreDatatype() instanceof XSDDatatype datatype && switch (datatype) {
            case DATE, DATETIME, TIME -> true;
            default -> false;
        };
    }

    private static QueryTypeErrorException incomparable() {
        return new QueryTypeErrorException(
                "RDF values are not order-comparable in a SPARQL expression");
    }
}
