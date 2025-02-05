package fr.inria.corese.core.sparql.triple.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import fr.inria.corese.core.kgram.api.core.PointerType;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2015
 *
 */
public class Metadata extends ASTObject
        implements Iterable<String> {
    static final String NL = System.getProperty("line.separator");
    static final String AT = "@";

    // Query
    public enum Type {
        UNDEFINED,
        TEST,
        DEBUG,
        TRACE,
        PUBLIC,
        IMPORT,
        NEW,
        SPARQL,
        RELAX,
        MORE,
        FEDERATE,
        DISPLAY,
        BIND,
        TYPE,
        COMPILE,
        SKIP,
        PATH,
        ENCODING,
        DB,
        DB_FACTORY,
        ALGEBRA,
        BOUNCE,
        SPARQL10,
        TRAP,
        FEDERATION,
        COUNT,
        PARALLEL,
        SEQUENCE,
        VARIABLE,
        SERVER,
        PROVENANCE,
        DUPLICATE,
        LDPATH,
        ENDPOINT,
        FILE,
        DETAIL,
        ACCEPT,
        REJECT,
        OPTION,
        SPLIT,
        LOCK,
        UNLOCK,
        LIMIT,
        GRAPH,
        FROM,
        UPDATE,
        BINDING,
        INDEX,
        LOG,
        EXPLAIN,
        WHY,
        MESSAGE,
        MERGE_SERVICE,
        BROWSE,
        EVENT,
        FORMAT,
        SELECT,
        REPORT,
        DISTINCT,
        ENUM,
        HEADER,
        COOKIE,
        TIMEOUT,
        RDF_STAR_SELECT,
        RDF_STAR_DELETE,
        METADATA,
        VISITOR,
        MOVE,
        PATH_TYPE,
        SLICE,
        FOCUS
    }

    static final String PREF = NSManager.KGRAM;
    public static final String DISPLAY_RDF_XML = PREF + "rdfxml";
    public static final String DISPLAY_TURTLE = PREF + "turtle";
    public static final String DISPLAY_JSON_LD = PREF + "jsonld";

    public static final String DISPLAY_JSON = PREF + "json";
    public static final String DISPLAY_XML = PREF + "xml";
    public static final String DISPLAY_RDF = PREF + "rdf";

    public static final String RELAX_URI = PREF + "uri";
    public static final String RELAX_PROPERTY = PREF + "property";
    public static final String RELAX_LITERAL = PREF + "literal";

    public static final String PROBE = PREF + "probe";
    public static final String VERBOSE = PREF + "verbose";
    public static final String SELECT_SOURCE = PREF + "select";
    public static final String SELECT_FILTER = PREF + "selectfilter";
    public static final String GROUP = PREF + "group";
    public static final String MERGE = PREF + "merge";
    public static final String SIMPLIFY = PREF + "simplify";
    public static final String EXIST = PREF + "exist";
    public static final String SKIP_STR = PREF + "skip";
    public static final String ALL = "all";
    public static final String EMPTY = "empty";

    public static final String DISTRIBUTE_NAMED = PREF + "distributeNamed";
    public static final String DISTRIBUTE_DEFAULT = PREF + "distributeDefault";
    public static final String REWRITE_NAMED = PREF + "rewriteNamed";

    public static final String METHOD = "@method";
    public static final String ACCESS = "@access";
    public static final String LEVEL = "@level";
    public static final String POST = "@post";
    public static final String GET = "@get";
    public static final String FORM = "@form";
    public static final String OLD_SERVICE = "@oldService";
    public static final String SHOW = "@show";
    public static final String SELECTION = "@selection";
    public static final String DISCOVERY = "@discovery";
    public static final String LOOP = AT + URLParam.LOOP;
    public static final String START = AT + URLParam.START;
    public static final String UNTIL = AT + URLParam.UNTIL;;
    public static final String HIDE = "@hide";
    public static final String LIMIT_STR = "@limit";

    public static final String FED_BGP = "@federateBgp";
    public static final String FED_JOIN = "@federateJoin";
    public static final String FED_OPTIONAL = "@federateOptional";
    public static final String FED_MINUS = "@federateMinus";
    public static final String FED_UNDEFINED = "@federateUndefined";
    public static final String FED_COMPLETE = "@federateComplete";
    public static final String FED_PARTITION = "@federatePartition";
    public static final String FED_SUCCESS = "@" + URLParam.FED_SUCCESS;
    public static final String FED_LENGTH = "@" + URLParam.FED_LENGTH;
    public static final String FED_INCLUDE = "@" + URLParam.FED_INCLUDE;
    public static final String FED_EXCLUDE = "@exclude";
    public static final String FED_BLACKLIST = "@blacklist";
    public static final String FED_WHITELIST = "@whitelist";
    public static final String FED_CLASS = "@federateClass";
    public static final String SAVE = "@save";

    private static HashMap<String, Type> annotation;
    private static HashMap<Type, String> back;

    HashMap<String, String> map;
    HashMap<String, List<String>> value;
    HashMap<String, IDatatype> literal;
    // inherited metadata such as @public { function ... }
    private Metadata metadata;

    static {
        initAnnotate();
    }

    static void initAnnotate() {
        annotation = new HashMap();
        back = new HashMap();
        define("@debug", Type.DEBUG);
        define("@trace", Type.TRACE);
        define("@test", Type.TEST);
        define("@new", Type.NEW);
        define("@parallel", Type.PARALLEL);
        define("@sequence", Type.SEQUENCE);
        define("@variable", Type.VARIABLE);
        define("@provenance", Type.PROVENANCE);
        define("@log", Type.LOG);
        define("@duplicate", Type.DUPLICATE);
        define("@distinct", Type.DISTINCT);
        define("@count", Type.COUNT);
        define("@server", Type.SERVER);
        define("@export", Type.PUBLIC);
        define("@public", Type.PUBLIC);
        define("@more", Type.MORE);
        define("@relax", Type.RELAX);
        define("@federate", Type.FEDERATE);
        define("@federation", Type.FEDERATION);
        define("@sparql", Type.SPARQL);
        define("@index", Type.INDEX);
        define(LIMIT_STR, Type.LIMIT);
        define("@slice", Type.SLICE);
        define("@move", Type.MOVE);
        define("@bounce", Type.BOUNCE);
        define("@sparqlzero", Type.SPARQL10);
        define("@encoding", Type.ENCODING);
        define("@bind", Type.BIND); // @event @bind
        define("@binding", Type.BINDING); // service bind: to differ from @event @bind
        define("@import", Type.IMPORT);
        define("@display", Type.DISPLAY);
        define("@type", Type.TYPE);
        define("@compile", Type.COMPILE);
        define("@path", Type.PATH);
        define("@pathtype", Type.PATH_TYPE);
        define("@skip", Type.SKIP);
        define("@db", Type.DB);
        define("@dbfactory", Type.DB_FACTORY);
        define("@algebra", Type.ALGEBRA);
        define("@metadata", Type.METADATA);
        define("@visitor", Type.VISITOR);
        define("@trap", Type.TRAP);
        define("@ldpath", Type.LDPATH);
        define("@endpoint", Type.ENDPOINT);
        define("@file", Type.FILE);
        define("@detail", Type.DETAIL);
        define("@report", Type.REPORT);
        define("@header", Type.HEADER);
        define("@cookie", Type.COOKIE);
        define("@timeout", Type.TIMEOUT);
        define("@enum", Type.ENUM);
        define("@accept", Type.ACCEPT);
        define("@reject", Type.REJECT);
        define("@option", Type.OPTION);
        define("@split", Type.SPLIT);
        define("@lock", Type.LOCK);
        define("@unlock", Type.UNLOCK);
        define("@graph", Type.GRAPH);
        define("@from", Type.FROM);
        define("@explain", Type.EXPLAIN);
        define("@why", Type.WHY);
        define("@message", Type.MESSAGE);
        define("@browse", Type.BROWSE);
        define("@merge", Type.MERGE_SERVICE);
        define("@focus", Type.FOCUS);
        define("@format", Type.FORMAT);
        // update query evaluated as select query
        define("@select", Type.SELECT);
        define("@selectrdfstar", Type.RDF_STAR_SELECT);
        define("@deleterdfstar", Type.RDF_STAR_DELETE);

        define("@update", Type.UPDATE);
        define("@event", Type.EVENT);
    }

    static void define(String str, Type type) {
        annotation.put(str, type);
        back.put(type, str);
    }

    public Metadata() {
        map = new HashMap<>();
        value = new HashMap();
        literal = new HashMap<>();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Metadata:");
        sb.append(NL);
        for (String m : this) {
            sb.append(m);
            List<String> list = getValues(m);
            if (list != null && !list.isEmpty()) {
                sb.append(" : ");
                sb.append(getValues(m));
            }
            sb.append(NL);
        }

        for (String key : literal.keySet()) {
            sb.append(key).append(" : ").append(getDatatypeValue(key));
        }

        return sb.toString();
    }

    /**
     * Subset of Metadata for xt:sparql() see PluginImpl
     */
    public Metadata selectSparql() {
        if (hasMetadata(Type.REPORT)) {
            return new Metadata().add(Type.REPORT);
        }
        return null;
    }

    public Metadata share(Metadata meta) {
        add(meta);
        return this;
    }

    public Metadata add(String str) {
        map.put(str, str);
        return this;
    }

    public Metadata add(Type type) {
        String name = name(type);
        if (name != null) {
            add(name);
        }
        return this;
    }

    public Metadata remove(Type type) {
        String name = name(type);
        if (name != null) {
            map.remove(name);
        }
        return this;
    }

    public Metadata add(Type type, String value) {
        String name = name(type);
        if (name != null) {
            add(name, value);
        }
        return this;
    }

    public void add(String name, String val) {
        add(name);
        List<String> list = value.get(name);
        if (list == null) {
            list = new ArrayList<>();
            value.put(name, list);
        }
        if (!list.contains(val)) {
            list.add(val);
        }
    }

    public void set(Type type, List<String> list) {
        String name = name(type);
        if (name != null) {
            set(name, list);
        }
    }

    public void set(String name, List<String> list) {
        if (!list.isEmpty()) {
            add(name, list.get(0));
        }
        value.put(name, list);
    }

    public void add(String name, Constant val) {
        if (val.isResource()) {
            add(name, val.getLongName());
        } else if (val.isLiteral()) {
            literal.put(name, val.getDatatypeValue());
        }
    }

    public void add(Type type, IDatatype val) {
        add(name(type), val);
    }

    public void add(String name, IDatatype val) {
        if (val.isURI()) {
            add(name, val.getLabel());
        } else if (val.isLiteral()) {
            literal.put(name, val);
        }
    }

    public boolean hasMetadata(Type type) {
        String str = name(type);
        if (str == null) {
            return false;
        }
        return hasMetadata(str);
    }

    public boolean hasMetadata(Type... type) {
        for (Type val : type) {
            if (hasMetadata(val)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasMetadata(String... type) {
        for (String val : type) {
            if (hasMetadata(val)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasMetadata(String name) {
        return map.containsKey(name);
    }

    // add without overloading local
    public void add(Metadata m) {
        for (String name : m) {
            if (!hasMetadata(name)) {
                add(name);
                if (m.getValues(name) != null) {
                    value.put(name, m.getValues(name));
                }
            }
        }
    }

    public HashMap<String, String> getMap() {
        return map;
    }

    public String getValue(Type type) {
        return getValue(name(type));
    }

    public IDatatype getDatatypeValue(Type type) {
        return getDatatypeValue(name(type));
    }

    public IDatatype getDatatypeValue(String type) {
        return literal.get(type);
    }

    public int intValue(Type type) {
        IDatatype dt = getDatatypeValue(type);
        if (dt == null) {
            return -1;
        }
        return dt.intValue();
    }

    public boolean hasDatatypeValue(Type type) {
        return getDatatypeValue(type) != null;
    }

    public boolean hasDatatypeValue(String type) {
        return getDatatypeValue(type) != null;
    }

    public String getStringValue(Type type) {
        String value = getValue(type);
        if (value == null) {
            return null;
        }
        return NSManager.nstrip(value);
    }

    boolean hasReportKey(String key) {
        List<String> list = getValues(Type.REPORT);
        if (list == null) {
            return true;
        }
        // @report empty: empty is not a key
        if (list.size() == 1 && list.contains(EMPTY)) {
            return true;
        }
        return list.contains(key);
    }

    public boolean hasValue(Type meta) {
        return getValue(meta) != null;
    }

    public boolean hasValue(Type meta, String value) {
        String str = getValue(meta);
        return str != null && str.equals(value);
    }

    public boolean hasValues(Type meta, String value) {
        List<String> list = getValues(meta);
        if (list == null) {
            return false;
        }
        return list.contains(value);
    }

    public String getValue(String name) {
        if (name == null) {
            return null;
        }
        List<String> list = getValues(name);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    public List<String> getValues(Type type) {
        return getValues(name(type));
    }

    public List<String> getValues(String name) {
        if (name == null) {
            return null;
        }
        return value.get(name);
    }

    @Override
    public Iterator<String> iterator() {
        return map.keySet().iterator();
    }

    public Collection<String> getMetadataList() {
        return map.keySet();
    }

    public Type type(String name) {
        Type i = annotation.get(name);
        if (i == null) {
            i = Type.UNDEFINED;
        }
        return i;
    }

    public String name(Type type) {
        return back.get(type);
    }

    @Override
    public PointerType pointerType() {
        return PointerType.METADATA;
    }

    @Override
    public IDatatype getList() {
        ArrayList<IDatatype> list = new ArrayList<IDatatype>();
        for (String key : map.keySet()) {
            IDatatype name = DatatypeMap.newLiteral(key);
            list.add(name);
        }
        return DatatypeMap.createList(list);
    }

    @Override
    public String getDatatypeLabel() {
        return String.format("[Metadata: size=%s]", size());
    }

    /**
     * @return the metadata
     */
    public Metadata getMetadata() {
        return metadata;
    }

    /**
     * @param metadata the metadata to set
     */
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    // ________________________________________________

    // @graph <server1> <g1> <g2> <server2> <g3>
    public List<String> getGraphList(String service) {
        List<String> graphList = getValues(Type.FROM);
        List<String> serverList = getValues(Type.FEDERATE);
        ArrayList<String> res = new ArrayList<>();
        boolean find = false;
        if (graphList != null && serverList != null) {
            for (String str : graphList) {
                if (find) {
                    if (serverList.contains(str)) {
                        break;
                    } else {
                        res.add(str);
                    }
                } else if (str.equals(service)) {
                    find = true;
                }
            }
        }

        return res;
    }

}
