package fr.inria.corese.core.next.api.model.impl.basic.literal;

import fr.inria.corese.core.next.api.exception.IncorrectOperationException;
import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.model.base.CoreDatatype;
import fr.inria.corese.core.next.api.model.impl.literal.AbstractDuration;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Optional;

public class BasicDuration extends AbstractDuration {

    private final TemporalAmount temporalAmount;

    public BasicDuration(String stringValue) {
        this.temporalAmount = Duration.parse(stringValue);
    }

    @Override
    public boolean isBNode() {
        return super.isBNode();
    }

    @Override
    public boolean isIRI() {
        return super.isIRI();
    }

    @Override
    public boolean isResource() {
        return super.isResource();
    }

    @Override
    public boolean isLiteral() {
        return super.isLiteral();
    }

    @Override
    public boolean isTriple() {
        return super.isTriple();
    }

    @Override
    public String stringValue() {
        return this.temporalAmount.toString();
    }

    @Override
    public String getLabel() {
        return null;
    }

    @Override
    public Optional<String> getLanguage() {
        return Optional.empty();
    }

    @Override
    public IRI getDatatype() {
        return null;
    }

    @Override
    public boolean booleanValue() {
        return false;
    }

    @Override
    public byte byteValue() {
        return 0;
    }

    @Override
    public short shortValue() {
        return 0;
    }

    @Override
    public int intValue() {
        return 0;
    }

    @Override
    public long longValue() {
        return 0;
    }

    @Override
    public BigInteger integerValue() {
        return null;
    }

    @Override
    public BigDecimal decimalValue() {
        return null;
    }

    @Override
    public float floatValue() {
        return 0;
    }

    @Override
    public double doubleValue() {
        return 0;
    }

    @Override
    public TemporalAccessor temporalAccessorValue() throws DateTimeException {
        return super.temporalAccessorValue();
    }

    @Override
    public TemporalAmount temporalAmountValue() throws DateTimeException {
        return super.temporalAmountValue();
    }

    @Override
    public XMLGregorianCalendar calendarValue() {
        return null;
    }

    @Override
    public CoreDatatype getCoreDatatype() {
        return null;
    }

    @Override
    public void setCoreDatatype(CoreDatatype coreDatatype) {
        throw new IncorrectOperationException("Cannot set the core datatype of a duration.");
    }
}
