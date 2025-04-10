package fr.inria.corese.core.next.api.base.vocabulary;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.impl.basic.BasicIRI;

/**
 * Defines the XSD vocabulary.
 * Because a lot of the datatype names are also Java keywords, the names are exceptions to the naming convention.
 */
public enum XSD implements Vocabulary {

    xsdBoolean("boolean"),
    xsdInteger("integer"),
    xsdLong("long"),
    xsdDecimal("decimal"),
    xsdInt("int"),
    xsdShort("short"),
    xsdByte("byte"),
    xsdFloat("float"),
    xsdDouble("double"),
    xsdNonNegativeInteger("nonNegativeInteger"),
    xsdNonPositiveInteger("nonPositiveInteger"),
    xsdPositiveInteger("positiveInteger"),
    xsdNegativeInteger("negativeInteger"),
    xsdUnsignedLong("unsignedLong"),
    xsdUnsignedInt("unsignedInt"),
    xsdUnsignedShort("unsignedShort"),
    xsdUnsignedByte("unsignedByte"),
    xsdDuration("duration"),
    xsdDayTimeDuration("dayTimeDuration"),
    xsdYearMonthDuration("yearMonthDuration"),
    xsdDate("date"),
    xsdDateTime("dateTime"),
    xsdDay("gDay"),
    xsdMonth("gMonth"),
    xsdYear("gYear"),
    xsdYearMonth("gYearMonth"),
    xsdMonthDay("gMonthDay"),
    xsdTime("time"),
    xsdHexBinary("hexBinary"),
    xsdBase64Binary("base64Binary"),
    xsdAnyURI("anyURI"),
    xsdString("string"),
    xsdNormalizedString("normalizedString"),
    xsdToken("token"),
    xsdLanguage("language"),
    xsdName("Name"),
    xsdNCName("NCName");

    private IRI iri;

    XSD(String localName) {
        this.iri = new BasicIRI(getNamespace() + localName);
    }

    @Override
    public IRI getIRI() {
        return this.iri;
    }

    @Override
    public String getNamespace() {
        return "http://www.w3.org/2001/XMLSchema#";
    }

    @Override
    public String getPreferredPrefix() {
        return "xsd";
    }

}
