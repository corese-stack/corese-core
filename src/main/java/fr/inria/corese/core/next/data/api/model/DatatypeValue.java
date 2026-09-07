package fr.inria.corese.core.next.data.api.model;

import fr.inria.corese.core.next.data.api.exception.IncorrectOperationException;
import fr.inria.corese.core.next.data.api.literal.CoreDatatype;
import fr.inria.corese.core.next.data.api.literal.RDFDatatype;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.term.BNode;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.term.Triple;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

import javax.xml.datatype.DatatypeConstants;

/**
 * Runtime contract shared by RDF values and the query engine.
 *
 * <p>The contract deliberately contains no dependency on the historical Corese
 * datatype hierarchy. Query execution can therefore manipulate values produced
 * by any {@code next.data} implementation without unwrapping implementation
 * objects.</p>
 */
public interface DatatypeValue extends Serializable {

    /** Returns the lexical representation of this RDF value. */
    String stringValue();

    /** Returns the lexical representation of this RDF value. */
    default String getLabel() {
        return stringValue();
    }

    /**
     * Returns the Java value used for value-space operations.
     *
     * <p>Implementations may override this method with a more specific value.
     * The lexical form is a safe default for RDF resources and custom literals.</p>
     */
    default Object getValue() {
        return stringValue();
    }

    /** Returns the datatype IRI of a literal, or {@code null} for non-literals. */
    default String getDatatypeURI() {
        return this instanceof Literal literal && literal.getDatatype() != null
                ? literal.getDatatype().stringValue()
                : null;
    }

    /** Returns whether this value is an RDF IRI. */
    default boolean isIRI() {
        return this instanceof IRI;
    }

    /** Returns whether this value is an RDF blank node. */
    default boolean isBNode() {
        return this instanceof BNode;
    }

    /** Returns whether this value is an RDF literal. */
    default boolean isLiteral() {
        return this instanceof Literal;
    }

    /** Returns whether this value is an RDF-star triple term. */
    default boolean isTriple() {
        return this instanceof Triple;
    }

    /** Returns whether this literal belongs to the XML Schema numeric family. */
    default boolean isNumber() {
        if (!(this instanceof Literal literal)) {
            return false;
        }
        CoreDatatype datatype = literal.getCoreDatatype();
        return datatype instanceof XSDDatatype xsd && switch (xsd) {
            case BYTE, SHORT, INT, LONG, INTEGER,
                    UNSIGNED_BYTE, UNSIGNED_SHORT, UNSIGNED_INT, UNSIGNED_LONG,
                    POSITIVE_INTEGER, NEGATIVE_INTEGER, NON_NEGATIVE_INTEGER,
                    NON_POSITIVE_INTEGER, DECIMAL, FLOAT, DOUBLE -> true;
            default -> false;
        };
    }

    /** Returns this literal as an integer. */
    default int intValue() {
        if (this instanceof Literal literal) {
            return literal.intValue();
        }
        throw new IncorrectOperationException("Cannot convert a non-literal RDF value to int");
    }

    /** Returns this literal as a double. */
    default double doubleValue() {
        if (this instanceof Literal literal) {
            return literal.doubleValue();
        }
        throw new IncorrectOperationException("Cannot convert a non-literal RDF value to double");
    }

    /**
     * Computes the SPARQL effective boolean value.
     *
     * <p>Unsupported RDF terms have no effective boolean value and return
     * {@code false}; expression evaluation is responsible for retaining the
     * distinction between false and an evaluation error where required.</p>
     */
    default boolean isTrue() {
        if (!(this instanceof Literal literal)) {
            return false;
        }
        CoreDatatype datatype = literal.getCoreDatatype();
        if (datatype == XSDDatatype.BOOLEAN) {
            return literal.booleanValue();
        }
        if (isNumber()) {
            double value = literal.doubleValue();
            return value != 0.0d && !Double.isNaN(value);
        }
        if (datatype == XSDDatatype.STRING || datatype == RDFDatatype.LANGSTRING) {
            return !literal.getLabel().isEmpty();
        }
        return false;
    }

    /** RDF-term equality, without value-space coercion. */
    default boolean sameTerm(DatatypeValue other) {
        return Objects.equals(this, other);
    }

    /**
     * SPARQL value equality for the value families required by the native
     * execution pipeline.
     */
    default boolean equalsWE(DatatypeValue other) {
        if (other == null) {
            return false;
        }
        if (this instanceof Literal left && other instanceof Literal right) {
            return literalValueEquals(left, right);
        }
        return sameTerm(other);
    }

    /** Compares two runtime values using the deterministic RDF term order. */
    default int compare(DatatypeValue other) {
        return RdfValueOrder.compareValues(this, other);
    }

    private static boolean isFloatingPoint(Literal literal) {
        return literal.getCoreDatatype() == XSDDatatype.FLOAT
                || literal.getCoreDatatype() == XSDDatatype.DOUBLE;
    }

    private static boolean literalValueEquals(Literal left, Literal right) {
        if (left.isNumber() && right.isNumber()) {
            return numericValueEquals(left, right);
        }
        if (left.getCoreDatatype() == XSDDatatype.BOOLEAN
                && right.getCoreDatatype() == XSDDatatype.BOOLEAN) {
            return left.booleanValue() == right.booleanValue();
        }
        if (isComparableCalendar(left, right)) {
            return left.calendarValue().compare(right.calendarValue()) == DatatypeConstants.EQUAL;
        }
        if (left.getCoreDatatype() == RDFDatatype.LANGSTRING
                && right.getCoreDatatype() == RDFDatatype.LANGSTRING) {
            return left.getLabel().equals(right.getLabel())
                    && left.getLanguage().orElse("")
                    .equalsIgnoreCase(right.getLanguage().orElse(""));
        }
        return left.sameTerm(right);
    }

    private static boolean numericValueEquals(Literal left, Literal right) {
        if (isFloatingPoint(left) || isFloatingPoint(right)) {
            double leftValue = left.doubleValue();
            double rightValue = right.doubleValue();
            return !Double.isNaN(leftValue)
                    && !Double.isNaN(rightValue)
                    && leftValue == rightValue;
        }
        return decimalValue(left).compareTo(decimalValue(right)) == 0;
    }

    private static boolean isComparableCalendar(Literal left, Literal right) {
        if (left.getCoreDatatype() != right.getCoreDatatype()) {
            return false;
        }
        return left.getCoreDatatype() instanceof XSDDatatype xsd && switch (xsd) {
            case DATE, DATETIME, TIME -> true;
            default -> false;
        };
    }

    private static BigDecimal decimalValue(Literal literal) {
        CoreDatatype datatype = literal.getCoreDatatype();
        if (datatype instanceof XSDDatatype xsd && switch (xsd) {
            case BYTE, SHORT, INT, LONG, INTEGER,
                    UNSIGNED_BYTE, UNSIGNED_SHORT, UNSIGNED_INT, UNSIGNED_LONG,
                    POSITIVE_INTEGER, NEGATIVE_INTEGER, NON_NEGATIVE_INTEGER,
                    NON_POSITIVE_INTEGER -> true;
            default -> false;
        }) {
            BigInteger value = literal.integerValue();
            return new BigDecimal(value);
        }
        return literal.decimalValue();
    }
}
