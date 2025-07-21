package fr.inria.corese.core.next.impl.temp;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.datatype.XMLGregorianCalendar;

import fr.inria.corese.core.next.api.BNode;
import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Literal;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Statement;
import fr.inria.corese.core.next.api.Triple;
import fr.inria.corese.core.next.api.Value;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.model.literal.AbstractLiteral;
import fr.inria.corese.core.next.api.literal.CoreDatatype;
import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.impl.exception.InternalException;
import fr.inria.corese.core.next.impl.temp.literal.CoreseBNode;
import fr.inria.corese.core.next.impl.temp.literal.CoreseBoolean;
import fr.inria.corese.core.next.impl.temp.literal.CoreseDate;
import fr.inria.corese.core.next.impl.temp.literal.CoreseDatetime;
import fr.inria.corese.core.next.impl.temp.literal.CoreseDecimal;
import fr.inria.corese.core.next.impl.temp.literal.CoreseDuration;
import fr.inria.corese.core.next.impl.temp.literal.CoreseInteger;
import fr.inria.corese.core.next.impl.temp.literal.CoreseLanguageTaggedStringLiteral;
import fr.inria.corese.core.next.impl.temp.literal.CoreseTime;
import fr.inria.corese.core.next.impl.temp.literal.CoreseTyped;

/**
 * Factory for all the Corese adapted values.
 */
public class CoreseAdaptedValueFactory implements ValueFactory {

    private final AtomicLong nodeID = new AtomicLong(ThreadLocalRandom.current().nextLong());

    public CoreseAdaptedValueFactory() {
    }

    /**
     * @param iri Valid IRI string
     * @return  A new CoreseIRI object
     */
    @Override
    public IRI createIRI(String iri) {
        return new CoreseIRI(iri);
    }

    /**
     * @param namespace Valid namespace string
     * @param localName Valid local name string
     * @return  A new CoreseIRI object
     */
    @Override
    public IRI createIRI(String namespace, String localName) {
        return new CoreseIRI(namespace, localName);
    }

    /**
     *
     * @return a new CoreseBNode object with a randomly selected ID
     */
    @Override
    public BNode createBNode() {
        return new CoreseBNode(Long.toHexString(Math.abs(nodeID.getAndIncrement())));
    }

    /**
     * @param nodeID Valid node ID string
     * @return  A new CoreseBNode object with the given ID
     */
    @Override
    public BNode createBNode(String nodeID) {
        return new CoreseBNode(nodeID);
    }

    /**
     * @param label Valid label string
     * @return  A new CoreseDatatypeAdapter object with the given label
     */
    @Override
    public Literal createLiteral(String label) {
        return new CoreseTyped(label);
    }

    /**
     * @param label Valid label string
     * @param language Valid language string
     * @return  A new CoreseDatatypeAdapter object with the given label and language
     */
    @Override
    public Literal createLiteral(String label, String language) {
        return new CoreseLanguageTaggedStringLiteral(label, language);
    }

    /**
     * @param label Valid label string
     * @param datatype Valid datatype IRI
     * @return  A new CoreseDatatypeAdapter object with the given label and datatype. The core datatype of the literal will be deterimined by the datatype IRI if possible. Otherwise, a string literal will be created.
     */
    @Override
    public Literal createLiteral(String label, IRI datatype) {
        // Temporal types
        if (XSD.DATE.getIRI().equals(datatype)) {
            return new CoreseDate(label);
        } else if (XSD.DATETIME.getIRI().equals(datatype)) {
            return new CoreseDatetime(label);
        } else if (XSD.TIME.getIRI().equals(datatype)) {
            return new CoreseTime(label);
        } else if (XSD.DURATION.getIRI().equals(datatype)) {
            return new CoreseDuration(label);
        }

        // Numeric types
        else if(AbstractLiteral.isIriOfIntegerCoreDatatype(datatype)) {
            return new CoreseInteger(new BigInteger(label));
        } else if(AbstractLiteral.isIriOfDecimalCoreDatatype(datatype)) {
            return new CoreseDecimal(new BigDecimal(label));
        }

        // Boolean types
        else if (XSD.BOOLEAN.getIRI().equals(datatype)) {
            return new CoreseBoolean(Boolean.parseBoolean(label));
        }

        // String literals
        else if (XSD.STRING.getIRI().equals(datatype)) {
            return new CoreseTyped(label);
        }

        // unknown datatype
        return new CoreseTyped(label, datatype);
    }

    /**
     * Creates a literal with the given label and datatype.
     * @param label Lexical value
     * @param coreDatatype he core datatype of the literal that will also serves as it datatype IRI
     * @return a new CoreseDatatypeAdapter object with the given label and core datatype.
     */
    @Override
    public Literal createLiteral(String label, CoreDatatype coreDatatype) {
        // Temporal types
        if (XSD.DATE.equals(coreDatatype)) {
            return new CoreseDate(label);
        } else if (XSD.DATETIME.equals(coreDatatype)) {
            return new CoreseDatetime(label);
        } else if (XSD.TIME.equals(coreDatatype)) {
            return new CoreseTime(label);
        } else if (XSD.DURATION.equals(coreDatatype)) {
            return new CoreseDuration(label);
        }

        // Numeric types
        else if(AbstractLiteral.isIntegerCoreDatatype(coreDatatype)) {
            return new CoreseInteger(label, coreDatatype.getIRI(), coreDatatype);
        } else if(AbstractLiteral.isDecimalCoreDatatype(coreDatatype)) {
            return new CoreseDecimal(label, coreDatatype.getIRI(), coreDatatype);
        }

        // Boolean types
        else if (XSD.BOOLEAN.equals(coreDatatype)) {
            return new CoreseBoolean(Boolean.parseBoolean(label));
        }

        // String literals
        else if (XSD.STRING.equals(coreDatatype)) {
            return new CoreseTyped(label);
        }
        return new CoreseTyped(label, coreDatatype.getIRI(), coreDatatype);
    }

    /**
     * Creates a literal with the given label, datatype and core datatype.
     * @param label Lexical value
     * @param datatype Datatype IRI
     * @param coreDatatype The core datatype of the literal that will also serves as it datatype IRI
     * @return a new CoreseDatatypeAdapter object with the given label, datatype and core datatype. The core datatype will be used even if there is a mismatch with the datatype IRI.
     */
    @Override
    public Literal createLiteral(String label, IRI datatype, CoreDatatype coreDatatype) {
        // Temporal types
        if (XSD.DATE.equals(coreDatatype)) {
            return new CoreseDate(label, datatype, coreDatatype);
        } else if (XSD.DATETIME.equals(coreDatatype)) {
            return new CoreseDatetime(label, datatype, coreDatatype);
        } else if (XSD.TIME.equals(coreDatatype)) {
            return new CoreseTime(label, datatype, coreDatatype);
        } else if (XSD.DURATION.equals(coreDatatype)) {
            return new CoreseDuration(label, datatype, coreDatatype);
        }

        // Numeric types
        else if(AbstractLiteral.isIntegerCoreDatatype(coreDatatype)) {
            return new CoreseInteger(label, datatype, coreDatatype);
        } else if(AbstractLiteral.isDecimalCoreDatatype(coreDatatype)) {
            return new CoreseDecimal(label, datatype, coreDatatype);
        }

        // String literals
        else if (XSD.STRING.equals(coreDatatype)) {
            return new CoreseTyped(label, datatype, coreDatatype);
        }
        return new CoreseTyped(label, datatype, coreDatatype);
    }

    /**
     *
     * @param value boolean value
     * @return a new CoreseDatatypeAdapter object with the given boolean value
     */
    @Override
    public Literal createLiteral(boolean value) {
        return new CoreseBoolean(value);
    }

    /**
     * Creates a literal with the given byte value.
     * @param value byte value
     * @return a new CoreseInteger object with the given byte value
     */
    @Override
    public Literal createLiteral(byte value) {
        return new CoreseInteger(value);
    }

    /**
     * Creates a literal with the given short value.
     * @param value short value
     * @return a new CoreseInteger object with the given short value
     */
    @Override
    public Literal createLiteral(short value) {
        return new CoreseInteger(value);
    }

    /**
     * Creates a literal with the given int value.
     * @param value int value
     * @return a new CoreseInteger object with the given int value
     */
    @Override
    public Literal createLiteral(int value) {
        return new CoreseInteger(value);
    }

    /**
     * Creates a literal with the given long value.
     * @param value long value
     * @return a new CoreseInteger object with the given long value
     */
    @Override
    public Literal createLiteral(long value) {
        return new CoreseInteger(value);
    }

    /**
     * Creates a literal with the given float value.
     * @param value float value
     * @return a new CoreseDecimal object with the given float value
     */
    @Override
    public Literal createLiteral(float value) {
        return new CoreseDecimal(value);
    }

    /**
     * Creates a literal with the given double value.
     * @param value double value
     * @return a new CoreseDecimal object with the given double value
     */
    @Override
    public Literal createLiteral(double value) {
        return new CoreseDecimal(value);
    }

    /**
     * Creates a literal with the given BigDecimal value.
     * @param bigDecimal BigDecimal value
     * @return a new CoreseDecimal object with the given BigDecimal value
     */
    @Override
    public Literal createLiteral(BigDecimal bigDecimal) {
        return new CoreseDecimal(bigDecimal);
    }

    /**
     * Creates a literal with the given BigInteger value.
     * @param bigInteger BigInteger value
     * @return a new CoreseInteger object with the given BigInteger value
     */
    @Override
    public Literal createLiteral(BigInteger bigInteger) {
        return new CoreseInteger(bigInteger);
    }

    /**
     * Creates a literal with the given TemporalAccessor value.
     * @param value TemporalAccessor value
     * @return a new CoreseDatatypeAdapter object with the given TemporalAccessor value. Depending of the value, the most adequate literal type will be used among CoreseDate, CoreseTime, CoreseDatetime.
     */
    @Override
    public Literal createLiteral(TemporalAccessor value) {
        if(value.isSupported(ChronoField.HOUR_OF_DAY) && value.isSupported(ChronoField.MINUTE_OF_HOUR) && value.isSupported(ChronoField.SECOND_OF_MINUTE)) {
            if(value.isSupported(ChronoField.YEAR) && value.isSupported(ChronoField.MONTH_OF_YEAR) && value.isSupported(ChronoField.DAY_OF_MONTH)) {
                return new CoreseDatetime(value.toString());
            } else {
                return new CoreseTime(value.toString());
            }
        } else if(value.isSupported(ChronoField.YEAR) && value.isSupported(ChronoField.MONTH_OF_YEAR) && value.isSupported(ChronoField.DAY_OF_MONTH)) {
            return new CoreseDate(value.toString());
        } else {
            return new CoreseDatetime(value.toString());
        }
    }

    /**
     * There are no classes that implement TemporalAmount in Corese. the returned object is based on CoreseUndefLiteral and offer no operation on durations
     * @param value the TemporalAmount to be represented as a Literal
     * @return CoreseDuration
     */
    @Override
    public Literal createLiteral(TemporalAmount value) {
        return new CoreseDuration(value);
    }

    /**
     * Creates a literal with the given XMLGregorianCalendar value.
     * @param calendar XMLGregorianCalendar value
     * @return a new CoreseDatetime object with the given XMLGregorianCalendar value
     */
    @Override
    public Literal createLiteral(XMLGregorianCalendar calendar) {
        return new CoreseDatetime(calendar);
    }

    /**
     * Creates a literal with the given Date value.
     * @param date Date value
     * @return a new CoreseDate object with the given Date value
     */
    @Override
    public Literal createLiteral(Date date) {
        return new CoreseDate(date);
    }

    /**
     * Creates a {@link Statement} with a subject, predicate, and object, and no context.
     * This method is used to create a simple RDF-like statement where the context is not provided.
     * @param subject the subject of the statement (cannot be null)
     * @param predicate the predicate of the statement (cannot be null)
     * @param object the object of the statement (cannot be null)
     * @return a new {@link CoreseStatement} with the given subject, predicate, and object, and no context
     * @throws InternalException if any of the parameters are {@code null}
     */
    @Override
    public Statement createStatement(Resource subject, IRI predicate, Value object) {

        if (subject == null) {
            throw new InternalException("Subject cannot be null");
        }
        if (predicate == null) {
            throw new InternalException("Predicate cannot be null");
        }
        if (object == null) {
            throw new InternalException("Object cannot be null");
        }
        return new CoreseStatement(subject, predicate, object, null);
    }

    /**
     * Creates a {@link Statement} with a subject, predicate, object, and context.
     * This method is used to create an RDF-like statement with a specified context (graph).
     * @param subject the subject of the statement (cannot be null)
     * @param predicate the predicate of the statement (cannot be null)
     * @param object the object of the statement (cannot be null)
     * @param context the context (graph) of the statement (can be null)
     * @return a new {@link CoreseStatement} with the given subject, predicate, object, and context
     * @throws InternalException if any of the parameters are {@code null}
     */
    @Override
    public Statement createStatement(Resource subject, IRI predicate, Value object, Resource context) {
        if (subject == null) {
            throw new InternalException("Subject cannot be null");
        }
        if (predicate == null) {
            throw new InternalException("Predicate cannot be null");
        }
        if (object == null) {
            throw new InternalException("Object cannot be null");
        }
        return new CoreseStatement(subject, predicate, object, context);
    }

    @Override
    public Triple createTriple(Resource subject, IRI predicate, Value object) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
