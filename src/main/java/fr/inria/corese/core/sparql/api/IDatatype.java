package fr.inria.corese.core.sparql.api;

import fr.inria.corese.core.kgram.api.core.*;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.exceptions.CoreseDatatypeException;
import fr.inria.corese.core.sparql.storage.api.IStorage;
import fr.inria.corese.core.sparql.triple.parser.NSManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Interface for Corese XSD datatypes
 *
 * @author Olivier Corby & Olivier Savoie & Virginie Bottollier
 */
public interface IDatatype
        extends Iterable<IDatatype>, Node, Loopable, DatatypeValue, Comparable {

    int VALUE = -1;
    int RESULT = -2;

    // use case: cast
    enum Datatype {
        UNDEFINED,
        LITERAL,
        STRING,
        XMLLITERAL,
        NUMBER,
        DATE,
        BOOLEAN,
        STRINGABLE,
        URI,
        UNDEF,
        BLANK,
        DOUBLE,
        FLOAT,
        DECIMAL,
        LONG,
        INTEGER,
        URI_LITERAL,
        TRIPLE,
        // Pseudo codes (target is Integer or String ...)
        DAY,
        MONTH,
        YEAR,
        DURATION,
        DATETIME,
        GENERIC_INTEGER
    }
    String KGRAM = ExpType.KGRAM;
    String RULE = KGRAM + "Rule";
    String QUERY = KGRAM + "Query";
    String GRAPH = KGRAM + "Graph";
    String MAPPINGS = KGRAM + "Mappings";
    String ENTITY_DATATYPE = ExpType.DT + "entity";
    String RESOURCE_DATATYPE = ExpType.DT + "resource";
    String URI_DATATYPE = ExpType.DT + "uri";
    String BNODE_DATATYPE = ExpType.DT + "bnode";
    String LITERAL_DATATYPE = ExpType.DT + "literal";
    String STANDARD_DATATYPE = ExpType.DT + "standard";
    String EXTENDED_DATATYPE = ExpType.DT + "extended";
    String ERROR_DATATYPE = ExpType.DT + "error";
    String GRAPH_DATATYPE = ExpType.DT + "graph";    // same as PointerType.GRAPH.name;
    String MAPPINGS_DATATYPE = ExpType.DT + "mappings"; // same as PointerType.MAPPINGS.name;
    String MAPPING_DATATYPE = ExpType.DT + "mapping"; // same as PointerType.MAPPING.name;
    String ITERATE_DATATYPE = ExpType.DT + "iterate";
    String MAP_DATATYPE = ExpType.DT + "map";
    String LIST_DATATYPE = ExpType.DT + "list";
    String JSON_DATATYPE = ExpType.DT + "json";
    String XML_DATATYPE = ExpType.DT + "xml";
    String SYSTEM = ExpType.DT + "system";

    IDatatype copy();

    boolean isSkolem();

    boolean isXMLLiteral();

    @Override
    boolean isUndefined();

    boolean isGeneralized(); // isExtension or isUndefined

    boolean isList();

    boolean isMap();

    boolean isJSON();

    default boolean isXML() {
        return false;
    }

    boolean isLoop();

    default boolean isDateElement() {
        return false;
    }

    List<IDatatype> getValues();

    default IDatatype keys() {
        return DatatypeMap.newList();
    }

    @Override
    List<IDatatype> getValueList();

    @Override
    IDatatype getValue(String varString, int n);

    IDatatype toList();

    IDatatypeList getList();

    default Map<IDatatype, IDatatype> getMap() {
        return null;
    }

    default IDatatype member(IDatatype elem) {
        return null;
    }

    @Override
    Iterable getLoop();

    IDatatype has(IDatatype dt);

    IDatatype get(int n);

    IDatatype get(IDatatype name);

    // json is newInstance, map is newResource
    default IDatatype get(String name) {
        if (name == null) {
            return null;
        }
        return get(DatatypeMap.newResource(name));
    }

    // xml to json
    default IDatatype json() {
        return null;
    }

    // json pointer
    default IDatatype path(IDatatype list) {
        return null;
    }

    default IDatatype path(IDatatype list, int n) {
        return null;
    }

    IDatatype set(IDatatype name, IDatatype value);

    /**
     * Utilitary functions
     */
    default IDatatype set(String name, Object value) {
        if (value == null) {
            return null;
        }
        return set(name, value.toString());
    }

    /**
     * @todo CoreseMap set(name, value) use newResource(name)
     * whereas here we use key/newInstance(name)
     * ServiceReport make the assumption that it is key/newInstance
     */
    default IDatatype set(String name, IDatatype value) {
        if (value == null) {
            return null;
        }
        return set(DatatypeMap.key(name), value);
    }

    default IDatatype set(String name, Date value) {
        if (value == null) {
            return null;
        }
        return set(DatatypeMap.key(name), DatatypeMap.newInstance(value));
    }

    default IDatatype set(String name, String value) {
        if (value == null) {
            return null;
        }
        return set(DatatypeMap.key(name), DatatypeMap.newInstance(value));
    }

    default IDatatype set(String name, int value) {
        return set(DatatypeMap.key(name), DatatypeMap.newInstance(value));
    }

    default IDatatype set(String name, double value) {
        return set(DatatypeMap.key(name), DatatypeMap.newInstance(value));
    }

    default IDatatype set(String name, boolean value) {
        return set(DatatypeMap.key(name), DatatypeMap.newInstance(value));
    }

    /**
     * this datatype: iterable of json (or map)
     *
     * @param keys: iterable of key
     * @return list of (key_i (val_i1 .. val_in))
     */
    default IDatatype iterate(IDatatype keys) {
        ArrayList<IDatatype> list = new ArrayList<>();

        for (IDatatype key : keys) {
            ArrayList<IDatatype> alist = new ArrayList<>();

            for (IDatatype report : this) {
                IDatatype dt = report.get(key);
                if (dt != null) {
                    alist.add(dt);
                }
            }

            if (!alist.isEmpty()) {
                list.add(DatatypeMap.newList(key, DatatypeMap.newList(alist)));
            }
        }
        return DatatypeMap.newList(list);
    }

    /**
     * Use case: complete ServiceReport
     */
    default IDatatype complete(String key, IDatatype value) {
        if (isList()) {
            for (IDatatype dt : this) {
                dt.set(key, value);
            }
        } else {
            set(key, value);
        }
        return this;
    }

    @Override
    int size();

    IDatatype length();

    @Override
    boolean isBlank();

    @Override
    boolean isLiteral();

    @Override
    boolean isURI();

    boolean conform(IDatatype dt);

    IDatatype isWellFormed();

    IDatatype isBlankNode();

    IDatatype isLiteralNode();

    IDatatype isURINode();

    @Override
    boolean isFuture();

    boolean isPointer();

    @Override
    boolean isExtension();

    @Override
    PointerType pointerType();

    @Override
    Pointerable getPointerObject();

    default void setPointerObject(Pointerable o) {
    }

    @Override
    default Edge getEdge() {
        if (getPointerObject() != null &&
                getPointerObject().getEdge() != null) {
            return getPointerObject().getEdge();
        }
        return null;
    }

    @Override
    default void setEdge(Edge e) {
        setPointerObject(e);
    }

    @Override
    default boolean isTriple() {
        return false;
    }

    default void setTriple(boolean b) {
    }

    // triple reference with edge inside
    @Override
    default boolean isTripleWithEdge() {
        return isTriple() && getEdge() != null;
    }

    void setTripleStore(TripleStore store);

    /**
     * Compare 2 datatypes
     *
     * @param dt another datatype
     * @return 0 if they are equals, an int > 0 if the datatype is greater than
     * dt2, an int &lt; 0 if the datatype is lesser
     */
    int compareTo(IDatatype dt);

    int compareTriple(IDatatype dt) throws CoreseDatatypeException;

    // for TreeMap
    int mapCompareTo(IDatatype dt);

    // compare values (e.g. for numbers)
    int compare(IDatatype dt) throws CoreseDatatypeException;

    /**
     * Cast a value
     *
     * @param datatype ex: xsd:integer
     * @return the datatype casted
     */
    IDatatype cast(IDatatype datatype);

    IDatatype cast(String datatype);

    /**
     * @return the lang as a datatype
     */
    IDatatype getDataLang();

    /**
     * @return the Sparql form of the datatype
     */
    String toSparql();

    String toSparql(boolean prefix);

    String toSparql(boolean prefix, boolean xsd);

    String toSparql(boolean prefix, boolean xsd, NSManager nsm);

    String toSparql(boolean prefix, boolean xsd, boolean skipUndefPrefix, NSManager nsm);

    default String trace() {
        return String.format("trace: %s code: %s datatype: %s label: %s",
                this,
                getCode(),
                getDatatypeURI(),
                getLabel());
    }

    // Used by XMLLiteral to store a XML DOM
    @Override
    void setObject(Object obj);

    @Override
    Object getNodeObject();

    @Deprecated
    IDatatype getPublicDatatypeValue();

    @Deprecated
    IDatatype setPublicDatatypeValue(IDatatype dt);

    String getContent();

    default String pretty() {
        return toString();
    }

    IDatatype display();

    void setVariable(boolean b);

    void setValue(int n);

    void setValue(BigDecimal n);

    /**
     * test if this.getLowerCaseLabel() contains iod.getLowerCaseLabel()
     *
     * @param iod the instance to be tested with
     * @return this.getLowerCaseLabel() contains iod.getLowerCaseLabel()
     */
    boolean contains(IDatatype iod);

    /*
     * ************************************************************************
     */

    /**
     * test if this.getLowerCaseLabel() starts with iod.getLowerCaseLabel()
     *
     * @param iod the instance to be tested with
     * @return this.getLowerCaseLabel() starts with iod.getLowerCaseLabel()
     */
    boolean startsWith(IDatatype iod);

    /**
     * test the equality (by value) between two instances of datatype class
     *
     * @param iod the instance to be tested with this
     * @return true if the param has the same runtime class and if values are
     * equals, else false note: equals correponds to the SPARQL equals, with
     * type checking
     */
    boolean equalsWE(IDatatype iod) throws CoreseDatatypeException;

    /**
     * test the equality (by value) between two instances of datatype class
     *
     * @param iod the instance to be tested with this
     * @return true if the param has the same runtime class and if values are
     * equals, else false
     */
    boolean sameTerm(IDatatype iod);

    /**
     * @param iod
     * @return iod.getValue() &lt; this.getValue() @throws Core seDatatypeException
     */
    boolean less(IDatatype iod) throws CoreseDatatypeException;

    /**
     * @param iod
     * @return iod.getValue() &lt;= to this.getValue() @throws CoreseDa
     * tatypeException
     */
    boolean lessOrEqual(IDatatype iod)
            throws CoreseDatatypeException;

    /**
     * @param iod
     * @return iod.getValue() > this.getValue()
     * @throws CoreseDatatypeException
     */
    boolean greater(IDatatype iod) throws CoreseDatatypeException;

    /**
     * @param iod
     * @return iod.getValue() >= to this.getValue()
     * @throws CoreseDatatypeException
     */
    boolean greaterOrEqual(IDatatype iod)
            throws CoreseDatatypeException;

    IDatatype eq(IDatatype dt);

    IDatatype ne(IDatatype dt);

    IDatatype ge(IDatatype dt);

    IDatatype gt(IDatatype dt);

    IDatatype lt(IDatatype dt);

    IDatatype le(IDatatype dt);

    IDatatype neq(IDatatype dt);

    IDatatype plus(IDatatype dt);

    IDatatype minus(IDatatype dt);

    IDatatype mult(IDatatype dt);

    IDatatype divis(IDatatype dt);

    IDatatype div(IDatatype dt);

    IDatatype minus(long val);

    IDatatype getDatatype();

    /*
     * ************************************************************************
     */

    void setDatatype(String uri);

    // IDatatype value of Pointer Object (eg XML TEXT Node as xsd:string)
    IDatatype getObjectDatatypeValue();

    /**
     * @return the lang of this ('fr', 'en',...)
     */
    @Override
    String getLang();

    void setLang(String str);

    /**
     * @return the datatype of this as a URI
     */
    @Override
    String getDatatypeURI();

    /**
     * @return the string depending on the datatype
     * <br>representing the value of this
     */
    @Override
    String getLabel();

    // <<s p o>>
    String getPrettyLabel();

    String getID();

    StringBuilder getStringBuilder();

    void setStringBuilder(StringBuilder s);

    /**
     * @return true if this instance class is a number
     */
    @Override
    boolean isNumber();

    boolean isDecimalInteger();

    // exact datatype xsd:integer
    default boolean isXSDInteger() {
        return false;
    }

    boolean isDate();

    @Override
    boolean isBoolean();

    /*
     * ************************************************
     */

    Class getJavaClass();

    @Deprecated
    String getNormalizedLabel();

    @Deprecated
    String getLowerCaseLabel();

    Datatype getCode();

    default NodeKind getNodeKind() {
        return NodeKind.UNDEF;
    }

    boolean hasLang();

    boolean isTrue() throws CoreseDatatypeException;

    default boolean isTrueTest() {
        try {
            return isTrue();
        } catch (CoreseDatatypeException e) {
            return false;
        }
    }

    boolean isTrueAble();

    void setValue(String str) throws CoreseDatatypeException;

    void setValue(String str, int id, IStorage pmgr);

    void setValue(IDatatype dt);

    default IDatatype duplicate() {
        return this;
    }

    default IDatatype dispatch(String name) {
        return this;
    }

    enum NodeKind {
        URI(0), BNODE(1), TRIPLE(2), LITERAL(3), UNDEF(4);
        int index;

        NodeKind(int n) {
            index = n;
        }

        public static int size() {
            return NodeKind.values().length;
        }

        public int getIndex() {
            return index;
        }
    }

}
