package fr.inria.corese.core.next.query.impl.sparql.io.serializer.json;

/**
 * Constants used in the serialization of SPARQL results to JSON
 */
public class JSONSerializerConstants {

    /**
     * Forbids instantiation
     */
    private JSONSerializerConstants() {}

    public static final String HEAD = "head";
    public static final String VARS = "vars";
    public static final String LINK = "link";
    public static final String RESULTS = "results";
    public static final String BINDINGS = "bindings";
    public static final String BOOLEAN = "boolean";
    public static final String TYPE = "type";
    public static final String VALUE = "value";
    public static final String URI = "uri";
    public static final String LITERAL = "literal";
    public static final String BNODE = "bnode";
    public static final String LANG = "xml:lang";
    public static final String DATATYPE = "datatype";
}
