package fr.inria.corese.core.next.api;

import fr.inria.corese.core.next.api.literal.CoreDatatype;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Date;

public interface ValueFactory {

	/**
	 * Create an IRI from a string.
	 * @param iri Valid IRI string
	 * @return IRI
	 */
	IRI createIRI(String iri);

	/**
	 * Create an IRI from a namespace and a local name.
	 * @param namespace Namespace
	 * @param localName Local name
	 * @return IRI
	 */
	IRI createIRI(String namespace, String localName);

	/**
	 * Create a blank node.
	 * @return Blank node
	 */
	BNode createBNode();

	/**
	 * Create a blank node with a node ID.
	 * @param nodeID Node ID
	 * @return Blank node
	 */
	BNode createBNode(String nodeID);

	/**
	 * Create a string literal from its lexical value.
	 * @param label lexical value
	 * @return Literal
	 */
	Literal createLiteral(String label);

	/**
	 * Create a string literal from its lexical value and language tag.
	 * @param label Lexical value
	 * @param language Language tag
	 * @return Literal
	 */
	Literal createLiteral(String label, String language);

	/**
	 * Create a typed literal from its lexical value and datatype IRI.
	 * If the datatype is a CoreDatatype, the coreDatatype parameter should be set to the corresponding value.
	 * @param label Lexical value
	 * @param datatype Datatype IRI
	 * @return Literal
	 */
	Literal createLiteral(String label, IRI datatype);

	/**
	 * Create a typed literal from its lexical value and core datatype IRI. The datatype of the literal is set to be the datatype IRI corresponding to the core datatype.
	 * @param label Lexical value
	 * @param datatype Core datatype
	 * @return Literal
	 */
	Literal createLiteral(String label, CoreDatatype datatype);

	/**
	 * Create a typed literal from its lexical value, datatype IRI and core datatype IRI.
	 * @param label Lexical value
	 * @param datatype Datatype IRI
	 * @param coreDatatype Core datatype
	 * @return Literal
	 */
	Literal createLiteral(String label, IRI datatype, CoreDatatype coreDatatype);

	/**
	 * Create a literal typed by xsd:boolean.
	 * @param value Value
	 * @return Literal that has a boolean value.
	 */
	Literal createLiteral(boolean value);

	/**
	 * Create a literal typed by xsd:byte.
	 * @param value Value
	 * @return Literal that has a byte value.
	 */
	Literal createLiteral(byte value);

	/**
	 * Create a literal typed by xsd:short.
	 * @param value Value
	 * @return Literal that has a short value.
	 */
	Literal createLiteral(short value);

	/**
	 * Create a literal typed by xsd:integer.
	 * @param value Value
	 * @return Literal that has an int value.
	 */
	Literal createLiteral(int value);

	/**
	 * Create a literal typed by xsd:long.
	 * @param value Value
	 * @return Literal that has a long value.
	 */
	Literal createLiteral(long value);

	/**
	 * Create a literal typed by xsd:float.
	 * @param value Value
	 * @return Literal that has a float value.
	 */
	Literal createLiteral(float value);

	/**
	 * Create a literal typed by xsd:double.
	 * @param value Value
	 * @return Literal that has a double value.
	 */
	Literal createLiteral(double value);

	/**
	 * Create a literal typed by xsd:decimal.
	 * @param bigDecimal Value
	 * @return Literal that has a BigDecimal value.
	 */
	Literal createLiteral(BigDecimal bigDecimal);

	/**
	 * Create a literal typed by xsd:integer.
	 * @param bigInteger Value
	 * @return Literal that has a BigInteger value.
	 */
	Literal createLiteral(BigInteger bigInteger);

	/**
	 * Create a literal typed by xsd:dateTime.
	 * @param value Value
	 * @return Literal that has a TemporalAccessor value.
	 */
	Literal createLiteral(TemporalAccessor value);

	/**
	 * Create a literal typed by xsd:duration.
	 * @param value Value
	 * @return Literal that has a TemporalAmount value.
	 */
	Literal createLiteral(TemporalAmount value);

	/**
	 * Create a literal typed by xsd:dateTime.
	 * @param calendar Value
	 * @return Literal that has a XMLGregorianCalendar value.
	 */
	Literal createLiteral(XMLGregorianCalendar calendar);

	/**
	 * Create a literal typed by xsd:date.
	 * @param date Value
	 * @return Literal that has a Date value.
	 */
	Literal createLiteral(Date date);

	/**
	 * Create a statement with the given subject, predicate and object.
	 * @param subject Resource subject of the statement
	 * @param predicate IRI predicate of the statement
	 * @param object Value object of the statement
	 * @return Statement without context
	 */
	Statement createStatement(Resource subject, IRI predicate, Value object);

	/**
	 *  Create a statement with the given subject, predicate, object and context.
	 * @param subject Resource subject of the statement
	 * @param predicate IRI predicate of the statement
	 * @param object Value object of the statement
	 * @param context Resource context of the statement
	 * @return Statement with context
	 */
	Statement createStatement(Resource subject, IRI predicate, Value object, Resource context);

	/**
	 * Create a triple with the given subject, predicate and object.
	 * @param subject Resource subject of the triple
	 * @param predicate IRI predicate of the triple
	 * @param object Value object of the triple
	 * @return Triple
	 */
	Triple createTriple(Resource subject, IRI predicate, Value object);

}
