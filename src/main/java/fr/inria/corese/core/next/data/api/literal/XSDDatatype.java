/**
 * Enumeration of the XML Schema Datatypes (XSD) to be used as core datatype in literals.
 */
package fr.inria.corese.core.next.data.api.literal;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.vocabulary.XSD;

public enum XSDDatatype implements CoreDatatype {
    /**
     * true, false
     */
    BOOLEAN(XSD.xsdBoolean.getIRI()),

    /**
     * Arbitrary-size integer numbers
     */
    INTEGER(XSD.xsdInteger.getIRI()),

    /**
     * -9223372036854775808…+9223372036854775807 (64 bit)
     */
    LONG(XSD.xsdLong.getIRI()),

    /**
     * Arbitrary-precision decimal numbers
     */
    DECIMAL(XSD.xsdDecimal.getIRI()),

    /**
     * -2147483648…+2147483647 (32 bit)
     */
    INT(XSD.xsdInt.getIRI()),

    /**
     * -32768…+32767 (16 bit)
     */
    SHORT(XSD.xsdShort.getIRI()),

    /**
     * -128…+127 (8 bit)
     */
    BYTE(XSD.xsdByte.getIRI()),

    /**
     * 32-bit floating point numbers incl. ±Inf, ±0, NaN
     */
    FLOAT(XSD.xsdFloat.getIRI()),

    /**
     * 64-bit floating point numbers incl. ±Inf, ±0, NaN
     */
    DOUBLE(XSD.xsdDouble.getIRI()),

    /**
     * Integer numbers >0
     */
    POSITIVE_INTEGER(XSD.xsdPositiveInteger.getIRI()),

    /**
     * Integer numbers &lt;0
     */
    NEGATIVE_INTEGER(XSD.xsdNegativeInteger.getIRI()),

    /**
     * Integer numbers ≥0
     */
    NON_NEGATIVE_INTEGER(XSD.xsdNonNegativeInteger.getIRI()),

    /**
     * Integer numbers ≤0
     */
    NON_POSITIVE_INTEGER(XSD.xsdNonPositiveInteger.getIRI()),

    /**
     * 0…18446744073709551615 (64 bit)
     */
    UNSIGNED_LONG(XSD.xsdUnsignedLong.getIRI()),

    /**
     * 0…4294967295 (32 bit)
     */
    UNSIGNED_INT(XSD.xsdUnsignedInt.getIRI()),

    /**
     * 0…65535 (16 bit)
     */
    UNSIGNED_SHORT(XSD.xsdUnsignedShort.getIRI()),

    /**
     * 0…255 (8 bit)
     */
    UNSIGNED_BYTE(XSD.xsdUnsignedByte.getIRI()),

    /**
     * Duration of time
     */
    DURATION(XSD.xsdDuration.getIRI()),

    /**
     * Duration of time (days, hours, minutes, seconds only)
     */
    DAYTIME_DURATION(XSD.xsdDayTimeDuration.getIRI()),

    /**
     * Duration of time (months and years only)
     */
    YEARMONTH_DURATION(XSD.xsdYearMonthDuration.getIRI()),

    /**
     * Dates (yyyy-mm-dd) with or without timezone
     */
    DATE(XSD.xsdDate.getIRI()),

    /**
     * Date and time with or without timezone
     */
    DATETIME(XSD.xsdDateTime.getIRI()),

    /**
     * Gregorian calendar day of the month
     */
    DAY(XSD.xsdDay.getIRI()),

    /**
     * Gregorian calendar month
     */
    MONTH(XSD.xsdMonth.getIRI()),

    /**
     * Gregorian calendar year
     */
    YEAR(XSD.xsdYear.getIRI()),

    /**
     * Gregorian calendar year and month
     */
    YEARMONTH(XSD.xsdYearMonth.getIRI()),

    /**
     * Gregorian calendar month and day
     */
    MONTHDAY(XSD.xsdMonthDay.getIRI()),

    /**
     * Times (hh:mm:ss.sss…) with or without timezone
     */
    TIME(XSD.xsdTime.getIRI()),

    /**
     * Hex-encoded binary data
     */
    HEXBINARY(XSD.xsdHexBinary.getIRI()),

    /**
     * Base64-encoded binary data
     */
    BASE64BINARY(XSD.xsdBase64Binary.getIRI()),

    /**
     * Resolved or relative URI and IRI references
     */
    ANYURI(XSD.xsdAnyURI.getIRI()),

    /**
     * Character strings
     */
    STRING(XSD.xsdString.getIRI()),

    /**
     * Whitespace-normalized strings
     */
    NORMALIZEDSTRING(XSD.xsdNormalizedString.getIRI()),

    /**
     * Tokenized strings
     */
    TOKEN(XSD.xsdToken.getIRI()),

    /**
     * Language tags per &lt;a href="<a href="https://www.rfc-editor.org/rfc/rfc5646">...</a>">rfc5646&lt;/>
     */
    LANGUAGE(XSD.xsdLanguage.getIRI()),

    /**
     * XML Names
     */
    NAME(XSD.xsdName.getIRI()),

    /**
     * XML NCNames
     */
    NCNAME(XSD.xsdNCName.getIRI()),
    ;
    private final IRI iri;

    XSDDatatype(IRI iri) {
        this.iri = iri;
    }

    @Override
    public IRI getIRI() {
        return iri;
    }
}
