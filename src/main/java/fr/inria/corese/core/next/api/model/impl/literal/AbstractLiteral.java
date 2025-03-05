package fr.inria.corese.core.next.api.model.impl.literal;

import fr.inria.corese.core.next.api.exception.IncorrectOperationException;
import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.model.Literal;
import fr.inria.corese.core.next.api.model.base.CoreDatatype;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.DateTimeException;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Optional;

public abstract class AbstractLiteral implements Literal {

    protected IRI datatype;

    protected AbstractLiteral(IRI datatype) {
        this.datatype = datatype;
    }

    public abstract void setCoreDatatype(CoreDatatype coreDatatype);

    @Override
    public boolean isLiteral() {
        return true;
    }

    @Override
    public Optional<String> getLanguage() {
        return Optional.empty();
    }

    @Override
    public IRI getDatatype() {
        return this.datatype;
    }

    @Override
    public boolean booleanValue() {
        throw new IncorrectOperationException("Cannot convert to boolean");
    }

    @Override
    public byte byteValue() {
        throw new IncorrectOperationException("Cannot convert to byte");
    }

    @Override
    public short shortValue() {
        throw new IncorrectOperationException("Cannot convert to short");
    }

    @Override
    public int intValue() {
        throw new IncorrectOperationException("Cannot convert to int");
    }

    @Override
    public long longValue() {
        throw new IncorrectOperationException("Cannot convert to long");
    }

    @Override
    public BigInteger integerValue() {
        throw new IncorrectOperationException("Cannot convert to integer");
    }

    @Override
    public BigDecimal decimalValue() {
        throw new IncorrectOperationException("Cannot convert to decimal");
    }

    @Override
    public float floatValue() {
        throw new IncorrectOperationException("Cannot convert to float");
    }

    @Override
    public double doubleValue() {
        throw new IncorrectOperationException("Cannot convert to double");
    }

    @Override
    public TemporalAccessor temporalAccessorValue() {
        throw new IncorrectOperationException("Cannot convert to temporal accessor");
    }

    @Override
    public TemporalAmount temporalAmountValue() {
        throw new IncorrectOperationException("Cannot convert to temporal amount");
    }

    @Override
    public XMLGregorianCalendar calendarValue() {
        throw new IncorrectOperationException("Cannot convert to XML calendar");
    }
}
