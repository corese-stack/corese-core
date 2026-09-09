package fr.inria.corese.core.next.data.api.term;

import java.io.Serial;
import java.time.temporal.TemporalAccessor;
import java.util.Objects;
import java.util.Optional;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import fr.inria.corese.core.next.data.api.exception.IncorrectOperationException;
import fr.inria.corese.core.next.data.api.literal.CoreDatatype;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.spi.term.literal.AbstractTemporalPointLiteral;

/** RDF temporal literal preserving its lexical form and supplied calendar reference. */
public final class SimpleDate extends AbstractTemporalPointLiteral {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String label;
    private final XMLGregorianCalendar calendar;

    public SimpleDate(String lexicalValue) {
        this(lexicalValue, XSDDatatype.DATE.getIRI(), XSDDatatype.DATE);
    }

    public SimpleDate(XMLGregorianCalendar calendar) {
        super(XSDDatatype.DATE.getIRI());
        this.calendar = Objects.requireNonNull(calendar, "calendar");
        this.label = calendar.toXMLFormat();
    }

    public SimpleDate(String lexicalValue, IRI datatype) {
        this(lexicalValue, datatype, null);
    }

    @SuppressWarnings("java:S1172") // Kept for source compatibility with the temporal literal API.
    public SimpleDate(String lexicalValue, IRI datatype, CoreDatatype coreDatatype) {
        super(datatype == null ? XSDDatatype.DATE.getIRI() : datatype);
        this.label = Objects.requireNonNull(lexicalValue, "lexicalValue");
        this.calendar = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(lexicalValue);
    }

    @Override
    public XMLGregorianCalendar calendarValue() {
        return calendar;
    }

    @Override
    public TemporalAccessor temporalAccessorValue() {
        return calendar.toGregorianCalendar().toZonedDateTime();
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String stringValue() {
        return label;
    }

    @Override
    public Optional<String> getLanguage() {
        return Optional.empty();
    }

    @Override
    public CoreDatatype getCoreDatatype() {
        return XSDDatatype.DATE;
    }

    @Override
    public IRI getDatatype() {
        return this.datatype;
    }

    @Override
    protected void setCoreDatatype(CoreDatatype coreDatatype) {
        throw new IncorrectOperationException("SimpleDate is immutable");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Literal other)) {
            return false;
        }
        return label.equals(other.getLabel())
                && datatype.equals(other.getDatatype())
                && Objects.equals(getLanguage(), other.getLanguage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, datatype, getLanguage());
    }
}
