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

@SuppressWarnings({"java:S2160", "java:S2143"})
public class CoreseDate extends AbstractTemporalPointLiteral implements CoreseDatatypeAdapter {

    private final transient fr.inria.corese.core.sparql.datatype.CoreseDate coreseObject;

    /**
     * Constructor for CoreseDate.
     *
     * @param coreseObject the CoreseDate object
     */
    public CoreseDate(IDatatype coreseObject) {
        super(new CoreseIRI(coreseObject.getDatatypeURI()));
        if (coreseObject instanceof fr.inria.corese.core.sparql.datatype.CoreseDate dateObj) {
            this.coreseObject = dateObj;
        } else {
            throw new IncorrectOperationException("Cannot create CoreseDate from a non-date Corese object.");
        }
    }

    /**
     * Constructor for CoreseDate.
     *
     * @param calendar the XMLGregorianCalendar object
     */
    public CoreseDate(XMLGregorianCalendar calendar) {
        this(new fr.inria.corese.core.sparql.datatype.CoreseDate(calendar.toXMLFormat()));
    }

    /**
     * Constructor for CoreseDate.
     *
     * @param date the Date object.
     */
    public CoreseDate(java.util.Date date) {
        this(date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString());
    }

    /**
     * Constructor for CoreseDate.
     *
     * @param localDate the LocalDate object.
     */
    public CoreseDate(java.time.LocalDate localDate) {
        this(localDate.toString());
    }

    /**
     * Constructor for CoreseDate.
     *
     * @param value    the string representation of the date
     * @param datatype the datatype of the literal
     */
    public CoreseDate(String value, IRI datatype) {
        this(new fr.inria.corese.core.sparql.datatype.CoreseDate(value));
        this.datatype = datatype;
    }

    /**
     * Constructor for CoreseDate.
     *
     * @param value        the string representation of the date
     * @param datatype     the datatype of the literal
     * @param coreDatatype the CoreDatatype of the literal. Must be XSDDatatype.DATE.
     */
    public CoreseDate(String value, IRI datatype, CoreDatatype coreDatatype) {
        this(value, datatype);
        if (coreDatatype != null && coreDatatype != XSDDatatype.DATE) {
            throw new InvalidDatatypeException("Cannot create CoreseDate with a non-date CoreDatatype.");
        }
    }

    /**
     * Constructor for CoreseDate.
     *
     * @param date the string representation of the date
     */
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

    /**
     * @throws InvalidDatatypeException as date cannot be converted to boolean
     */
    @Override
    public boolean booleanValue() {
        throw new InvalidDatatypeException("Cannot convert date to boolean value.");
    }

    /**
     * @throws InvalidDatatypeException as date cannot be converted to byte
     */
    @Override
    public byte byteValue() {
        throw new InvalidDatatypeException("Cannot convert date to byte value.");
    }

    /**
     * @throws InvalidDatatypeException as date cannot be converted to float
     */
    @Override
    public short shortValue() {
        throw new InvalidDatatypeException("Cannot convert date to short value.");
    }

    /**
     * @throws InvalidDatatypeException as date cannot be converted to int
     */
    @Override
    public int intValue() {
        throw new InvalidDatatypeException("Cannot convert date to int value.");
    }

    /**
     * @throws InvalidDatatypeException as date cannot be converted to long
     */
    @Override
    public long longValue() {
        throw new InvalidDatatypeException("Cannot convert date to long value.");
    }

    /**
     * @throws InvalidDatatypeException as date cannot be converted to BigInteger
     */
    @Override
    public BigInteger integerValue() {
        throw new InvalidDatatypeException("Cannot convert date to integer value.");
    }

    /**
     * @throws InvalidDatatypeException as date cannot be converted to BigDecimal
     */
    @Override
    public BigDecimal decimalValue() {
        throw new InvalidDatatypeException("Cannot convert date to decimal value.");
    }

    /**
     * @throws InvalidDatatypeException as date cannot be converted to float
     */
    @Override
    public float floatValue() {
        throw new InvalidDatatypeException("Cannot convert date to float value.");
    }

    /**
     * @throws InvalidDatatypeException as date cannot be converted to double
     */
    @Override
    public double doubleValue() {
        throw new InvalidDatatypeException("Cannot convert date to double value.");
    }

    @Override
    public XMLGregorianCalendar calendarValue() {
        return this.coreseObject.getCalendar();
    }

    @Override
    public TemporalAccessor temporalAccessorValue() {
        return this.coreseObject.getCalendar().toGregorianCalendar().toZonedDateTime();
    }

    /**
     *
     * @return XSDDatatype.DATE as the core datatype for this date object.
     */
    @Override
    public CoreDatatype getCoreDatatype() {
        return XSDDatatype.DATE;
    }

    /**
     * @throws IncorrectOperationException if the CoreDatatype is not XSDDatatype.DATE
     */
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
