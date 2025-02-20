package fr.inria.corese.core.next.api.model;

import fr.inria.corese.core.next.api.model.base.CoreDatatype;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.DateTimeException;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Optional;

public interface Literal extends Value {

	@Override
	default boolean isLiteral() {
		return true;
	}

	String getLabel();

	Optional<String> getLanguage();

	IRI getDatatype();

	boolean booleanValue();

	byte byteValue();

	short shortValue();

	int intValue();

	long longValue();

	BigInteger integerValue();

	BigDecimal decimalValue();

	float floatValue();

	double doubleValue();

	default TemporalAccessor temporalAccessorValue() throws DateTimeException {
		throw new UnsupportedOperationException();
	}

	default TemporalAmount temporalAmountValue() throws DateTimeException {
		throw new UnsupportedOperationException();
	}

	XMLGregorianCalendar calendarValue();

	CoreDatatype getCoreDatatype();

	@Override
	boolean equals(Object other);

	@Override
	int hashCode();

}
