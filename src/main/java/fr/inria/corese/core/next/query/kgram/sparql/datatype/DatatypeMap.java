package fr.inria.corese.core.next.query.kgram.sparql.datatype;

import fr.inria.corese.core.next.data.impl.common.vocabulary.RDFS;
import fr.inria.corese.core.next.data.impl.common.vocabulary.XSD;
import fr.inria.corese.core.next.query.kgram.api.core.*;
import fr.inria.corese.core.next.query.kgram.sparql.api.IDatatype;
import fr.inria.corese.core.next.query.kgram.sparql.exceptions.CoreseDatatypeException;
import fr.inria.corese.core.next.query.kgram.sparql.triple.parser.NSManager;
import fr.inria.corese.core.next.query.kgram.sparql.datatype.extension.CoreseList;
import fr.inria.corese.core.next.query.kgram.sparql.datatype.extension.CoreseJSON;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.util.*;

/**
 * <p>
 * Title: Corese</p>
 * <p>
 * Description: A Semantic Search Engine</p>
 * <p>
 * Copyright: Copyright INRIA (c) 2007</p>
 * <p>
 * Company: INRIA</p>
 * <p>
 * Project: Acacia</p>
 * <br>
 * This class is used to map a datatype name to its java type representation
 * ands its marker set.
 * <br>
 *
 * @author Olivier Corby, Olivier Savoie
 */
public class DatatypeMap implements Cst, RDF, DatatypeValueFactory {

    public static final IDatatype ERROR = CoreseUndefLiteral.ERROR;
    public static final IDatatype UNBOUND = CoreseUndefLiteral.UNBOUND;
    public static final IDatatype URI_DATATYPE = newResource(IDatatype.URI_DATATYPE);
    public static final IDatatype BNODE_DATATYPE = newResource(IDatatype.BNODE_DATATYPE);
    public static final IDatatype EMPTY_STRING = newInstance("");
    /**
     * logger from log4j
     */
    private static final Logger logger = LoggerFactory.getLogger(DatatypeMap.class);
    private static final String DEFAULT = "default";
    private static final String NWFL = "NWFL";
    private static final String BLANK = BLANKSEED + "bb";
    private static final int INTMAX = 100;
    // if true, number values are equal by = but not match same sparql variable
    // otherwise same value space, match same sparql variable
    //
    public static boolean SEVERAL_NUMBER_SPACE = true;
    // corese behaviour:
    public static boolean DISPLAY_AS_TRIPLE = true;
    public static CoreseBoolean TRUE = CoreseBoolean.TRUE;
    public static CoreseBoolean FALSE = CoreseBoolean.FALSE;
    // if true: no datatype entailment, literal as string
    public static boolean SPARQLCompliant = false;
    public static boolean DATATYPE_ENTAILMENT = true;
    static long nbObject = 0;
    static DatatypeMap dm;
    static DatatypeFactory factory;
    static long COUNT = 0;
    static IDatatype[] intCache;
    public static final IDatatype ZERO = newInstance(0);
    public static final IDatatype ONE = newInstance(1);
    public static final IDatatype TWO = newInstance(2);
    public static final IDatatype THREE = newInstance(3);
    public static final IDatatype FOUR = newInstance(4);
    public static final IDatatype FIVE = newInstance(5);
    public static final IDatatype SIX = newInstance(6);
    public static final IDatatype SEVEN = newInstance(7);
    public static final IDatatype EIGHT = newInstance(8);
    public static final IDatatype NINE = newInstance(9);
    public static final IDatatype MINUSONE = newInstance(-1);
    static boolean LITERAL_AS_STRING = true;
    private static Hashtable<String, Mapping> ht;
    private static HashMap<String, IDatatype.Datatype> dtCode;

    static {
        intCache = new IDatatype[INTMAX];
        dm = DatatypeMap.create();
        try {
            factory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException ex) {
            logger.error("DatatypeFactory DatatypeConfigurationException");
        }
    }

    public DatatypeMap() {
        if (ht == null) {
            ht = new Hashtable<>();
            dtCode = new HashMap<String, IDatatype.Datatype>();
        }

        init();
    }

    public static DatatypeMap create() {
        return new DatatypeMap();
    }

    public static String getJavaType(String datatype) {
        return dm.getJType(datatype);
    }

    // define specific code for datatype
    // for subtype of integer and long -> GENERIC_INTEGER
    static IDatatype.Datatype getCode(String datatype) {
        IDatatype.Datatype i = dtCode.get(datatype);
        if (i == null) {
            return IDatatype.Datatype.UNDEFINED;
        }
        return i;
    }

    static boolean isNumber(String name) {
        switch (getCode(name)) {
            case INTEGER:
            case DOUBLE:
            case FLOAT:
            case DECIMAL:
            case GENERIC_INTEGER:
                return true;
            default:
                return false;
        }
    }

    /**
     * URI of a literal datatype xsd: rdf:XMLLiteral rdf:PlainLiteral
     */
    public static boolean isDatatype(String range) {
        return range.startsWith(XSD.getVocabularyNamespace())
                || range.equals(fr.inria.corese.core.next.data.impl.common.vocabulary.RDF.XMLLiteral.getIRI().stringValue())
                || range.equals(fr.inria.corese.core.next.data.impl.common.vocabulary.RDF.langString.getIRI().stringValue());
    }

    public static boolean isUndefined(IDatatype dt) {
        return dt.getCode() == IDatatype.Datatype.UNDEF;
    }

    /**
     * @todo: leverage extension and pointer datatype
     * @todo: isTriple()
     */
    public static IDatatype copy(IDatatype dt) {
        if (dt.isBlank()) {
            return createBlank(dt.getLabel());
        } else if (dt.isURI()) {
            return newResource(dt.getLabel());
        } else if (dt.getCode() == IDatatype.Datatype.LITERAL && dt.getLang() == null) {
            return newBasicLiteral(dt.getLabel());
        } else {
            return newInstance(dt.getLabel(), dt.getDatatypeURI(), dt.getLang());
        }
    }

    public static IDatatype cast(Object obj) {
        if (obj instanceof Number) {
            if (obj instanceof Integer) {
                return newInstance((Integer) obj);
            } else if (obj instanceof Float) {
                return newInstance((Float) obj);
            } else if (obj instanceof Double) {
                return newInstance((Double) obj);
            } else if (obj instanceof BigDecimal) {
                return newInstance((BigDecimal) obj);
            } else if (obj instanceof Short) {
                return newInstance((Short) obj);
            } else if (obj instanceof Byte) {
                return newInstance((Byte) obj);
            }
        } else if (obj instanceof Boolean) {
            return newInstance((Boolean) obj);
        } else if (obj instanceof String) {
            return newInstance((String) obj);
        }

        return null;
    }

    public static IDatatype castObject(Object obj) {
        if (obj == null) {
            return null;
        }
        IDatatype dt = cast(obj);
        if (dt != null) {
            return dt;
        }
        return createObject(obj);
    }

    public static IDatatype newInstance(String label, String datatype) {
        return createLiteral(label, datatype, null);
    }

    public static IDatatype newInstance(String label, String datatype, String lang) {
        return createLiteral(label, datatype, lang);
    }

    public static IDatatype newInstance(double result) {
        return new CoreseDouble(result);
    }

    public static IDatatype newInstance(double result, String datatype) {
        switch (getCode(datatype)) {
            case INTEGER:
                return newInstance((int) result);
            case FLOAT:
                return new CoreseFloat(result);
            case DECIMAL:
                return new CoreseDecimal(result);
            case GENERIC_INTEGER:
                return new CoreseGenericInteger((int) result, datatype);
            default:
                return new CoreseDouble(result);
        }
    }

    public static IDatatype newInstance(float result) {
        return new CoreseFloat(result);
    }

    public static IDatatype newInstance(IDatatype.Datatype result) {
        return getValue(result);
    }

    public static IDatatype create(int result) {
        return new CoreseInteger(result);
    }

    public static IDatatype newInstance(long result) {
        return getValue(result);
    }

    public static IDatatype newInstance(BigDecimal result) {
        return new CoreseDecimal(result);
    }

    /**
     * Use case: LDScript Java compiler
     */
    public static IDatatype newLong(long result) {
        return new CoreseGenericInteger(result);
    }

    public static IDatatype newInteger(int result) {
        return getValue(result);
    }

    public static IDatatype newInteger(long result) {
        return getValue(result);
    }

    public static IDatatype newDouble(double result) {
        return newInstance(result);
    }

    public static IDatatype newFloat(float result) {
        return newInstance(result);
    }

    public static IDatatype newFloat(double result) {
        return new CoreseFloat((float) result);
    }

    public static IDatatype newDecimal(double result) {
        return new CoreseDecimal(result);
    }

    public static IDatatype newDecimal(BigDecimal result) {
        return newInstance(result);
    }

    public static IDatatype newDecimal(int result) {
        return newInstance(result);
    }

    public static IDatatype getValue(Object value) {
        if (value instanceof IDatatype) {
            return (IDatatype) value;
        }
        if (value instanceof Node) {
            return  ((Node) value).getDatatypeValue();
        }
        if (value instanceof List valueList) {
            return getValue(valueList);
        }
        IDatatype dt = DatatypeMap.castObject(value);
        return dt;
    }

    static IDatatype getValue(long value) {
        if (value >= 0 && value < INTMAX) {
            return getValueCache((int) value);
        }
        return new CoreseInteger(value);
    }

    static IDatatype getValue(int value) {
        if (value >= 0 && value < INTMAX) {
            return getValueCache(value);
        }
        return new CoreseInteger(value);
    }

    static IDatatype getValueCache(int value) {
        if (intCache == null) {
            intCache = new IDatatype[INTMAX];
        }
        if (intCache[value] == null) {
            intCache[value] = new CoreseInteger(value);
        }
        return intCache[value];
    }

    public static IDatatype newValue(String result) {
        if (result.equals("true")) {
            return TRUE;
        }
        if (result.equals("false")) {
            return FALSE;
        }
        if (result.startsWith("http://")) {
            return newResource(result);
        }
        try {
            return newInstance(Integer.parseInt(result));
        } catch (Exception ignored) {
        }
        return newInstance(result);
    }

    public static IDatatype newInstance(String result) {
        return new CoreseString(result);
    }

    // key for map/json
    public static IDatatype key(String result) {
        return newInstance(result);
    }

    public static IDatatype newStringBuilder(StringBuilder result) {
        return new CoreseStringBuilder(result);
    }

    public static IDatatype newStringBuilder(String result) {
        return new CoreseStringBuilder(new StringBuilder(result));
    }

    public static IDatatype newInstance(boolean result) {
        if (result) {
            return CoreseBoolean.TRUE;
        }
        return CoreseBoolean.FALSE;
    }

    public static IDatatype newResource(String result) {
        return new CoreseURI(result);
    }

    public static IDatatype newResourceOrLiteral(String result) {
        if (result.startsWith("http://")) {
            return newResource(result);
        }
        return new CoreseString(result);
    }

    public static IDatatype json(String... param) {
        return init(json(), param);
    }

    public static CoreseJSON json() {
        return new CoreseJSON(new JSONObject());
    }

    public static IDatatype newServiceReport(String... param) {
        return json(param);
    }

    public static IDatatype newResource(String ns, String name) {
        return newResource(ns + name);
    }

    public static IDatatype uri(String ns, String name) {
        return newResource(ns + name);
    }

    public static IDatatype newDate() {
        return new CoreseDateTime();
    }

    public static IDatatype newDate(String date) {
        return new CoreseDate(date);
    }

    static String clean(String date) {
        String[] str = date.split("T");
        String[] adate = str[0].split("-");
        if (adate.length == 3 && adate[2].length() == 4) {
            String mydate = adate[2] + "-" + adate[1] + "-" + adate[0];
            if (str.length == 1) {
                return mydate;
            } else {
                return mydate + "T" + str[1];
            }
        }
        return date;
    }

    public static IDatatype newDate(Date date) {
        return newInstance(date);
    }

    public static IDatatype newInstance(XMLGregorianCalendar date) {
        return new CoreseDate(date);
    }

    public static IDatatype newInstance(Date date) {
        XMLGregorianCalendar cal = newXMLGregorianCalendar(date);
        if (cal == null) {
            return null;
        }
        return newInstance(cal);
    }

    public static XMLGregorianCalendar newXMLGregorianCalendar(Date date) {
        long milli = date.getTime() % 1000;
        return factory.newXMLGregorianCalendar(
                year(date), month(date), date.getDate(),
                date.getHours(), date.getMinutes(), date.getSeconds(), (int) Math.max(0, milli), timeZone(date)
        );
    }

    public static XMLGregorianCalendar newXMLGregorianCalendar() {
        return factory.newXMLGregorianCalendar(new GregorianCalendar());
    }

    public static XMLGregorianCalendar newXMLGregorianCalendar(String label) {
        return factory.newXMLGregorianCalendar(label);
    }

    static int timeZone(Date d) {
        // -60 -> + 60
        return d.getTimezoneOffset() + 120;
    }

    static int year(Date d) {
        return d.getYear() + 1900;
    }

    static int month(Date d) {
        return d.getMonth() + 1;
    }

    public static IDatatype newDateTime(String date) {
        return new CoreseDateTime(date);
    }

    /**
     * Create a datatype. If it is a not well formed number, create a
     * CoreseUndef
     */
    public static IDatatype createLiteral(String label, String datatype) {
        return createLiteral(label, datatype, null);
    }

    public static IDatatype createLiteral(String label, String datatype, String lang) {
        IDatatype dt = null;
        try {
            dt = createLiteralWE(label, datatype, lang);
        } catch (CoreseDatatypeException e) {
            logger.error(e.getMessage());
            dt = createUndef(label, datatype);
        }
        return dt;
    }

    public static IDatatype createLiteralWE(String label, String datatype, String lang)
            throws CoreseDatatypeException {
        if (datatype == null) {
            datatype = datatypeURI(lang);
        }
        String javaType = dm.getJType(datatype);
        return CoreseDatatype.create(javaType, datatype, label, lang);
    }

    public static IDatatype newXMLLiteral(String label, org.w3c.dom.Node node) {
        IDatatype dt = new CoreseXMLLiteral(label);
        dt.setObject(node);
        return dt;
    }

    public static IDatatype newLiteral(String label) {
        if (LITERAL_AS_STRING) {
            return newInstance(label);
        } else {
            return newBasicLiteral(label);
        }
    }

    public static IDatatype newBasicLiteral(String label) {
        return new CoreseLiteral(label);
    }

    public static IDatatype[] toArray(IDatatype dt) {
        List<IDatatype> list = dt.getValueList();
        IDatatype[] args = new IDatatype[list.size()];
        return list.toArray(args);
    }

    static String defaultName(Object obj) {
        return Long.toString(obj.hashCode());
    }

    static String defaultName(Pointerable obj) {
        return Long.toString(obj.getDatatypeLabel().hashCode());
    }

    public static IDatatype createObject(String name, Object obj, String datatype) {
        IDatatype dt = createUndef(name, datatype);
        dt.setObject(obj);
        return dt;
    }

    public static IDatatype createObject(Object obj) {
        return createObject(null, obj);
    }

    public static IDatatype createObject(String name, Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Node) {
            return  ((Node) obj).getDatatypeValue();
        }
        throw new CoreseDatatypeException("Reference to LDScript pointers has been removed, could not create object");
    }

    public static IDatatype createUndef(String label, String datatype) {
        IDatatype dt = new CoreseUndefLiteral(label);
        dt.setDatatype(datatype);
        return dt;
    }

    public static IDatatype map(String... param) {
        return init(map(), param);
    }

    static IDatatype init(IDatatype obj, String... param) {
        for (int i = 0; i < param.length; i++) {
            obj.set(newInstance(param[i++]), newInstance(param[i]));
        }
        return obj;
    }

    public static IDatatype newList(Object... ldt) {
        ArrayList<IDatatype> list = new ArrayList<>();
        for (Object obj : ldt) {
            list.add(getValue(obj));
        }
        return newList(list);
    }

    public static List<Node> toNodeList(IDatatype dt) {
        ArrayList<Node> list = new ArrayList<>();
        if (dt.isList()) {
            for (IDatatype val : dt) {
                list.add(val);
            }
        } else {
            list.add(dt);
        }
        return list;
    }

    public static List<String> toStringList(IDatatype dt) {
        ArrayList<String> list = new ArrayList<>();
        if (dt.isList()) {
            for (IDatatype val : dt) {
                list.add(val.getLabel());
            }
        } else {
            list.add(dt.getLabel());
        }
        return list;
    }

    public static IDatatype newStringList(List<String> alist) {
        ArrayList<IDatatype> list = new ArrayList<>();
        for (String str : alist) {
            list.add(newInstance(str));
        }
        return newList(list);
    }

    public static IDatatype newResourceList(List<String> alist) {
        ArrayList<IDatatype> list = new ArrayList<>();
        for (String str : alist) {
            list.add(newResource(str));
        }
        return newList(list);
    }


    public static IDatatype toList(List<Node> list) {
        ArrayList<IDatatype> l = new ArrayList<>();
        for (Node node : list) {
            l.add(node.getDatatypeValue());
        }
        return newList(l);
    }

    /**
     * Order of Datatypes &amp; rdfs:Literal vs xsd:string
     */
    public static void setSPARQLCompliant(boolean b) {
        SPARQLCompliant = b;
        LITERAL_AS_STRING = !b;
    }

    public static boolean isLiteralAsString() {
        return LITERAL_AS_STRING;
    }

    public static void setLiteralAsString(boolean b) {
        LITERAL_AS_STRING = b;
    }

    static String datatypeURI(String lang) {
        if (LITERAL_AS_STRING && (lang == null || lang.equals(""))) {
            return XSD.xsdString.getIRI().stringValue();
        } else {
            return RDFS.Literal.getIRI().stringValue();
        }
    }

    public static String datatype(String lang) {
        if (LITERAL_AS_STRING && (lang == null || lang.equals(""))) {
            return qxsdString;
        } else {
            return qrdfsLiteral;
        }
    }

    public static IDatatype createResource(String label) {
        return new CoreseURI(label);
    }

    public static IDatatype createSkolem(String label) {
        return new CoreseURI(label);
    }

    public static IDatatype createBlank(String label) {
        return new CoreseBlankNode(label);
    }

    public static IDatatype createBlank() {
        return new CoreseBlankNode(blankID());
    }

    public static String blankID() {
        return BLANK + COUNT++;
    }

    public static IDatatype createTripleReference(String label) {
        IDatatype dt = new CoreseTriple(label);
        return dt;
    }

    public static IDatatype createTripleReference(Edge e) {
        IDatatype dt = new CoreseTriple(blankID());
        dt.setEdge(e);
        return dt;
    }

    // dt1 share dt2
    public static void shareTripleReference(IDatatype dt1, IDatatype dt2) {
        if (dt2.isTriple()) {
            dt1.setTriple(true);
            if (dt2.getPointerObject() != null && dt1.getPointerObject() == null) {
                dt1.setPointerObject(dt2.getPointerObject());
            }
        }
    }

    /**
     * *****************************
     * <p>
     * Accessors
     */
    public static boolean isStringLiteral(IDatatype dt) {
        return isString(dt) || isLiteral(dt);
    }

    // literal with or without lang
    public static boolean isLiteral(IDatatype dt) {
        return (dt instanceof CoreseLiteral);
    }

    public static boolean isString(IDatatype dt) {
        return (dt instanceof CoreseString);
    }

    // literal without lang
    public static boolean isSimpleLiteral(IDatatype dt) {
        return isLiteral(dt) && !dt.hasLang();
    }

    public static boolean isInteger(IDatatype dt) {
        return dt.getCode() == IDatatype.Datatype.INTEGER;
    }

    public static boolean isLong(IDatatype dt) {
        return dt.getCode() == IDatatype.Datatype.INTEGER && dt.getDatatypeURI().equals(XSD.xsdLong.getIRI().stringValue());
    }

    public static boolean isFloat(IDatatype dt) {
        return dt.getCode() == IDatatype.Datatype.FLOAT;
    }

    public static boolean isDouble(IDatatype dt) {
        return dt.getCode() == IDatatype.Datatype.DOUBLE;
    }

    public static boolean isDecimal(IDatatype dt) {
        return dt.getCode() == IDatatype.Datatype.DECIMAL;
    }

    public static IDatatype getTZ(IDatatype dt) {
        if (!dt.isDate()) {
            return null;
        }
        return getDate(dt).getTZ();
    }

    static CoreseDate getDate(IDatatype dt) {
        return (CoreseDate) dt;
    }

    public static IDatatype getTimezone(IDatatype dt) {
        if (!dt.isDate()) {
            return null;
        }
        return getDate(dt).getTimezone();
    }

    public static IDatatype getYear(IDatatype dt) {
        if (!dt.isDate()) {
            return null;
        }
        return getDate(dt).getYear();
    }

    public static IDatatype getMonth(IDatatype dt) {
        if (!dt.isDate()) {
            return null;
        }
        return getDate(dt).getMonth();
    }

    public static IDatatype getDay(IDatatype dt) {
        if (!dt.isDate()) {
            return null;
        }
        return getDate(dt).getDay();
    }

    public static IDatatype getHour(IDatatype dt) {
        if (!dt.isDate()) {
            return null;
        }
        return getDate(dt).getHour();
    }

    public static IDatatype getMinute(IDatatype dt) {
        if (!dt.isDate()) {
            return null;
        }
        return getDate(dt).getMinute();
    }

    public static IDatatype getSecond(IDatatype dt) {
        if (!dt.isDate()) {
            return null;
        }
        return getDate(dt).getSecond();
    }

    // for literal only
    public static boolean check(IDatatype dt, String range) {
        if (dt.isNumber()) {
            if (!isNumber(range)) {
                return false;
            }
        } else if (isNumber(range)) {
            return false;
        }
        return dt.getDatatypeURI().equals(range);
    }

    public static boolean persistentable(IDatatype dt) {

        return (dt instanceof CoreseStringLiteral
                || dt instanceof CoreseLiteral
                || dt instanceof CoreseUndefLiteral
                || dt instanceof CoreseXMLLiteral
                || dt instanceof CoreseString);
    }

    public static IDatatype kind(IDatatype dt) {
        if (dt.isLiteral()) {
            return dt.getDatatype();
        }
        if (dt.isURI()) {
            return URI_DATATYPE;
        }
        return BNODE_DATATYPE;
    }

    public static IDatatype setPublicDatatypeValue(IDatatype dt) {
        TRUE.setPublicDatatypeValue(dt);
        return dt;
    }

    public static IDatatype getPublicDatatypeValue() {
        return TRUE.getPublicDatatypeValue();
    }

    public static DatatypeMap getDatatypeMap() {
        return dm;
    }

    public static DatatypeMap getSingleton() {
        return dm;
    }

    public static boolean isLiteralDatatype(IDatatype type) {
        String label = type.getLabel();
        return label.startsWith(fr.inria.corese.core.next.data.impl.common.vocabulary.XSD.getVocabularyNamespace())
                || label.equals(fr.inria.corese.core.next.data.impl.common.vocabulary.RDF.XMLLiteral.getIRI().stringValue())
                || label.equals(fr.inria.corese.core.next.data.impl.common.vocabulary.RDF.langString.getIRI().stringValue())
                || label.equals(fr.inria.corese.core.next.data.impl.common.vocabulary.RDF.HTML.getIRI().stringValue());
    }

    public static IDatatype URIDomain(IDatatype dt) {
        return URIDomain(dt, TRUE);
    }

    public static IDatatype URIDomain(IDatatype dt, IDatatype scheme) {
        String dom = NSManager.domain(dt.getLabel(), scheme.booleanValue());
        if (dom == null) {
            return null;
        }
        return newResource(dom);
    }

    public static IDatatype split(IDatatype dt1, IDatatype dt2) {
        return split(dt1, dt2.getLabel());
    }

    public static IDatatype split(IDatatype dt1, String sep) {
        String[] split = dt1.stringValue().split(sep);
        return cast(split);
    }

    public static IDatatype cast(String[] arr) {
        ArrayList<IDatatype> list = new ArrayList<>();
        for (String str : arr) {
            list.add(newInstance(str));
        }
        return newList(list);
    }

    public void put(String dt, String jtype, Hashtable<String, String> htms) {
        Mapping map = new Mapping(jtype);
        ht.put(dt, map);
    }

    public void put(String dt, String jtype, String dtms) {
        Mapping map = new Mapping(jtype);
        ht.put(dt, map);
    }

    /**
     * Return the java class name implementing the datatype
     */
    public String getJType(String dt) {
        if (dt == null) {
            return null;
        }
        Mapping map = getMapping(dt);
        if (map == null) {
            return null;
        }
        return map.getJavaType();
    }


    // DatatypeValueFactory

    Mapping getMapping(String dt) {
        Mapping map = ht.get(dt);
        if (map == null) {
            // create a new literal space (i.e. dt) value for this unknown datatype
            put(dt, jTypeUndef, dt); // CoreseUndefLiteral
            map = ht.get(dt);
        }
        return map;
    }

    /**
     * Defines the datatype map between XSD datatypes and the java class that
     * implements the datatype and the marker set that contain the marker. We
     * separate number value spaces by giving different marker set to integer
     * and float
     */
    public void init() {

        //define the hashtable that 1 MS for (LITERAL,lang)
        Hashtable<String, String> htlang = new Hashtable<>();
        htlang.put(DEFAULT, RDFS.Literal.getIRI().stringValue());
        put(RDFS.Literal.getIRI().stringValue(), jTypeLiteral, htlang);
        put(fr.inria.corese.core.next.data.impl.common.vocabulary.RDF.langString.getIRI().stringValue(), jTypeLiteral, htlang);

        put(fr.inria.corese.core.next.data.impl.common.vocabulary.RDF.XMLLiteral.getIRI().stringValue(), jTypeXMLString, fr.inria.corese.core.next.data.impl.common.vocabulary.RDF.XMLLiteral.getIRI().stringValue());
        put(XSD.xsdString.getIRI().stringValue(), jTypeString, XSD.xsdString.getIRI().stringValue());
        put(XSD.xsdBoolean.getIRI().stringValue(), jTypeBoolean, XSD.xsdBoolean.getIRI().stringValue());
        put(XSD.xsdAnyURI.getIRI().stringValue(), jTypeURILiteral, XSD.xsdAnyURI.getIRI().stringValue());

        put(XSD.xsdNormalizedString.getIRI().stringValue(), jTypeString, XSD.xsdString.getIRI().stringValue());
        put(XSD.xsdToken.getIRI().stringValue(), jTypeString, XSD.xsdString.getIRI().stringValue());
        put(XSD.xsdNMTOKEN.getIRI().stringValue(), jTypeString, XSD.xsdString.getIRI().stringValue());
        put(XSD.xsdName.getIRI().stringValue(), jTypeString, XSD.xsdString.getIRI().stringValue());
        put(XSD.xsdNCName.getIRI().stringValue(), jTypeString, XSD.xsdString.getIRI().stringValue());
        put(XSD.xsdLanguage.getIRI().stringValue(), jTypeString, XSD.xsdString.getIRI().stringValue());

        String intSpace = XSD.xsdInteger.getIRI().stringValue();
        // Integer + store datatype URI ?
        String intJType = jTypeInteger;
        String genericIntJType = jTypeGenericInteger;

        put(XSD.xsdDouble.getIRI().stringValue(), jTypeDouble, XSD.xsdDouble.getIRI().stringValue());
        put(XSD.xsdFloat.getIRI().stringValue(), jTypeFloat, XSD.xsdFloat.getIRI().stringValue());
        put(XSD.xsdDecimal.getIRI().stringValue(), jTypeDecimal, XSD.xsdDecimal.getIRI().stringValue());
        put(XSD.xsdInteger.getIRI().stringValue(), jTypeInteger, intSpace);

        put(XSD.xsdLong.getIRI().stringValue(), genericIntJType, intSpace);
        put(XSD.xsdShort.getIRI().stringValue(), genericIntJType, intSpace);
        put(XSD.xsdInt.getIRI().stringValue(), genericIntJType, intSpace);
        put(XSD.xsdByte.getIRI().stringValue(), genericIntJType, intSpace);
        put(XSD.xsdNonNegativeInteger.getIRI().stringValue(), genericIntJType, intSpace);
        put(XSD.xsdNonPositiveInteger.getIRI().stringValue(), genericIntJType, intSpace);
        put(XSD.xsdPositiveInteger.getIRI().stringValue(), genericIntJType, intSpace);
        put(XSD.xsdNegativeInteger.getIRI().stringValue(), genericIntJType, intSpace);
        put(XSD.xsdUnsignedLong.getIRI().stringValue(), genericIntJType, intSpace);
        put(XSD.xsdUnsignedInt.getIRI().stringValue(), genericIntJType, intSpace);
        put(XSD.xsdUnsignedShort.getIRI().stringValue(), genericIntJType, intSpace);
        put(XSD.xsdUnsignedByte.getIRI().stringValue(), genericIntJType, intSpace);

        put(XSD.xsdDate.getIRI().stringValue(), jTypeDate, XSD.xsdDate.getIRI().stringValue());
        put(XSD.xsdDateTime.getIRI().stringValue(), jTypeDateTime, XSD.xsdDateTime.getIRI().stringValue());
        put(XSD.xsdDay.getIRI().stringValue(), jTypeDay, XSD.xsdDay.getIRI().stringValue());
        put(XSD.xsdMonth.getIRI().stringValue(), jTypeMonth, XSD.xsdMonth.getIRI().stringValue());
        put(XSD.xsdYear.getIRI().stringValue(), jTypeYear, XSD.xsdYear.getIRI().stringValue());
        put(XSD.xsdDayTimeDuration.getIRI().stringValue(), jTypeGeneric, XSD.xsdDayTimeDuration.getIRI().stringValue());

        //special use case: to get the implementation java type for Resource and Blank
        put(RDFS.Resource.getIRI().stringValue(), jTypeURI, RDFS.Resource.getIRI().stringValue());

        define(RDFS.Literal.getIRI().stringValue(), IDatatype.Datatype.LITERAL);
        define(fr.inria.corese.core.next.data.impl.common.vocabulary.RDF.langString.getIRI().stringValue(), IDatatype.Datatype.LITERAL);
        define(fr.inria.corese.core.next.data.impl.common.vocabulary.RDF.XMLLiteral.getIRI().stringValue(), IDatatype.Datatype.XMLLITERAL);
        define(XSD.xsdBoolean.getIRI().stringValue(), IDatatype.Datatype.BOOLEAN);
        define(XSD.xsdAnyURI.getIRI().stringValue(), IDatatype.Datatype.URI_LITERAL);
        define(XSD.xsdString.getIRI().stringValue(), IDatatype.Datatype.STRING);
        define(RDFS.Resource.getIRI().stringValue(), IDatatype.Datatype.URI);

        defineString(XSD.xsdNormalizedString.getIRI().stringValue());
        defineString(XSD.xsdToken.getIRI().stringValue());
        defineString(XSD.xsdNMTOKEN.getIRI().stringValue());
        defineString(XSD.xsdName.getIRI().stringValue());
        defineString(XSD.xsdNCName.getIRI().stringValue());
        defineString(XSD.xsdLanguage.getIRI().stringValue());

        define(XSD.xsdDouble.getIRI().stringValue(), IDatatype.Datatype.DOUBLE);
        define(XSD.xsdFloat.getIRI().stringValue(), IDatatype.Datatype.FLOAT);
        define(XSD.xsdDecimal.getIRI().stringValue(), IDatatype.Datatype.DECIMAL);
        define(XSD.xsdInteger.getIRI().stringValue(), IDatatype.Datatype.INTEGER);

        defineInteger(XSD.xsdLong.getIRI().stringValue());
        defineInteger(XSD.xsdShort.getIRI().stringValue());
        defineInteger(XSD.xsdInt.getIRI().stringValue());
        defineInteger(XSD.xsdByte.getIRI().stringValue());
        defineInteger(XSD.xsdNonNegativeInteger.getIRI().stringValue());
        defineInteger(XSD.xsdNonPositiveInteger.getIRI().stringValue());
        defineInteger(XSD.xsdPositiveInteger.getIRI().stringValue());
        defineInteger(XSD.xsdNegativeInteger.getIRI().stringValue());
        defineInteger(XSD.xsdUnsignedLong.getIRI().stringValue());
        defineInteger(XSD.xsdUnsignedShort.getIRI().stringValue());
        defineInteger(XSD.xsdUnsignedInt.getIRI().stringValue());
        defineInteger(XSD.xsdUnsignedByte.getIRI().stringValue());

        define(XSD.xsdDate.getIRI().stringValue(), IDatatype.Datatype.DATE);
        define(XSD.xsdDateTime.getIRI().stringValue(), IDatatype.Datatype.DATETIME);

        define(XSD.xsdDay.getIRI().stringValue(), IDatatype.Datatype.DAY);
        define(XSD.xsdMonth.getIRI().stringValue(), IDatatype.Datatype.MONTH);
        define(XSD.xsdYear.getIRI().stringValue(), IDatatype.Datatype.YEAR);

        define(XSD.xsdDayTimeDuration.getIRI().stringValue(), IDatatype.Datatype.DURATION);

    }

    void define(String datatype, IDatatype.Datatype code) {
        dtCode.put(datatype, code);
    }

    void defineString(String datatype) {
        dtCode.put(datatype, IDatatype.Datatype.STRING);
    }

    void defineInteger(String datatype) {
        dtCode.put(datatype, IDatatype.Datatype.GENERIC_INTEGER);
    }

    IDatatype.Datatype getType(String datatype) {
        return IDatatype.Datatype.UNDEF;
    }

    IDatatype create(String label, String datatype, String lang) {
        if (getType(datatype) == IDatatype.Datatype.STRING) {
            return new CoreseString(label);
        }

        return null;
    }

    public static IDatatype createList() {
        return createList(new ArrayList<>(0));
    }

    public static IDatatype createList(List<IDatatype> ldt) {
        IDatatype dt = CoreseList.create(ldt);
        return dt;
    }

    public static IDatatype createList(IDatatype... ldt) {
        return new CoreseList(ldt);
    }

    @Override
    public Node nodeList(List<Node> list) {
        return toList(list);
    }

    @Override
    public IDatatype nodeValue(int n) {
        return newInstance(n);
    }

    private static class Mapping {

        private String javatype = "";

        public Mapping(String jtype) {
            javatype = jtype;
        }

        String getJavaType() {
            return javatype;
        }
    }

}
