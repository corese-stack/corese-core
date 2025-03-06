package fr.inria.corese.core.next.api.model.impl.corese;

import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.next.api.model.*;
import fr.inria.corese.core.next.api.model.base.CoreDatatype;
import fr.inria.corese.core.next.api.model.impl.corese.literal.CoreseDate;
import fr.inria.corese.core.next.api.model.impl.corese.literal.CoreseDatetime;
import fr.inria.corese.core.next.api.model.impl.corese.literal.CoreseDuration;
import fr.inria.corese.core.next.api.model.impl.corese.literal.CoreseTime;
import fr.inria.corese.core.next.api.model.vocabulary.XSD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Date;

public class CoreseAdaptedValueFactory implements ValueFactory {

    private static Logger logger = LoggerFactory.getLogger(CoreseAdaptedValueFactory.class);

    public CoreseAdaptedValueFactory() {
    }

    @Override
    public IRI createIRI(String iri) {
        return new CoreseIRI(iri);
    }

    @Override
    public IRI createIRI(String namespace, String localName) {
        return new CoreseIRI(namespace, localName);
    }

    @Override
    public BNode createBNode() {
        return null;
    }

    @Override
    public BNode createBNode(String nodeID) {
        return null;
    }

    @Override
    public Literal createLiteral(String label) {
        return null;
    }

    @Override
    public Literal createLiteral(String label, String language) {
        return null;
    }

    @Override
    public Literal createLiteral(String label, IRI datatype) {
        if (XSD.xsdDate.getIRI().equals(datatype)) {
            return new CoreseDate(label);
        } else if (XSD.xsdDateTime.getIRI().equals(datatype)) {
            return new CoreseDatetime(label);
        } else if (XSD.xsdTime.getIRI().equals(datatype)) {
            return new CoreseTime(label);
        } else if (XSD.xsdDuration.getIRI().equals(datatype)) {
            return new CoreseDuration(label);
        }
        return null;
    }

    @Override
    public Literal createLiteral(String label, CoreDatatype datatype) {
        if (XSD.xsdDate.equals(datatype)) {
            return new CoreseDate(label);
        } else if (XSD.xsdDateTime.equals(datatype)) {
            return new CoreseDatetime(label);
        } else if (XSD.xsdTime.equals(datatype)) {
            return new CoreseTime(label);
        } else if (XSD.xsdDuration.equals(datatype)) {
            return new CoreseDuration(label);
        }
        return null;
    }

    @Override
    public Literal createLiteral(String label, IRI datatype, CoreDatatype coreDatatype) {
        return null;
    }

    @Override
    public Literal createLiteral(boolean value) {
        return null;
    }

    @Override
    public Literal createLiteral(byte value) {
        return null;
    }

    @Override
    public Literal createLiteral(short value) {
        return null;
    }

    @Override
    public Literal createLiteral(int value) {
        return null;
    }

    @Override
    public Literal createLiteral(long value) {
        return null;
    }

    @Override
    public Literal createLiteral(float value) {
        return null;
    }

    @Override
    public Literal createLiteral(double value) {
        return null;
    }

    @Override
    public Literal createLiteral(BigDecimal bigDecimal) {
        return null;
    }

    @Override
    public Literal createLiteral(BigInteger bigInteger) {
        return null;
    }

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
        logger.debug("Creating a Literal from TemporalAmount");
        return new CoreseDuration(value);
    }

    @Override
    public Literal createLiteral(XMLGregorianCalendar calendar) {
        return new CoreseDatetime(calendar);
    }

    @Override
    public Literal createLiteral(Date date) {
        return new CoreseDate(date);
    }

    @Override
    public Statement createStatement(Resource subject, IRI predicate, Value object) {
        return null;
    }

    @Override
    public Statement createStatement(Resource subject, IRI predicate, Value object, Resource context) {
        return null;
    }

    @Override
    public Triple createTriple(Resource subject, IRI predicate, Value object) {
        return ValueFactory.super.createTriple(subject, predicate, object);
    }
}
