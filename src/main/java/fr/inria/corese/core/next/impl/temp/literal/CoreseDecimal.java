package fr.inria.corese.core.next.impl.temp.literal;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.base.model.literal.AbstractLiteral;
import fr.inria.corese.core.next.api.base.model.literal.XSD;
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

    public CoreseDecimal(double value) {
        super(new CoreseDouble(value), XSD.DECIMAL.getIRI());
    }

    public CoreseDecimal(CoreseDouble coreseObject) {
        super(coreseObject, XSD.DECIMAL.getIRI());
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
        super(new CoreseDouble(bigDecimal.doubleValue()), XSD.DECIMAL.getIRI());
    }

    @Override
    public CoreDatatype getCoreDatatype() {
        return XSD.DECIMAL;
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
