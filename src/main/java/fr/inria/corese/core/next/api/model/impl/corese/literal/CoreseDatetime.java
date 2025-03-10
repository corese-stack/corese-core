package fr.inria.corese.core.next.api.model.impl.corese.literal;

import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.api.exception.IncorrectDatatypeException;
import fr.inria.corese.core.next.api.exception.IncorrectOperationException;
import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.model.base.CoreDatatype;
import fr.inria.corese.core.next.api.model.impl.corese.CoreseIRI;
import fr.inria.corese.core.next.api.model.impl.literal.AbstractTemporalPointLiteral;
import fr.inria.corese.core.next.api.model.vocabulary.XSD;
import fr.inria.corese.core.sparql.api.IDatatype;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;

public class CoreseDatetime extends AbstractTemporalPointLiteral implements CoreseDatatypeAdapter {
    private final fr.inria.corese.core.sparql.datatype.CoreseDateTime coreseObject;

    public CoreseDatetime(IDatatype coreseObject) {
        super(new CoreseIRI(coreseObject.getDatatypeURI()));
        if (coreseObject instanceof fr.inria.corese.core.sparql.datatype.CoreseDateTime) {
            this.coreseObject = (fr.inria.corese.core.sparql.datatype.CoreseDateTime) coreseObject;
        } else {
            throw new IncorrectOperationException("Cannot create CoreseDatetime from a non-date Corese object.");
        }
    }

    public CoreseDatetime(XMLGregorianCalendar calendar) {
        this(new fr.inria.corese.core.sparql.datatype.CoreseDateTime(calendar.toXMLFormat()));
    }

    public CoreseDatetime(String dateXMLDateFormat) {
        this(new fr.inria.corese.core.sparql.datatype.CoreseDateTime(dateXMLDateFormat));
    }

    public CoreseDatetime(String value, IRI datatype) {
        this(new fr.inria.corese.core.sparql.datatype.CoreseDateTime(value));
        this.datatype = datatype;
    }

    public CoreseDatetime(String value, IRI datatype, CoreDatatype coreDatatype) {
        this(value, datatype);
        if(coreDatatype != null && coreDatatype != XSD.xsdDateTime && coreDatatype != XSD.xsdTime) {
            throw new IncorrectDatatypeException("Cannot create CoreseDatetime with a core datatype other than xsd:dateTime or xsd:time.");
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

    @Override
    public boolean booleanValue() {
        throw new IncorrectOperationException("Cannot convert a datetime to boolean.");
    }

    @Override
    public byte byteValue() {
        throw new IncorrectOperationException("Cannot convert a datetime to byte.");
    }

    @Override
    public short shortValue() {
        throw new IncorrectOperationException("Cannot convert a datetime to short.");
    }

    @Override
    public int intValue() {
        throw new IncorrectOperationException("Cannot convert a datetime to int.");
    }

    @Override
    public long longValue() {
        throw new IncorrectOperationException("Cannot convert a datetime to long.");
    }

    @Override
    public BigInteger integerValue() {
        throw new IncorrectOperationException("Cannot convert a datetime to integer.");
    }

    @Override
    public BigDecimal decimalValue() {
        throw new IncorrectOperationException("Cannot convert a datetime to decimal.");
    }

    @Override
    public float floatValue() {
        throw new IncorrectOperationException("Cannot convert a datetime to float.");
    }

    @Override
    public double doubleValue() {
        throw new IncorrectOperationException("Cannot convert a datetime to double.");
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
        return XSD.xsdDateTime;
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
