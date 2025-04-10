package fr.inria.corese.core.next.impl.temp.literal;

import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.impl.exception.IncorrectDatatypeException;
import fr.inria.corese.core.next.impl.exception.IncorrectOperationException;
import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.literal.CoreDatatype;
import fr.inria.corese.core.next.api.base.model.literal.XSD;
import fr.inria.corese.core.next.impl.temp.CoreseIRI;
import fr.inria.corese.core.next.api.base.model.literal.AbstractTemporalPointLiteral;
import fr.inria.corese.core.sparql.api.IDatatype;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Optional;

public class CoreseDate extends AbstractTemporalPointLiteral implements CoreseDatatypeAdapter {

    private final fr.inria.corese.core.sparql.datatype.CoreseDate coreseObject;

    public CoreseDate(IDatatype coreseObject) {
        super(new CoreseIRI(coreseObject.getDatatypeURI()));
        if (coreseObject instanceof fr.inria.corese.core.sparql.datatype.CoreseDate) {
            this.coreseObject = (fr.inria.corese.core.sparql.datatype.CoreseDate) coreseObject;
        } else {
            throw new IncorrectOperationException("Cannot create CoreseDate from a non-date Corese object.");
        }
    }

    public CoreseDate(XMLGregorianCalendar calendar) {
        this(new fr.inria.corese.core.sparql.datatype.CoreseDate(calendar.toXMLFormat()));
    }

    public CoreseDate(Date date) {
        this((new SimpleDateFormat("yyyy-MM-dd" )).format(date));
    }

    public CoreseDate(String value, IRI datatype) {
        this(new fr.inria.corese.core.sparql.datatype.CoreseDate(value));
        this.datatype = datatype;
    }

    public CoreseDate(String value, IRI datatype, CoreDatatype coreDatatype) {
        this(value, datatype);
        if(coreDatatype != null && coreDatatype != XSD.DATE) {
            throw new IncorrectDatatypeException("Cannot create CoreseDate with a non-date CoreDatatype.");
        }
    }

    public CoreseDate(String date) {
        this(new fr.inria.corese.core.sparql.datatype.CoreseDate(date));
    }

    @Override
    public String getLabel() {
        return coreseObject.getLabel();
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
        throw new IncorrectDatatypeException("Cannot convert date to boolean value.");
    }

    @Override
    public byte byteValue() {
        throw new IncorrectDatatypeException("Cannot convert date to byte value.");
    }

    @Override
    public short shortValue() {
        throw new IncorrectDatatypeException("Cannot convert date to short value.");
    }

    @Override
    public int intValue() {
        throw new IncorrectDatatypeException("Cannot convert date to int value.");
    }

    @Override
    public long longValue() {
        throw new IncorrectDatatypeException("Cannot convert date to long value.");
    }

    @Override
    public BigInteger integerValue() {
        throw new IncorrectDatatypeException("Cannot convert date to integer value.");
    }

    @Override
    public BigDecimal decimalValue() {
        throw new IncorrectDatatypeException("Cannot convert date to decimal value.");
    }

    @Override
    public float floatValue() {
        throw new IncorrectDatatypeException("Cannot convert date to float value.");
    }

    @Override
    public double doubleValue() {
        throw new IncorrectDatatypeException("Cannot convert date to double value.");
    }

    @Override
    public XMLGregorianCalendar calendarValue() {
        return this.coreseObject.getCalendar();
    }

    @Override
    public TemporalAccessor temporalAccessorValue() {
        return this.coreseObject.getCalendar().toGregorianCalendar().toZonedDateTime();
    }

    @Override
    public CoreDatatype getCoreDatatype() {
        return XSD.DATE;
    }

    @Override
    public void setCoreDatatype(CoreDatatype coreDatatype) {
        throw new IncorrectOperationException("Cannot set core datatype for this date object.");
    }

    @Override
    public String stringValue() {
        return this.getCoreseNode().getLabel();
    }

    @Override
    public Node getCoreseNode() {
        return this.coreseObject;
    }

    @Override
    public IDatatype getIDatatype() {
        return this.coreseObject;
    }
}
