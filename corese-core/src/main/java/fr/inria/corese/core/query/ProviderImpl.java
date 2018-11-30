package fr.inria.corese.core.query;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import fr.inria.corese.sparql.triple.parser.ASTQuery;
import fr.inria.corese.compiler.parser.Pragma;
import fr.inria.corese.compiler.result.XMLResult;
import fr.inria.corese.kgram.api.core.Node;
import fr.inria.corese.kgram.api.query.Environment;
import fr.inria.corese.kgram.api.query.Producer;
import fr.inria.corese.kgram.api.query.Provider;
import fr.inria.corese.kgram.core.Exp;
import fr.inria.corese.kgram.core.Mapping;
import fr.inria.corese.kgram.core.Mappings;
import fr.inria.corese.kgram.core.Query;
import fr.inria.corese.core.Event;
import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.SPARQLResult;
import fr.inria.corese.kgram.core.Eval;
import fr.inria.corese.sparql.datatype.DatatypeMap;
import fr.inria.corese.sparql.triple.parser.Metadata;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;

/**
 * Implements service expression There may be local QueryProcess for some URI
 * (use case: W3C test case) Send query to sparql endpoint using HTTP POST query
 * There may be a default QueryProcess
 *
 * TODO: check use same ProducerImpl to generate Nodes ?
 *
 * @author Olivier Corby, Edelweiss INRIA 2011
 *
 */
public class ProviderImpl implements Provider {

    private static final String DB = "db:";
    private static final String SERVICE_ERROR = "Service error: ";
    private static Logger logger = LoggerFactory.getLogger(ProviderImpl.class);
    static final String LOCALHOST = "http://localhost:8080/sparql";
    static final String LOCALHOST2 = "http://localhost:8090/sparql";
    static final String DBPEDIA = "http://fr.dbpedia.org/sparql";
    HashMap<String, QueryProcess> table;
    Hashtable<String, Double> version;
    QueryProcess defaut;
    private int limit = 30;

    private ProviderImpl() {
        table = new HashMap<String, QueryProcess>();
        version = new Hashtable<String, Double>();
    }

    public static ProviderImpl create() {
        ProviderImpl p = new ProviderImpl();
        p.set(LOCALHOST, 1.1);
        p.set(LOCALHOST2, 1.1);
        p.set("https://data.archives-ouvertes.fr/sparql", 1.1);
        p.set("http://corese.inria.fr/sparql", 1.1);
        return p;
    }

    @Override
    public void set(String uri, double version) {
        this.version.put(uri, version);
    }

    @Override
    public boolean isSparql0(Node serv) {
        //if (true) return false;
        if (serv.getLabel().startsWith(LOCALHOST)) {
            return false;
        }
        Double f = version.get(serv.getLabel());
        return (f == null || f == 1.0);
    }

    /**
     * Define a QueryProcess for this URI
     */
    public void add(String uri, Graph g) {
        QueryProcess exec = QueryProcess.create(g);
        exec.set(this);
        table.put(uri, exec);
    }

    /**
     * Define a default QueryProcess
     */
    public void add(Graph g) {
        QueryProcess exec = QueryProcess.create(g);
        exec.set(this);
        defaut = exec;
    }

    /**
     * If there is a QueryProcess for this URI, use it Otherwise send query to
     * spaql endpoint If endpoint fails, use default QueryProcess if it exists
     */
//    public Mappings service(Node serv, Exp exp, Environment env) {
//        return service(serv, exp, null, env);
//    }
//    
//    public Mappings service(Node serv, Exp exp, Mappings lmap, Environment env) {
//        return service(serv, exp, lmap, env, null);
//    }

    @Override
    public Mappings service(Node serv, Exp exp, Mappings lmap, Eval eval) {
        Exp body = exp.rest();
        Query q = body.getQuery();

        QueryProcess exec = null ;
        
        if (serv != null) {
            exec = table.get(serv.getLabel());
        }

        if (exec == null) {
            
            Mappings map = globalSend(serv, q, exp, lmap, eval);
            if (map != null) {
                return map;
            }

            if (defaut == null) {
                map = Mappings.create(q);
                if (q.isSilent()) {
                    map.add(Mapping.create());
                }
                return map;
            } else {
                exec = defaut;
            }
        }

        ASTQuery ast = exec.getAST(q);
        Mappings map = exec.query(ast);

        return map;
    }
    
    Graph getGraph(Producer p) {
        return (Graph) p.getGraph();
    }

    /**
     * Take Mappings into account when sending service to remote endpoint
     */
    Mappings globalSend(Node serv, Query q, Exp exp, Mappings map, Eval eval) {
        CompileService compiler = new CompileService(this);

        // share prefix
        compiler.prepare(q);

        int slice = getSlice(q, serv, eval.getEnvironment(), map); //compiler.slice(q);

        ASTQuery ast = (ASTQuery) q.getAST();
        boolean hasValues = ast.getValues() != null;
        boolean skipBind = ast.getGlobalAST().hasMetadata(Metadata.BIND, Metadata.SKIP_STR);
        Mappings res = null;
        Graph g = getGraph(eval.getProducer());
        if (map == null || slice == 0 || hasValues || skipBind) {
            // if query has its own values {}, do not include Mappings
            return sliceSend(g, compiler, serv, q, exp, (skipBind) ? null : map, eval, false, slice);
            //return basicSend(g, compiler, serv, q, exp, (skipBind) ? null : map, eval, 0, 0);
        } else {
            res = sliceSend(g, compiler, serv, q, exp, map, eval, true, slice);
        }

        if (!hasValues) {
            ast.setValues(null);
        }

        return res;
    }
    
    /**
     * Generalized service clause with possibly several service URIs
     * If service is unbound variable, retrieve service URI in Mappings 
     * Take Mappings variable binding into account when sending service
     * Split Mappings into buckets with size = slice
     * Iterate service on each bucket
     */
    Mappings sliceSend(Graph g, CompileService compiler, Node serviceNode, Query q, Exp exp, Mappings map, Eval eval, boolean slice, int length) {
        
        List<Node> list = getServerList(exp, map, eval.getEnvironment());
        g.getEventManager().start(Event.Service, list);
       
        if (list.isEmpty()) {
            logger.error("Undefined service: " + exp.getServiceNode());
        }

        ArrayList<Mappings> mapList = new ArrayList<>();
        ArrayList<ProviderThread> pList = new ArrayList<>();
        // With @new annotation => service in parallel
        boolean parallel = q.getOuterQuery().isNew();
        
        for (Node service : list) {
            if (eval.isStop()) {
                break;
            }
            g.getEventManager().process(Event.Service, service);

            Mappings input = map;
            if (slice) {
                // select appropriate subset of distinct Mappings with service URI 
                input = getMappings(exp.getServiceNode(), service, map, eval.getEnvironment());
                if (input.size() > 0) {
                    g.getEventManager().process(Event.Service, input.toString(true, false, 20));
                }
            }
            
            Mappings sol = new Mappings();
            mapList.add(sol);
            
            if (parallel) {
                ProviderThread p = parallelProcess(q, service, exp, input, sol, eval, compiler, slice, length);
                pList.add(p);
            }
            else {
                process(q, service, exp, input, sol, eval, compiler, slice, length);
            }
        }
        
        // Wait for parallel threads to stop
        for (ProviderThread p : pList) {
            try {
                p.join();
            } catch (InterruptedException ex) {
                logger.warn(ex.toString());
            }
        }
        
        Mappings res = getResult(mapList);
        
        if (list.size() > 1) {
            eval.getVisitor().service(eval, DatatypeMap.toList(list), exp, res);
        }
        g.getEventManager().finish(Event.Service);
        return res;
    }
    
    /**
     * Execute service in a parallel thread 
     */
    ProviderThread parallelProcess(Query q, Node service, Exp exp, Mappings map, Mappings sol, Eval eval, CompileService compiler, boolean slice, int length) {
        ProviderThread thread = new ProviderThread(this, q, service, exp, map, sol, eval, compiler, slice, length);
        thread.start();
        return thread;
    }
       
    /**
     * Execute service with possibly input Mappings map and possibly slicing map into packet of size length
     * Add results into Mappings sol which is empty when entering
     */
    void process(Query q, Node service, Exp exp, Mappings map, Mappings sol, Eval eval, CompileService compiler, boolean slice, int length) {
        int size = 0;
        if (slice) {
            while (size < map.size()) {
                if (eval.isStop()) {
                    break;
                }
                // consider subset of Mappings of size slice
                // it may produce bindings for target service
                Mappings res = send(compiler, service, q, map, eval.getEnvironment(), size, size + length);
                // join (serviceNode = serviceURI)
                complete(exp.getServiceNode(), service, res, eval.getEnvironment());
                addResult(sol, res);
                size += length;
            }
        } else {
            Mappings res = send(compiler, service, q, map, eval.getEnvironment(), 0, 0);
            // join (serviceNode = serviceURI)
            complete(exp.getServiceNode(), service, res, eval.getEnvironment());
            addResult(sol, res);
        }

        eval.getVisitor().service(eval, service, exp, sol);
    }
    
    void addResult(Mappings sol, Mappings res) {
        if (res != null && !res.isEmpty()) {
            sol.add(res);
            if (sol.getQuery() == null) {
                sol.setQuery(res.getQuery());
                sol.init(res.getQuery());
            }
        }
    }
    
    Mappings getResult(List<Mappings> mapList) {
        Mappings res = null;
        for (Mappings m : mapList) {
            if (res == null) {
                if (!m.isEmpty()) {
                    res = m;
                }
            }
            else if (!m.isEmpty()){
                res.add(m);
            }
        }
        return res;
    }
       
    /**
     * service ?s { BGP }
     * When ?s is unbound, join (?s = URI) to Mappings, reject those that are incompatible 
     * TODO: optimize map.join()
     */
    void complete(Node serviceNode, Node serviceURI, Mappings map, Environment env){
        if (map != null && serviceNode.isVariable() && ! env.isBound(serviceNode)) {
            map.join(serviceNode, serviceURI);
        }
    }
    
    /**
     * Select subset of distinct Mappings where serviceNode = serviceURI
     */
    Mappings getMappings(Node serviceNode, Node serviceURI, Mappings map, Environment env) {
        if (serviceNode.isVariable() && ! env.isBound(serviceNode)) {
           return map.getMappings(serviceNode, serviceURI);
        }
        return map;
    }
    
    /**
     * 
     * Determine service URIs
     */
    List<Node> getServerList(Exp exp, Mappings map, Environment env) {
        if (exp.getNodeSet() == null) {
            Node serviceNode = exp.getServiceNode();
            List<Node> list = new ArrayList<>();
            if (serviceNode.isVariable()) {
                Node value = env.getNode(serviceNode);
                if (value == null){
                    return getServerList(serviceNode, map);
                }
                else {
                    list.add(value);
                }
            }
            else {
                list.add(serviceNode);
            }
            return list;
        } else {
            // service <uri1> <uri2> {}
            return exp.getNodeSet();
        }
    }
    
    /**
     * service ?s { }
     * Retrieve service URIs for ?s in Mappings
     */
    List<Node> getServerList(Node serviceNode, Mappings map) {
        if (map == null) {
            logger.error("Unbound variable: " + serviceNode);
            return new ArrayList<>();
        }
        return map.aggregate(serviceNode);
    }
    
   
    @Deprecated
    Mappings basicSend(Graph g, CompileService compiler, Node serviceNode, Query q, Exp exp, Mappings map, Eval eval, int min, int max) {
        List<Node> list = getServerList(exp, map, eval.getEnvironment());
        g.getEventManager().start(Event.Service, list);
        if (list.size() == 1) { 
            Mappings res = send(compiler, list.get(0), q, map, eval.getEnvironment(), min, max);
            eval.getVisitor().service(eval, list.get(0), exp, res);
            g.getEventManager().finish(Event.Service);
            return res;
        } else {
            Mappings res = null;
            for (Node service : list) {
                if (eval.isStop()) {
                    break;
                }
                g.getEventManager().process(Event.Service, service);
                Mappings sol = send(compiler, service, q, map, eval.getEnvironment(), min, max);
                eval.getVisitor().service(eval, service, exp, sol);
                complete(exp.getServiceNode(), serviceNode, sol, eval.getEnvironment());
                if (res == null) {
                    res = sol;
                } else if (sol != null) {
                    res.add(sol);
                }
            }
            if (list.size() > 1) {
                eval.getVisitor().service(eval, DatatypeMap.toList(list), exp, res);
            }
            g.getEventManager().finish(Event.Service);
            return res;
        }
    }

    /**
     * Send query to sparql endpoint using a POST HTTP query
     */
    Mappings send(CompileService compiler, Node serv, Query q, Mappings map, Environment env, int start, int limit) {
        Query gq = q.getGlobalQuery();
        try {

            // generate bindings from map/env if any
            boolean hasBind = compiler.compile(serv, q, map, env, start, limit);
            
            if (! hasBind && start > 0){
                // this is not the first slice and there is no bindings: skip it
                if (gq.isDebug()) {logger.info("Skip slice for absence of relevant binding");}
                return Mappings.create(q);
            }
            
            if (gq.isDebug()) {
                logger.info("** Provider query: \n" + q.getAST());
            }
            Mappings res = eval(q, serv, env);
            
            if (gq.isDebug()) {
                if (res.size() <= 100 || gq.isDetail()) {
                    logger.info("** Provider result: \n" + res.toString(true));
                }
                else {
                   logger.info("** Provider result: \n" + res.size()); 
                }
            }
            if (res != null && res.isError()) {
                logger.info("Parse error in result of: " + serv.getLabel());
            }
            return res;
        } catch (IOException e) {
            logger.error(q.getAST().toString(), e);
            gq.addError(SERVICE_ERROR, e);
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (gq.isDebug()) {
            logger.info("** Provider error");
        }
        return null;
    }
    
    int getTimeout(Query q, Node serv, Environment env) {
        Integer time = (Integer) q.getGlobalQuery().getPragma(Pragma.TIMEOUT);
        if (time == null) {
            return env.getEval().getVisitor().timeout(serv);
        }
        return time;
    }
    
    int getSlice(Query q, Node serv, Environment env, Mappings map) {
        // former: 
        q.getGlobalQuery().getSlice();
        return env.getEval().getVisitor().slice(serv, map);
    }
    
    Mappings eval(Query q, Node serv, Environment env) throws IOException, ParserConfigurationException, SAXException {
        if (isDB(serv)){
            return db(q, serv);
        }
        return send(q, serv,env);
    }
    
    /**
     * service <db:/tmp/human_db> { GP }
     * service overloaded to query a database
     */
    Mappings db(Query q, Node serv){
        QueryProcess exec = QueryProcess.dbCreate(Graph.create(), true, QueryProcess.DB_FACTORY, serv.getLabel().substring(DB.length()));
        return exec.query((ASTQuery) q.getAST());
    }
    
    boolean isDB(Node serv){
        return serv.getLabel().startsWith(DB);
    }
    
    Mappings send(Query q, Node serv, Environment env) throws IOException, ParserConfigurationException, SAXException {
        ASTQuery ast = (ASTQuery) q.getAST();
        boolean trap = ast.isFederate() || ast.getGlobalAST().hasMetadata(Metadata.TRAP);
        String query = ast.toString();
        InputStream stream = doPost(serv.getLabel(), query, getTimeout(q, serv, env));       
        return parse(stream, trap);
    }

    /**
     * ********************************************************************
     *
     * SPARQL Protocol client
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     *
     */
    Mappings parse(StringBuffer sb) throws ParserConfigurationException, SAXException, IOException {
        ProducerImpl p = ProducerImpl.create(Graph.create());
        XMLResult r = XMLResult.create(p);
        Mappings map = r.parseString(sb.toString());
        return map;
    }

    Mappings parse(InputStream stream, boolean trap) throws ParserConfigurationException, SAXException, IOException {
        ProducerImpl p = ProducerImpl.create(Graph.create());
        XMLResult r = SPARQLResult.create(p);
        r.setTrapError(trap);
        Mappings map = r.parse(stream);
        return map;
    }

    public StringBuffer doPost2(String server, String query) throws IOException {
        URLConnection cc = post(server, query, 0);
        return getBuffer(cc.getInputStream());
    }

    public InputStream doPost(String server, String query, int timeout) throws IOException {
        URLConnection cc = post(server, query, timeout);
        return cc.getInputStream();
    }

    URLConnection post(String server, String query, int timeout) throws IOException {
        String qstr = "query=" + URLEncoder.encode(query, "UTF-8");

        URL queryURL = new URL(server);
        HttpURLConnection urlConn = (HttpURLConnection) queryURL.openConnection();
        urlConn.setRequestMethod("POST");
        urlConn.setDoOutput(true);
        urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        //urlConn.setRequestProperty("Accept", "application/rdf+xml,  application/sparql-results+xml");
        urlConn.setRequestProperty("Accept", "application/sparql-results+xml, application/rdf+xml");
        urlConn.setRequestProperty("Content-Length", String.valueOf(qstr.length()));
        urlConn.setRequestProperty("Accept-Charset", "UTF-8");
        urlConn.setReadTimeout(timeout);

        OutputStreamWriter out = new OutputStreamWriter(urlConn.getOutputStream());
        out.write(qstr);
        out.flush();

        return urlConn;

    }

    StringBuffer getBuffer(InputStream stream) throws IOException {
        InputStreamReader r = new InputStreamReader(stream, "UTF-8");
        BufferedReader br = new BufferedReader(r);
        StringBuffer sb = new StringBuffer();
        String str = null;

        while ((str = br.readLine()) != null) {
            sb.append(str);
            sb.append("\n");
        }

        return sb;
    }
//	public String callSoapEndPoint() {
//		SparqlSoapClient client = new SparqlSoapClient();
//		SparqlResult result = client.sparqlQuery("http://dbpedia.inria.fr/sparql", "select ?x ?r ?y where { ?x ?r ?y} limit 100");
//		String stringResult = result.toString();
//		return stringResult;
//	}
//
//	public static void main(String[] args) {
//		ProviderImpl impl = new ProviderImpl();
//		System.out.println(impl.callSoapEndPoint());
//	}
}
