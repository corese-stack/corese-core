package fr.inria.corese.core.load;

import fr.inria.corese.sparql.triple.parser.ASTQuery;
import fr.inria.corese.sparql.triple.parser.Metadata;
import fr.inria.corese.kgram.core.Mapping;
import fr.inria.corese.kgram.core.Mappings;
import fr.inria.corese.kgram.core.Query;
import fr.inria.corese.core.Graph;
import fr.inria.corese.core.print.ResultFormat;
import static fr.inria.corese.core.print.ResultFormat.RDF_XML;
import static fr.inria.corese.core.print.ResultFormat.SPARQL_RESULTS_XML;
import fr.inria.corese.core.query.CompileService;
import fr.inria.corese.core.util.Property;
import static fr.inria.corese.core.util.Property.Value.SERVICE_LIMIT;
import fr.inria.corese.sparql.triple.function.term.Binding;
import fr.inria.corese.sparql.triple.parser.Access;
import fr.inria.corese.sparql.triple.parser.HashMapList;
import fr.inria.corese.sparql.triple.parser.URLParam;
import fr.inria.corese.sparql.triple.parser.URLServer;
import fr.inria.corese.sparql.triple.parser.context.ContextLog;
import java.io.InputStream;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Cookie;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Send a SPARQL query to a SPARQL endpoint
 * Return a Mappings
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2015
 */
public class Service implements URLParam {
    static Logger logger = LoggerFactory.getLogger(Service.class);
     static final String ENCODING = "UTF-8";
     // load take URL parameter into account, e.g. format=rdfxml
    public static boolean LOAD_WITH_PARAMETER = false;
    public static final String MIME_TYPE = "application/sparql-results+xml,application/rdf+xml";
    public static final String XML = SPARQL_RESULTS_XML;
    public static final String RDF = RDF_XML;
    
    private ClientBuilder clientBuilder;
    private ContextLog log;

    private boolean isDebug = false;
    private boolean post = true;
    private boolean showResult = false;
    private boolean trap = false;
    private int timeout = 0;
    private int count = 0;
    private URLServer url;
    private Binding bind;
    private Access.Level level = Access.Level.DEFAULT;
    private String format;
    private ServiceParser parser;
    
    public Service() {
        clientBuilder = ClientBuilder.newBuilder();
    }

    public Service(URLServer serv) {
        this(serv, ClientBuilder.newBuilder());
    }
    public Service(String serv) {
        this(new URLServer(serv), ClientBuilder.newBuilder());
    }
    public Service(URLServer serv, ClientBuilder builder) {
        this.clientBuilder = builder;
        setURL(serv);
        setParser(new ServiceParser(serv));
    }


    public Mappings select(String query) throws LoadException {
        return getParser().parseMapping(query, process(query), ENCODING);
    }

    public Graph construct(String query) throws LoadException {
        return getParser().parseGraph(process(query));
    }

    public Mappings query(Query query, Mapping m) throws LoadException {
        return query(query, getAST(query), m);
    }
    
    /**
     * @todo: 
     * accept = sparql-results+xml ; rdf+xml
     * bug with: @federate construct
     * corese server return sparql-results+xml and Service perform parseGraph
     */
    public Mappings query(Query query, ASTQuery ast, Mapping m) throws LoadException {
        metadata(ast);
        Mappings map;
        if (m != null) {
            ast = mapping(query, m);
        }
        String astq = ast.toString();
        if (ast.isSelect() || ast.isAsk() || ast.isUpdate()) {
            map = getParser().parseMapping(astq, process(ast, astq), encoding(ast));
        } 
        else {
            Graph g = getParser().parseGraph(process(ast, astq), encoding(ast));
            map = new Mappings();
            map.setGraph(g);
        }
        map.setQuery(query);
        map.init(query);
        return map;
    }
    
    public String process(ASTQuery ast, String query) {
        return process(query, accept(ast));
    }
       
    public String process(String query) {
        return process(query, getAccept());
    }
    
    public String process(String query, String mime) {
        if (isPost()) {
            return post(query, mime);
        }
        else {
            return get(query, mime);
        }
    }
    
    // https://docs.oracle.com/javaee/7/api/index.html
    public String post(String url, String query, String mime) {
//        if (isDebug()) {
//            System.out.println("service post " + url);
//            System.out.println(query);
//        }
        clientBuilder.connectTimeout(timeout, TimeUnit.MILLISECONDS);
        clientBuilder.readTimeout(timeout, TimeUnit.MILLISECONDS);
        Client client = clientBuilder.build(); 
        WebTarget target = client.target(url);
        Form form = getForm();
        form.param(QUERY, query);
        try {
            //String res = target.request(mime).post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), String.class);
            // request() return Invocation.Builder
            Cookie cook = new Cookie(COUNT, Integer.toString(getCount()), url, getURL().getServer());
            Cookie cook2 = new Cookie(PLATFORM, CORESE);
            
            Builder rb = target.request(mime); // .cookie(cook)
            setHeader(rb);
            
            Date d1 = new Date();
            if (isDebug()) {
                logger.info("Post " + getURL().getURL());
            }
            
            Response resp =  rb.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
            
            if (isDebug()) {
                Date d2 = new Date();
                logger.info("After post: " + resp.getStatus() + " " + resp.getStatusInfo());
                logger.info("Time post: " + ((d2.getTime()-d1.getTime())/1000.0));
            }

            String res = resp.readEntity(String.class);
            
            if (isDebug()) {
                Date d2 = new Date();
                logger.info("Time read: " + ((d2.getTime() - d1.getTime()) / 1000.0));
            }
            
            if (resp.getStatus() == Response.Status.SEE_OTHER.getStatusCode()) {
                String myUrl = resp.getLocation().toString();
                logger.warn(String.format("Service redirection: %s to: %s", url, myUrl));
                if (myUrl.equals(url)) {
                    throw new RedirectionException(resp);
                }
                return post(myUrl, query, mime);
            }
               
            trace(resp);
            
            if (resp.getStatus() >= Response.Status.BAD_REQUEST.getStatusCode()) {
                throw new ResponseProcessingException(resp, res);
            }
            
            recordFormat(resp.getMediaType().toString());            
            trace(res);
            return res;
        } catch (RedirectionException ex) {
            String uri = ex.getLocation().toString();
            logger.warn(String.format("Service redirection: %s to: %s", url, uri));
            if (uri.equals(url)) {
                throw ex;
            }
            return post(uri, query, mime);
        }
        catch (Exception e) {
            logger.error(e.getClass().getName() + " " + e.getMessage());
            throw e;
        }
    }
    
    void trace(Response res) {
        if (getURL().hasParameter(DISPLAY, HEADER)) {
            System.out.println("service header: " + getURL().getURL());
            for (String name : res.getHeaders().keySet()) {
                System.out.println("header: " + name + "=" + res.getHeaderString(name));
            }
        }
    }
    
    void trace(String str) {
        if (isShowResult()) {
            System.out.println("Service string result");
            System.out.println(str);
        }
    }
    
    Builder setHeader(Builder rb) {
        if (getURL().hasParameter(HEADER)) {
            for (String header : getURL().getParameterList(HEADER)) {
                String[] pair = header.split(":");
                if (pair.length >= 2) {
                    rb.header(pair[0], pair[1]);
                }
            }
        }
        return rb;
    }

    ASTQuery mapping(Query q, Mapping m) {
        Mappings map = new Mappings();
        map.add(m);
        CompileService cs = new CompileService();
        ASTQuery ast = cs.filter(getURL(), q, map, 0, 1);
        return ast;
    }


    //   /sparql?format=json
    String getAccept() {
        String ft = getURL().getParameter(FORMAT);
        if (ft == null) {
            return MIME_TYPE;
        }
        return ResultFormat.decode(ft);
    }
    
    String accept(ASTQuery ast) {
        String ft = getURL().getParameter(FORMAT);
        if (ft == null) {
            return format(ast);
        }
        logger.info("Accept: "+ ft + " " + ResultFormat.decode(ft));
        return ResultFormat.decode(ft);
    }
        

    String format(ASTQuery ast) {
        if (ast.isConstruct() || ast.isDescribe()) {
            return RDF_XML;
        }
        return XML;
    }

    public String post(String query, String mime) {
        // Server URL without parameters
        return post(getURL().getServer(), query, mime);
    }
    

    
    Form getForm() {
        return getURL().getMap() == null ? new Form() : new Form(getMap(getURL().getMap()));
    }
    
    MultivaluedMap<String, String> getMap(HashMapList<String> map) {
        MultivaluedMap<String, String> amap = new MultivaluedHashMap<>();
        for (String key : map.keySet()) {
            amap.put(key, map.get(key));
        }
        return amap;
    }
    
//    void complete(Form form) {
//        if (getURL().getMap() != null) {
//            for (String key : getURL().getMap().keySet()) {
//                //System.out.println("service: " + key + "=" + getURL().getParameter(key));
//                form.param(key, getURL().getParameter(key));
//            }
//        }
//    }

    public String get(String query, String mime) {
        // Server URL without parameters
        return get(getURL().getServer(), query, mime);
    }

    public String get(String uri, String query, String mime) {
        if (isDebug()) {
            System.out.println(query);
        }
        String url;
        try {
            url = complete(uri, URLEncoder.encode(query, "UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            url = complete(uri, query);
        }
        return getBasic(url, mime);
    }
    
    String complete(String uri, String query) {
        if (getURL().hasParameter()) {
            return String.format("%s?%s&query=%s", uri, param(), query);
        }
        return String.format("%s?query=%s", uri, query);
    }
    
    /**
     * Generate the parameter string
     * We can skip some key=val parameters here.
     */
    String param() {
        StringBuilder sb = new StringBuilder();
        for (String key : getURL().getMap().keySet()) {
            for (String value : getURL().getMap().get(key)) {
                String format = sb.length() == 0 ? "%s=%s" : "&%s=%s";
                sb.append(String.format(format, key, value));
            }
        }
        return sb.toString();
    }
        
    public String getBasic(String url, String mime) {
        Response resp = getResponse(url, mime);
        String res = resp.readEntity(String.class);
        trace(res);
        return res;
    }
            
    public InputStream getStream(String url, String mime)  {
        Response resp = getResponse(url, mime);
        InputStream res = resp.readEntity(InputStream.class);
        return res;
    }

    // may take URL parameter into account:    ?format=rdfxml
    public InputStream load(String url, String mime)  {
        if (LOAD_WITH_PARAMETER) {
            return getStream(getURL().getServer(), getFormat(mime));
        }
        return getStream(url, mime);
    }
    
    // may take URL parameter into account:    ?format=rdfxml
    String getFormat(String defaut) {
        String format = getURL().getParameter(URLServer.FORMAT);
        if (format != null) {
            // format=rdfxml | application/rdf+xml
            String res = ResultFormat.decodeLoadFormat(format);
            if (res != null) {
                // res = application/rdf+xml
                return res;
            }
        }
        return defaut;
    }
    
    Response getResponse(String url, String mime) {
        //System.out.println("Service:  " + url + " " + mime);
        clientBuilder.connectTimeout(timeout, TimeUnit.MILLISECONDS);
        Client client = clientBuilder.build();
        WebTarget target = client.target(url);
        Response resp = target.request(mime).get();
        if (resp.getMediaType()!=null) {
            recordFormat(resp.getMediaType().toString());
        }

        if (resp.getStatus() == Response.Status.SEE_OTHER.getStatusCode()) {
            String myUrl = resp.getLocation().toString();
            logger.warn(String.format("Service redirection: %s to: %s", url, myUrl));
            if (myUrl.equals(url)) {
                throw new javax.ws.rs.RedirectionException(resp);
            }
            return getResponse(myUrl, mime);
        }
        return resp;
    }
    
    
    public Response get(String uri) {
        Client client = clientBuilder.build();
        WebTarget target = client.target(uri);
        Response res = target.request().get();
        return res;
    }
    
    public String getString(String uri) {
        return get(uri).readEntity(String.class);
    }
    
    public JSONObject getJson(String uri) {
        String text = getString(uri);
        if (text != null && ! text.isEmpty()) {
            return new JSONObject(text);
        }
        return null;
    }

    String encoding(ASTQuery ast) {
        if (ast.hasMetadata(Metadata.ENCODING)) {
            return ast.getMetadata().getStringValue(Metadata.ENCODING);
        }
        return ENCODING;
    }


    /**
     * @return the level
     */
    public Access.Level getLevel() {
        return level;
    }

    /**
     * @param level the level to set
     */
    public void setLevel(Access.Level level) {
        this.level = level;
    }

    /**
     * @return the post
     */
    public boolean isPost() {
        return post;
    }

    /**
     * @param post the post to set
     */
    public void setPost(boolean post) {
        this.post = post;
    }

    /**
     * @return the showResult
     */
    public boolean isShowResult() {
        return showResult;
    }

    /**
     * @param showResult the showResult to set
     */
    public void setShowResult(boolean showResult) {
        this.showResult = showResult;
    }

    /**
     * @return the trap
     */
    public boolean isTrap() {
        return trap;
    }

    /**
     * @param trap the trap to set
     */
    public void setTrap(boolean trap) {
        this.trap = trap;
    }

    
    public URLServer getURL() {
        return url;
    }

   
    public void setURL(URLServer url) {
        this.url = url;
    }
   
    public String getFormat() {
        return format;
    }


    public void setFormat(String format) {
        this.format = format; 
    }
    
    void recordFormat(String format) {
        setFormat(format);
        if (getParser()!=null) {
            getParser().setFormat(format);
        }
        logger.info("Content type: " + format);
        if (getLog() != null) {
            getLog().getFormatList().add(format);
        }
    }

    
    public int getTimeout() {
        return timeout;
    }

    
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    
    public int getCount() {
        return count;
    }

    
    public void setCount(int count) {
        this.count = count;
    }
    
        ASTQuery getAST(Query q) {
        return  q.getAST();
    }
    
    void metadata(ASTQuery ast) {
        if (!ast.hasLimit()) {
            if (ast.hasMetadata(Metadata.LIMIT)) {
                ast.setLimit(ast.getMetadata().getDatatypeValue(Metadata.LIMIT).intValue());
            }
            // DRAFT: for testing (modify ast ...)
            Integer lim = getURL().intValue(LIMIT);
            if (lim != -1) {
                ast.setLimit(lim);
            }
            lim = Property.intValue(SERVICE_LIMIT);
            if (lim != null) {
                ast.setLimit(lim);
            }
            
        }
        if (getURL().isGET() || ast.getGlobalAST().hasMetadata(Metadata.GET)) {
            setPost(false);
        }
        if (getURL().hasParameter(MODE, SHOW)) {
            setShowResult(true);
        }
        if (ast.getGlobalAST().isDebug()) {
            System.out.println(isPost()?"POST":"GET");
        }
        getParser().setTrap(getURL().hasParameter(MODE, TRAP) || ast.getGlobalAST().hasMetadata(Metadata.TRAP));
        if (! isShowResult()) {
            setShowResult(ast.getGlobalAST().hasMetadata(Metadata.SHOW));
            getParser().setShowResult(isShowResult());        
        }
    }

    /**
     * @return the bind
     */
    public Binding getBind() {
        return bind;
    }

    /**
     * @param bind the bind to set
     */
    public void setBind(Binding bind) {
        this.bind = bind;
        getParser().setBind(bind);
    }

    /**
     * @return the parser
     */
    public ServiceParser getParser() {
        return parser;
    }

    /**
     * @param parser the parser to set
     */
    public void setParser(ServiceParser parser) {
        this.parser = parser;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public void setDebug(boolean isDebug) {
        this.isDebug = isDebug;
    }

    public ContextLog getLog() {
        return log;
    }

    public void setLog(ContextLog log) {
        this.log = log;
    }
    
}
