package fr.inria.corese.core.next.api.model;

import fr.inria.corese.core.next.api.model.base.CoreDatatype;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.DateTimeException;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Optional;

/**
 * Represents a literal value in RDF.
 * A literal is composed of three elements: a lexical value, a datatype IRI and a CoreDatatype.
 * The lexical value is a string that represents the value of the literal.
 * The datatype IRI is an IRI that represents the datatype of the literal that can be any IRI.
 * The CoreDatatype must be an implementation of the CoreDatatype interface that will direct the treatment of the literal by the library. It is based on an IRI and can have the same lexical value as the datatype IRI.
 * @see <a href="https://www.w3.org/TR/rdf11-concepts/#section-Graph-Literal">RDF 1.1 Concepts and Abstract Syntax</a>
 */
public interface Literal extends Value {

	@Override
	default boolean isLiteral() {
		return true;
	}

	/**
	 * @return the lexical value of the literal
	 */
	String getLabel();

	/**
	 * @return the language tag of the literal if it exists, empty otherwise. The language tag is a string that represents the language of the literal.
	 */
	Optional<String> getLanguage();

	/**
	 * @return the datatype IRI of the literal
	 */
	IRI getDatatype();

	/**
	 * @return the value of the literal as a boolean if possible
	 */
	boolean booleanValue();

	/**
	 * @return the value of the literal as a byte if possible
	 */
	byte byteValue();

	/**
	 * @return the value of the literal as a short if possible
	 */
	short shortValue();

	/**
	 * @return the value of the literal as an int if possible
	 */
	int intValue();

	/**
	 * @return the value of the literal as a long if possible
	 */
	long longValue();

	/**
	 * @return the value of the literal as a BigInteger if possible
	 */
	BigInteger integerValue();

	/**
	 * @return the value of the literal as a BigDecimal if possible
	 */
	BigDecimal decimalValue();

	/**
	 * @return the value of the literal as a float if possible
	 */
	float floatValue();

	/**
	 * @return the value of the literal as a double if possible
	 */
	double doubleValue();

	/**
	 * @return the value of the literal as a TemporalAccessor if possible
	 */
	TemporalAccessor temporalAccessorValue();

	/**
	 * @return the value of the literal as a TemporalAmount if possible
	 */
	TemporalAmount temporalAmountValue();

	/**
	 * @return the value of the literal as a XMLGregorianCalendar if possible
	 */
	XMLGregorianCalendar calendarValue();

	/**
	 * @return the CoreDatatype of the literal
	 */
	CoreDatatype getCoreDatatype();

	@Override
	boolean equals(Object other);

	@Override
	int hashCode();

}
