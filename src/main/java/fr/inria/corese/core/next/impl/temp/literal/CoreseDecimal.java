package fr.inria.corese.core.next.impl.temp.literal;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.base.model.literal.AbstractLiteral;
import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.api.literal.CoreDatatype;
import fr.inria.corese.core.next.impl.exception.IncorrectDatatypeException;
import fr.inria.corese.core.sparql.datatype.CoreseDouble;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Super class for all numeric literal containing floating points in the XD datatype hierarchy
 * @ImplNotes Against the XSD hierarchy, the legacy Corese super class for floating point numbers is CoreseDouble, the legacy CoreseDecimal inherits from CoreseDouble. This class is a wrapper for CoreseDouble, and is used to represent the XSD decimal datatype.
 */
public class CoreseDecimal extends AbstractCoreseNumber {

    /**
     * Constructor for CoreseDecimal.
     *
     * @param value  the value of the decimal literal
     */
    public CoreseDecimal(double value) {
        super(new CoreseDouble(value), XSD.DECIMAL.getIRI());
    }

    /**
     * Constructor for CoreseDecimal.
     * @param coreseObject the CoreseDouble object
     */
    public CoreseDecimal(CoreseDouble coreseObject) {
        super(coreseObject, XSD.DECIMAL.getIRI());
    }

    /**
     * Constructor for CoreseDecimal.
     * @param coreseObject the CoreseDouble object
     * @param datatype the datatype of the literal
     */
    public CoreseDecimal(CoreseDouble coreseObject, IRI datatype) {
        super(coreseObject, datatype);
    }

    /**
     * Constructor for CoreseDecimal.
     * @param value the value of the decimal literal
     */
    public CoreseDecimal(String value) {
        this(new CoreseDouble(value));
    }

    /**
     * Constructor for CoreseDecimal.
     * @param value the string value of the decimal literal
     * @param datatype the datatype of the literal
     */
    public CoreseDecimal(String value, IRI datatype) {
        this(new CoreseDouble(value), datatype);
    }

    /**
     * Constructor for CoreseDecimal.
     * @param value the string value of the decimal literal
     * @param datatype the datatype of the literal
     * @param coreDatatype the CoreDatatype of the literal. Must be a decimal CoreDatatype, e.g xsd:decimal, xsd:double, etc.
     */
    public CoreseDecimal(String value, IRI datatype, CoreDatatype coreDatatype) {
        this(value, datatype);
        if(! AbstractLiteral.isDecimalCoreDatatype(coreDatatype)) {
            throw new IncorrectDatatypeException("Cannot create CoreseDecimal with a non-integer CoreDatatype.");
        }
    }

    /**
     * Constructor for CoreseDecimal.
     * @param bigDecimal the BigInteger value of the decimal literal
     * @ImplNote The Corese object that this object adapts do not allows for the storing of large floting point numbers, so the value is stored as a double. This may lead to loss of precision.
     */
    public CoreseDecimal(BigDecimal bigDecimal) {
        super(new CoreseDouble(bigDecimal.doubleValue()), XSD.DECIMAL.getIRI());
    }

    /**
     *
     * @return XSD.DECIMAL
     */
    @Override
    public CoreDatatype getCoreDatatype() {
        return XSD.DECIMAL;
    }

    /**
     * Set the CoreDatatype of the literal.
     * @param coreDatatype the CoreDatatype to set. Must be a decimal CoreDatatype, e.g xsd:decimal, xsd:double, etc.
     */
    @Override
    protected void setCoreDatatype(CoreDatatype coreDatatype) {
        if(! AbstractLiteral.isDecimalCoreDatatype(coreDatatype)) {
            throw new IncorrectDatatypeException("Cannot set a non-decimal CoreDatatype for a CoreseDecimal.");
        }
    }

    /**
     * @return the value of the literal as a byte
     */
    @Override
    public byte byteValue() {
        return (byte) this.doubleValue();
    }

    /**
     * @return the value of the literal as a int
     */
    @Override
    public int intValue() {
        return (int) this.doubleValue();
    }

    /**
     * @return the value of the literal as a long
     */
    @Override
    public long longValue() {
        return (long) this.doubleValue();
    }

    /**
     * @return the value of the literal as a short
     */
    @Override
    public short shortValue() {
        return (short) this.doubleValue();
    }

    /**
     * @return the value of the literal as a double
     */
    @Override
    public double doubleValue() {
        return this.coreseObject.doubleValue();
    }

    /**
     * @return the value of the literal as an integer
     */
    @Override
    public BigInteger integerValue() {
        return BigInteger.valueOf(this.longValue());
    }

    /**
     * @return the value of the literal as a BigDecimal
     */
    @Override
    public BigDecimal decimalValue() {
        return BigDecimal.valueOf(this.doubleValue());
    }
}
