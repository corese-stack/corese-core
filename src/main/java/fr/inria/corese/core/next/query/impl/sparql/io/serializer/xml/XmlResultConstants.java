package fr.inria.corese.core.next.query.impl.sparql.io.serializer.xml;

final class XmlResultConstants {

    /**
     * Forbids instantiation
     */
    private XmlResultConstants() {}

    public static final String SPARQL_RESULT_NS = "http://www.w3.org/2005/sparql-results#";
    public static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";
    public static final String SPARQL_QNAME = "sparql";
    public static final String HEAD_QNAME = "head";
    public static final String RESULTS_QNAME = "results";
    public static final String RESULT_QNAME = "result";
    public static final String BOOLEAN_QNAME = "boolean";
    public static final String VARIABLE_QNAME = "variable";
    public static final String BINDING_QNAME = "binding";
    public static final String URI_QNAME = "uri";
    public static final String LITERAL_QNAME = "literal";
    public static final String BNODE_QNAME = "bnode";
    public static final String LINK_QNAME = "link";
    public static final String NAME_ATTR = "name";
    public static final String LANG_ATTR = "xml:lang";
    public static final String DATATYPE_ATTR = "datatype";
    public static final String HREF_ATTR = "href";
    public static final String SCHEMA_LOCATION_ATTR = "xsi:schemaLocation";

    public static final String YES_PROPERTY_VALUE = "yes";
    public static final String NO_PROPERTY_VALUE = "no";
    public static final String XML_PROPERTY_VALUE = "xml";
    public static final String HTML_PROPERTY_VALUE = "html";
    public static final String TEXT_PROPERTY_VALUE = "text";
    public static final String SPARQL_RESULT_SCHEMA_VALUE = "http://www.w3.org/2007/SPARQL/result.xsd";
}
