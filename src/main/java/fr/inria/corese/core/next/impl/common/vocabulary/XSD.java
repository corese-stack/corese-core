package fr.inria.corese.core.next.impl.common.vocabulary;

import fr.inria.corese.core.next.api.IRI ;
import fr.inria.corese.core.next.impl.common.BasicIRI;

/**
 * Defines the XSD vocabulary.
 *
 * Because a lot of the datatype names are also Java keywords, the names are exceptions to the naming convention.
 */
public enum XSD implements Vocabulary {

    /**
     * @see <a href="https://www.w3.org/TR/xmlschema-2/#boolean">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdBoolean("boolean"),

    /**
     * @see <a href="https://www.w3.org/TR/xmlschema-2/#byte">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdInteger("integer"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#nonNegativeInteger">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdNonNegativeInteger("nonNegativeInteger"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#nonPositiveInteger">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdNonPositiveInteger("nonPositiveInteger"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#positiveInteger">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdPositiveInteger("positiveInteger"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#negativeInteger">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdNegativeInteger("negativeInteger"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#int">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdInt("int"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#unsignedInt">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdUnsignedInt("unsignedInt"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#long">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdLong("long"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#unsignedLong">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdUnsignedLong("unsignedLong"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#short">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdDecimal("decimal"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#short">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdShort("short"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#unsignedShort">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdUnsignedShort("unsignedShort"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#byte">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdByte("byte"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#unsignedByte">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdUnsignedByte("unsignedByte"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#float">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdFloat("float"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#double">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdDouble("double"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#duration">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdDuration("duration"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#dayTimeDuration">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdDayTimeDuration("dayTimeDuration"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#yearMonthDuration">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdYearMonthDuration("yearMonthDuration"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#date">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdDate("date"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#dateTime">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdDateTime("dateTime"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#gDay">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdDay("gDay"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#gMonth">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdMonth("gMonth"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#gYear">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdYear("gYear"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#gYearMonth">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdYearMonth("gYearMonth"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#gMonthDay">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdMonthDay("gMonthDay"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#time">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdTime("time"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#hexBinary">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdHexBinary("hexBinary"),

    /**
     *  <a href="https://www.w3.org/TR/xmlschema-2/#base64Binary">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdBase64Binary("base64Binary"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#anyURI">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdAnyURI("anyURI"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#QName">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdString("string"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#QName">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdNormalizedString("normalizedString"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#QName">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdToken("token"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#QName">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdLanguage("language"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#Name">XML Schema Part 2: Datatypes Second Edition</a>
     */
    xsdName("Name"),

    /**
     * <a href="https://www.w3.org/TR/xmlschema-2/#QName">XML Schema Part 2: Datatypes Second Edition</a>
     */
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
