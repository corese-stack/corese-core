package fr.inria.corese.core.util;

import fr.inria.corese.core.EdgeFactory;
import fr.inria.corese.core.Graph;
import fr.inria.corese.core.NodeImpl;
import fr.inria.corese.core.compiler.eval.Interpreter;
import fr.inria.corese.core.compiler.eval.QuerySolver;
import fr.inria.corese.core.compiler.federate.FederateVisitor;
import fr.inria.corese.core.compiler.federate.RewriteBGPList;
import fr.inria.corese.core.compiler.federate.SelectorFilter;
import fr.inria.corese.core.compiler.federate.SelectorIndex;
import fr.inria.corese.core.index.EdgeManagerIndexer;
import fr.inria.corese.core.kgram.core.Eval;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.load.Service;
import fr.inria.corese.core.load.*;
import fr.inria.corese.core.producer.DataFilter;
import fr.inria.corese.core.query.CompileService;
import fr.inria.corese.core.query.MatcherImpl;
import fr.inria.corese.core.query.ProviderService;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.rule.RuleEngine;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.CoreseDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.function.script.Function;
import fr.inria.corese.core.sparql.triple.function.term.Binding;
import fr.inria.corese.core.sparql.triple.function.term.TermEval;
import fr.inria.corese.core.sparql.triple.parser.*;
import fr.inria.corese.core.sparql.triple.parser.Access.Level;
import fr.inria.corese.core.sparql.triple.parser.context.ContextLog;
import fr.inria.corese.core.sparql.triple.parser.visitor.ASTParser;
import fr.inria.corese.core.visitor.solver.QuerySolverVisitorRule;
import fr.inria.corese.core.visitor.solver.QuerySolverVisitorTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static fr.inria.corese.core.util.Property.Value.*;

/**
 * Corese configuration with properties such as: LOAD_IN_DEFAULT_GRAPH
 * Usage:
 * Property.set(LOAD_IN_DEFAULT_GRAPH, true);
 * Property.load(property-file-path);
 * Property.init(graph);
 * corese-gui.jar -init property-file-path
 * corese-server.jar -init property-file-path
 * A property file example is in core resources/data/corese/property.properties
 * Define variable:
 * VARIABLE = $home=/user/home/
 * LOAD_DATASET = $home/file.ttl
 * <p>
 * Olivier Corby
 */
public class Property {

    public static final String STAR = "*";
    public static final String RDF_XML = "rdf+xml";
    public static final String TURTLE = "turtle";
    public static final String TRIG = "trig";
    public static final String JSON = "json";
    public static final String XML = "xml";
    public static final String DATASET = "dataset";
    public static final String DB = "db";
    public static final String DB_ALL = "db_all";
    static final String VAR_CHAR = "$";
    static final String LOCAL = "./";
    static final String SEP = ";";
    static final String EQ = "=";
    private static final Logger logger = LoggerFactory.getLogger(Property.class);
    private static final String STD = "std";
    private static Property singleton = null;
    private Map<Value, Boolean> booleanProperty;
    private Map<Value, String> stringProperty;
    private Map<Value, Integer> integerProperty;
    private Map<Value, List<String>> listProperty;
    private Map<String, String> variableMap;
    private Map<String, String> imports;

    // Java Properties manage property file (at user option)
    private Properties properties;

    private String path;
    private String parent;

    Property() {
        booleanProperty = new EnumMap<>(Value.class);
        stringProperty = new EnumMap<>(Value.class);
        integerProperty = new EnumMap<>(Value.class);
        listProperty = new EnumMap<>(Value.class);
        properties = new Properties();
        variableMap = new HashMap<>();
        imports = new HashMap<>();
    }

    protected static Property getSingleton() {
        if (singleton == null) {
            singleton = new Property();
            set(SERVICE_SEND_PARAMETER, true);
            set(DATATYPE_ENTAILMENT, true);
        }
        return singleton;
    }

    public static void load(String path) throws IOException {
        getSingleton().basicLoad(path);
    }

    /**
     * Use case: corese gui initialize graph
     */
    public static void init(Graph g) {
        getSingleton().basicInit(g);
    }

    public static void set(Value value, boolean b) {
        basicSet(value, b);
    }

    public static void set(Value value, String str) {
        basicSet(value, str);
    }

    public static void set(Value value, String... str) {
        basicSet(value, str);
    }

    public static void set(Value value, double d) {
        basicSet(value, Double.toString(d));
    }

    public static void set(Value value, int n) {
        basicSet(value, n);
    }

    public static boolean hasProperty(Value value) {
        return getSingleton().getBooleanProperty().containsKey(value) || getSingleton().getStringProperty().containsKey(value) || getSingleton().getIntegerProperty().containsKey(value) || getSingleton().getListProperty().containsKey(value);
    }

    public static Object get(Value value) {
        if (getSingleton().hasBooleanProperty(value))
            return getBooleanValue(value);
        if (getSingleton().hasStringProperty(value))
            return getStringValue(value);
        if (getSingleton().hasIntegerProperty(value))
            return getIntegerValue(value);
        if (getSingleton().hasListProperty(value))
            return getListValue(value);
        return null;
    }

    public static boolean getBooleanValue(Value value) {
        Boolean b = getSingleton().getBooleanProperty().get(value);
        return b != null && b;
    }

    public static String getStringValue(Value value) {
        return getSingleton().getStringProperty().get(value);
    }

    public static int getIntegerValue(Value value) {
        return getSingleton().getIntegerProperty().get(value);
    }

    public static List<String> getListValue(Value value) {
        return getSingleton().getListProperty().get(value);
    }

    public static boolean hasValue(Value value, boolean b) {
        return get(value) != null && getBooleanValue(value) == b;
    }

    public static boolean hasValue(Value value, String str) {
        return get(value) != null && getStringValue(value).equals(str);
    }

    public static boolean hasValue(Value value, int n) {
        return get(value) != null && getIntegerValue(value) == n;
    }

    public static boolean hasValue(Value value, String... str) {
        return get(value) != null && getListValue(value).containsAll(Arrays.asList(str));
    }

    public static Set<Value> getPropertySet() {
        HashSet<Value> result = new HashSet<>();
        result.addAll(getSingleton().getBooleanProperty().keySet());
        result.addAll(getSingleton().getStringProperty().keySet());
        result.addAll(getSingleton().getIntegerProperty().keySet());
        result.addAll(getSingleton().getListProperty().keySet());
        return result;
    }

    public static String display() {
        return getSingleton().basicDisplay();
    }

    /**
     * Implementation for singleton
     */

    public static String basicDisplay() {
        return String.format("Property:\n%s\n%s",
                getSingleton().getBooleanProperty().toString(), getSingleton().getStringProperty().toString());
    }

    public static void define(String name, String value) {
        Value pname = Value.valueOf(name);
        if (value.equals("true") || value.equals("false")) {
            boolean b = Boolean.parseBoolean(value);
            basicSet(pname, b);
        } else {
            try {
                int n = Integer.parseInt(value);
                basicSet(pname, n);
            } catch (Exception e) {
                basicSet(pname, value);
            }
        }
    }

    static void basicSet(Value value, boolean b) {
        getSingleton().getBooleanProperty().put(value, b);

        switch (value) {

            case OWL_CLEAN:
                RuleEngine.OWL_CLEAN = b;
                break;

            case RULE_DATAMANAGER_OPTIMIZE:
                RuleEngine.RULE_DATAMANAGER_OPTIMIZE = b;
                break;

            case LOAD_WITH_PARAMETER:
                Service.LOAD_WITH_PARAMETER = b;
                break;

            case DISPLAY_URI_AS_PREFIX:
                Constant.DISPLAY_AS_PREFIX = b;
                CoreseDatatype.DISPLAY_AS_PREFIX = b;
                break;

            case DISPLAY_AS_TRIPLE:
                DatatypeMap.DISPLAY_AS_TRIPLE = b;
                break;

            case GRAPH_NODE_AS_DATATYPE:
                NodeImpl.byIDatatype = b;
                break;

            case CONSTRAINT_NAMED_GRAPH:
                Graph.CONSTRAINT_NAMED_GRAPH = b;
                break;

            case CONSTRAINT_GRAPH:
                Graph.CONSTRAINT_GRAPH = b;
                break;

            case EXTERNAL_NAMED_GRAPH:
                Graph.EXTERNAL_NAMED_GRAPH = b;
                break;

            case LOAD_IN_DEFAULT_GRAPH:
                Load.setDefaultGraphValue(b);
                break;

            case SKOLEMIZE:
                Graph.setDefaultSkolem(b);
                break;

            case GRAPH_INDEX_END:
                EdgeManagerIndexer.RECORD_END = b;
                break;

            case RDF_STAR_TRIPLE:
                EdgeFactory.EDGE_TRIPLE_NODE = b;
                EdgeFactory.OPTIMIZE_EDGE = !b;

            case RDF_STAR:
                Graph.setRDFStar(b);
                ASTParser.RDF_STAR = b;
                break;

            case RDF_STAR_VALIDATION:
                // check subject literal is an error
                ParserHandler.rdf_star_validation = b;
                MatcherImpl.RDF_STAR_VALIDATION = b;
                break;

            case RDF_STAR_SELECT:
                DataFilter.RDF_STAR_SELECT = b;
                break;

            case DATATYPE_ENTAILMENT:
                // when true: graph match can join 1, 01, 1.0
                DatatypeMap.DATATYPE_ENTAILMENT = b;
                break;

            case SPARQL_COMPLIANT:
                // default is false
                // true: literal is different from string
                // true: from named without from is sparql compliant
                DatatypeMap.setSPARQLCompliant(b);
                QuerySolver.SPARQL_COMPLIANT_DEFAULT = b;
                // SPARQL_COMPLIANT => ! DATATYPE_ENTAILMENT
                set(DATATYPE_ENTAILMENT, !b);
                break;

            case SPARQL_ORDER_UNBOUND_FIRST:
                Mappings.setOrderUnboundFirst(b);
                break;

            case REENTRANT_QUERY:
                QueryProcess.setOverwrite(b);
                break;

            case ACCESS_LEVEL:
                Access.setActive(b);
                if (b) {
                    Access.setDefaultUserLevel(Level.DEFAULT);
                } else {
                    Access.setDefaultUserLevel(Level.SUPER_USER);
                }
                break;

            case ACCESS_RIGHT:
                AccessRight.setActive(b);
                break;

            case EVENT:
                QuerySolver.setVisitorable(b);
                break;

            case VERBOSE:
                Graph.setDefaultVerbose(b);
                break;

            case LDSCRIPT_DEBUG:
                Function.nullcheck = b;
                break;

            case LDSCRIPT_CHECK_DATATYPE:
                Function.typecheck = b;
                break;

            case LDSCRIPT_CHECK_RDFTYPE:
                Function.rdftypecheck = b;
                break;

            case INTERPRETER_TEST:
                Interpreter.testNewEval = b;
                break;

            case FEDERATE_BGP:
                FederateVisitor.FEDERATE_BGP = b;
                break;

            case FEDERATE_PARTITION:
                FederateVisitor.PARTITION = b;
                break;

            case FEDERATE_COMPLETE:
                FederateVisitor.COMPLETE_BGP = b;
                break;

            case FEDERATE_FILTER:
                FederateVisitor.SELECT_FILTER = b;
                break;

            case FEDERATE_JOIN:
                FederateVisitor.SELECT_JOIN = b;
                FederateVisitor.USE_JOIN = b;
                break;

            case FEDERATE_OPTIONAL:
                FederateVisitor.OPTIONAL = b;
                break;

            case FEDERATE_MINUS:
                FederateVisitor.MINUS = b;
                break;

            case FEDERATE_UNDEFINED:
                FederateVisitor.UNDEFINED = b;
                break;

            case FEDERATE_JOIN_PATH:
                FederateVisitor.SELECT_JOIN_PATH = b;
                break;

            case TRACE_GENERIC:
                FederateVisitor.TRACE_FEDERATE = b;
                RewriteBGPList.TRACE_BGP_LIST = b;
                break;

            case SOLVER_SORT_CARDINALITY:
                QueryProcess.setSort(b);
                break;

            case SOLVER_OVERLOAD:
                TermEval.OVERLOAD = b;
                break;

            case RDFS_ENTAILMENT:
                Graph.RDFS_ENTAILMENT_DEFAULT = b;
                break;

            case SERVICE_REPORT:
                ASTParser.SERVICE_REPORT = b;
                break;

            case SERVICE_DISPLAY_MESSAGE:
                ServiceParser.DISPLAY_MESSAGE = b;
                break;

            case STRICT_MODE:
                ASTQuery.STRICT_MODE = b;
                break;
        }
    }

    static void basicSet(Value value, String... str) {
        switch (value) {
            case FEDERATE_BLACKLIST:
                getSingleton().blacklist(str);
                break;
            case FEDERATE_BLACKLIST_EXCEPT:
                getSingleton().blacklistExcept(str);
                break;
        }
    }

    static void basicSet(Value value, String str) {
        getSingleton().getStringProperty().put(value, str);
        switch (value) {

            case FEDERATION:
                getSingleton().defineFederation(str);
                break;

            case FEDERATE_INDEX_PATTERN:
                SelectorIndex.QUERY_PATTERN = str;
                break;

            case FEDERATE_QUERY_PATTERN:
                getSingleton().setQueryPattern(str);
                break;

            case FEDERATE_PREDICATE_PATTERN:
                getSingleton().setPredicatePattern(str);
                break;

            case FEDERATE_FILTER_ACCEPT:
                getSingleton().setFilterAccept(str);
                break;

            case FEDERATE_FILTER_REJECT:
                getSingleton().setFilterReject(str);
                break;

            case FEDERATE_INDEX_SKIP:
                getSingleton().setIndexSkip(str);
                break;

            case FEDERATE_BLACKLIST:
                getSingleton().blacklist(str);
                break;

            case FEDERATE_BLACKLIST_EXCEPT:
                getSingleton().blacklistExcept(str);
                break;

            case FEDERATE_SPLIT:
                getSingleton().split(str);
                break;

            case FEDERATE_INDEX_SUCCESS:
                FederateVisitor.NB_SUCCESS = Double.parseDouble(str);
                break;

            case LOAD_FORMAT:
                Load.LOAD_FORMAT = str;
                break;

            case SERVICE_BINDING:
                CompileService.setBinding(str);
                break;

            case SERVICE_PARAMETER:
                // set in table
                break;

            case SERVICE_HEADER:
                getSingleton().getListProperty().put(SERVICE_HEADER, getSingleton().getList(str));
                break;

            case LDSCRIPT_VARIABLE:
                getSingleton().variable();
                break;

            case BLANK_NODE:
                Graph.BLANK = str;
                break;

            case SOLVER_QUERY_PLAN:
                getSingleton().queryPlan(str);
                break;

            case SOLVER_VISITOR:
                QueryProcess.setVisitorName(getSingleton().expand(str));
                break;

            case RULE_VISITOR:
                QuerySolverVisitorRule.setVisitorName(getSingleton().expand(str));
                break;

            case TRANSFORMER_VISITOR:
                QuerySolverVisitorTransformer.setVisitorName(getSingleton().expand(str));
                break;

            case SERVER_VISITOR:
                QueryProcess.setServerVisitorName(getSingleton().expand(str));
                break;

            case ACCESS_LEVEL:
                getSingleton().accessLevel(str);
                break;

            case PREFIX:
                getSingleton().prefix();
                break;

            case LOAD_FUNCTION:
                getSingleton().loadFunction(str);
                break;
        }
    }

    static void basicSet(Value value, int n) {
        getSingleton().getIntegerProperty().put(value, n);

        switch (value) {

            case SERVICE_SLICE:
            case SERVICE_LIMIT:
            case SERVICE_TIMEOUT:
                // use integer table
                break;

            case SERVICE_DISPLAY_RESULT:
                ProviderService.DISPLAY_RESULT_MAX = n;
                ContextLog.DISPLAY_RESULT_MAX = n;
                Eval.DISPLAY_RESULT_MAX = n;
                break;

            case LOAD_LIMIT:
                Load.setLimitDefault(n);
                break;

            case FUNCTION_PARAMETER_MAX:
                ASTExtension.FUNCTION_PARAMETER_MAX = n;
                break;

            case FEDERATE_INDEX_LENGTH:
                FederateVisitor.NB_ENDPOINT = n;
                break;
        }
    }

    public static List<List<String>> getStorageparameters() {

        String storages = stringValue(STORAGE);

        List<List<String>> storageList = new ArrayList<>();
        if (storages == null) {
            return storageList;
        }

        for (String storageStr : storages.split(SEP)) {

            String[] storageLst = storageStr.split(",", 3);
            storageList.add(List.of(storageLst).stream().map(str -> getSingleton().expand(str)).collect(Collectors.toList()));
        }
        return storageList;
    }

    public static Integer intValue(Value val) {
        return getSingleton().getIntegerProperty().get(val);
    }

    public static List<String> listValue(Value value) {
        return getSingleton().getListProperty().get(value);
    }

    public static String stringValue(Value val) {
        return getSingleton().getStringProperty().get(val);
    }

    public static String[] stringValueList(Value val) {
        String str = stringValue(val);
        if (val == null || str == null) {
            return new String[0];
        }
        return str.split(SEP);
    }

    // current storage mode to create QueryProcess
    public static boolean isDataset() {
        if (stringValue(STORAGE) == null) {
            // there is no db storage path
            return true;
        }
        if (stringValue(STORAGE_MODE) == null) {
            // there is db storage path and no mode specified -> db mode, not dataset
            return false;
        }
        // STORAGE_MODE = db|dataset
        return stringValue(STORAGE_MODE).equals(DATASET);
    }

    // current storage mode
    public static boolean isStorage() {
        return !isDataset();
    }

    // consider all db
    public static boolean isStorageAll() {
        return isStorage() &&
                getSingleton().protectEquals(stringValue(STORAGE_MODE), DB_ALL);
    }

    private boolean hasBooleanProperty(Value value) {
        return getBooleanProperty().containsKey(value);
    }

    private boolean hasStringProperty(Value value) {
        return getStringProperty().containsKey(value);
    }

    private boolean hasIntegerProperty(Value value) {
        return getIntegerProperty().containsKey(value);
    }

    private boolean hasListProperty(Value value) {
        return getListProperty().containsKey(value);
    }

    void basicLoad(String path) throws IOException {
        setPath(path);
        File file = new File(path);
        setParent(file.getParent());
        getImports().put(path, path);
        try (FileReader loadPathReader = new FileReader(path)) {
            getProperties().load(loadPathReader);
        } catch (IOException e) {
            logger.error("Error loading: " + path);
            throw e;
        }
        // start with variable because import may use variable,
        // as well as other properties
        defineVariable();
        imports();
        init();
    }

    /**
     * @todo: ./ in imported file is the path of main property file
     * @todo: there is no recursive import
     */
    private void imports() throws IOException {
        if (getProperties().containsKey(IMPORT.toString())) {

            for (String name : ((String) get(IMPORT)).split(SEP)) {
                String importPath = expand(name);

                if (getImports().containsKey(importPath)) {
                } else {
                    getImports().put(importPath, importPath);
                    try (FileReader importReader = new FileReader(importPath)) {
                        getProperties().load(importReader);
                    }
                    // @note: imported variable overload Properties and Property variable property
                    // but complete variable hashmap properly
                    defineVariable();
                }
            }
        }
    }

    void init() {
        defineProperty();
    }

    void defineProperty() {
        for (String name : getProperties().stringPropertyNames()) {
            String value = getProperties().getProperty(name);
            try {
                define(name, value);
            } catch (Exception e) {
                logger.error("Incorrect Property: " + name + " " + value);
            }
        }
    }

    /**
     * Do it first
     * VARIABLE = $gui=/a/path
     */
    void defineVariable() {
        if (getProperties().containsKey(VARIABLE.toString())) {
            basicSet(VARIABLE, (String) get(VARIABLE));
            // variable definitions in a hashmap
            defineVariableMap();
        }
    }

    void setQueryPattern(String str) {
        QueryLoad ql = QueryLoad.create();
        for (Pair pair : getValueList(Value.FEDERATE_QUERY_PATTERN)) {
            try {
                SelectorIndex.defineQueryPattern(pair.getKey(), ql.readWE(pair.getPath()));
            } catch (LoadException ex) {
                logger.error(ex.getMessage());
            }
        }
    }

    void setPredicatePattern(String str) {
        QueryLoad ql = QueryLoad.create();
        for (Pair pair : getValueList(Value.FEDERATE_PREDICATE_PATTERN)) {
            try {
                SelectorIndex.definePredicatePattern(pair.getKey(), ql.readWE(pair.getPath()));
            } catch (LoadException ex) {
                logger.error(ex.getMessage());
            }
        }
    }

    void setFilterAccept(String str) {
        for (String ope : str.split(SEP)) {
            SelectorFilter.defineOperator(ope, true);
        }
    }

    void setFilterReject(String str) {
        for (String ope : str.split(SEP)) {
            SelectorFilter.rejectOperator(ope, true);
        }
    }

    void setIndexSkip(String str) {
        for (String ope : str.split(SEP)) {
            SelectorIndex.skipPredicate(ope);
        }
    }

    void split(String list) {
        ArrayList<String> alist = new ArrayList<>();
        for (String str : list.split(SEP)) {
            alist.add(NSManager.nsm().toNamespace(str));
        }
        FederateVisitor.DEFAULT_SPLIT = alist;
    }

    List<String> getList(String list) {
        return new ArrayList<>(Arrays.asList(list.split(SEP)));
    }

    void blacklist(String list) {
        FederateVisitor.BLACKLIST = getSingleton().getList(list);
    }

    void blacklistExcept(String list) {
        FederateVisitor.BLACKLIST_EXCEPT = getSingleton().getList(list);
    }

    void blacklist(String... list) {
        FederateVisitor.BLACKLIST = new ArrayList<>(Arrays.asList(list));
    }

    void blacklistExcept(String... list) {
        ArrayList<String> alist = new ArrayList<>();
        Collections.addAll(alist, list);
        FederateVisitor.BLACKLIST_EXCEPT = alist;
    }

    // variable definition may use preceding variables
    private void defineVariableMap() {
        for (Pair pair : getValueListBasic(Value.VARIABLE)) {
            String variable = varName(pair.getKey());

            getVariableMap().put(variable, expand(pair.getValue()));
        }
    }

    String varName(String key) {
        return key.startsWith(VAR_CHAR) ? key : VAR_CHAR + key;
    }

    String expand(String value) {
        if (value.startsWith(VAR_CHAR)) {
            for (String variable : getVariableMap().keySet()) {
                if (value.startsWith(variable)) {
                    return value.replace(variable, getVariableMap().get(variable));
                }
            }
        } else if (value.startsWith("./")) {
            // relative path
            return complete(value);
        }
        return value;
    }

    String complete(String value) {
        return getParent().concat(value.substring(1));
    }

    void queryPlan(String str) {
        switch (str) {
            case STD:
                QuerySolver.QUERY_PLAN = Query.QP_DEFAULT;
                break;
            default:
                QuerySolver.QUERY_PLAN = Query.QP_HEURISTICS_BASED;
                break;
        }
    }

    void defineFederation(String path) {
        QueryProcess exec = QueryProcess.create(Graph.create());
        try {
            Graph g = exec.defineFederation(path);
        } catch (IOException | EngineException | LoadException ex) {
            logger.error(ex.toString());
        }
    }

    void loadFunction(String str) {
        QueryProcess exec = QueryProcess.create();
        for (String name : str.split(SEP)) {
            try {
                exec.imports(name);
            } catch (EngineException ex) {
                logger.error(ex.toString());
            }
        }
    }

    /**
     * Init graph with properties such as load dataset
     * use case: corese gui
     */
    void basicInit(Graph g) {
        for (String name : getProperties().stringPropertyNames()) {
            String value = getProperties().getProperty(name);
            try {
                define(name, value, g);
            } catch (Exception e) {
                logger.error(e.toString());
            }
        }

        // after load dataset
        if (getStringProperty().containsKey(LOAD_RULE)) {
            loadRule(g, getStringProperty().get(LOAD_RULE));
        }
    }

    void define(String name, String value, Graph g) {
        try {
            Value pname = Value.valueOf(name);
            define(pname, value, g);
        } catch (Exception ignored) {

        }
    }

    void define(Value name, String value, Graph g) {
        switch (name) {
            case LOAD_DATASET:
                loadList(g, value);
                break;
        }
    }

    void loadRule(Graph g, String path) {
        for (String name : path.split(SEP)) {
            RuleEngine re = RuleEngine.create(g);
            try {
                String file = expand(name);
                re.setProfile(file);
                re.process();
            } catch (LoadException | EngineException ex) {
                logger.error(ex.toString());
            }
        }
    }

    void loadList(Graph g, String path) {
        Load ld = Load.create(g);
        for (String name : path.split(SEP)) {
            try {
                String file = expand(name);
                ld.parse(file.strip());
            } catch (LoadException ex) {
                logger.error(ex.toString());
            }
        }
    }

    /**
     * LDScript static variable
     * LDSCRIPT_VARIABLE = var=val;var=val
     */
    void variable() {
        for (Pair pair : getValueListBasic(Value.LDSCRIPT_VARIABLE)) {
            String variable = pair.getKey().strip();
            String val = pair.getValue().strip();
            IDatatype dt = DatatypeMap.newValue(val);
            Binding.setStaticVariable(variable, dt);
        }
    }

    public List<Pair> getValueList(Value val) {
        return getValueListBasic(val);
    }

    public List<Pair> getValueListBasic(Value val) {
        String str = stringValue(val);
        ArrayList<Pair> list = new ArrayList<>();
        if (str == null) {
            return list;
        }
        for (String elem : str.split(SEP)) {
            String[] def = elem.split(EQ, 2);
            if (def.length >= 2) {
                list.add(new Pair(def[0], def[1]));
            }
        }
        return list;
    }

    void prefix() {
        for (Pair pair : getValueListBasic(PREFIX)) {
            NSManager.defineDefaultPrefix(pair.getKey().strip(), pair.getValue().strip());
        }
    }

    void accessLevel(String str) {
        try {
            Level level = Level.valueOf(str);
            Access.setDefaultUserLevel(level);
        } catch (Exception e) {
            logger.error("Undefined Access Level: {}", str);
        }
    }

    protected Map<Value, Boolean> getBooleanProperty() {
        return booleanProperty;
    }

    protected void setBooleanProperty(Map<Value, Boolean> booleanProperty) {
        this.booleanProperty = booleanProperty;
    }

    protected Map<Value, String> getStringProperty() {
        return stringProperty;
    }

    protected void setStringProperty(Map<Value, String> stringProperty) {
        this.stringProperty = stringProperty;
    }

    protected Properties getProperties() {
        return properties;
    }

    protected void setProperties(Properties properties) {
        this.properties = properties;
    }

    protected Map<Value, Integer> getIntegerProperty() {
        return integerProperty;
    }

    protected void setIntegerProperty(Map<Value, Integer> integerProperty) {
        this.integerProperty = integerProperty;
    }

    protected String pathValue(Value val) {
        return expand(stringValue(val));
    }

    protected Map<String, String> getVariableMap() {
        return variableMap;
    }

    protected void setVariableMap(Map<String, String> variableMap) {
        this.variableMap = variableMap;
    }

    protected String getPath() {
        return path;
    }

    protected void setPath(String path) {
        this.path = path;
    }

    protected String getParent() {
        return parent;
    }

    protected void setParent(String parent) {
        this.parent = parent;
    }

    protected Map<String, String> getImports() {
        return imports;
    }

    protected void setImports(Map<String, String> imports) {
        this.imports = imports;
    }

    protected Map<Value, List<String>> getListProperty() {
        return listProperty;
    }

    protected void setListProperty(Map<Value, List<String>> listProperty) {
        this.listProperty = listProperty;
    }

    boolean protectEquals(String variable, String value) {
        return variable != null && variable.equals(value);
    }

    public enum Value {
        // VARIABLE = home=/home/name/dir
        VARIABLE,
        IMPORT,

        TRACE_MEMORY,
        TRACE_GENERIC,
        // generic property for testing purpose
        TEST_FEDERATE,
        // turtle file path where federation are defined
        FEDERATION,
        // generate partition of connected bgp
        FEDERATE_BGP,
        // do not split complete partition if any
        FEDERATE_PARTITION,
        // test and use join between right and left exp of optional
        FEDERATE_OPTIONAL,
        FEDERATE_MINUS,
        FEDERATE_UNDEFINED,
        // complete bgp partition with additional partition of triple alone (as before)
        FEDERATE_COMPLETE,
        // source selection with filter
        FEDERATE_FILTER,
        FEDERATE_FILTER_ACCEPT,
        FEDERATE_FILTER_REJECT,
        // source selection with bind (exists {t1 . t2} as ?b_i)
        FEDERATE_JOIN,
        // authorize path in join test
        FEDERATE_JOIN_PATH,
        FEDERATE_SPLIT,

        // index query pattern skip predicate for source discovery
        FEDERATE_INDEX_SKIP,
        FEDERATE_INDEX_PATTERN,
        FEDERATE_INDEX_SUCCESS,
        FEDERATE_INDEX_LENGTH,

        FEDERATE_BLACKLIST,
        FEDERATE_BLACKLIST_EXCEPT,
        FEDERATE_QUERY_PATTERN,
        FEDERATE_PREDICATE_PATTERN,

        // boolan value
        DISPLAY_URI_AS_PREFIX,
        // rdf star reference node displayed as nested triple
        DISPLAY_AS_TRIPLE,
        // Graph node implemented as IDatatype instead of NodeImpl
        GRAPH_NODE_AS_DATATYPE,
        BLANK_NODE,
        // load rdf file into graph kg:default instead of graph file-path
        LOAD_IN_DEFAULT_GRAPH,
        // constraint rule error in specific named graph
        CONSTRAINT_NAMED_GRAPH,
        // constraint rule error in external named graph
        CONSTRAINT_GRAPH,
        // graph ?g { } iterate std and external named graph
        EXTERNAL_NAMED_GRAPH,
        GRAPH_INDEX_END,
        GRAPH_INDEX_TRANSITIVE,
        GRAPH_INDEX_LOAD_SKIP,
        // rdf* draft
        RDF_STAR,
        // enforce compliance: no literal as subject
        RDF_STAR_VALIDATION,
        // joker: asserted query triple return asserted and nested triple (default
        // false)
        RDF_STAR_SELECT,
        // joker: asserted delete triple deletes asserted and nested triple (default
        // false)
        RDF_STAR_DELETE,
        // use TripleNode implementation
        RDF_STAR_TRIPLE,
        // corese server for micro services
        REENTRANT_QUERY,
        // activate access level control (default is true)
        ACCESS_LEVEL,
        // activate access right for rdf triples wrt to namespace (default is false)
        ACCESS_RIGHT,
        // activate @event ldscript function call for sparql query processing
        EVENT,
        SKOLEMIZE,

        VERBOSE,
        SOLVER_DEBUG,
        TRANSFORMER_DEBUG,

        LOG_NODE_INDEX,
        LOG_RULE_CLEAN,

        SOLVER_SORT_CARDINALITY,
        SOLVER_QUERY_PLAN, // STD | ADVANCED
        // string value
        SOLVER_VISITOR,
        SOLVER_OVERLOAD,
        RULE_VISITOR,
        TRANSFORMER_VISITOR,
        SERVER_VISITOR,
        PREFIX,
        // Testing purpose
        INTERPRETER_TEST,
        // 1 ; 01 ; 1.0 have different Node
        // when true: nodes can be joined by graph matching
        // when false: they do not join
        DATATYPE_ENTAILMENT,
        SPARQL_COMPLIANT,
        SPARQL_ORDER_UNBOUND_FIRST,

        OWL_AUTO_IMPORT,
        OWL_CLEAN,
        OWL_CLEAN_QUERY,
        OWL_RL,

        RULE_TRANSITIVE_FUNCTION,
        RULE_TRANSITIVE_OPTIMIZE,
        // rule engine use edge index with data manager
        RULE_DATAMANAGER_OPTIMIZE,
        // replace kg:rule_i by kg:rule
        RULE_DATAMANAGER_CLEAN,
        // for testing edge iterator filter edge index
        RULE_DATAMANAGER_FILTER_INDEX,
        RULE_TRACE,

        FUNCTION_PARAMETER_MAX,

        // init graph
        GUI_TITLE,
        GUI_BROWSE,
        GUI_XML_MAX,
        GUI_TRIPLE_MAX,
        GUI_INDEX_MAX,
        // rdf+xml turtle json
        GUI_CONSTRUCT_FORMAT,
        GUI_SELECT_FORMAT,
        GUI_DEFAULT_QUERY,
        GUI_QUERY_LIST,
        GUI_TEMPLATE_LIST,
        GUI_EXPLAIN_LIST,
        GUI_RULE_LIST,

        // application/rdf+xml
        LOAD_FORMAT,
        // integer value
        // max number of triples for each rdf file load
        LOAD_LIMIT,
        LOAD_WITH_PARAMETER,
        LOAD_DATASET,
        LOAD_QUERY,
        LOAD_FUNCTION,
        LOAD_RULE,

        RDFS_ENTAILMENT,

        LDSCRIPT_VARIABLE,
        LDSCRIPT_DEBUG,
        LDSCRIPT_CHECK_DATATYPE,
        LDSCRIPT_CHECK_RDFTYPE,

        SERVICE_BINDING,
        SERVICE_SLICE,
        SERVICE_LIMIT,
        SERVICE_TIMEOUT,
        SERVICE_SEND_PARAMETER,
        SERVICE_PARAMETER,
        SERVICE_LOG,
        SERVICE_REPORT,
        SERVICE_DISPLAY_RESULT,
        SERVICE_DISPLAY_MESSAGE,
        SERVICE_HEADER,

        // service result may be RDF graph (e.g. when format=turtle)
        // apply service query on the graph
        SERVICE_GRAPH,

        STORAGE,
        STORAGE_SERVICE,
        // default storage: db|dataset
        STORAGE_MODE,

        // parser configuration
        STRICT_MODE,

        // Elasticsearch parameters
        // TODO Change class to be able to define application-specific properties
        ELASTICSEARCH_API_KEY,
        ELASTICSEARCH_API_ADDRESS,
    }

    public class Pair {

        private String first;
        private String second;

        Pair(String f, String r) {
            first = f;
            second = r;
        }

        public String getKey() {
            return first;
        }

        public void setFirst(String first) {
            this.first = first;
        }

        public String getValue() {
            return second;
        }

        public String getPath() {
            return expand(getValue());
        }

        public void setSecond(String second) {
            this.second = second;
        }

    }

}
