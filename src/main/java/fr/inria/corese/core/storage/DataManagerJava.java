package fr.inria.corese.core.storage;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.load.LoadException;
import fr.inria.corese.core.load.QueryLoad;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.function.proxy.GraphSpecificFunction;
import fr.inria.corese.core.sparql.triple.parser.Access;
import fr.inria.corese.core.sparql.triple.parser.HashMapList;
import fr.inria.corese.core.sparql.triple.parser.NSManager;
import fr.inria.corese.core.sparql.triple.parser.URLServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * DataManager on top of JSON (or XML) document.
 *
 */
public class DataManagerJava extends CoreseGraphDataManager {
    private static final Logger logger = LoggerFactory.getLogger(DataManagerJava.class);
    private static final String QUERY = "query";
    private static final String PATH = "path";
    private static final String PARAM = "param";
    private static final String MODE = "mode";
    private static final String LOAD = "load";
    private static final String LDSCRIPT = "ldscript";

    String iterateFunction = NSManager.USER + "iterate";
    String readFunction = NSManager.USER + "read";
    // json document
    private IDatatype jsonDocument;
    IDatatype joker;
    private QueryProcess queryProcess;
    private boolean ldscript = false;
    String queryPath;
    private String query;
    private HashMapList<String> map;
    private List<String> load;

    // Local path field (since parent doesn't have setPath/getPath)
    private final String path;

    /**
     * Constructs a DataManagerJava with a path to query or LDScript functions.
     *
     * @param path Path to INSERT WHERE query or LDScript functions
     */
    public DataManagerJava(String path) {
        super(); // Call parent constructor
        URLServer url = new URLServer(path);
        // Remove parameter if any
        this.path = url.getServer();
        setQueryPath(url.getServer());
    }

    /**
     * Starts the data manager with configuration.
     * Called at creation time in StorageFactory.
     *
     * @param map Configuration parameters
     */
    public void start(HashMapList<String> map) {
        logger.info("Start data manager: {}", path);
        if (map == null) {
            init();
        } else {
            boolean isInit = localInit(map);
            if (!isInit) {
                init();
            }
        }
    }

    /**
     * Initializes with path parameter map.
     * Called by service store clause in ProviderService.
     *
     * @param map Configuration parameters
     */
    public void init(HashMapList<String> map) {
        localInit(map);
    }

    boolean localInit(HashMapList<String> map) {
        super.init(map);
        return parameter(map);
    }

    /**
     * Processes parameters and returns true when init() is performed.
     *
     * @param map Configuration parameters
     * @return true if initialization was performed
     */
    boolean parameter(HashMapList<String> map) {
        setMap(map);
        String queryPath = map.getFirst(PATH);
        if (map.containsKey(MODE) && map.get(MODE).contains(LDSCRIPT)) {
            setLdscript(true);
        }
        if (map.containsKey(LOAD)) {
            setLoad(map.get(LOAD));
        }
        if (queryPath != null) {
            setQueryPath(queryPath);
            setQuery(null);
            logger.info("Service query path= {}", queryPath);
            init();
            return true;
        } else {
            String query = map.getFirst(QUERY);
            if (query != null) {
                query = clean(query);
                setQuery(query);
                setQueryPath(null);
                logger.info("Service query = {}", query);
                init();
                return true;
            }
        }
        return false;
    }

    String clean(String str) {
        return str.replace("%20", " ");
    }

    /**
     * Initializes based on mode (Graph or LDScript).
     */
    void init() {
        if (isLdscript()) {
            initldscript();
        } else {
            initgraph();
        }
    }

    /**
     * Initializes in Graph mode: creates graph from JSON using UPDATE query.
     */
    void initgraph() {
        logger.info("Mode graph");
        // Graph to be created by update query
        // Create new graph and reinitialize parent components
        Graph newGraph = Graph.create();

        setQueryProcess(QueryProcess.create(this));
        QueryLoad ql = QueryLoad.create();
        Load ld = Load.create(newGraph);
        ld.setDataManager(this);

        // Temporarily authorize xt:read file to read e.g. json document
        // authorize xt:read() because accept list is empty during this initialization
        Access.setDefaultResultWhenEmptyAccept(true);

        try {
            // Load data files if specified
            if (getLoad() != null) {
                for (String name : getLoad()) {
                    logger.info("Load {}", name);
                    ld.parse(name);
                }
            }

            // Get query (from path or direct string)
            String q;
            if (getQueryPath() != null) {
                logger.info("Load {}", getQueryPath());
                q = ql.readWE(getQueryPath());
            } else if (getQuery() != null) {
                q = getQuery();
            } else {
                return;
            }

            // Format query with parameters if provided
            if (getMap() != null && getMap().containsKey(PARAM)) {
                q = String.format(q, getMap().getFirst(PARAM));
            }

            logger.info("Process query:\n{}", q);

            // Execute UPDATE query to create RDF graph (from JSON)
            // This is the graph of current DataManager
            Mappings mappings = getQueryProcess().query(q);
            if (mappings.getGraph() != null) {
                // CONSTRUCT WHERE query result
                newGraph = (Graph) mappings.getGraph();
            }
            newGraph.init();

            logger.info("Graph initialized with {} edges", newGraph.size());

        } catch (LoadException | EngineException ex) {
            logger.error("Failed to initialize graph mode: {}", ex.getMessage());
        } finally {
            Access.setDefaultResultWhenEmptyAccept(false);
        }
    }

    /**
     * Initializes in LDScript mode: prepares for JSON iteration.
     */
    void initldscript() {
        logger.info("Mode ldscript");
        try {
            setQueryProcess(QueryProcess.create(Graph.create()));

            // Import LDScript functions
            getQueryProcess().imports(getQueryPath());

            // Read JSON document using LDScript us:read() function
            setJsonDocument(getQueryProcess().funcall(readFunction));

            // Joker for null pattern matching
            joker = DatatypeMap.newInstance(GraphSpecificFunction.JOKER);

            logger.info("LDScript initialized, JSON document loaded");

        } catch (EngineException ex) {
            logger.error("Failed to initialize ldscript mode: {}", ex.getMessage());
        }
    }

    /**
     * Gets edges with LDScript support.
     *
     * @param s         Subject (null for any)
     * @param p         Predicate (null for any)
     * @param o         Object (null for any)
     * @param graphList Contexts
     * @return Iterable of edges
     */
    public Iterable<Edge> getEdges(Node s, Node p, Node o, List<Node> graphList) {
        if (isLdscript()) {
            return iterateJson(s, p, o);
        }
        // In graph mode, use QueryOperations from parent
        return getGraph().iterate(s, p, o, graphList);
    }

    /**
     * Iterates over JSON document using LDScript us:iterate() function.
     *
     * @param s Subject pattern (null for any)
     * @param p Predicate pattern (null for any)
     * @param o Object pattern (null for any)
     * @return Iterable of matching edges
     */
    Iterable<Edge> iterateJson(Node s, Node p, Node o) {
        try {
            IDatatype dt = getQueryProcess().funcall(
                    iterateFunction,
                    getJsonDocument(),
                    value(s),
                    value(p),
                    value(o)
            );
            if (dt == null) {
                return new ArrayList<>(0);
            }
            return cast(dt);
        } catch (EngineException ex) {
            logger.error("Failed to iterate JSON: {}", ex.getMessage());
        }
        return new ArrayList<>(0);
    }

    /**
     * Converts Node to IDatatype value, or joker if null.
     *
     * @param n Node to convert
     * @return IDatatype value or joker
     */
    IDatatype value(Node n) {
        if (n == null) {
            return joker;
        }
        return n.getDatatypeValue();
    }

    /**
     * Converts IDatatype list to Edge list.
     *
     * @param list IDatatype list of triple references
     * @return List of edges
     */
    Iterable<Edge> cast(IDatatype list) {
        ArrayList<Edge> edgeList = new ArrayList<>();
        for (IDatatype dt : list) {
            edgeList.add(dt.getEdge());
        }
        return edgeList;
    }

    public QueryProcess getQueryProcess() {
        return queryProcess;
    }

    public void setQueryProcess(QueryProcess queryProcess) {
        this.queryProcess = queryProcess;
    }

    public IDatatype getJsonDocument() {
        return jsonDocument;
    }

    public void setJsonDocument(IDatatype json) {
        this.jsonDocument = json;
    }

    public String getQueryPath() {
        return queryPath;
    }

    public void setQueryPath(String path) {
        queryPath = path;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public HashMapList<String> getMap() {
        return map;
    }

    public void setMap(HashMapList<String> map) {
        this.map = map;
    }

    public boolean isLdscript() {
        return ldscript;
    }

    public void setLdscript(boolean ldscript) {
        this.ldscript = ldscript;
    }

    public List<String> getLoad() {
        return load;
    }

    public void setLoad(List<String> load) {
        this.load = load;
    }

    public String getPath() {
        return path;
    }
}