package fr.inria.corese.core.next.data.impl.adapter.literal;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.support.term.literal.AbstractLiteral;
import fr.inria.corese.core.next.data.api.support.term.literal.AbstractNumber;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.literal.CoreDatatype;
import fr.inria.corese.core.next.data.api.exception.InvalidDatatypeException;
import fr.inria.corese.core.next.data.api.exception.IncorrectOperationException;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Super class for all the integer based literal in the XD datatype hierarchy
 */
public class CoreseInteger extends AbstractCoreseNumber {

    @Override
    protected fr.inria.corese.core.sparql.datatype.CoreseInteger createCoreseObject(String value) {
        return new fr.inria.corese.core.sparql.datatype.CoreseInteger(value);
    }

    /**
     * Constructor for CoreseInteger.
     *
     * @param value  the value of the integer literal
     */
    public CoreseInteger(long value) {
        super(new fr.inria.corese.core.sparql.datatype.CoreseInteger(value), XSDDatatype.INTEGER.getIRI());
    }

    /**
     * Constructor for CoreseInteger.
     * @param coreseObject the CoreseInteger object
     */
    public CoreseInteger(fr.inria.corese.core.sparql.datatype.CoreseInteger coreseObject) {
        super(coreseObject, XSDDatatype.INTEGER.getIRI());
    }

    /**
     * Constructor for CoreseInteger.
     * @param coreseObject the CoreseInteger object
     * @param datatype the datatype of the literal
     */
    public CoreseInteger(fr.inria.corese.core.sparql.datatype.CoreseInteger coreseObject, IRI datatype) {
        super(coreseObject, datatype);
    }

    /**
     * Constructor for CoreseInteger.
     * @param value the value of the integer literal
     */
    public CoreseInteger(String value) {
        this(new fr.inria.corese.core.sparql.datatype.CoreseInteger(value));
    }

    /**
     * Constructor for CoreseInteger.
     * @param value the string value of the integer literal
     * @param datatype the datatype of the literal
     */
    public CoreseInteger(String value, IRI datatype) {
        this(new fr.inria.corese.core.sparql.datatype.CoreseInteger(value), datatype);
    }

    /**
     * Constructor for CoreseInteger.
     * @param value the string value of the integer literal
     * @param datatype the datatype of the literal
     * @param coreDatatype the CoreDatatype of the literal. Must be an integer core datatype, e.g xsd:integer, xsd:int, etc.
     */
    public CoreseInteger(String value, IRI datatype, CoreDatatype coreDatatype) {
        this(value, datatype);
        if(! AbstractLiteral.isIntegerCoreDatatype(coreDatatype)) {
            throw new InvalidDatatypeException("Cannot create CoreseInteger with a non-integer CoreDatatype.");
        }
    }

    /**
     * Constructor for CoreseInteger.
     * @param bigInteger the BigInteger value of the integer literal
     */
    public CoreseInteger(BigInteger bigInteger) {
        super(new fr.inria.corese.core.sparql.datatype.CoreseInteger(bigInteger.longValue()), XSDDatatype.INTEGER.getIRI());
    }

    /**
     *
     * @return XSDDatatype.INTEGER
     */
    @Override
    public CoreDatatype getCoreDatatype() {
        return XSDDatatype.INTEGER;
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
    }

    /**
     * @return the value of the literal as a byte
     */
    @Override
    public byte byteValue() {
        return (byte) coreseObject().longValue();
    }

    /**
     * @return the value of the literal as a int
     */
    @Override
    public int intValue() {
        return (int) coreseObject().longValue();
    }

    /**
     * @return the value of the literal as a long
     */
    @Override
    public long longValue() {
        return coreseObject().longValue();
    }

    /**
     * @return the value of the literal as a short
     */
    @Override
    public short shortValue() {
        return (short) coreseObject().longValue();
    }

    /**
     * @return the value of the literal as a double
     */
    @Override
    public double doubleValue() {
        return coreseObject().longValue();
    }

    /**
     * @return the value of the literal as a BigInteger
     */
    @Override
    public BigInteger integerValue() {
        return BigInteger.valueOf(coreseObject().longValue());
    }

    /**
     * @return the value of the literal as a BigDecimal
     */
    @Override
    public BigDecimal decimalValue() {
        return BigDecimal.valueOf(coreseObject().longValue());
    }

    @Override
    public int compareTo(AbstractNumber abstractNumber) {
        return Math.toIntExact(this.longValue() - abstractNumber.longValue());
    }
}
