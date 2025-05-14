/**
 * Enumeration of the XML Schema Datatypes (XSD) to be used as core datatype in literals.
 */
package fr.inria.corese.core.next.impl.common.literal;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.literal.CoreDatatype;

public enum XSD implements CoreDatatype {
    /**
     * true, false
     */
    BOOLEAN(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdBoolean.getIRI()),

    /**
     * Arbitrary-size integer numbers
     */
    INTEGER(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdInteger.getIRI()),

    /**
     * -9223372036854775808…+9223372036854775807 (64 bit)
     */
    LONG(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdLong.getIRI()),

    /**
     * Arbitrary-precision decimal numbers
     */
    DECIMAL(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdDecimal.getIRI()),

    /**
     * -2147483648…+2147483647 (32 bit)
     */
    INT(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdInt.getIRI()),

    /**
     * -32768…+32767 (16 bit)
     */
    SHORT(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdShort.getIRI()),

    /**
     * -128…+127 (8 bit)
     */
    BYTE(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdByte.getIRI()),

    /**
     * 32-bit floating point numbers incl. ±Inf, ±0, NaN
     */
    FLOAT(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdFloat.getIRI()),

    /**
     * 64-bit floating point numbers incl. ±Inf, ±0, NaN
     */
    DOUBLE(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdDouble.getIRI()),

    /**
     * Integer numbers >0
     */
    POSITIVE_INTEGER(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdPositiveInteger.getIRI()),

    /**
     * Integer numbers &lt;0
     */
    NEGATIVE_INTEGER(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdNegativeInteger.getIRI()),

    /**
     * Integer numbers ≥0
     */
    NON_NEGATIVE_INTEGER(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdNonNegativeInteger.getIRI()),

    /**
     * Integer numbers ≤0
     */
    NON_POSITIVE_INTEGER(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdNonPositiveInteger.getIRI()),

    /**
     * 0…18446744073709551615 (64 bit)
     */
    UNSIGNED_LONG(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdUnsignedLong.getIRI()),

    /**
     * 0…4294967295 (32 bit)
     */
    UNSIGNED_INT(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdUnsignedInt.getIRI()),

    /**
     * 0…65535 (16 bit)
     */
    UNSIGNED_SHORT(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdUnsignedShort.getIRI()),

    /**
     * 0…255 (8 bit)
     */
    UNSIGNED_BYTE(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdUnsignedByte.getIRI()),

    /**
     * Duration of time
     */
    DURATION(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdDuration.getIRI()),

    /**
     * Duration of time (days, hours, minutes, seconds only)
     */
    DAYTIME_DURATION(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdDayTimeDuration.getIRI()),

    /**
     * Duration of time (months and years only)
     */
    YEARMONTH_DURATION(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdYearMonthDuration.getIRI()),

    /**
     * Dates (yyyy-mm-dd) with or without timezone
     */
    DATE(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdDate.getIRI()),

    /**
     * Date and time with or without timezone
     */
    DATETIME(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdDateTime.getIRI()),

    /**
     * Gregorian calendar day of the month
     */
    DAY(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdDay.getIRI()),

    /**
     * Gregorian calendar month
     */
    MONTH(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdMonth.getIRI()),

    /**
     * Gregorian calendar year
     */
    YEAR(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdYear.getIRI()),

    /**
     * Gregorian calendar year and month
     */
    YEARMONTH(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdYearMonth.getIRI()),

    /**
     * Gregorian calendar month and day
     */
    MONTHDAY(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdMonthDay.getIRI()),

    /**
     * Times (hh:mm:ss.sss…) with or without timezone
     */
    TIME(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdTime.getIRI()),

    /**
     * Hex-encoded binary data
     */
    HEXBINARY(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdHexBinary.getIRI()),

    /**
     * Base64-encoded binary data
     */
    BASE64BINARY(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdBase64Binary.getIRI()),

    /**
     * Resolved or relative URI and IRI references
     */
    ANYURI(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdAnyURI.getIRI()),

    /**
     * Character strings
     */
    STRING(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdString.getIRI()),

    /**
     * Whitespace-normalized strings
     */
    NORMALIZEDSTRING(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdNormalizedString.getIRI()),

    /**
     * Tokenized strings
     */
    TOKEN(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdToken.getIRI()),

    /**
     * Language tags per &lt;a href="https://www.rfc-editor.org/rfc/rfc5646">rfc5646&lt;/>
     */
    LANGUAGE(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdLanguage.getIRI()),

    /**
     * XML Names
     */
    NAME(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdName.getIRI()),

    /**
     * XML NCNames
     */
    NCNAME(fr.inria.corese.core.next.impl.common.vocabulary.XSD.xsdNCName.getIRI()),
    ;
    IRI iri;

    XSD(IRI iri) {
        this.iri = iri;
    }

    @Override
    public IRI getIRI() {
        return iri;
    }
}
