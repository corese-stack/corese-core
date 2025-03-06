package fr.inria.corese.core.next.api.model.impl.corese;

import fr.inria.corese.core.next.api.model.*;
import fr.inria.corese.core.next.api.model.base.CoreDatatype;
import fr.inria.corese.core.next.api.model.impl.corese.literal.CoreseDate;
import fr.inria.corese.core.next.api.model.impl.corese.literal.CoreseDatetime;
import fr.inria.corese.core.next.api.model.vocabulary.XSD;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Date;

public class CoreseAdaptedValueFactory implements ValueFactory {

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
        return null;
    }

    @Override
    public Literal createLiteral(String label, CoreDatatype datatype) {
        if (XSD.xsdDate.equals(datatype)) {
            return new CoreseDate(label);
        } else if (XSD.xsdDateTime.equals(datatype)) {
            return new CoreseDatetime(label);
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
        return ValueFactory.super.createLiteral(value);
    }

    /**
     * There are no classes that implement TemporalAmount in Corese
     * @param value
     * @return
     */
    @Override
    public Literal createLiteral(TemporalAmount value) {
        return ValueFactory.super.createLiteral(value);
    }

    @Override
    public Literal createLiteral(XMLGregorianCalendar calendar) {
        return null;
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
