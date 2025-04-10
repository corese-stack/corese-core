package fr.inria.corese.core.next.api.base.model.literal;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.literal.CoreDatatype;

/**
 * Enumeration representing XML Schema Datatypes (XSD) to be used as core datatype in literals.
 */
public enum XSD implements CoreDatatype {
    BOOLEAN(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdBoolean.getIRI()),
    INTEGER(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdInteger.getIRI()),
    LONG(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdLong.getIRI()),
    DECIMAL(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdDecimal.getIRI()),
    INT(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdInt.getIRI()),
    SHORT(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdShort.getIRI()),
    BYTE(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdByte.getIRI()),
    FLOAT(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdFloat.getIRI()),
    DOUBLE(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdDouble.getIRI()),
    NON_NEGATIVE_INTEGER(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdNonNegativeInteger.getIRI()),
    NON_POSITIVE_INTEGER(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdNonPositiveInteger.getIRI()),
    UNSIGNED_LONG(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdUnsignedLong.getIRI()),
    UNSIGNED_INT(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdUnsignedInt.getIRI()),
    UNSIGNED_SHORT(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdUnsignedShort.getIRI()),
    UNSIGNED_BYTE(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdUnsignedByte.getIRI()),
    DURATION(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdDuration.getIRI()),
    DAYTIME_DURATION(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdDayTimeDuration.getIRI()),
    YEARMONTH_DURATION(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdYearMonthDuration.getIRI()),
    DATE(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdDate.getIRI()),
    DATETIME(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdDateTime.getIRI()),
    DAY(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdDay.getIRI()),
    MONTH(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdMonth.getIRI()),
    YEAR(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdYear.getIRI()),
    YEARMONTH(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdYearMonth.getIRI()),
    MONTHDAY(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdMonthDay.getIRI()),
    TIME(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdTime.getIRI()),
    HEXBINARY(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdHexBinary.getIRI()),
    BASE64BINARY(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdBase64Binary.getIRI()),
    ANYURI(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdAnyURI.getIRI()),
    STRING(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdString.getIRI()),
    NORMALIZEDSTRING(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdNormalizedString.getIRI()),
    TOKEN(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdToken.getIRI()),
    LANGUAGE(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdLanguage.getIRI()),
    NAME(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdName.getIRI()),
    NCNAME(fr.inria.corese.core.next.api.base.vocabulary.XSD.xsdNCName.getIRI()),
    ;
    private final IRI iri;

    XSD(IRI iri) {
        this.iri = iri;
    }

    @Override
    public IRI getIRI() {
        return iri;
    }
}
