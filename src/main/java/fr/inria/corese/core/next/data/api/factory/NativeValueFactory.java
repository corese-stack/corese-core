package fr.inria.corese.core.next.data.api.factory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.model.SimpleStatement;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.spi.term.literal.AbstractLiteral;
import fr.inria.corese.core.next.data.api.term.BNode;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Triple;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.api.term.SimpleIRI;
import fr.inria.corese.core.next.data.api.term.SimpleBNode;
import fr.inria.corese.core.next.data.api.term.SimpleLiteral;
import fr.inria.corese.core.next.data.api.term.SimpleInteger;
import fr.inria.corese.core.next.data.api.term.SimpleDecimal;
import fr.inria.corese.core.next.data.api.term.SimpleDouble;
import fr.inria.corese.core.next.data.api.term.SimpleDate;
import fr.inria.corese.core.next.data.api.term.SimpleDateTime;
import fr.inria.corese.core.next.data.api.term.SimpleTime;
import fr.inria.corese.core.next.data.api.term.SimpleDuration;
import fr.inria.corese.core.next.data.api.term.SimpleTriple;

/** Thread-safe factory for native RDF terms and statements. */
public class NativeValueFactory implements ValueFactory {

    private static final DateTimeFormatter XML_TIME = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_TIME).optionalStart().appendOffsetId().optionalEnd()
            .toFormatter();
    private static final DateTimeFormatter XML_DATE_TIME = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME).optionalStart().appendOffsetId().optionalEnd()
            .toFormatter();

    private static final SecureRandom secureRandom = new SecureRandom();
    private final AtomicLong nodeID = new AtomicLong(secureRandom.nextLong());

    @Override
    public IRI createIRI(String iri) {
        return new SimpleIRI(iri);
    }

    @Override
    public IRI createIRI(String namespace, String localName) {
        return new SimpleIRI(namespace, localName);
    }

    @Override
    public BNode createBNode() {
        return new SimpleBNode(Long.toHexString(Math.abs(nodeID.getAndIncrement())));
    }

    @Override
    public BNode createBNode(String nodeID) {
        return new SimpleBNode(nodeID);
    }

    @Override
    public Literal createLiteral(String label) {
        return new SimpleLiteral(label);
    }

    @Override
    public Literal createLiteral(String label, String language) {
        return new SimpleLiteral(label, language);
    }

    @Override
    public Literal createLiteral(boolean value) {
        return new SimpleLiteral(Boolean.toString(value), XSDDatatype.BOOLEAN.getIRI(), XSDDatatype.BOOLEAN);
    }

    @Override
    public Literal createLiteral(byte value) {
        return new SimpleInteger(Byte.toString(value), XSDDatatype.BYTE.getIRI(), XSDDatatype.BYTE);
    }

    @Override
    public Literal createLiteral(short value) {
        return new SimpleInteger(Short.toString(value), XSDDatatype.SHORT.getIRI(), XSDDatatype.SHORT);
    }

    @Override
    public Literal createLiteral(int value) {
        return new SimpleInteger(Integer.toString(value), XSDDatatype.INT.getIRI(), XSDDatatype.INT);
    }

    @Override
    public Literal createLiteral(long value) {
        return new SimpleInteger(Long.toString(value), XSDDatatype.LONG.getIRI(), XSDDatatype.LONG);
    }

    @Override
    public Literal createLiteral(float value) {
        return new SimpleDouble(value);
    }

    @Override
    public Literal createLiteral(double value) {
        return new SimpleDouble(value);
    }

    @Override
    public Literal createLiteral(BigDecimal value) {
        return new SimpleDecimal(value);
    }

    @Override
    public Literal createLiteral(BigInteger value) {
        return new SimpleInteger(value);
    }

    @Override
    public Literal createLiteral(TemporalAmount value) {
        return new SimpleDuration(value);
    }

    @Override
    public Statement createStatement(Resource subject, IRI predicate, Value object) {
        return new SimpleStatement(subject, predicate, object);
    }

    @Override
    public Statement createStatement(Resource subject, IRI predicate, Value object, Resource context) {
        return new SimpleStatement(subject, predicate, object, context);
    }

    @Override
    public Triple createTriple(Resource subject, IRI predicate, Value object) {
        return new SimpleTriple(subject, predicate, object);
    }

    @Override
    public Literal createLiteral(String label, IRI datatype) {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(datatype, "datatype");
        try {
            if (XSDDatatype.DATE.getIRI().equals(datatype)) {
                return new SimpleDate(label, datatype);
            }
            if (XSDDatatype.DATETIME.getIRI().equals(datatype)) {
                return new SimpleDateTime(label, datatype);
            }
            if (XSDDatatype.TIME.getIRI().equals(datatype)) {
                return new SimpleTime(label, datatype);
            }
            if (XSDDatatype.DURATION.getIRI().equals(datatype)) {
                return new SimpleDuration(label, datatype);
            }
            if (AbstractLiteral.isIriOfIntegerCoreDatatype(datatype)) {
                return new SimpleInteger(label, datatype);
            }
            if (XSDDatatype.DECIMAL.getIRI().equals(datatype)) {
                return new SimpleDecimal(label, datatype);
            }
            if (XSDDatatype.DOUBLE.getIRI().equals(datatype) || XSDDatatype.FLOAT.getIRI().equals(datatype)) {
                return new SimpleDouble(label, datatype);
            }
            if (XSDDatatype.BOOLEAN.getIRI().equals(datatype)) {
                return new SimpleLiteral(label, datatype, XSDDatatype.BOOLEAN);
            }
        } catch (RuntimeException invalidLexicalForm) {
            return new SimpleLiteral(label, datatype);
        }
        if (XSDDatatype.STRING.getIRI().equals(datatype)) {
            return new SimpleLiteral(label);
        }
        return new SimpleLiteral(label, datatype);
    }

    @Override
    public Literal createLiteral(TemporalAccessor value) {
        Objects.requireNonNull(value, "value");
        if (value.isSupported(ChronoField.HOUR_OF_DAY) && value.isSupported(ChronoField.MINUTE_OF_HOUR)
                && value.isSupported(ChronoField.SECOND_OF_MINUTE)) {
            if (value.isSupported(ChronoField.YEAR) && value.isSupported(ChronoField.MONTH_OF_YEAR)
                    && value.isSupported(ChronoField.DAY_OF_MONTH)) {
                return new SimpleDateTime(XML_DATE_TIME.format(value));
            } else {
                return new SimpleTime(XML_TIME.format(value));
            }
        } else if (value.isSupported(ChronoField.YEAR) && value.isSupported(ChronoField.MONTH_OF_YEAR)
                && value.isSupported(ChronoField.DAY_OF_MONTH)) {
            return new SimpleDate(DateTimeFormatter.ISO_DATE.format(value));
        } else {
            return new SimpleDateTime(DateTimeFormatter.ISO_INSTANT.format(value));
        }
    }

    @Override
    public Literal createLiteral(XMLGregorianCalendar calendar) {
        Objects.requireNonNull(calendar, "calendar");
        QName schemaType = calendar.getXMLSchemaType();
        if (DatatypeConstants.DATE.equals(schemaType)) {
            return new SimpleDate(calendar);
        }
        if (DatatypeConstants.TIME.equals(schemaType)) {
            return new SimpleTime(calendar);
        }
        return new SimpleDateTime(calendar);
    }
}
