package fr.inria.corese.core.query;

import fr.inria.corese.core.Event;
import fr.inria.corese.core.EventManager;
import fr.inria.corese.core.Graph;
import fr.inria.corese.core.api.DataBroker;
import fr.inria.corese.core.api.DataBrokerConstruct;
import fr.inria.corese.core.api.Loader;
import fr.inria.corese.core.api.Log;
import fr.inria.corese.core.approximate.ext.ASTRewriter;
import fr.inria.corese.core.compiler.eval.Interpreter;
import fr.inria.corese.core.compiler.eval.QuerySolver;
import fr.inria.corese.core.compiler.eval.QuerySolverVisitor;
import fr.inria.corese.core.compiler.federate.FederateVisitor;
import fr.inria.corese.core.compiler.parser.Transformer;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.api.query.Evaluator;
import fr.inria.corese.core.kgram.api.query.Matcher;
import fr.inria.corese.core.kgram.api.query.ProcessVisitor;
import fr.inria.corese.core.kgram.api.query.Producer;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.kgram.core.*;
import fr.inria.corese.core.kgram.tool.MetaProducer;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.load.LoadException;
import fr.inria.corese.core.load.QueryLoad;
import fr.inria.corese.core.load.Service;
import fr.inria.corese.core.logic.Entailment;
import fr.inria.corese.core.print.LogManager;
import fr.inria.corese.core.print.TripleFormat;
import fr.inria.corese.core.producer.DataBrokerConstructExtern;
import fr.inria.corese.core.query.update.GraphManager;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.api.QueryVisitor;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.function.script.Funcall;
import fr.inria.corese.core.sparql.triple.function.script.Function;
import fr.inria.corese.core.sparql.triple.function.term.Binding;
import fr.inria.corese.core.sparql.triple.parser.*;
import fr.inria.corese.core.sparql.triple.parser.Access.Feature;
import fr.inria.corese.core.sparql.triple.parser.Access.Level;
import fr.inria.corese.core.sparql.triple.parser.context.ContextLog;
import fr.inria.corese.core.storage.api.dataManager.DataManager;
import fr.inria.corese.core.transform.TemplateVisitor;
import fr.inria.corese.core.util.Extension;
import fr.inria.corese.core.util.Property;
import jakarta.ws.rs.client.ResponseProcessingException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static fr.inria.corese.core.util.Property.Value.SERVICE_HEADER;

/**
 * Evaluator of SPARQL query by KGRAM
 * <p>
 * Query and Update are synchronized by a read/write lock on the graph There may
 * be several query in parallel OR only one update In addition, graph.init() is
 * synchronized because it may modify the graph
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 */
public class QueryProcess extends QuerySolver {
    public static final String SHACL = "http://ns.inria.fr/sparql-template/function/datashape/main.rq";
    static final String DB_FACTORY = "fr.inria.corese.tinkerpop.Factory";
    static final String DB_INPUT = "fr.inria.corese.tinkerpop.dbinput";
    static final String FUNLIB = "/function/";
    private static final String EVENT = "event";
    static HashMap<String, Producer> dbmap;
    private static Logger logger = LoggerFactory.getLogger(QueryProcess.class);
    private static ProducerImpl dbProducer;
    private static String solverVisitorName = null;
    private static String serverVisitorName = null;
    // sort query edges taking cardinality into account
    private static boolean isSort = false;
    // mode where update is authorized within query execution
    // pragma: the update is done an external named graph
    // hence it does not brake the graph that is queried
    private static boolean overWrite = false;

    static {
        dbmap = new HashMap<>();
        new Extension().process();
    }

    Transformer transformer;
    Loader load;
    // fake eval for funcall public function
    Eval eval;
    ReentrantReadWriteLock lock;
    // Producer may perform match locally
    boolean isMatch = false;
    private QueryProcessUpdate queryProcessUpdate;
    private ProducerImpl localProducer;
    private DataBrokerConstruct dataBrokerUpdate;
    // true: execute start/end transaction before query
    // false: case where we execute a subquery (e.g. xt:sparql)
    // or a query within rule engine
    private boolean processTransaction = true;

    public QueryProcess() {
    }

    protected QueryProcess(Producer p, Interpreter e, Matcher m) {
        super(p, e, m);
        Graph g = getGraph(p);
        complete();
        init();
    }

    public static QueryProcess create() {
        return create(Graph.create());
    }

    public static QueryProcess create(Graph g) {
        return create(g, false);
    }

    /**
     * Query processor for external graph
     * Provide DataManager for query and update of external graph
     * DataManager is stored in ProducerImpl
     * DataManager is used when create GraphManager for update
     * There is still a local corese graph for compatibility
     * Use of DataManager is done in core.producer.DataBrokerExtern
     * and core.producer.DataBrokerUpdateExtern
     * SPARQL construct where return a corese graph
     */
    public static QueryProcess create(DataManager dm) {
        return create(Graph.create(), dm);
    }

    public static QueryProcess create(Graph g, DataManager dm) {
        QueryProcess exec = create(g);
        exec.defineDataManager(dm);
        return exec;
    }

    /**
     * Query processor for external graph
     * Provide DataManager for query and update of external graph
     * DataManager is stored in ProducerImpl
     * DataManager is used when create GraphManager for update
     * There is still a local corese graph for compatibility
     * Use of DataManager is done in core.producer.DataBrokerExtern
     * and core.producer.DataBrokerUpdateExtern
     * SPARQL construct where return a corese graph
     */

    // several Producer for several DataManager
    public static QueryProcess create(Graph g, DataManager[] dmList) {
        QueryProcess exec = create(g);

        if (dmList.length > 0) {
            exec.setDataManager(g, dmList);
        }

        return exec;
    }

    /**
     * isMatch = true: ?x a h:Person return one occurrence for each instance of
     * Person
     * isMatch = false: ?x a h:Person return all occurrences for each instance of
     * Person
     * where the instance has several types which match Person, such as x a h:Man,
     * h:Person
     * default isMatch = false
     * In addition, each Producer perform local Matcher.match() on its own
     * graph for subsumption Hence each graph can have its own ontology and
     * return one occurrence of each resource for ?x rdf:type aClass isMatch =
     * false: (default) Global producer perform Matcher.match()
     */
    public static QueryProcess create(Graph g, boolean isMatch) {
        String factory = System.getProperty("fr.inria.corese.factory");
        if (factory == null || factory.compareTo("") == 0) {
            return stdCreate(g, isMatch);
        } else {
            return dbCreate(g, isMatch, factory, null);
        }
    }

    public static QueryProcess stdCreate(Graph g, boolean isMatch) {
        ProducerImpl p = ProducerImpl.create(g);
        p.setMatch(isMatch);
        QueryProcess exec = QueryProcess.create(p);
        exec.setMatch(isMatch);
        return exec;
    }

    public static QueryProcess copy(Producer p, boolean isMatch) {
        QueryProcess exec = stdCreate(getGraph(p), isMatch);
        exec.defineDataManager(exec.getDataManager(p));
        return exec;
    }

    public static QueryProcess create(ProducerImpl p) {
        Matcher match = MatcherImpl.create(p.getGraph());
        p.set(match);
        if (p.isMatch()) {
            // there is local match in Producer
            // create global match with Relax mode
            match = MatcherImpl.create(p.getGraph());
            match.setMode(Matcher.RELAX);
        }
        QueryProcess exec = QueryProcess.create(p, createInterpreter(p, match), match);
        exec.setLocalProducer(p);
        return exec;
    }

    public static QueryProcess create(Producer p) {
        if (p instanceof ProducerImpl) {
            return create((ProducerImpl) p);
        } else {
            return createExtern(p);
        }
    }

    public static QueryProcess createExtern(Producer p) {
        Matcher match = MatcherImpl.create(Graph.create());
        match.setMode(Matcher.RELAX);
        QueryProcess exec = QueryProcess.create(p, createInterpreter(p, match), match);
        // for compatibility reason e.g. with DataManager:
        exec.setLocalProducer(new ProducerImpl(Graph.create()));
        return exec;
    }

    public static QueryProcess create(Producer prod, Interpreter eval, Matcher match) {
        return new QueryProcess(prod, eval, match);
    }

    /**
     * When there is a graph database to manage the graph
     */
    public static QueryProcess dbCreate(Graph g, boolean isMatch, String factory, String db) {
        Producer p = getCreateProducer(g, factory, db);
        QueryProcess exec = QueryProcess.create(p);
        exec.setMatch(isMatch);
        return exec;
    }

    public static synchronized Producer getCreateProducer(Graph g, String factory, String db) {
        if (db == null) {
            if (dbProducer == null) {
                logger.info("property fr.inria.corese.factory defined. Using factory: " + factory);
                dbProducer = createProducer(g, factory, db);
            }
            return dbProducer;
        } else {
            Producer prod = dbmap.get(db);
            if (prod == null) {
                prod = createProducer(g, factory, db);
                dbmap.put(db, prod);
            }
            return prod;
        }
    }

    static ProducerImpl createProducer(Graph g, String factory, String db) {
        if (db != null) {
            System.setProperty(DB_INPUT, db);
        }
        try {
            Class<?> classFactory = Class.forName(factory);
            Method method = classFactory.getMethod("create", Graph.class);
            ProducerImpl p = (ProducerImpl) method.invoke(null, g);
            logger.info("Connect db");
            return p;
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException ex) {
            logger.error("impossible to create a producer, aborting", ex);
        }
        return ProducerImpl.create(g);
    }

    public static QueryProcess create(Graph g, Graph g2) {
        QueryProcess qp = QueryProcess.create(g);
        qp.add(g2);
        return qp;
    }

    /**
     * Create an Eval initialized with a query q that contains function
     * definitions This Eval can be used to call these functions:
     * eval.eval(name, param) Use case: define callback functions.
     */
    public static Eval createEval(Graph g, String q) throws EngineException {
        QueryProcess exec = create(g);
        return exec.createEval(q, null);
    }

    public static Eval createEval(Graph g, Query q) throws EngineException {
        QueryProcess exec = create(g);
        return exec.createEval(q);
    }

    public static void setSort(boolean b) {
        isSort = b;
    }

    /**
     * Filter and LDScript Interpreter
     */
    public static Interpreter createInterpreter(Producer p, Matcher m) {
        Interpreter eval = new Interpreter();
        eval.setPlugin(PluginImpl.create(m));
        eval.setProducer(p);
        return eval;
    }

    static boolean isOverwrite() {
        return isReentrant();
    }

    public static void setOverwrite(boolean b) {
        setReentrant(b);
    }

    public static boolean isReentrant() {
        return overWrite;
    }

    public static void setReentrant(boolean b) {
        overWrite = b;
    }

    static Graph getGraph(Producer p) {
        if (p.getGraph() instanceof Graph) {
            return (Graph) p.getGraph();
        }
        return Graph.create();
    }

    public static String getVisitorName() {
        return solverVisitorName;
    }

    public static void setVisitorName(String aSolverVisitorName) {
        solverVisitorName = aSolverVisitorName;
    }

    public static String getServerVisitorName() {
        return serverVisitorName;
    }

    public static void setServerVisitorName(String name) {
        serverVisitorName = name;
    }

    void complete() {
        // service
        set(ProviderImpl.create(this));
        setQueryProcessUpdate(new QueryProcessUpdate(this));
    }

    void init() {
        Graph g = getGraph();
        if (isSort && g != null) {
            set(SorterImpl.create(g));
        }

        if (g != null) {
            lock = g.getLock();
        } else {
            // TODO: the lock should be unique to all calls
            // hence it should be provided by Producer
            lock = new ReentrantReadWriteLock();
        }
    }

    public void defineDataManager(DataManager dm) {
        if (dm != null && getLocalProducer() != null) {
            getLocalProducer().defineDataManager(dm);
        }
    }

    void setDataManager(Graph g, DataManager[] dmList) {
        getLocalProducer().defineDataManager(dmList[0]);
        MetaProducer meta = MetaProducer.create();

        for (DataManager dm : dmList) {
            ProducerImpl p = ProducerImpl.create(g);
            Matcher match = MatcherImpl.create(g);
            p.set(match);
            p.defineDataManager(dm);
            meta.add(p);
        }

        setProducer(meta);
    }

    // inherit DataManager if any
    public QueryProcess copy() {
        return copy(getProducer(), isMatch());
    }

    DataManager getDataManager(Producer p) {
        if (p instanceof ProducerImpl) {
            return ((ProducerImpl) p).getDataManager();
        }
        return null;
    }

    public Loader getLoader() {
        return load;
    }

    public void setLoader(Loader ld) {
        load = ld;
    }

    public boolean isMatch() {
        return isMatch;
    }

    void setMatch(boolean b) {
        isMatch = b;
    }

    public Producer add(Graph g) {
        ProducerImpl p = ProducerImpl.create(g);
        Matcher match = MatcherImpl.create(g);
        p.set(match);
        if (isMatch) {
            p.setMatch(true);
        }
        add(p);
        return p;
    }

    /**
     * query = select from g where g is an external named graph
     * return a new QueryProcess with Producer(g)
     */
    QueryProcess focusFrom(Query q) {
        String name = q.getFromName();
        if (name != null) {
            Graph g = getGraph();
            Graph gg = g.getNamedGraph(name);
            if (gg != null) {
                q.getFrom().clear();
                if (isReentrant()) {
                    synchronized (g) {
                        // gg inherits external named graphs of g
                        gg.shareNamedGraph(g);
                    }
                }
                return create(gg);
            }
        }
        return this;
    }

    /*
     * *************************************************************
     *
     * API for query
     *
     ***************************************************************
     */
    public Mappings update(String squery) throws EngineException {
        return doQuery(squery, null, null);
    }

    @Override
    public Mappings query(String squery) throws EngineException {
        Mappings map = doQuery(squery, null, null);
        return map;
    }

    // rdf is a turtle document
    // parse it as sparql query graph pattern (where bnode are variable)
    public Mappings queryTurtle(String rdf) throws EngineException {
        return doQuery(rdf, null, Dataset.create().setLoad(true));
    }

    // translate graph g as turtle ast query graph pattern
    public Mappings queryTurtle(Graph g) throws EngineException {
        String rdf = TripleFormat.create(g).setGraphQuery(true).toString();
        return doQuery(rdf, null, Dataset.create().setLoad(true));
    }

    // translate graph g as trig ast query graph pattern
    public Mappings queryTrig(Graph g) throws EngineException {
        // trig where default graph kg:default is printed
        // in turtle without embedding graph kg:default { }
        String rdf = TripleFormat.create(g, true).setGraphQuery(true).toString();
        return doQuery(rdf, null, Dataset.create().setLoad(true));
    }

    // translate graph g as trig ast query graph pattern
    public Mappings query(Graph g) throws EngineException {
        return queryTrig(g);
    }

    /**
     * defaut and named specify a Dataset if the query has no from/using (resp.
     * named), kgram use defaut (resp. named) if it exist for update, defaut is
     * also used in the delete clause (when there is no with in the query) W3C
     * sparql test cases use this function
     */
    @Override
    public Mappings query(String squery, Mapping map, Dataset ds) throws EngineException {
        return doQuery(squery, map, ds);
    }

    public Mappings query(String squery, Dataset ds) throws EngineException {
        return query(squery, null, ds);
    }

    public Mappings query(String squery, Context c) throws EngineException {
        return query(squery, null, Dataset.create(c));
    }

    public Mappings query(String squery, AccessRight access) throws EngineException {
        return query(squery, new Context(access));
    }

    @Override
    public Mappings query(String squery, Mapping map) throws EngineException {
        return query(squery, map, null);
    }

    public Mappings query(String squery, Binding b) throws EngineException {
        return query(squery, Mapping.create(b), null);
    }

    public Mappings query(String squery, Context c, Binding b) throws EngineException {
        return query(squery, Mapping.create(b), Dataset.create(c));
    }

    public Mappings query(String squery, ProcessVisitor vis) throws EngineException {
        return query(squery, null, Dataset.create(vis));
    }

    Mappings doQuery(String squery, Mapping map, Dataset ds) throws EngineException {
        Query q = compile(squery, ds);
        return query(null, q, map, ds);
    }

    Mappings query(Node gNode, Query q, Mapping m, Dataset ds) throws EngineException {
        return basicQuery(gNode, q, m, ds);
    }

    @Override
    public Query compile(String squery, Dataset ds) throws EngineException {
        if (!hasVisitor()) {
            // Rewrite query when @relax annotation, otherwise do nothing
            addVisitor(new ASTRewriter());
        }
        Query q = super.compile(squery, ds);
        if (q.getAST().getLog().getASTSelect() != null) {
            getLog().share(q.getAST().getLog());
        }
        return q;
    }

    public Mappings modifier(String str, Mappings map) throws SparqlException {
        Query q = compile(str, new Context().setAST(map.getAST()));
        return modifier(q, map);
    }

    @Override
    public Query compile(String squery) throws EngineException {
        return compile(squery, (Dataset) null);
    }

    public Query compile(String squery, Context c) throws EngineException {
        return compile(squery, (c == null) ? (Dataset) null : new Dataset(c));
    }

    public ASTQuery ast(String q) throws EngineException {
        Query qq = compile(q);
        return getAST(qq);
    }

    /**
     * defaut and named specify a Dataset if the query has no from/using (resp.
     * using named), kgram use this defaut (resp. named) if it exist for update,
     * this using is *not* used in the delete clause W3C sparql protocol use
     * this function
     */
    @Override
    public Mappings query(Query q) throws EngineException {
        return query(null, q, null, null);
    }

    /**
     * Use case: LDScript function execute query(construct where) or
     * query(insert where)
     * called by Interpreter exist()
     */
    @Override
    public Mappings eval(Query query, Mapping m, Producer p) throws EngineException {
        return eval(null, query, m, p);
    }

    @Override
    // @todo: getUpdateDataset ???
    public Mappings eval(Node gNode, Query query, Mapping m, Producer p) throws EngineException {
        Dataset ds = getUpdateDataset(query);
        if (p == null || p == getProducer()) {
            return protectQuery(gNode, query, m, ds);
        }
        return create(p).protectQuery(gNode, query, m, ds);
    }

    /**
     * Protect LDScript update query wrt select where use case isSynchronized()
     * :
     *
     * @event function call may perform update before or after select query
     * example: visitor init()
     */
    Mappings protectQuery(Node gNode, Query query, Mapping m, Dataset ds) throws EngineException {
        if (query.isUpdate()) {
            if (lock.getReadLockCount() > 0 && !isReentrant() && !isSynchronized()) {
                logger.info("Update rejected to avoid deadlock");
                return Mappings.create(query);
            }
        }
        return basicQuery(gNode, query, m, ds);
    }

    Dataset getUpdateDataset(Query q) {
        Context c = getContext(q);
        if (c != null && q.isUpdate()) {
            Dataset ds = new Dataset();
            ds.setContext(c);
            return ds;
        }
        return null;
    }

    /**
     * KGRAM + full SPARQL compliance : - type of arguments of functions (e.g.
     * sparql regex require string) - variable in select with group by - specify
     * the dataset
     */
    public Mappings sparql(String squery, Dataset ds) throws EngineException {
        return sparqlQueryUpdate(squery, ds, RDFS_ENTAILMENT);
    }

    public Mappings sparql(String squery, Dataset ds, int entail) throws EngineException {
        return sparqlQueryUpdate(squery, ds, entail);
    }

    public Mappings query(ASTQuery ast) throws EngineException {
        if (ast.isUpdate()) {
            return update(ast);
        }
        return query(ast, (Dataset) null);
    }

    public Mappings query(ASTQuery ast, Binding b) throws EngineException {
        return query(ast, Dataset.create(b));
    }

    public Mappings query(ASTQuery ast, Dataset ds) throws EngineException {
        if (ds != null) {
            ast.setDefaultDataset(ds);
        }
        Transformer transformer = transformer();
        Query query = transformer.transform(ast);
        try {
            return query(null, query, null, ds);
        } catch (EngineException e) {
            return Mappings.create(query);
        }
    }

    /**
     * equivalent of std query(ast) but for update
     */
    public Mappings update(ASTQuery ast) throws EngineException {
        Transformer transformer = transformer();
        Query query = transformer.transform(ast);
        return query(query);
    }

    /*
     * ****************************************
     *
     * Secure Query OR Update
     *
     *****************************************
     */
    public Mappings sparqlQuery(String squery) throws EngineException {
        Query q = compile(squery);
        if (q.isUpdate()) {
            throw new EngineException("Unauthorized Update in SPARQL Query:\n" + squery);
        }
        return query(q);
    }

    public Mappings sparqlQuery(String squery, Mapping map, Dataset ds) throws EngineException {
        Query q = compile(squery, ds);
        return sparqlQuery(q, map, ds);
    }

    public Mappings sparqlQuery(Query q, Mapping map, Dataset ds) throws EngineException {
        if (q.isUpdate()) {
            throw new EngineException("Unauthorized Update in SPARQL Query:\n" + q.getAST().toString());
        }
        return query(null, q, map, ds);
    }

    public Mappings sparqlUpdate(String squery) throws EngineException {
        Query q = compile(squery);
        if (!q.isUpdate()) {
            throw new EngineException("Unauthorized Query in SPARQL Update:\n" + squery);
        }
        return query(q);
    }

    public Mappings sparqlQueryUpdate(String squery) throws EngineException {
        return query(squery);
    }

    /*
     * *************************************************************************
     *
     * Main query function
     *
     ***************************************************************************
     */
    Mappings basicQuery(Node gNode, Query q, Mapping m, Dataset ds) throws EngineException {
        String path = q.getAST().getDataset().getStoragePath();
        if (path != null && StorageFactory.getDataManager(path) != null) {
            return basicQueryStorage(gNode, q, m, ds);
        }
        return basicQueryProcess(gNode, q, m, ds);
    }

    Mappings basicQueryProcess(Node gNode, Query q, Mapping m, Dataset ds) throws EngineException {
        ASTQuery ast = getAST(q);
        if (ast.isLDScript()) {
            if (Access.reject(Feature.LDSCRIPT, getLevel(m, ds))) {
                throw new EngineException("LDScript unauthorized");
            }
        }
        m = completeMappings(q, m, ds);
        pragma(q);
        for (QueryVisitor vis : getAST(q).getVisitorList()) {
            vis.visit(q, getGraph());
        }
        if (q.getService() != null) {
            // @federate <http://dbpedia.org/sparql>
            // select where {}
            return service(q, m);
        } else {
            dbProducer(q);
        }
        Mappings map = null;

        if (q.isUpdate() || q.isRule()) {
            try {
                startUpdate();
                log(Log.Operation.UPDATE, q);
                if (Access.reject(Access.Feature.SPARQL_UPDATE, getLevel(m, ds))) {
                    throw new EngineException("SPARQL Update unauthorized");
                }
                map = getQueryProcessUpdate().synUpdate(q, m, ds);
                // map is the result of the last Update in q
                // hence the query in map is a local query corresponding to the last Update in q
                // return the Mappings of the last Update and the global query q
                map.setQuery(q);
            } finally {
                endUpdate();
            }

        } else {
            try {
                startQuery();
                map = synQuery(gNode, q, m);
                if (q.isConstruct()) {
                    // construct where
                    construct(map, getAccessRight(m));
                }
                log(Log.Operation.QUERY, q, map);
            } finally {
                endQuery();
            }
        }

        finish(q, map);
        return map;
    }

    // select * from <store:/my/path> where {}
    // @todo: copy this QueryProcess
    Mappings basicQueryStorage(Node gNode, Query q, Mapping m, Dataset ds) throws EngineException {
        return QueryProcess.create(getGraph(),
                StorageFactory.getDataManager(q.getAST().getDataset().getStoragePath()))
                .basicQueryProcess(gNode, q, m, ds);
    }

    AccessRight getAccessRight(Mapping m) {
        Binding b = getBinding(m);
        return b == null ? null : b.getAccessRight();
    }

    void dbProducer(Query q) {
        ASTQuery ast = q.getAST();
        if (ast.hasMetadata(Metadata.DB)) {
            String factory = DB_FACTORY;
            if (ast.hasMetadata(Metadata.DB_FACTORY)) {
                factory = ast.getMetadataValue(Metadata.DB_FACTORY);
            }
            Producer prod = getCreateProducer(getGraph(), factory, ast.getMetadataValue(Metadata.DB));
            setProducer(prod);
        }
    }

    void finish(Query q, Mappings map) {
        Eval finishEval = map.getEval();
        if (finishEval != null) {
            finishEval.finish(q, map);
            map.setEval(null);
        }
        if (q.getAST().hasMetadata(Metadata.LOG)) {
            processLog(q, map);
        }
        if (!getLog().getLinkList().isEmpty()) {
            map.setLinkList(getLog().getLinkList());
        }
        traceLog(map);
    }

    // display service http header log
    // header properties specified by SERVICE_HEADER = p1;p2
    // display whole header: SERVICE_HEADER = *
    void traceLog(Mappings map) {
        List<String> header = Property.listValue(SERVICE_HEADER);

        if (header != null) {
            String log = getLog().log(header);

            if (!log.isEmpty()) {
                logger.info("\n" + log);
                // record log in query info
                // to be displayed as comment in XML Results format
                map.getQuery().addInfo(log);
                traceLogFile(map);
            }
        }
    }

    // write header log to file
    // @save
    // @save <filename>
    void traceLogFile(Mappings map) {
        if (map.getAST().hasMetadata(Metadata.SAVE)) {
            String fileName = map.getAST().getMetadata().getValue(Metadata.SAVE);

            try {
                if (fileName == null) {
                    File tempFile = File.createTempFile("log-", ".txt");
                    fileName = tempFile.getAbsolutePath();
                }
                getLog().logToFile(fileName);
            } catch (IOException ex) {
                logger.error(ex.getMessage());
            }
        }
    }

    void processLog(Query q, Mappings map) {
        LogManager man = getLogManager(map);
        String fileName = q.getAST().getMetadata().getValue(Metadata.LOG);

        try {
            man.toFile(fileName);
        } catch (IOException ex) {
            logger.error(ex.getMessage());
        }
    }

    // translate log header into Mappings
    // use case: gui display log header as query results
    public Mappings log2Mappings(ContextLog log) throws EngineException {
        return log2Mappings(log, false);
    }

    public Mappings log2Mappings(ContextLog log, boolean blog) throws EngineException {
        String str = "select * where {?s ?p ?o}";
        Query q = compile(str);
        Mappings map = Mappings.create(q);
        map.init(q);
        Collection<String> nameList = log.getLabelList();

        for (String url : log.getSubjectMap().getKeys()) {
            if (blog) {
                nameList = log.getPropertyMap(url).keySet();
            }
            for (String name : nameList) {
                IDatatype value = log.getLabel(url, name);

                if (value != null) {
                    ArrayList<Node> valueList = new ArrayList<>();
                    valueList.add(DatatypeMap.newResource(url));
                    valueList.add(DatatypeMap.newResource(name));
                    valueList.add(value);
                    Mapping m = Mapping.create(q.getSelect(), valueList);
                    map.add(m);
                }
            }
        }

        return map;
    }

    Mappings synQuery(Node gNode, Query query, Mapping m) throws EngineException {
        Mappings map = null;
        try {
            syncReadLock(query);
            logStart(query);
            // select from g where
            // if g is an external named graph, create specific Producer(g)
            return basicQuery(gNode, query, m);
        } finally {
            logFinish(query, map);
            syncReadUnlock(query);
        }
    }

    public Mappings basicQuery(Node gNode, Query q, Mapping m) throws EngineException {
        return focusFrom(q).query(gNode, q, m);
    }

    void log(Log.Operation type, Query q) {
        Graph g = getGraph();
        if (g != null) {
            g.log(type, q);
        }
    }

    void log(Log.Operation type, Query q, Mappings m) {
        Graph g = getGraph();
        if (g != null) {
            g.log(type, q, m);
        }
    }

    Context getContext(Query q) {
        Context c = q.getContext();
        if (c == null) {
            return q.getAST().getContext();
        }
        return c;
    }

    /**
     * There may be a Context for access level
     * There may be a Binding for global variables (which contains access level).
     */
    Level getLevel(Mapping m, Dataset ds) {
        if (ds != null && ds.getContext() != null) {
            return ds.getContext().getLevel();
        }
        return Access.getLevel(m);
    }

    /**
     * Annotated query with a service send query to server
     *
     * @federate <http://dbpedia.org/sparql>
     * select where {}
     * Mapping m may contain Binding which may contain Log
     * use case: xt:sparql("@federate <uri> select where")
     */
    Mappings service(Query q, Mapping m) throws EngineException {
        Service serv = new Service(q.getService());
        serv.setBind(getCreateBinding(m));
        serv.setLog(true);
        try {
            return serv.query(q, m);
        } catch (LoadException | ResponseProcessingException ex) {
            throw new EngineException(ex);
        }
    }

    public EventManager getEventManager() {
        return getGraph().getEventManager();
    }

    /**
     * @return Proxy to graph for sparql update
     */
    public GraphManager getUpdateGraphManager() {
        GraphManager mgr = new GraphManager(getGraph());
        if (hasDataManager()) {
            // external graph DataManager (stored in ProducerImpl)
            mgr.setDataBroker(new DataBrokerConstructExtern(getDataManager()));
        }
        return mgr;
    }

    GraphManager getConstructGraphManager(Graph g) {
        return new GraphManager(g);
    }

    /**
     * Implement SPARQL compliance
     */
    Mappings sparqlQueryUpdate(String squery, Dataset ds, int entail) throws EngineException {
        getEvaluator().setMode(Evaluator.SPARQL_MODE);
        setSPARQLCompliant(true);

        if (entail != STD_ENTAILMENT) {
            // include RDF/S entailments in the default graph
            if (ds == null) {
                ds = Dataset.create();
            }
            if (ds.getFrom() == null) {
                ds.defFrom();
            }
            complete(ds);
        }

        // SPARQL compliance
        ds.complete();

        Mappings map = query(squery, null, ds);

        if (!map.getQuery().isCorrect()) {
            map.clear();
        }
        return map;
    }

    void complete(Dataset ds) {
        if (ds != null && ds.hasFrom()) {
            ds.clean();
            // add the default graphs where insert or entailment may have been done
            // previously
            for (String src : Entailment.GRAPHS) {
                ds.addFrom(src);
            }
        }
    }

    public Graph getGraph(Mappings map) {
        return (Graph) map.getGraph();
    }

    public Graph getGraph() {
        return getGraph(getProducer());
    }

    /**
     * construct {} where {} *
     */
    void construct(Mappings map, AccessRight access) {
        Query query = map.getQuery();
        Graph gg = getGraph().construct();
        // can be required to skolemize
        gg.setSkolem(isSkolem());
        Construct cons = Construct.createConstruct(query, getConstructGraphManager(gg));
        cons.construct(map);
        cons.setAccessRight(access);
        map.setGraph(gg);
        getVisitor().construct(map);
    }

    /**
     * Pragma specific to kgraph (in addition to generic pragma in QuerySolver)
     */
    void pragma(Query query) {
        ASTQuery ast = query.getAST();

        if (ast != null && ast.getPragma() != null) {
            PragmaImpl.create(this, query).parse();
        }

        if (getPragma() != null) {
            PragmaImpl.create(this, query).parse(getPragma());
        }
    }

    private Lock getReadLock() {
        return lock.readLock();
    }

    private Lock getWriteLock() {
        return lock.writeLock();
    }

    private void syncReadLock(Query q) {
        if (!isSynchronized()) {
            readLock(q);
        }
    }

    private void syncReadUnlock(Query q) {
        if (!isSynchronized()) {
            readUnlock(q);
        }
    }

    // if query comes from workflow or from RuleEngine cleaner,
    // it is synchronized by graph.init()
    // and it already has a lock by synQuery/synUpdate
    // hence do nothing
    void syncWriteLock(Query q) {
        if (!isSynchronized()) {
            writeLock(q);
        }
    }

    void syncWriteUnlock(Query q) {
        if (!isSynchronized()) {
            writeUnlock(q);
        }
    }

    private void readLock(Query q) {
        if (q.isLock()) {
            getReadLock().lock();
        }
    }

    private void readUnlock(Query q) {
        if (q.isLock()) {
            getReadLock().unlock();
        }
    }

    private void writeLock(Query q) {
        if (q.isLock()) {
            getWriteLock().lock();
        }
    }

    private void writeUnlock(Query q) {
        if (q.isLock()) {
            getWriteLock().unlock();
        }
    }

    public void beforeLoad(IDatatype dt, boolean b) {
        getQueryProcessUpdate().beforeLoad(dt, b);
    }

    public void afterLoad(IDatatype dt, boolean b) {
        getQueryProcessUpdate().afterLoad(dt, b);
    }

    /*
     **************************************************************************
     *
     * Function call and event function
     *
     ************************************************************************
     */

    /**
     * skolemize the blank nodes of the result Mappings
     */
    public Mappings skolem(Mappings map) {
        Graph g = getGraph();
        if (map.getGraph() != null) {
            // result of construct where
            g = (Graph) map.getGraph();
        }
        for (Mapping m : map) {
            Node[] nodes = m.getNodes();
            int i = 0;
            for (Node n : nodes) {
                if (n.isBlank()) {
                    nodes[i] = g.skolem(n);
                }
                i++;
            }
        }
        return map;
    }

    public void logStart(Query query) {
        if (getGraph() != null) {
            getGraph().logStart(query);
        }
    }

    public void logFinish(Query query, Mappings m) {
        if (getGraph() != null) {
            getGraph().logFinish(query, m);
        }
    }

    public void close() {
        if (dbProducer != null) {
            dbProducer.close();
            dbProducer = null;
        }
    }

    /**
     * Logger xt:method(us:start, us:Event, event, obj)
     * Use case: event logger
     *
     * @deprecated
     */
    @Deprecated
    public void event(Event name, Event e, Object o) throws EngineException {
        IDatatype[] param = (o == null) ? param(DatatypeMap.createObject(e))
                : param(DatatypeMap.createObject(e), DatatypeMap.createObject(o));
        EventManager mgr = getGraph().getEventManager();
        method(NSManager.USER + name.toString().toLowerCase(), NSManager.USER + e.toString(), param);
    }

    IDatatype[] param(IDatatype... ldt) {
        return ldt;
    }

    /**
     * method call: name of method, name of type
     */
    public IDatatype method(String name, String type, IDatatype[] param) throws EngineException {
        Function function = getFunction(name, type, param);
        if (function == null) {
            return null;
        }
        return call(EVENT, function, null, null, param);
    }

    /**
     * Execute LDScript function defined as @public
     */
    // @Override
    public IDatatype funcall(String name, IDatatype... param) throws EngineException {
        return funcall(name, null, null, param);
    }

    public IDatatype funcall(String name, Binding b, IDatatype... param) throws EngineException {
        return funcall(name, null, b, param);
    }

    public IDatatype funcall(String name, Context c, IDatatype... param) throws EngineException {
        return funcall(name, c, null, param);
    }

    public IDatatype funcall(String name, Context c, Binding b, IDatatype... param) throws EngineException {
        Function function = getLinkedFunction(name, param);
        if (function == null) {
            return null;
        }
        return call(name, function, c, b, param);
    }

    // @todo: clean Binding/Context AccessLevel
    IDatatype call(String name, Function function, Context c, Binding b, IDatatype... param) throws EngineException {
        Eval eval = getCreateEval();
        eval.getEnvironment().getQuery().setContext(c);
        Binding bind = eval.getBinding();
        bind.share(b, c);
        return new Funcall(name).callWE(eval.getEvaluator(),
                bind, eval.getEnvironment(), eval.getProducer(), function, param);
    }

    // Use case: funcall @public functions
    @Override
    public Eval getCreateEval() throws EngineException {
        if (eval == null) {
            eval = createEval("select where {}  ", null);
        }
        return eval;
    }

    /**
     * event @update: take care of query @event functions
     * create current Eval with a ProcessVisitor
     */
    public void init(Query q, Mapping m) {
        q.setInitMode(true);
        try {
            super.query(q, m);
        } catch (EngineException ex) {
            logger.error(ex.getMessage());
        }
        q.setInitMode(false);
        // set Visitor ready to work (hence, it is not yet active, it is ready to be
        // active)
        getCurrentEval().getVisitor().setActive(false);
    }

    /**
     * call @public @prepare function us:prepare() {} before lock graph
     * to complete initialization before query processing
     * to be called explicitely by user
     * use case: GUI QueryExec call prepare()
     * use case: xt:entailment()
     */
    public void prepare() {
        try {
            new QuerySolverVisitor(getCreateEval()).prepare();
        } catch (EngineException ex) {
        }
    }

    // Default Visitor to execute @event functions
    public ProcessVisitor getDefaultVisitor() {
        try {
            return getCreateEval().getVisitor();
        } catch (EngineException ex) {
            return new ProcessVisitorDefault();
        }
    }

    // Visitor associated to current eval
    // To execute @event functions
    public ProcessVisitor getVisitor() {
        if (getCurrentEval() == null || getCurrentEval().getVisitor() == null) {
            return getDefaultVisitor();
        }
        return getCurrentEval().getVisitor();
    }

    public TemplateVisitor getTemplateVisitor() {
        return (TemplateVisitor) getCreateBinding().getTransformerVisitor();
    }

    @Override
    public ProcessVisitor createProcessVisitor(Eval eval) {
        if (getVisitorName() == null) {
            return super.createProcessVisitor(eval);
        }
        ProcessVisitor vis = createProcessVisitor(eval, getVisitorName());
        if (vis == null) {
            return super.createProcessVisitor(eval);
        }
        return vis;
    }

    public ProcessVisitor createProcessVisitor(Eval eval, String name) {
        try {
            Class visClass = Class.forName(name);
            Object obj = visClass.getDeclaredConstructor(Eval.class).newInstance(eval);
            if (obj instanceof ProcessVisitor) {
                return (ProcessVisitor) obj;
            } else {
                logger.error("Incorrect QuerySolverVisitor: ", name);
            }
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
                | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            logger.error("Undefined QuerySolverVisitor: ", name);
        }

        return null;
    }

    Function getLinkedFunction(String name, IDatatype[] param) throws EngineException {
        Function function = getFunction(name, param);
        if (function == null) {
            // setLinkedFunction(true);
            getLinkedFunction(name);
            function = getFunction(name, param);
        }
        return function;
    }

    Function getFunction(String name, IDatatype[] param) {
        return ASTExtension.getSingleton().get(name, param.length);
    }

    /**
     * Search a method
     *
     * @public @type us:Event us:start(?e, ?o)
     */
    Function getFunction(String name, String type, IDatatype[] param) {
        return ASTExtension.getSingleton().getMethod(
                name, DatatypeMap.newResource(type),
                param);
    }

    /**
     * @import <uri>
     * use case: FunctionCompiler @import <uri>
     */
    @Override
    public ASTQuery parse(String path, Level level) throws EngineException {
        String str = QueryLoad.create().basicParse(path);
        Transformer t = transformer();
        t.setBase(path);
        Dataset ds = Dataset.create().setLevel(level);
        t.setDataset(ds);
        return t.parse(str);
    }

    /**
     * 1- Linked Function 2- owl:imports
     */
    @Override
    public Query parseQuery(String path) throws EngineException {
        return parseQuery(path, Level.USER_DEFAULT);
    }

    @Override
    public Query parseQuery(String path, Level level) throws EngineException {
        String str = QueryLoad.create().basicParse(path);
        Dataset ds = Dataset.create().setBase(path);
        ds.setContext(new Context(level));
        return compile(str, ds);
    }

    // import function definition as public function
    // use case: Java API to import e.g. shacl interpreter
    public boolean imports(String path) throws EngineException {
        return imports(path, true);
    }

    // bypass access control
    public boolean imports(String path, boolean pub) throws EngineException {
        String qp = "@public  @import <%s> select where {}";
        String ql = "@import <%s> select where {}";
        boolean b = Access.skip(true);
        try {
            Query q = compile(String.format((pub) ? qp : ql, path));
            return !q.isImportFailure();
        } finally {
            Access.skip(b);
        }
    }

    @Override
    public void getLinkedFunction(String label) throws EngineException {
        getTransformer().getLinkedFunction(label);
    }

    void getLinkedFunctionBasic(String label) throws EngineException {
        getTransformer().getLinkedFunctionBasic(label);
    }

    public Graph defineFederation(String path) throws IOException, EngineException, LoadException {
        Graph g = Graph.create();
        Load ld = Load.create(g);
        ld.parse(path);
        QueryLoad ql = QueryLoad.create();
        String str = ql.getResource("/query/federation.rq");
        QueryProcess exec = QueryProcess.create(g);
        Mappings map = exec.query(str);

        for (Mapping m : map) {
            IDatatype dt = m.getValue("?uri");
            IDatatype list = m.getValue("?list");
            if (dt != null) {
                FederateVisitor.declareFederation(dt.getLabel(), list.getValueList());

                for (IDatatype serv : list.getValueList()) {
                    Access.define(serv.getLabel(), true);
                }
            }
        }

        return g;
    }

    public void defineFederation(String name, List<String> list) {
        FederateVisitor.defineFederation(name, list);
    }

    public void defineFederation(String name, String... list) {
        FederateVisitor.defineFederation(name, Arrays.asList(list));
    }

    Transformer getTransformer() {
        if (transformer == null) {
            transformer = Transformer.create();
            transformer.setSPARQLEngine(this);
        }
        return transformer;
    }

    //***********************************************************************

    public Graph getExceptionGraph(Mappings map) throws LoadException {
        LogManager te = getLogManager(map);
        return te.parse();
    }

    /**
     * Manager for local and remote endpoint log
     * getLinkList() is a list of link href url of log document recorded in AST
     * Context
     * use case:
     * service http://corese.inria.fr/d2kab/sparql generates a log document on
     * corese server
     * with URL http://corese.inria.fr/log/url.ttl
     * Query Results XML format contains
     * <link href='http://corese.inria.fr/log/url.ttl' />
     * client receive result and parse link url
     */
    public LogManager getLogManager(Mappings map) {
        return new LogManager(getLog(map));
    }

    public JSONObject getMessage(Mappings map) {
        String text = getStringMessage(map);
        if (text == null) {
            return null;
        }
        return new JSONObject(text);
    }

    public String getStringMessage(Mappings map) {
        String url = map.getLastLink(URLParam.MES);
        if (url == null) {
            return null;
        }
        return new Service().getString(url);
    }

    public QueryProcessUpdate getQueryProcessUpdate() {
        return queryProcessUpdate;
    }

    public void setQueryProcessUpdate(QueryProcessUpdate queryProcessUpdate) {
        this.queryProcessUpdate = queryProcessUpdate;
    }

    public ProducerImpl getLocalProducer() {
        return localProducer;
    }

    public void setLocalProducer(ProducerImpl localProducer) {
        this.localProducer = localProducer;
    }

    // null with corese graph
    public DataManager getDataManager() {
        return getLocalProducer().getDataManager();
    }

    public boolean hasDataManager() {
        return getDataManager() != null;
    }

    public DataBroker getDataBroker() {
        return getLocalProducer().getDataBroker();
    }

    public DataBrokerConstruct getDataBrokerUpdate() {
        return dataBrokerUpdate;
    }

    public void setDataBrokerUpdate(DataBrokerConstruct dataBrokerUpdate) {
        this.dataBrokerUpdate = dataBrokerUpdate;
    }

    public boolean isProcessTransaction() {
        return processTransaction;
    }

    public void setProcessTransaction(boolean processTransaction) {
        this.processTransaction = processTransaction;
    }

    boolean processTransaction() {
        return isProcessTransaction() && hasDataManager();
    }

    public void startQuery() {
        if (processTransaction()) {
            getDataManager().startReadTransaction();
        }
    }

    public void endQuery() {
        if (processTransaction()) {
            getDataManager().endReadTransaction();
        }
    }

    public void startUpdate() {
        if (processTransaction()) {
            getDataManager().startWriteTransaction();
        }
    }

    public void endUpdate() {
        if (processTransaction()) {
            getDataManager().endWriteTransaction();
        }
    }

}
