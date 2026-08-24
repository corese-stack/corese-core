package fr.inria.corese.core.next.data.impl.adapter.literal;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.support.term.literal.AbstractLiteral;
import fr.inria.corese.core.next.data.api.support.term.literal.AbstractNumber;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.literal.CoreDatatype;
import fr.inria.corese.core.next.data.api.literal.CoreDatatypes;
import fr.inria.corese.core.next.data.api.exception.IncorrectOperationException;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.CoreseUndefLiteral;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/**
 * Super class for all the integer based literal in the XD datatype hierarchy
 */
public class CoreseInteger extends AbstractCoreseNumber {

    private CoreDatatype coreDatatype;
    private final BigInteger value;

    @Override
    protected IDatatype createCoreseObject(String lexicalValue) {
        return createLegacyObject(lexicalValue, datatype);
    }

    /**
     * Constructor for CoreseInteger.
     *
     * @param value  the value of the integer literal
     */
    public CoreseInteger(long value) {
        this(Long.toString(value));
    }

    /**
     * Constructor for CoreseInteger.
     * @param coreseObject the CoreseInteger object
     */
    public CoreseInteger(fr.inria.corese.core.sparql.datatype.CoreseInteger coreseObject) {
        this(coreseObject, XSDDatatype.INTEGER.getIRI());
    }

    /**
     * Constructor for CoreseInteger.
     * @param coreseObject the CoreseInteger object
     * @param datatype the datatype of the literal
     */
    public CoreseInteger(fr.inria.corese.core.sparql.datatype.CoreseInteger coreseObject, IRI datatype) {
        super(coreseObject, datatype);
        this.value = new BigInteger(coreseObject.getLabel());
        this.coreDatatype = integerDatatypeOrDefault(datatype);
    }

    /**
     * Constructor for CoreseInteger.
     * @param value the value of the integer literal
     */
    public CoreseInteger(String value) {
        this(value, XSDDatatype.INTEGER.getIRI());
    }

    /**
     * Constructor for CoreseInteger.
     * @param value the string value of the integer literal
     * @param datatype the datatype of the literal
     */
    public CoreseInteger(String value, IRI datatype) {
        super(value, createLegacyObject(value, datatype), datatype);
        this.value = new BigInteger(value);
        this.coreDatatype = integerDatatypeOrDefault(datatype);
    }

    /**
     * Constructor for CoreseInteger.
     * @param value the string value of the integer literal
     * @param datatype the datatype of the literal
     * @param coreDatatype the CoreDatatype of the literal. Must be an integer core datatype, e.g xsd:integer, xsd:int, etc.
     */
    public CoreseInteger(String value, IRI datatype, CoreDatatype coreDatatype) {
        this(value, datatype);
        setCoreDatatype(coreDatatype);
    }

    /**
     * Constructor for CoreseInteger.
     * @param bigInteger the BigInteger value of the integer literal
     */
    public CoreseInteger(BigInteger bigInteger) {
        this(Objects.requireNonNull(bigInteger, "bigInteger").toString());
    }

    /**
     *
     * @return XSDDatatype.INTEGER
     */
    @Override
    public CoreDatatype getCoreDatatype() {
        return coreDatatype;
    }

    /**
     *  Set the CoreDatatype of this literal.
     * @param coreDatatype the CoreDatatype to set. Must be an integer core datatype, e.g xsd:integer, xsd:int, etc.
     */
    @Override
    protected void setCoreDatatype(CoreDatatype coreDatatype) {
        if(! AbstractLiteral.isIntegerCoreDatatype(coreDatatype)) {
            throw new IncorrectOperationException("Cannot set a non-integer CoreDatatype for a CoreseInteger.");
        }
        this.coreDatatype = coreDatatype;
    }

    private static CoreDatatype integerDatatypeOrDefault(IRI datatype) {
        CoreDatatype resolved = CoreDatatypes.from(datatype);
        return AbstractLiteral.isIntegerCoreDatatype(resolved) ? resolved : XSDDatatype.INTEGER;
    }

    /**
     * @return the value of the literal as a byte
     */
    @Override
    public byte byteValue() {
        return value.byteValue();
    }

    /**
     * @return the value of the literal as a int
     */
    @Override
    public int intValue() {
        return value.intValue();
    }

    /**
     * @return the value of the literal as a long
     */
    @Override
    public long longValue() {
        return value.longValue();
    }

    /**
     * @return the value of the literal as a short
     */
    @Override
    public short shortValue() {
        return value.shortValue();
    }

    /**
     * @return the value of the literal as a double
     */
    @Override
    public double doubleValue() {
        return value.doubleValue();
    }

    /**
     * @return the value of the literal as a BigInteger
     */
    @Override
    public BigInteger integerValue() {
        return value;
    }

    /**
     * @return the value of the literal as a BigDecimal
     */
    @Override
    public BigDecimal decimalValue() {
        return new BigDecimal(value);
    }

    @Override
    public int compareTo(AbstractNumber abstractNumber) {
        if (abstractNumber instanceof CoreseInteger integer) {
            return value.compareTo(integer.value);
        }
        return Double.compare(doubleValue(), abstractNumber.doubleValue());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || super.equals(other);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    private static IDatatype createLegacyObject(String lexicalValue, IRI datatype) {
        BigInteger parsed = new BigInteger(Objects.requireNonNull(lexicalValue, "value"));
        if (parsed.bitLength() < Long.SIZE) {
            return new fr.inria.corese.core.sparql.datatype.CoreseInteger(lexicalValue);
        }
        return new CoreseUndefLiteral(lexicalValue, datatype.stringValue());
    }
}
