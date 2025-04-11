package fr.inria.corese.core.next.impl.temp.literal;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.base.model.literal.AbstractLiteral;
import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.api.literal.CoreDatatype;
import fr.inria.corese.core.next.impl.exception.IncorrectDatatypeException;
import fr.inria.corese.core.next.impl.exception.IncorrectOperationException;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Super class for all the integer based literal in the XD datatype hierarchy
 */
public class CoreseInteger extends AbstractCoreseNumber {

    /**
     * Constructor for CoreseInteger.
     *
     * @param value  the value of the integer literal
     */
    public CoreseInteger(long value) {
        super(new fr.inria.corese.core.sparql.datatype.CoreseInteger(value), XSD.INTEGER.getIRI());
    }

    /**
     * Constructor for CoreseInteger.
     * @param coreseObject the CoreseInteger object
     */
    public CoreseInteger(fr.inria.corese.core.sparql.datatype.CoreseInteger coreseObject) {
        super(coreseObject, XSD.INTEGER.getIRI());
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
            throw new IncorrectDatatypeException("Cannot create CoreseInteger with a non-integer CoreDatatype.");
        }
    }

    /**
     * Constructor for CoreseInteger.
     * @param bigInteger the BigInteger value of the integer literal
     */
    public CoreseInteger(BigInteger bigInteger) {
        super(new fr.inria.corese.core.sparql.datatype.CoreseInteger(bigInteger.longValue()), XSD.INTEGER.getIRI());
    }

    /**
     *
     * @return XSD.INTEGER
     */
    @Override
    public CoreDatatype getCoreDatatype() {
        return XSD.INTEGER;
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
        return (byte) this.coreseObject.longValue();
    }

    /**
     * @return the value of the literal as a int
     */
    @Override
    public int intValue() {
        return (int) this.coreseObject.longValue();
    }

    /**
     * @return the value of the literal as a long
     */
    @Override
    public long longValue() {
        return this.coreseObject.longValue();
    }

    /**
     * @return the value of the literal as a short
     */
    @Override
    public short shortValue() {
        return (short) this.coreseObject.longValue();
    }

    /**
     * @return the value of the literal as a double
     */
    @Override
    public double doubleValue() {
        return this.coreseObject.longValue();
    }

    /**
     * @return the value of the literal as a BigInteger
     */
    @Override
    public BigInteger integerValue() {
        return BigInteger.valueOf(this.coreseObject.longValue());
    }

    /**
     * @return the value of the literal as a BigDecimal
     */
    @Override
    public BigDecimal decimalValue() {
        return BigDecimal.valueOf(this.coreseObject.longValue());
    }
}
