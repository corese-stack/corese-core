package fr.inria.corese.core.next.query.kgram.api.core;

import fr.inria.corese.core.sparql.exceptions.CoreseDatatypeException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Datatype value interface for KGRAM.
 */
public interface DatatypeValue {

    /**
     * Returns the string label of this datatype value.
     *
     * @return the label
     */
    String getLabel();

    /**
     * Returns the underlying Java object value.
     *
     * @return the value object
     */
    Object getValue();

    /**
     * Returns the datatype URI.
     *
     * @return the datatype URI as a string
     */
    String getDatatypeURI();

    /**
     * Tests if this value represents a boolean true.
     * Used for filter evaluation.
     *
     * @return true if this value is truthy
     */
    boolean isTrue() throws CoreseDatatypeException;

    /**
     * Tests if this value is equivalent to another for comparison purposes.
     *
     * @param other the other value
     * @return true if equivalent
     */
//    boolean equalsWE(DatatypeValue other) throws CoreseDatatypeException;

    /**
     * Compares this value to another.
     *
     * @param other the other value
     * @return negative if less, 0 if equal, positive if greater
     */
//    int compare(DatatypeValue other) throws CoreseDatatypeException;

    /**
     * Returns the integer value if this is a numeric type.
     *
     * @return the integer value
     * @throws NumberFormatException if not numeric
     */
    int intValue();

    /**
     * Returns the double value if this is a numeric type.
     *
     * @return the double value
     * @throws NumberFormatException if not numeric
     */
    double doubleValue();

    /**
     * Returns the BigDecimal value if this is a numeric type.
     *
     * @return the BigDecimal value
     * @throws NumberFormatException if not numeric
     */
    BigDecimal decimalValue();

    /**
     * Returns the float value if this is a numeric type.
     *
     * @return the float value
     * @throws NumberFormatException if not numeric
     */
    float floatValue();

    /**
     * Returns the long value if this is a numeric type.
     *
     * @return the long value
     * @throws NumberFormatException if not numeric
     */
    long longValue();

    /**
     * Tests if this is a number type.
     *
     * @return true if numeric
     */
    boolean isNumber();

    /**
     * Tests if this is a literal (not a URI or blank node).
     *
     * @return true if literal
     */
    boolean isLiteral();

    /**
     * Tests if this is a URI.
     *
     * @return true if URI
     */
    boolean isURI();


    /**
     * Tests if this is a boolean literal.
     *
     * @return true if boolean literal
     */
    boolean isBoolean();

    /**
     * Returns the boolean value if this is a boolean type.
     *
     * @return the boolean value
     */
    boolean booleanValue();

    /**
     * Tests if this is a blank node.
     *
     * @return true if blank node
     */
    boolean isBlank();

    /**
     * Returns the lang tag if this is a lang tagged literal.
     *
     * @return the lang tag as string
     */
    String getLang();

    /**
     * Returns the string value if this is a literal.
     *
     * @return the string value
     */
    String stringValue();

    boolean isUndefined();

    List getValueList();
}