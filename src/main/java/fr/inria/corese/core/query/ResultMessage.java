package fr.inria.corese.core.query;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.GraphDistance;
import fr.inria.corese.core.load.LoadException;
import fr.inria.corese.core.load.QueryLoad;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import fr.inria.corese.core.sparql.triple.parser.Context;
import fr.inria.corese.core.sparql.triple.parser.URLParam;
import fr.inria.corese.core.sparql.triple.parser.context.ContextLog;
import java.io.IOException;
import java.util.Date;
import jakarta.ws.rs.client.ResponseProcessingException;
import jakarta.ws.rs.core.Response;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Create JSON message with Context and ContextLog Message sent back to client
 * as LinkedResult
 */
public class ResultMessage {
    
    private JSONObject system;
    private static ResultMessage singleton = null;

    private Context context;
    private ContextLog log;
    private JSONObject json;
    private Graph graph;
    private ASTQuery ast;
    
    ResultMessage () {
        setSystem(new JSONObject());
        getSystem().put("system", getSingleton().system("uname -a"));
        getSystem().put("java", getSingleton().system("java --version"));
        getSystem().put("platform", "Corese 4.2.3");
        getSystem().put("date", new Date());
    }

    /**
     * JSON object with param=value sent back to client as Linked Result as
     * "message" return context as json object
     */
    public JSONObject process(Mappings map) {
        setAst(map.getAST());
        setJson(getContext().json());
        // select data from ContextLog
        if (getLog() != null) {
            getLog().message(getJson());
            // service endpoint exception
            messageException(getLog());
        }
        messageSystem();
        messageCardinality();
        messageDistance();
        return getJson();
    }
    
    void messageCardinality() {
        JSONObject obj = getGraph().cardinality(getAst());

        if (!obj.isEmpty()) {
            json.put(URLParam.CARDINALITY, obj);
        }
        getJson().put(URLParam.GRAPH_SIZE, getGraph().size());
    }
    
    void messageDistance() {
        JSONObject obj = distance();
        if (!obj.isEmpty()) {
            getJson().put(URLParam.EXPLAIN, obj);
        }
    }
    
    JSONObject distance() {
        int distance = GraphDistance.DISTANCE;
        
        if (getContext().hasValue(URLParam.DISTANCE)) {
            // URL parameter sv:distance=n
            IDatatype dt = getContext().getFirst(URLParam.DISTANCE);
            distance = Integer.parseInt(dt.getLabel());
        }

        return getGraph().match(getAst(), distance);
    }

    void messageException(ContextLog log) {
        if (!log.getExceptionList().isEmpty()) {
            JSONArray arr = new JSONArray();

            for (var ex : log.getExceptionList()) {
                JSONObject obj = message(ex);
                arr.put(obj);
            }

            getJson().put(URLParam.ERROR, arr);
        }
    }

    JSONObject message(EngineException e) {
        JSONObject messageJson = new JSONObject();

        if (e.getURL() != null) {
            messageJson.put(URLParam.URL, e.getURL().getServer());
        }

        if (e.getCause() instanceof ResponseProcessingException) {
            Response resp = (Response) e.getObject();
            if (resp != null) {
                messageJson.put("statusInfo", resp.getStatusInfo().toString());
                messageJson.put("status", resp.getStatus());
                String server = getServer(resp);
                if (server != null) {
                    messageJson.put("server", server);
                }
            }
        }

        messageJson.put(URLParam.MES, e.getMessage());
        return messageJson;
    }

    String getServer(Response resp) {
        if (resp.getHeaderString("Server") != null) {
            return resp.getHeaderString("Server");
        }
        return resp.getHeaderString("server");
    }
    
    void messageSystem() {
        getJson().put("system", getSingleton().getSystem());        
    }

    String system(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor();
            QueryLoad ql =  QueryLoad.create();
            return ql.readWE(p.getInputStream());
        } catch (IOException | InterruptedException | LoadException ignored) {
        }
        return null;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public ContextLog getLog() {
        return log;
    }

    public void setLog(ContextLog log) {
        this.log = log;
    }

    public JSONObject getJson() {
        return json;
    }

    public void setJson(JSONObject json) {
        this.json = json;
    }

    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    public ASTQuery getAst() {
        return ast;
    }

    public void setAst(ASTQuery ast) {
        this.ast = ast;
    }

    public static ResultMessage getSingleton() {
        if(singleton == null) {
            singleton = new ResultMessage();
        }
        return singleton;
    }

    public JSONObject getSystem() {
        return system;
    }

    public void setSystem(JSONObject system) {
        this.system = system;
    }

}
