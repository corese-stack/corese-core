package fr.inria.corese.core.next.api.model.base;

import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.model.vocabulary.XSD;
import fr.inria.corese.core.sparql.api.IDatatype;

/**
 * Represents a literal datatype.
 * It is necessary to declare a datatype as implementing CoreDatatype to implement operations specific to it in the Corese engine.
 */
public interface CoreDatatype {

    IRI getIRI();

    enum XSD implements CoreDatatype {
        BOOLEAN(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdBoolean.getIRI()),
        INTEGER(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdInteger.getIRI()),
        LONG(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdLong.getIRI()),
        DECIMAL(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdDecimal.getIRI()),
        INT(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdInt.getIRI()),
        SHORT(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdShort.getIRI()),
        BYTE(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdByte.getIRI()),
        FLOAT(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdFloat.getIRI()),
        DOUBLE(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdDouble.getIRI()),
        NON_NEGATIVE_INTEGER(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdNonNegativeInteger.getIRI()),
        NON_POSITIVE_INTEGER(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdNonPositiveInteger.getIRI()),
        UNSIGNED_LONG(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdUnsignedLong.getIRI()),
        UNSIGNED_INT(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdUnsignedInt.getIRI()),
        UNSIGNED_SHORT(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdUnsignedShort.getIRI()),
        UNSIGNED_BYTE(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdUnsignedByte.getIRI()),
        DURATION(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdDuration.getIRI()),
        DAYTIME_DURATION(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdDayTimeDuration.getIRI()),
        YEARMONTH_DURATION(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdYearMonthDuration.getIRI()),
        DATE(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdDate.getIRI()),
        DATETIME(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdDateTime.getIRI()),
        DAY(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdDay.getIRI()),
        MONTH(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdMonth.getIRI()),
        YEAR(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdYear.getIRI()),
        YEARMONTH(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdYearMonth.getIRI()),
        MONTHDAY(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdMonthDay.getIRI()),
        TIME(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdTime.getIRI()),
        HEXBINARY(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdHexBinary.getIRI()),
        BASE64BINARY(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdBase64Binary.getIRI()),
        ANYURI(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdAnyURI.getIRI()),
        STRING(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdString.getIRI()),
        NORMALIZEDSTRING(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdNormalizedString.getIRI()),
        TOKEN(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdToken.getIRI()),
        LANGUAGE(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdLanguage.getIRI()),
        NAME(fr.inria.corese.core.next.api.model.vocabulary.XSD.xsdName.getIRI()),
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

