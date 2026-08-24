package fr.inria.corese.core.next.data.impl.adapter.literal;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.support.term.literal.AbstractLiteral;
import fr.inria.corese.core.next.data.api.support.term.literal.AbstractNumber;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.literal.CoreDatatype;
import fr.inria.corese.core.next.data.api.literal.CoreDatatypes;
import fr.inria.corese.core.next.data.api.exception.InvalidDatatypeException;
import fr.inria.corese.core.sparql.datatype.CoreseDouble;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Super class for all numeric literal containing floating points in the XD datatype hierarchy
 * @ImplNotes Against the XSD hierarchy, the legacy Corese super class for floating point numbers is CoreseDouble, the legacy CoreseDecimal inherits from CoreseDouble. This class is a wrapper for CoreseDouble, and is used to represent the XSD decimal datatype.
 */
public class CoreseDecimal extends AbstractCoreseNumber {

    private CoreDatatype coreDatatype;

    @Override
    protected CoreseDouble createCoreseObject(String value) {
        return new fr.inria.corese.core.sparql.datatype.CoreseDecimal(new BigDecimal(value));
    }

    /**
     * Constructor for CoreseDecimal.
     *
     * @param value  the value of the decimal literal
     */
    public CoreseDecimal(double value) {
        super(new fr.inria.corese.core.sparql.datatype.CoreseDecimal(value), XSDDatatype.DECIMAL.getIRI());
        this.coreDatatype = XSDDatatype.DECIMAL;
    }

    /**
     * Constructor for CoreseDecimal.
     * @param coreseObject the CoreseDouble object
     */
    public CoreseDecimal(CoreseDouble coreseObject) {
        this(coreseObject, XSDDatatype.DECIMAL.getIRI());
    }

    /**
     * Constructor for CoreseDecimal.
     * @param coreseObject the CoreseDouble object
     * @param datatype the datatype of the literal
     */
    public CoreseDecimal(CoreseDouble coreseObject, IRI datatype) {
        super(coreseObject, datatype);
        this.coreDatatype = decimalDatatypeOrDefault(datatype);
    }

    /**
     * Constructor for CoreseDecimal.
     * @param value the value of the decimal literal
     */
    public CoreseDecimal(String value) {
        super(value,
                new fr.inria.corese.core.sparql.datatype.CoreseDecimal(new BigDecimal(value)),
                XSDDatatype.DECIMAL.getIRI());
        this.coreDatatype = XSDDatatype.DECIMAL;
    }

    /**
     * Constructor for CoreseDecimal.
     * @param value the string value of the decimal literal
     * @param datatype the datatype of the literal
     */
    public CoreseDecimal(String value, IRI datatype) {
        super(value,
                new fr.inria.corese.core.sparql.datatype.CoreseDecimal(new BigDecimal(value)),
                datatype);
        this.coreDatatype = decimalDatatypeOrDefault(datatype);
    }

    /**
     * Constructor for CoreseDecimal.
     * @param value the string value of the decimal literal
     * @param datatype the datatype of the literal
     * @param coreDatatype the CoreDatatype of the literal. Must be a decimal CoreDatatype, e.g xsd:decimal, xsd:double, etc.
     */
    public CoreseDecimal(String value, IRI datatype, CoreDatatype coreDatatype) {
        this(value, datatype);
        setCoreDatatype(coreDatatype);
    }

    /**
     * Constructor for CoreseDecimal.
     * @param bigDecimal the exact decimal value of the literal
     */
    public CoreseDecimal(BigDecimal bigDecimal) {
        super(new fr.inria.corese.core.sparql.datatype.CoreseDecimal(bigDecimal), XSDDatatype.DECIMAL.getIRI());
        this.coreDatatype = XSDDatatype.DECIMAL;
    }

    /**
     *
     * @return XSDDatatype.DECIMAL
     */
    @Override
    public CoreDatatype getCoreDatatype() {
        return coreDatatype;
    }

    /**
     * Set the CoreDatatype of the literal.
     * @param coreDatatype the CoreDatatype to set. Must be a decimal CoreDatatype, e.g xsd:decimal, xsd:double, etc.
     */
    @Override
    protected void setCoreDatatype(CoreDatatype coreDatatype) {
        if(! AbstractLiteral.isDecimalCoreDatatype(coreDatatype)) {
            throw new InvalidDatatypeException("Cannot set a non-decimal CoreDatatype for a CoreseDecimal.");
        }
        this.coreDatatype = coreDatatype;
    }

    private static CoreDatatype decimalDatatypeOrDefault(IRI datatype) {
        CoreDatatype resolved = CoreDatatypes.from(datatype);
        return AbstractLiteral.isDecimalCoreDatatype(resolved) ? resolved : XSDDatatype.DECIMAL;
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
        return coreseObject().doubleValue();
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
        return coreseObject().decimalValue();
    }

    @Override
    public int compareTo(AbstractNumber abstractNumber) {
        return Double.compare(this.doubleValue(), abstractNumber.doubleValue());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || super.equals(other);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
