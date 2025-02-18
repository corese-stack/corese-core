package fr.inria.corese.core.next.api.model;

import fr.inria.corese.core.next.api.model.base.CoreDatatype;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Date;

public interface ValueFactory {

	IRI createIRI(String iri);

	IRI createIRI(String namespace, String localName);

	BNode createBNode();

	BNode createBNode(String nodeID);

	Literal createLiteral(String label);

	Literal createLiteral(String label, String language);

	Literal createLiteral(String label, IRI datatype);

	Literal createLiteral(String label, CoreDatatype datatype);

	Literal createLiteral(String label, IRI datatype, CoreDatatype coreDatatype);

	Literal createLiteral(boolean value);

	Literal createLiteral(byte value);

	Literal createLiteral(short value);

	Literal createLiteral(int value);

	Literal createLiteral(long value);

	Literal createLiteral(float value);

	Literal createLiteral(double value);

	Literal createLiteral(BigDecimal bigDecimal);

	Literal createLiteral(BigInteger bigInteger);

	default Literal createLiteral(TemporalAccessor value) {
		throw new UnsupportedOperationException();
	}

	default Literal createLiteral(TemporalAmount value) {
		throw new UnsupportedOperationException();
	}

	Literal createLiteral(XMLGregorianCalendar calendar);

	Literal createLiteral(Date date);

	Statement createStatement(Resource subject, IRI predicate, Value object);

	Statement createStatement(Resource subject, IRI predicate, Value object, Resource context);

	default Triple createTriple(Resource subject, IRI predicate, Value object) {
		throw new UnsupportedOperationException();
	}

}
