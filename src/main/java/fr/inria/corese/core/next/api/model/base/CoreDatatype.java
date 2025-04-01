package fr.inria.corese.core.next.api.model.base;

import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.model.vocabulary.XSD;
import fr.inria.corese.core.sparql.api.IDatatype;

/**
 * Represents a literal datatype.
 * It is necessary to declare a datatype as implementing CoreDatatype to implement operations specific to it in the Corese engine.
 */
public interface CoreDatatype {

    CoreDatatype NONE = DefaultDatatype.NONE;

    IRI getIRI();

    static CoreDatatype from(IRI datatype) {
        if (datatype == null) {
            return CoreDatatype.NONE;
        }
        return CoreDatatypeHelper.getDatatypeMap().getOrDefault(datatype, CoreDatatype.NONE);
    }

    enum XSD implements CoreDatatype {
        /**
         * true, false
         */
        BOOLEAN(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdBoolean.getIRI()),

        /**
         * Arbitrary-size integer numbers
         */
        INTEGER(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdInteger.getIRI()),

        /**
         * -9223372036854775808…+9223372036854775807 (64 bit)
         */
        LONG(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdLong.getIRI()),

        /**
         * Arbitrary-precision decimal numbers
         */
        DECIMAL(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdDecimal.getIRI()),

        /**
         * -2147483648…+2147483647 (32 bit)
         */
        INT(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdInt.getIRI()),

        /**
         * -32768…+32767 (16 bit)
         */
        SHORT(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdShort.getIRI()),

        /**
         * -128…+127 (8 bit)
         */
        BYTE(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdByte.getIRI()),

        /**
         * 32-bit floating point numbers incl. ±Inf, ±0, NaN
         */
        FLOAT(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdFloat.getIRI()),

        /**
         * 64-bit floating point numbers incl. ±Inf, ±0, NaN
         */
        DOUBLE(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdDouble.getIRI()),

        /**
         * Integer numbers ≥0
         */
        NON_NEGATIVE_INTEGER(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdNonNegativeInteger.getIRI()),

        /**
         * Integer numbers ≤0
         */
        NON_POSITIVE_INTEGER(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdNonPositiveInteger.getIRI()),

        /**
         * 0…18446744073709551615 (64 bit)
         */
        UNSIGNED_LONG(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdUnsignedLong.getIRI()),

        /**
         * 0…4294967295 (32 bit)
         */
        UNSIGNED_INT(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdUnsignedInt.getIRI()),

        /**
         * 0…65535 (16 bit)
         */
        UNSIGNED_SHORT(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdUnsignedShort.getIRI()),

        /**
         * 0…255 (8 bit)
         */
        UNSIGNED_BYTE(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdUnsignedByte.getIRI()),

        /**
         * Duration of time
         */
        DURATION(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdDuration.getIRI()),

        /**
         * Duration of time (days, hours, minutes, seconds only)
         */
        DAYTIME_DURATION(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdDayTimeDuration.getIRI()),

        /**
         * Duration of time (months and years only)
         */
        YEARMONTH_DURATION(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdYearMonthDuration.getIRI()),

        /**
         * Dates (yyyy-mm-dd) with or without timezone
         */
        DATE(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdDate.getIRI()),

        /**
         * Date and time with or without timezone
         */
        DATETIME(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdDateTime.getIRI()),

        /**
         * Gregorian calendar day of the month
         */
        DAY(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdDay.getIRI()),

        /**
         * Gregorian calendar month
         */
        MONTH(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdMonth.getIRI()),

        /**
         * Gregorian calendar year
         */
        YEAR(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdYear.getIRI()),

        /**
         * Gregorian calendar year and month
         */
        YEARMONTH(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdYearMonth.getIRI()),

        /**
         * Gregorian calendar month and day
         */
        MONTHDAY(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdMonthDay.getIRI()),

        /**
         * Times (hh:mm:ss.sss…) with or without timezone
         */
        TIME(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdTime.getIRI()),

        /**
         * Hex-encoded binary data
         */
        HEXBINARY(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdHexBinary.getIRI()),

        /**
         * Base64-encoded binary data
         */
        BASE64BINARY(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdBase64Binary.getIRI()),

        /**
         * Resolved or relative URI and IRI references
         */
        ANYURI(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdAnyURI.getIRI()),

        /**
         * Character strings
         */
        STRING(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdString.getIRI()),

        /**
         * Whitespace-normalized strings
         */
        NORMALIZEDSTRING(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdNormalizedString.getIRI()),

        /**
         * Tokenized strings
         */
        TOKEN(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdToken.getIRI()),

        /**
         * Language tags per <a href="https://www.rfc-editor.org/rfc/rfc5646">rfc5646</>
         */
        LANGUAGE(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdLanguage.getIRI()),

        /**
         * XML Names
         */
        NAME(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdName.getIRI()),

        /**
         * XML NCNames
         */
        NCNAME(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdNCName.getIRI()),
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

    enum RDF implements CoreDatatype {
        LANGSTRING(fr.inria.corese.core.next.api.model.vocabulary.RDF.langString.getIRI()),
        HTML(fr.inria.corese.core.next.api.model.vocabulary.RDF.HTML.getIRI()),
        JSON(fr.inria.corese.core.next.api.model.vocabulary.RDF.JSON.getIRI()),
        XML_LITERAL(fr.inria.corese.core.next.api.model.vocabulary.RDF.XMLLiteral.getIRI()),
        ;
        IRI iri;
        RDF(IRI iri) {
            this.iri = iri;
        }

        @Override
        public IRI getIRI() {
            return iri;
        }
    }
}

enum DefaultDatatype implements CoreDatatype {
    NONE();

    private DefaultDatatype() {
    }

    @Override
    public IRI getIRI() {
        return null;
    }
}

