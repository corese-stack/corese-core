package fr.inria.corese.core.load;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;

import org.semarglproject.rdf.core.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.github.jsonldjava.core.JsonLdError;

import fr.com.hp.hpl.jena.rdf.arp.ARP;
import fr.com.hp.hpl.jena.rdf.arp.RDFListener;
import fr.inria.corese.core.Graph;
import fr.inria.corese.core.api.Loader;
import fr.inria.corese.core.api.Log;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.load.jsonld.CoreseJsonTripleCallback;
import fr.inria.corese.core.load.jsonld.JsonldLoader;
import fr.inria.corese.core.load.rdfa.CoreseRDFaTripleSink;
import fr.inria.corese.core.load.rdfa.RDFaLoader;
import fr.inria.corese.core.query.QueryEngine;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.rule.RuleEngine;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.exceptions.QueryLexicalException;
import fr.inria.corese.core.sparql.exceptions.QuerySyntaxException;
import fr.inria.corese.core.sparql.exceptions.SafetyException;
import fr.inria.corese.core.sparql.triple.function.term.TermEval;
import fr.inria.corese.core.sparql.triple.parser.Access;
import fr.inria.corese.core.sparql.triple.parser.Access.Feature;
import fr.inria.corese.core.sparql.triple.parser.AccessRight;
import fr.inria.corese.core.sparql.triple.parser.Constant;
import fr.inria.corese.core.sparql.triple.parser.LoadTurtle;
import fr.inria.corese.core.sparql.triple.parser.NSManager;
import fr.inria.corese.core.storage.api.dataManager.DataManager;
import fr.inria.corese.core.util.HTTPHeaders;
import fr.inria.corese.core.workflow.SemanticWorkflow;
import fr.inria.corese.core.workflow.WorkflowParser;

/**
 * Translate an RDF/XML document into a Graph use ARP
 *
 * @author Olivier Corby, Edelweiss INRIA 2010
 */
public class Load
        implements RDFListener, Loader {

    public static final Logger logger = LoggerFactory.getLogger(Load.class);
    // URL file protocol
    static final String FILE = "file";
    public static final String IMPORTS = NSManager.OWL + "imports";
    public static String LOAD_FORMAT = ALL_FORMAT_STR;
    private static Loader.format DEFAULT_FORMAT = Loader.format.RDFXML_FORMAT;
    // true: load files into kg:default graph when no named graph is given
    // false: load files into named graphs where name = URI of file
    private static boolean DEFAULT_GRAPH = false;
    // max number of triples to load
    private static int LIMIT_DEFAULT = Integer.MAX_VALUE;
    int maxFile = Integer.MAX_VALUE;
    Log log;
    RuleEngine engine;
    // load transformation
    QueryEngine qengine;
    // prevent loop in owl:import
    HashMap<String, String> importedURIs;
    // RDF/XML triple builder
    RdfxmlTripleCreator build;
    // list of namespace of predicate to exclude from load
    ArrayList<String> exclude;
    // RDF graph
    private Graph graph;
    // External graph implementation
    private DataManager dataManager;
    // For lock, event, import LinkedFunction
    private QueryProcess queryProcess;
    // Workflow with .sw extension
    private SemanticWorkflow workflow;
    // named graph URI for RDF/XML parser extension
    private String namedGraphURI;
    private String source;
    // when false: keep bnode ID from parser
    private boolean renameBlankNode = true;
    private boolean defaultGraph = DEFAULT_GRAPH;
    // Visitor event management
    private boolean event = true;
    // true when load transformation
    private boolean transformer = false;
    // when sparql update load statement
    private boolean sparqlUpdate = false;
    // max number of triples to load
    private int limit = LIMIT_DEFAULT;
    // authorize access right for load (e.g. LinkedFunction)
    private Access.Level level = Access.Level.USER_DEFAULT;
    // authorize specific namespaces for load
    private AccessRight accessRight;

    protected Load(Graph g) {
        this();
        set(g);
    }

    protected Load() {
        exclude = new ArrayList<>();
        setAccessRight(new AccessRight());
    }

    public static boolean isDefaultGraphValue() {
        return DEFAULT_GRAPH;
    }

    /**
     * true means load in default graph when no named graph is given
     */
    public static void setDefaultGraphValue(boolean b) {
        DEFAULT_GRAPH = b;
    }

    public static Load create(Graph g) {
        return new Load(g);
    }

    public static Load create(Graph g, DataManager man) {
        Load ld = new Load(g);
        ld.setDataManager(man);
        return ld;
    }

    public static Load create() {
        return new Load(new Graph());
    }

    public static void setDefaultFormat(Loader.format f) {
        DEFAULT_FORMAT = f;
    }

    public static void setLimitDefault(int max) {
        LIMIT_DEFAULT = max;
    }

    @Override
    public void init(Object o) {
        set((Graph) o);
    }

    void set(Graph g) {
        setGraph(g);
        log = g.getLog();
        importedURIs = new HashMap<>();
    }

    public void exclude(String ns) {
        getExclude().add(ns);
    }

    ArrayList<String> getExclude() {
        return exclude;
    }

    public void setEngine(RuleEngine eng) {
        engine = eng;
    }

    @Override
    public RuleEngine getRuleEngine() {
        return engine;
    }

    public void setEngine(QueryEngine eng) {
        qengine = eng;
    }

    public void setMax(int n) {
        maxFile = n;
    }

    Build getBuild() {
        return build;
    }

    public QueryEngine getQueryEngine() {
        return qengine;
    }

    String uri(String name) {
        return NSManager.toURI(name);
    }

    boolean isURL(String path) {
        try {
            new URL(path);
        } catch (MalformedURLException e) {
            return false;
        }
        return true;
    }

    Loader.format getTypeFormat(String contentType, Loader.format format) {
        return LoadFormat.getTypeFormat(contentType, format);
    }

    // UNDEF_FORMAT loaded as RDF/XML
    @Override
    public Loader.format getFormat(String path) {
        return getDefaultOrPathFormat(path, Loader.format.UNDEF_FORMAT);
    }

    boolean hasFormat(String path) {
        return hasFormat(path, Loader.format.UNDEF_FORMAT);
    }

    /**
     * format = undef : accept any correct format
     * format = some format : accept this format
     */
    boolean hasFormat(String path, Loader.format format) {
        if (format == Loader.format.UNDEF_FORMAT) {
            return getFormat(path) != Loader.format.UNDEF_FORMAT;
        } else {
            return getFormat(path) == format;
        }
    }

    public Loader.format getDefaultOrPathFormat(String path, Loader.format proposedFormat) {
        if (proposedFormat != Loader.format.UNDEF_FORMAT) {
            return proposedFormat;
        }
        return LoadFormat.getFormat(path);
    }

    @Override
    public boolean isRule(String path) {
        return getFormat(path) == Loader.format.RULE_FORMAT;
    }

    /**
     * parse directory content
     */
    public void parseDir(String path) throws LoadException {
        parseDir(path, null, false);
    }

    public void parseDir(String path, Loader.format format) throws LoadException {
        parseDir(path, null, false, format);
    }

    /**
     * Parse directory (not subdirectory)
     * name is named graph (if not null) else path is named graph
     * base is now the path (it used to be the name)
     */
    public void parseDir(String path, String name) throws LoadException {
        parseDir(path, name, false);
    }

    public void parseDirRec(String path) throws LoadException {
        parseDir(path, null, true);
    }

    public void parseDirRec(String path, String name) throws LoadException {
        parseDir(path, name, true);
    }

    public void parseDir(String path, String name, boolean rec) throws LoadException {
        parseDir(path, name, rec, Loader.format.UNDEF_FORMAT);
    }

    public void parseDir(String path, String name, boolean rec, Loader.format format) throws LoadException {
        parseDir(new File(path), path, name, rec, format);
    }

    /**
     * name is the named graph where to create triples
     * if name = null name := path of each file
     * Difference with loadWE:
     * recursion on subdirectory when rec = true
     * no recursion on directory with SW extension (even if rec = true)
     * base is now the path of each file (not the name)
     * format: required format unless UNDEF_FORMAT
     */
    void parseDir(File file, String path, String name, boolean rec, Loader.format format) throws LoadException {
        if (file.isDirectory()) {
            if (!path.endsWith(File.separator)) {
                path += File.separator;
            }
            for (String f : file.list()) {
                String pname = path + f;
                if (hasFormat(f, format)) {
                    parseDoc(pname, name);
                } else if (rec) {
                    File dir = new File(pname);
                    if (dir.isDirectory()) {
                        parseDir(dir, pname, name, rec, format);
                    }
                }
            }
        } else {
            parseDoc(path, name);
        }
    }

    /**
     * Load files according to filter extensions (use ExtensionFilter)
     */
    public void parse(File file, FileFilter ff, String name, boolean rec) throws LoadException {
        if (file.isDirectory()) {
            for (File f : file.listFiles(ff)) {
                if (!f.isDirectory()) {
                    parseDoc(f.getAbsolutePath(), name);
                }
            }
            if (rec) {
                for (File dir : file.listFiles()) {
                    if (dir.isDirectory()) {
                        parse(dir, ff, name, rec);
                    }
                }
            }
        } else if (ff.accept(file)) {
            parseDoc(file.getAbsolutePath(), name);
        }
    }

    boolean match(String path, Loader.format format) {
        if (format == Loader.format.UNDEF_FORMAT) {
            return true;
        }
        return getFormat(path) == format;
    }

    void parseDoc(String path, String name) throws LoadException {
        parse(path, name, path, Loader.format.UNDEF_FORMAT);
    }

    /**
     * format is a suggested format when path has no extension
     * default format is RDF/XML
     */
    @Override
    public void parse(String path) throws LoadException {
        parse(path, null, null, Loader.format.UNDEF_FORMAT);
    }

    public void parse(String path, Loader.format format) throws LoadException {
        parse(path, null, null, format);
    }

    @Override
    public void parse(String path, String name) throws LoadException {
        parse(path, name, null, Loader.format.UNDEF_FORMAT);
    }

    public void parse(String path, String name, Loader.format format) throws LoadException {
        parse(path, name, null, format);
    }

    public void parseWithFormat(String path, Loader.format format) throws LoadException {
        parse(path, null, null, format);
    }

    /**
     * name: the named graph (if null, name = path)
     * base: base for relative URI (if null, base = path)
     * getFormat:
     * if format = UNDEF use path extension if any
     * if format != UNDEF use format (even if it contradicts the extension)
     * use case: rdf/xml file has .xml extension but we want to load it as
     * RDFXML_FORMAT
     * if format is UNDEF and path is URI with content type: use content type format
     */
    // target format:
    // 1) format if any
    // 2) path format if any
    // 3) default load format
    // 4) URL HTTP content type format
    @Override
    public void parse(String path, String name, String base, Loader.format format) throws LoadException {
        name = target(name, path);
        base = (base == null) ? path : base;
        name = uri(name);
        base = uri(base);
        basicParse(path, base, name, getDefaultOrPathFormat(path, format));
    }

    /**
     *
     */
    String target(String name, String path) {
        if (name == null) {
            if (isDefaultGraph()) {
                return defaultGraph();
            } else {
                return path;
            }
        }
        return name;
    }

    public String defaultGraph() {
        Node node = getGraph().addDefaultGraphNode();
        return node.getLabel();
    }

    public void parse(InputStream stream) throws LoadException {
        parse(stream, Loader.format.UNDEF_FORMAT);
    }

    public void parse(InputStream stream, Loader.format format) throws LoadException {
        parse(stream, defaultGraph(), format);
    }

    public void parse(InputStream stream, String name, Loader.format format) throws LoadException {
        parse(stream, name, name, name, format);
    }

    // TODO: clean arg order
    public void parse(InputStream stream, String path, String name, String base, Loader.format format)
            throws LoadException {

        try {
            Reader read = reader(stream);
            synLoad(read, path, base, name, format);
        } catch (UnsupportedEncodingException e) {
            throw LoadException.create(e, path);
        }
    }

    /**
     * if base = null : base = uri(path)
     * if name = null : name = base
     * format : expected format according to path extension or specified by user
     */
    private void basicParse(String path, String base, String name, Loader.format format)
            throws LoadException {
        if (format == Loader.format.RULE_FORMAT) {
            loadRule(path, base);
            return;
        } else if (format == Loader.format.QUERY_FORMAT) {
            loadQuery(path, base);
            return;
        }

        Reader read = null;
        InputStream stream = null;
        Loader.format myFormat = format;

        try {
            if (NSManager.isResource(path)) {
                stream = getResourceStream(path);
                read = reader(stream);
            } else if (isURL(path)) {
                URL url = new URL(path);
                String contentType = null;

                if (url.getProtocol().equals(FILE)) {
                    URLConnection c = url.openConnection();
                    c.setRequestProperty(HTTPHeaders.ACCEPT, getActualFormat(myFormat));
                    stream = c.getInputStream();
                    contentType = c.getContentType();
                } else {
                    Service srv = new Service(path);
                    stream = srv.load(path, getActualFormat(myFormat));
                    contentType = srv.getFormat();
                }
                read = reader(stream);
                if (contentType != null) {
                    myFormat = getTypeFormat(contentType, myFormat);
                }

            } else {
                read = new FileReader(path);
            }
        } catch (Exception e) {
            logger.error(e.toString());
            logger.error(e.getMessage() + " " + path);
            throw LoadException.create(e, path);
        }

        if (base != null) {
            // ARP needs an URI for base
            base = uri(base);
        } else {
            base = uri(path);
        }

        if (name == null) {
            name = base;
        }

        synLoad(read, path, base, name, myFormat);

        close(stream);
    }

    @Deprecated
    public InputStream getStream(String path, String... formats)
            throws LoadException, IOException {
        String format = "*";
        if (formats.length > 0) {
            format = formats[0];
        }
        InputStream stream;

        if (NSManager.isResource(path)) {
            stream = getResourceStream(path);
        } else if (isURL(path)) {
            URL url = new URL(path);
            String contentType = null;

            if (url.getProtocol().equals(FILE)) {
                URLConnection c = url.openConnection();
                stream = c.getInputStream();
                contentType = c.getContentType();
            } else {
                Service srv = new Service(path);
                stream = srv.load(path, format);
                contentType = srv.getFormat();
            }
            if (contentType != null) {
                // logger.info("Content-type: " + contentType);
                Loader.format myFormat = getTypeFormat(contentType, Loader.format.UNDEF_FORMAT);
            }
            // System.out.println("load: " + contentType + " " + myFormat);

        } else {
            stream = new FileInputStream(path);
        }
        return stream;
    }

    String getActualFormat(Loader.format myFormat) {
        if (myFormat != Loader.format.UNDEF_FORMAT) {
            String testFormat = LoadFormat.getFormat(myFormat);
            if (testFormat != null) {
                return testFormat;
            }
        }
        return LOAD_FORMAT;
    }

    /**
     * http://ns.inria.fr/corese/ means load local resource
     */
    InputStream getResourceStream(String path) throws LoadException {
        String pname = NSManager.stripResource(path);
        InputStream stream = Load.class.getResourceAsStream(pname);
        if (stream == null) {
            throw LoadException.create(new IOException(path), path);
        }
        return stream;
    }

    Reader reader(InputStream stream) throws UnsupportedEncodingException {
        return new InputStreamReader(stream, StandardCharsets.UTF_8);
    }

    void close(InputStream stream) throws LoadException {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ex) {
                throw new LoadException(ex);
            }
        }
    }

    public void loadString(String str, Loader.format format) throws LoadException {
        loadString(str, defaultGraph(), format);
    }

    public void loadString(String str, String name, Loader.format format) throws LoadException {
        loadString(str, name, name, name, format);
    }

    public void loadString(String str, String path, String name, String base, Loader.format format)
            throws LoadException {
        parse(new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8)), path, name, base, format);
    }

    public void loadResource(String path, Loader.format format) throws LoadException {
        loadResource(path, defaultGraph(), format);
    }

    public void loadResource(String path, String name, Loader.format format) throws LoadException {
        InputStream stream = Load.class.getResourceAsStream(path);
        if (stream == null) {
            throw LoadException.create(new IOException(path), path);
        }
        parse(stream, name, format);
    }

    void synLoad(Reader stream, String path, String base, String name, Loader.format format) throws LoadException {
        if (isReadLocked()) {
            throw new LoadException(new EngineException("Read lock while parsing: " + path));
        }
        try {
            startLoad();
            parse(stream, path, base, name, format);
        } finally {
            endLoad();
        }
    }

    void startLoad() {
        lock();
        if (processTransaction()) {
            getDataManager().startWriteTransaction();
        }
    }

    void endLoad() {
        try {
            if (processTransaction()) {
                getDataManager().endWriteTransaction();
            }
        } finally {
            unlock();
        }
    }

    void lock() {
        if (getQueryProcess() != null && getQueryProcess().isSynchronized()) {
            // already locked
        } else {
            writeLock().lock();
        }
    }

    void unlock() {
        if (getQueryProcess() != null && getQueryProcess().isSynchronized()) {
            // already locked
        } else {
            writeLock().unlock();
        }
    }

    public void parse(Reader stream, String path, String base, String name, Loader.format format) throws LoadException {
        switch (format) {
            case TURTLE_FORMAT:
            case TRIG_FORMAT:
            case NT_FORMAT:
                loadTurtle(stream, path, base, name);
                break;

            case NQUADS_FORMAT:
                loadTurtle(stream, path, base, name, true);
                break;

            case RULE_FORMAT:
                loadRule(stream, name);
                break;

            case WORKFLOW_FORMAT:
                loadWorkflow(stream, path);
                break;

            case QUERY_FORMAT:
                loadQuery(stream, name);
                break;

            case RDFA_FORMAT:
                loadRDFa(stream, path, base, name);
                break;

            case JSONLD_FORMAT:
                loadJsonld(stream, path, base, name);
                break;

            case RDFXML_FORMAT:
                loadRDFXML(stream, path, base, name);
                break;

            case OWL_FORMAT:
                loadRDFXMLOrTurtle(stream, path, base, name);
                break;

            case XML_FORMAT:
            case JSON_FORMAT:
                // skip it
                break;

            case UNDEF_FORMAT:
            default:
                parse(stream, path, base, name,
                        (DEFAULT_FORMAT == Loader.format.UNDEF_FORMAT) ? Loader.format.RDFXML_FORMAT : DEFAULT_FORMAT);
        }
    }

    Lock writeLock() {
        return getGraph().writeLock();
    }

    boolean isReadLocked() {
        return getGraph().isReadLocked();
    }

    void loadWorkflow(Reader read, String path) throws LoadException {
        WorkflowParser wp = new WorkflowParser();
        try {
            wp.parse(read, path);
        } catch (SafetyException ex) {
            throw new LoadException(ex).setPath(path);
        }
        setWorkflow(wp.getWorkflowProcess());
    }

    /**
     * .owl is RDF/XML or Turtle
     */
    void loadRDFXMLOrTurtle(Reader stream, String path, String base, String name) throws LoadException {
        try {
            loadRDFXML(stream, path, base, name);
        } catch (LoadException e) {
            if (e.getException() != null
                    && e.getException().getMessage().contains("{E301}")) {
                parse(path, base, name, Loader.format.TURTLE_FORMAT);
            }
        }
    }

    // @name: named graph URI
    void loadRDFXML(Reader stream, String path, String base, String name) throws LoadException {
        String save = getNamedGraphURI();
        setNamedGraphURI(name);

        RdfxmlTripleCreator buildRDFXML = RdfxmlTripleCreator.create(getGraph(), this);
        buildRDFXML.setSource(name);
        if (name == null) {
            buildRDFXML.setSource(namedGraphURI);
        }
        buildRDFXML.setPath(path);
        buildRDFXML.setLimit(getLimit());
        buildRDFXML.exclude(getExclude());
        buildRDFXML.setDataManager(getDataManager());

        IDatatype dt = DatatypeMap.newResource(path);
        boolean b = getCreateQueryProcess().isSynchronized();
        before(dt, b);
        buildRDFXML.start();
        ARP arp = new ARP();
        try {
            arp.setRDFListener(this);
        } catch (java.lang.NoSuchMethodError ignored) {
        }
        arp.setStatementHandler(buildRDFXML);
        arp.setErrorHandler(buildRDFXML);
        try {
            arp.load(stream, base);
        } catch (SAXException | IOException e) {
            throw LoadException.create(e, arp.getLocator(), path);
        } finally {
            buildRDFXML.finish();
            after(dt, b);
            setNamedGraphURI(save);
            try {
                stream.close();
            } catch (IOException ex) {
                throw LoadException.create(ex, arp.getLocator(), path);
            }
        }
    }

    @Override
    public String getSource() {
        return getNamedGraphURI();
    }

    /**
     * interface RDFListener, extension of ARP RDF/XML parser
     * Process cos:graph named graph statement extension
     * TODO: generate different blanks in different graphs
     */
    @Override
    public void setSource(String s) {
        if (s != null) {
            source = s;
        }
    }

    void loadTurtle(Reader stream, String path, String base, String name) throws LoadException {
        loadTurtle(stream, path, base, name, false);
    }

    void loadTurtle(Reader stream, String path, String base, String name, boolean nquad) throws LoadException {
        TurtleSparqlTripleCreator cr = TurtleSparqlTripleCreator.create(getGraph(), this);
        cr.graph(Constant.create(name));
        cr.setRenameBlankNode(renameBlankNode);
        cr.setLimit(getLimit());
        cr.exclude(getExclude());
        cr.setDataManager(getDataManager());
        cr.start();

        IDatatype dt = DatatypeMap.newResource(path);
        boolean b = true;
        if (isEvent()) {
            b = getCreateQueryProcess().isSynchronized();
        }
        before(dt, b);
        cr.setPath(path);
        LoadTurtle ld = LoadTurtle.create(stream, cr, base);
        ld.setNquad(nquad);
        try {
            ld.load();
        } catch (QueryLexicalException | QuerySyntaxException e) {
            throw LoadException.create(e, path);
        } finally {
            after(dt, b);
            cr.finish();
        }
    }

    // load RDFa
    void loadRDFa(Reader stream, String path, String base, String name) throws LoadException {
        CoreseRDFaTripleSink sink = new CoreseRDFaTripleSink(getGraph(), getDataManager(), null, this);
        sink.setHelper(renameBlankNode, getLimit());

        RDFaLoader loader = RDFaLoader.create(stream, base);

        try {
            loader.load(sink);
        } catch (ParseException ex) {
            throw LoadException.create(ex, path);
        }

    }

    // load JSON-LD
    void loadJsonld(Reader stream, String path, String base, String name) throws LoadException {

        CoreseJsonTripleCallback callback = new CoreseJsonTripleCallback(getGraph(), getDataManager(), name, this);
        callback.setHelper(renameBlankNode, getLimit());
        JsonldLoader loader = JsonldLoader.create(stream, base);
        try {
            loader.load(callback);
        } catch (IOException | JsonLdError ex) {
            throw LoadException.create(ex, path);
        }
    }

    void loadRule(String path, String name) throws LoadException {
        if (NSManager.isResource(path)) {
            loadRuleResource(path, name);
        } else {
            loadRulePath(path, name);
        }
    }

    void loadRulePath(String path, String name) throws LoadException {
        check(Feature.LINKED_RULE, name, TermEval.LINKED_RULE_MESS);
        if (engine == null) {
            engine = RuleEngine.create(getGraph(), getDataManager());
        }
        // rule base
        RuleLoad load = RuleLoad.create(engine);
        load.setLevel(getLevel());
        try {
            load.parse(path);
        } catch (EngineException ex) {
            throw LoadException.create(ex, path);
        }
    }

    /**
     * path = http://ns.inria.fr/corese/rule/owl.rul
     * Load as resource
     */
    void loadRuleResource(String path, String name) throws LoadException {
        InputStream stream = null;
        try {
            stream = getResourceStream(path);
            Reader read = new InputStreamReader(stream);
            loadRule(read, name);
        } finally {
            close(stream);
        }
    }

    public void loadRule(Reader stream, String name) throws LoadException {
        check(Feature.LINKED_RULE, name, TermEval.LINKED_RULE_MESS);
        loadRuleBasic(stream, name);
    }

    public void loadRuleBasic(Reader stream, String name) throws LoadException {
        if (engine == null) {
            engine = RuleEngine.create(getGraph(), getDataManager());
        }
        RuleLoad load = RuleLoad.create(engine);
        load.setLevel(getLevel());
        try {
            load.parse(stream);
        } catch (EngineException ex) {
            if (ex.isSafetyException()) {
                ex.getSafetyException().setPath(name);
            }
            throw LoadException.create(ex, name);
        }
    }

    void loadQuery(String path, String name) throws LoadException {
        if (isTransformer()) {
            // use case: when load a transformation in a directory
            // each file .rq is loaded by loadQuery
            // in this case, load is authorized
            // PRAGMA: it may load function definition
            // to prevent it: deny DEFINE_FUNCTION
        } else {
            check(Feature.IMPORT_FUNCTION, name, TermEval.IMPORT_MESS);
        }
        if (qengine == null) {
            qengine = QueryEngine.create(getGraph());
        }
        qengine.setLevel(getLevel());
        QueryLoad load = QueryLoad.create(qengine);
        load.parse(path);
    }

    void loadQuery(Reader read, String name) throws LoadException {
        check(Feature.IMPORT_FUNCTION, name, TermEval.IMPORT_MESS);
        if (qengine == null) {
            qengine = QueryEngine.create(getGraph());
        }
        qengine.setLevel(getLevel());
        QueryLoad load = QueryLoad.create(qengine);
        load.parse(read);
    }

    /**
     * Imports an RDF resource from the given URI if it has not been loaded yet.
     * 
     * This method ensures that the URI is not imported multiple times by checking
     * the `importedURIs` map. If the URI is new, it is added to the map and
     * processed using `basicImport(uri)`.
     * 
     * The current `BuildImpl` instance is temporarily saved and restored after
     * the import to maintain consistency.
     * 
     * @param uri The URI of the RDF resource to import.
     */
    public void imports(String uri) {
        if (importedURIs.containsKey(uri)) {
            return;
        }

        importedURIs.put(uri, uri);
        RdfxmlTripleCreator previousBuild = build;

        try {
            basicImport(uri);
        } catch (LoadException ex) {
            logger.error(ex.getMessage());
        }

        build = previousBuild;
    }

    // RDF owl:imports <fun.rq>
    void basicImport(String uri) throws LoadException {
        switch (getFormat(uri)) {
            case QUERY_FORMAT: {
                check(Feature.IMPORT_FUNCTION, uri, TermEval.IMPORT_MESS);
                try {
                    Query q = QueryProcess.create().parseQuery(uri, getLevel());
                } catch (EngineException ex) {
                    throw LoadException.create(ex, uri);
                }
            }
                break;

            default:
                parse(uri);
        }
    }

    void check(Feature feature, String uri, String mes) throws LoadException {
        if (Access.reject(feature, getLevel(), uri)) {
            throw new LoadException(new SafetyException(mes, uri));
        }
    }

    void before(IDatatype dt, boolean b) {
        if (isEvent()) {
            getCreateQueryProcess().beforeLoad(dt, b);
        }
    }

    void after(IDatatype dt, boolean b) {
        if (isEvent()) {
            getCreateQueryProcess().afterLoad(dt, b);
        }
    }

    public boolean isRenameBlankNode() {
        return renameBlankNode;
    }

    public void setRenameBlankNode(boolean renameBlankNode) {
        this.renameBlankNode = renameBlankNode;
    }

    /*
     * *******************************************************
     */
    @Deprecated
    @Override
    public void load(InputStream stream, String source) throws LoadException {
        load(stream, source, source);
    }

    /**
     * source is the graph name path is a pseudo path that may have an extension
     * and hence specify the input format
     *
     * @deprecated
     */
    @Deprecated
    public void load(InputStream stream, String source, String path) throws LoadException {
        if (source == null) {
            source = defaultGraph();
        }
        if (path == null) {
            path = defaultGraph();
        }
        // ici source était aussi la base ... (au lieu de path)
        load(stream, path, source, source, getFormat(path));
    }

    @Override
    @Deprecated
    public void load(String path) {
        load(path, (String) null);
    }

    @Override
    @Deprecated
    public void loadWE(String path) throws LoadException {
        loadWE(path, (String) null);
    }

    @Deprecated
    public void loadWE(String path, Loader.format format) throws LoadException {
        loadWE(path, null, format);
    }

    @Override
    @Deprecated
    public void load(String path, String src) {
        File file = new File(path);
        if (file.isDirectory()) {
            path += File.separator;
            int i = 0;
            for (String f : file.list()) {
                if (!hasFormat(f)) {
                    continue;
                }
                if (i++ >= maxFile) {
                    return;
                }
                String name = path + f;
                load(name, src);
            }
        } else {
            try {
                load(path, src, null);
            } catch (LoadException e) {
                logger.error(e.getMessage());
            }
        }
    }

    @Override
    @Deprecated
    public void loadWE(String path, String src) throws LoadException {
        loadWE(path, src, Loader.format.UNDEF_FORMAT);
    }

    @Deprecated
    public void loadWE(String path, String source, Loader.format format) throws LoadException {
        File file = new File(path);
        if (file.isDirectory()) {
            path += File.separator;
            int i = 0;
            for (String f : file.list()) {
                if (!hasFormat(f)) {
                    continue;
                }
                if (i++ >= maxFile) {
                    return;
                }
                String name = path + f;
                loadWE(name, source, format);
            }
        } else {
            load(path, source, null, format);
        }
    }

    @Deprecated
    public void load(String path, Loader.format format) throws LoadException {
        basicParse(path, path, path, getDefaultOrPathFormat(path, format));
    }

    @Deprecated
    public void load(String path, String base, String source) throws LoadException {
        basicParse(path, base, source, getFormat(path));
    }

    @Deprecated
    public void load(String path, String base, String source, Loader.format format) throws LoadException {
        basicParse(path, base, source, getDefaultOrPathFormat(path, format));
    }

    @Deprecated
    public void load(InputStream stream) throws LoadException {
        load(stream, Loader.format.UNDEF_FORMAT);
    }

    @Deprecated
    public void load(InputStream stream, Loader.format format) throws LoadException {
        load(stream, defaultGraph(), format);
    }

    @Deprecated
    public void load(InputStream stream, String source, Loader.format format) throws LoadException {
        load(stream, source, source, source, format);
    }

    @Deprecated
    public void load(InputStream stream, String path, String source, String base, Loader.format format)
            throws LoadException {
        parse(stream, path, source, base, format);
    }

    public SemanticWorkflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(SemanticWorkflow workflow) {
        this.workflow = workflow;
    }

    public boolean isDefaultGraph() {
        return defaultGraph;
    }

    public void setDefaultGraph(boolean defaultGraph) {
        this.defaultGraph = defaultGraph;
    }

    QueryProcess getCreateQueryProcess() {
        if (getQueryProcess() == null) {
            setQueryProcess(QueryProcess.create(getGraph(), getDataManager()));
        }
        return getQueryProcess();
    }

    public QueryProcess getQueryProcess() {
        return queryProcess;
    }

    public void setQueryProcess(QueryProcess queryProcess) {
        this.queryProcess = queryProcess;
    }

    public boolean isEvent() {
        return event;
    }

    public void setEvent(boolean event) {
        this.event = event;
    }

    public AccessRight getAccessRight() {
        return accessRight;
    }

    public void setAccessRight(AccessRight accessRight) {
        this.accessRight = accessRight;
    }

    public Access.Level getLevel() {
        return level;
    }

    public void setLevel(Access.Level level) {
        this.level = level;
    }

    public boolean isTransformer() {
        return transformer;
    }

    public void setTransformer(boolean transformer) {
        this.transformer = transformer;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public void setDataManager(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int max) {
        limit = max;
    }

    public String getNamedGraphURI() {
        return namedGraphURI;
    }

    public void setNamedGraphURI(String namedGraphURI) {
        this.namedGraphURI = namedGraphURI;
    }

    public boolean isSparqlUpdate() {
        return sparqlUpdate;
    }

    public void setSparqlUpdate(boolean sparqlUpdate) {
        this.sparqlUpdate = sparqlUpdate;
    }

    // do not process transaction when load is in sparql update
    // because transaction is already processed by sparql update call
    boolean processTransaction() {
        return !isSparqlUpdate() && getDataManager() != null;
    }

}
