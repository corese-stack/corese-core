package fr.inria.corese.core.next.impl.temp.literal;

import fr.inria.corese.core.next.api.exception.IncorrectDatatypeException;
import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.model.base.literal.AbstractLiteral;
import fr.inria.corese.core.next.api.model.base.literal.CoreDatatype;
import fr.inria.corese.core.sparql.datatype.CoreseDouble;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Super class for all numeric literal containing floating points in the XD datatype hierarchy
 * @ImplNotes Against the XSD hierarchy, the legacy Corese super class for floating point numbers is CoreseDouble, the legacy CoreseDecimal inherits from CoreseDouble. This class is a wrapper for CoreseDouble, and is used to represent the XSD decimal datatype.
 */
public class CoreseDecimal extends AbstractCoreseNumber {

    public CoreseDecimal(double value) {
        super(new CoreseDouble(value), CoreDatatype.XSD.DECIMAL.getIRI());
    }

    public CoreseDecimal(CoreseDouble coreseObject) {
        super(coreseObject, CoreDatatype.XSD.DECIMAL.getIRI());
    }

    public CoreseDecimal(CoreseDouble coreseObject, IRI datatype) {
        super(coreseObject, datatype);
    }

    public CoreseDecimal(String value) {
        this(new CoreseDouble(value));
    }

    public CoreseDecimal(String value, IRI datatype) {
        this(new CoreseDouble(value), datatype);
    }

    public CoreseDecimal(String value, IRI datatype, CoreDatatype coreDatatype) {
        this(value, datatype);
        if(! AbstractLiteral.isDecimalCoreDatatype(coreDatatype)) {
            throw new IncorrectDatatypeException("Cannot create CoreseDecimal with a non-integer CoreDatatype.");
        }
    }

    public CoreseDecimal(BigDecimal bigDecimal) {
        super(new CoreseDouble(bigDecimal.doubleValue()), CoreDatatype.XSD.DECIMAL.getIRI());
    }

    @Override
    public CoreDatatype getCoreDatatype() {
        return CoreDatatype.XSD.DECIMAL;
    }

    @Override
    protected void setCoreDatatype(CoreDatatype coreDatatype) {
        if(! AbstractLiteral.isDecimalCoreDatatype(coreDatatype)) {
            throw new IncorrectDatatypeException("Cannot set a non-decimal CoreDatatype for a CoreseDecimal.");
        }
    }

    @Override
    public byte byteValue() {
        return (byte) this.doubleValue();
    }

    @Override
    public int intValue() {
        return (int) this.doubleValue();
    }

    @Override
    public long longValue() {
        return (long) this.doubleValue();
    }

    @Override
    public short shortValue() {
        return (short) this.doubleValue();
    }

    @Override
    public double doubleValue() {
        return this.coreseObject.doubleValue();
    }

    @Override
    public BigInteger integerValue() {
        return BigInteger.valueOf(this.longValue());
    }

    @Override
    public BigDecimal decimalValue() {
        return BigDecimal.valueOf(this.doubleValue());
    }
}
