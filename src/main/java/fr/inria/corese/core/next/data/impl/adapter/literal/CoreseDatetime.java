package fr.inria.corese.core.next.data.impl.adapter.literal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;

import javax.xml.datatype.XMLGregorianCalendar;

import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.support.term.literal.AbstractTemporalPointLiteral;
import fr.inria.corese.core.next.data.api.literal.CoreDatatype;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.exception.InvalidDatatypeException;
import fr.inria.corese.core.next.data.api.exception.IncorrectOperationException;
import fr.inria.corese.core.next.data.impl.adapter.node.CoreseIRI;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.CoreseDateTime;

/**
 * CoreseDatetime class that represents a date and time literal in the Corese
 * framework.
 * It extends the AbstractTemporalPointLiteral class and implements the
 * CoreseDatatypeAdapter interface.
 */
@SuppressWarnings("java:S2160")
public class CoreseDatetime extends AbstractTemporalPointLiteral implements CoreseDatatypeAdapter {
    private final transient CoreseDateTime coreseObject;

    /**
     * Constructor for CoreseDatetime.
     *
     * @param coreseObject the CoreseDateTime object
     */
    public CoreseDatetime(IDatatype coreseObject) {
        super(new CoreseIRI(coreseObject.getDatatypeURI()));
        if (coreseObject instanceof CoreseDateTime dateTimeObj) {
            this.coreseObject = dateTimeObj;
        } else {
            throw new IncorrectOperationException("Cannot create CoreseDatetime from a non-date Corese object.");
        }
    }

    /**
     * Constructor for CoreseDatetime.
     *
     * @param calendar the XMLGregorianCalendar object
     */
    public CoreseDatetime(XMLGregorianCalendar calendar) {
        this(new CoreseDateTime(calendar.toXMLFormat()));
    }

    /**
     * Constructor for CoreseDatetime.
     *
     * @param dateXMLDateFormat the date in XML date format
     */
    public CoreseDatetime(String dateXMLDateFormat) {
        this(new CoreseDateTime(dateXMLDateFormat));
    }

    /**
     * Constructor for CoreseDatetime.
     *
     * @param value    the date and time value
     * @param datatype the datatype of the literal
     */
    public CoreseDatetime(String value, IRI datatype) {
        this(new CoreseDateTime(value));
        this.datatype = datatype;
    }

    /**
     * Constructor for CoreseDatetime.
     *
     * @param value        the date and time value
     * @param datatype     the datatype of the literal
     * @param coreDatatype the core datatype. Must be xsd:dateTime, xsd:time, or
     *                     xsd:date.
     */
    public CoreseDatetime(String value, IRI datatype, CoreDatatype coreDatatype) {
        this(value, datatype);
        if (coreDatatype != null && coreDatatype != XSDDatatype.DATETIME && coreDatatype != XSDDatatype.TIME
                && coreDatatype != XSDDatatype.DATE) {
            throw new InvalidDatatypeException(
                    "Cannot create CoreseDatetime with a core datatype other than xsd:dateTime or xsd:time.");
        }
    }

    @Override
    public String getLabel() {
        return this.coreseObject.getLabel();
    }

    @Override
    public Optional<String> getLanguage() {
        return Optional.empty();
    }

    @Override
    public IRI getDatatype() {
        return this.datatype;
    }

    /**
     * @throws IncorrectOperationException as Datetime cannot be converted to
     *                                     boolean
     */
    @Override
    public boolean booleanValue() {
        throw new IncorrectOperationException("Cannot convert a datetime to boolean.");
    }

    /**
     * @throws IncorrectOperationException as Datetime cannot be converted to byte
     */
    @Override
    public byte byteValue() {
        throw new IncorrectOperationException("Cannot convert a datetime to byte.");
    }

    /**
     * @throws IncorrectOperationException as Datetime cannot be converted to short
     */
    @Override
    public short shortValue() {
        throw new IncorrectOperationException("Cannot convert a datetime to short.");
    }

    /**
     * @throws IncorrectOperationException as Datetime cannot be converted to char
     */
    @Override
    public int intValue() {
        throw new IncorrectOperationException("Cannot convert a datetime to int.");
    }

    /**
     * @throws IncorrectOperationException as Datetime cannot be converted to long
     */
    @Override
    public long longValue() {
        throw new IncorrectOperationException("Cannot convert a datetime to long.");
    }

    /**
     * @throws IncorrectOperationException as Datetime cannot be converted to float
     */
    @Override
    public BigInteger integerValue() {
        throw new IncorrectOperationException("Cannot convert a datetime to integer.");
    }

    /**
     * @throws IncorrectOperationException as Datetime cannot be converted to
     *                                     BigDecimal
     */
    @Override
    public BigDecimal decimalValue() {
        throw new IncorrectOperationException("Cannot convert a datetime to decimal.");
    }

    /**
     * @throws IncorrectOperationException as Datetime cannot be converted to
     *                                     BigInteger
     */
    @Override
    public float floatValue() {
        throw new IncorrectOperationException("Cannot convert a datetime to float.");
    }

    /**
     * @throws IncorrectOperationException as Datetime cannot be converted to double
     */
    @Override
    public double doubleValue() {
        throw new IncorrectOperationException("Cannot convert a datetime to double.");
    }

    /**
     *
     * @return the XMLGregorianCalendar representation of the datetime
     */
    @Override
    public XMLGregorianCalendar calendarValue() {
        return this.coreseObject.getCalendar();
    }

    /**
     * @return the TemporalAccessor representation of the datetime
     */
    @Override
    public TemporalAccessor temporalAccessorValue() {
        return this.coreseObject.getCalendar().toGregorianCalendar().toZonedDateTime();
    }

    /**
     * @return XSDDatatype.DATETIME as the core datatype
     */
    @Override
    public CoreDatatype getCoreDatatype() {
        return XSDDatatype.DATETIME;
    }

    @Override
    public void setCoreDatatype(CoreDatatype coreDatatype) {
        throw new IncorrectOperationException("Cannot set core datatype for this datetime object.");
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
