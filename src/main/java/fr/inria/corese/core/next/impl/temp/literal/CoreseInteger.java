package fr.inria.corese.core.next.impl.temp.literal;

import fr.inria.corese.core.next.api.exception.IncorrectDatatypeException;
import fr.inria.corese.core.next.api.exception.IncorrectOperationException;
import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.model.base.literal.AbstractLiteral;
import fr.inria.corese.core.next.api.model.base.literal.CoreDatatype;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Super class for all the integer based literal in the XD datatype hierarchy
 */
public class CoreseInteger extends AbstractCoreseNumber {

    public CoreseInteger(long value) {
        super(new fr.inria.corese.core.sparql.datatype.CoreseInteger(value), CoreDatatype.XSD.INTEGER.getIRI());
    }

    public CoreseInteger(fr.inria.corese.core.sparql.datatype.CoreseInteger coreseObject) {
        super(coreseObject, CoreDatatype.XSD.INTEGER.getIRI());
    }

    public CoreseInteger(fr.inria.corese.core.sparql.datatype.CoreseInteger coreseObject, IRI datatype) {
        super(coreseObject, datatype);
    }

    public CoreseInteger(String value) {
        this(new fr.inria.corese.core.sparql.datatype.CoreseInteger(value));
    }

    public CoreseInteger(String value, IRI datatype) {
        this(new fr.inria.corese.core.sparql.datatype.CoreseInteger(value), datatype);
    }

    public CoreseInteger(String value, IRI datatype, CoreDatatype coreDatatype) {
        this(value, datatype);
        if(! AbstractLiteral.isIntegerCoreDatatype(coreDatatype)) {
            throw new IncorrectDatatypeException("Cannot create CoreseInteger with a non-integer CoreDatatype.");
        }
    }

    public CoreseInteger(BigInteger bigInteger) {
        super(new fr.inria.corese.core.sparql.datatype.CoreseInteger(bigInteger.longValue()), CoreDatatype.XSD.INTEGER.getIRI());
    }

    @Override
    public CoreDatatype getCoreDatatype() {
        return CoreDatatype.XSD.INTEGER;
    }

    @Override
    protected void setCoreDatatype(CoreDatatype coreDatatype) {
        if(! AbstractLiteral.isIntegerCoreDatatype(coreDatatype)) {
            throw new IncorrectOperationException("Cannot set a non-integer CoreDatatype for a CoreseInteger.");
        }
    }

    @Override
    public byte byteValue() {
        return (byte) this.coreseObject.longValue();
    }

    @Override
    public int intValue() {
        return (int) this.coreseObject.longValue();
    }

    @Override
    public long longValue() {
        return this.coreseObject.longValue();
    }

    @Override
    public short shortValue() {
        return (short) this.coreseObject.longValue();
    }

    @Override
    public double doubleValue() {
        return this.coreseObject.longValue();
    }

    @Override
    public BigInteger integerValue() {
        return BigInteger.valueOf(this.coreseObject.longValue());
    }

    @Override
    public BigDecimal decimalValue() {
        return BigDecimal.valueOf(this.coreseObject.longValue());
    }
}
