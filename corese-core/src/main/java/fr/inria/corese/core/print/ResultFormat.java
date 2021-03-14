package fr.inria.corese.core.print;

import fr.inria.corese.sparql.triple.parser.ASTQuery;
import fr.inria.corese.sparql.triple.parser.Metadata;
import java.io.FileWriter;
import java.io.IOException;

import fr.inria.corese.compiler.parser.Pragma;
import fr.inria.corese.kgram.core.Mappings;
import fr.inria.corese.kgram.core.Query;
import fr.inria.corese.core.Graph;
import fr.inria.corese.core.transform.Transformer;
import fr.inria.corese.core.util.MappingsGraph;
import fr.inria.corese.kgram.api.core.Node;
import fr.inria.corese.sparql.api.IDatatype;
import fr.inria.corese.sparql.api.ResultFormatDef;
import java.util.HashMap;

/**
 * Select Result format according to query form and @display annotation.,
 * Olivier Corby Edelweiss INRIA 2011 - Wimmics 2016
 */
public class ResultFormat implements ResultFormatDef {
    
    public static final String SPARQL_RESULTS_XML  = "application/sparql-results+xml";
    public static final String SPARQL_RESULTS_JSON = "application/sparql-results+json";
    public static final String SPARQL_RESULTS_CSV  = "application/sparql-results+csv";
    public static final String SPARQL_RESULTS_TSV  = "application/sparql-results+tsv";
    
    public static final String XML         = "application/xml";
    
    public static final String JSON_LD     = "application/ld+json";
    public static final String JSON        = "application/json";
    public static final String RDF_XML     = "application/rdf+xml";
    public static final String TRIG        = "application/trig";
    public static final String TURTLE      = "application/turtle";
    public static final String TURTLE_TEXT = "text/turtle"; 
    public static final String TRIG_TEXT   = "text/trig"; 
    public static final String NT_TEXT     = "text/nt"; 
    public static final String TEXT        = "text/plain"; 
    
    public static int DEFAULT_SELECT_FORMAT    = XML_FORMAT;
    public static int DEFAULT_CONSTRUCT_FORMAT = RDF_XML_FORMAT;
    
    Mappings map;
    Graph graph;
    int type = UNDEF_FORMAT;
    private int construct_format = DEFAULT_CONSTRUCT_FORMAT;
    private int select_format = DEFAULT_SELECT_FORMAT;
    private long nbResult = Long.MAX_VALUE;
    private String contentType;
    private boolean selectAll = false;
    
    static HashMap<String, Integer> table, format;
    static HashMap<Integer, String> content;
    
    static {
        init();
        initFormat();
    }
    
    static void init(){
        table = new HashMap();
        table.put(Metadata.DISPLAY_TURTLE, TURTLE_FORMAT);
        table.put(Metadata.DISPLAY_RDF_XML, RDF_XML_FORMAT);
        table.put(Metadata.DISPLAY_JSON_LD, JSON_LD_FORMAT);
        
        table.put(Metadata.DISPLAY_RDF, RDF_FORMAT);
        table.put(Metadata.DISPLAY_XML, XML_FORMAT);
        table.put(Metadata.DISPLAY_JSON, JSON_FORMAT);
    }
    
    
    static void initFormat() {
        format = new HashMap<>();
        content = new HashMap<>();
        
        // use case: template without format
        defContent(TEXT, TEXT_FORMAT);
        
        // Mappings
        defContent(SPARQL_RESULTS_JSON, JSON_FORMAT);
        defContent(SPARQL_RESULTS_XML, XML_FORMAT);
        defContent(SPARQL_RESULTS_CSV, CSV_FORMAT);
        defContent(SPARQL_RESULTS_TSV, TSV_FORMAT);
        // Graph
        defContent(RDF_XML,           RDF_XML_FORMAT);        
        defContent(TURTLE_TEXT,       TURTLE_FORMAT);
        defContent(TRIG,              TRIG_FORMAT);
        defContent(JSON_LD,           JSON_LD_FORMAT);
        defContent(JSON,              JSON_FORMAT);
        
        format.put(TRIG_TEXT, TRIG_FORMAT);
        format.put(NT_TEXT, TURTLE_FORMAT);
        format.put(TURTLE, TURTLE_FORMAT);
        format.put(XML, XML_FORMAT);
    
        // shortcut for HTTP parameter format=
        format.put("text", TEXT_FORMAT);
        
        format.put("json", JSON_FORMAT);
        format.put("xml", XML_FORMAT);
        format.put("csv", CSV_FORMAT);
        format.put("tsv", TSV_FORMAT);
        
        format.put("jsonld", JSON_LD_FORMAT);
        format.put("rdf", TURTLE_FORMAT);
        format.put("turtle", TURTLE_FORMAT);
        format.put("trig", TRIG_FORMAT);
        format.put("rdfxml", RDF_XML_FORMAT);
    }
    
    static void defContent(String f, int t) {
        format.put(f, t);
        content.put(t, f);
    }

    ResultFormat(Mappings m) {
        map = m;
    }
    
     ResultFormat(Graph g) {
        graph = g;
    }
    
    ResultFormat(Mappings m, int type) {
        this(m);
        this.type = type;
    }
    
    ResultFormat(Mappings m, int sel, int cons) {
        this(m);
        this.select_format = sel;
        this.construct_format = cons;
    }
    
    ResultFormat(Graph g, int type) {
        this(g);
        this.type = type;
    }
    

    static public ResultFormat create(Mappings m) {
        return new ResultFormat(m, type(m));
    }
    
    /**
     * format: application/sparql-results+xml 
     * format may be null
     */
    static public ResultFormat create(Mappings m, String format) {
        String myFormat = tuneFormat(m, format);
        if (myFormat == null) {
            return create(m);
        }
        int type = getType(myFormat);
        return new ResultFormat(m, type);
    }
    
    // special case: template without format considered as text format
    static String tuneFormat(Mappings m, String format) {
        if (m.getQuery() != null) {
            if (format == null) {
                if (m.getQuery().isTemplate()) {
                    return "text/plain";
                }
            } 
        }
        return format;
    }
    
    // in case where type = text
    static int defaultType(Mappings map) {
        return map.getGraph() == null ? DEFAULT_SELECT_FORMAT : TURTLE_FORMAT; 
    }
    
    static String defaultFormat(Mappings map) {
        return getFormat(defaultType(map));
    }
    
   
    static public ResultFormat format(Mappings m) {
        return new ResultFormat(m, DEFAULT_SELECT_FORMAT, TURTLE_FORMAT);
    }
    
    static public ResultFormat create(Mappings m, int type) {
        return new ResultFormat(m, type);
    }
    
    static public ResultFormat create(Mappings m, int sel, int cons) {
        return new ResultFormat(m, sel, cons);
    }
    
    static public ResultFormat create(Graph g) {
        return new ResultFormat(g);
    }
    
    static public ResultFormat create(Graph g, int type) {
        return new ResultFormat(g, type);
    }
    
    static public ResultFormat create(Graph g, String type) {
        return new ResultFormat(g, getSyntax(type));
    }

    public static void setDefaultSelectFormat(int i) {
        DEFAULT_SELECT_FORMAT = i;
    }

    public static void setDefaultConstructFormat(int i) {
        DEFAULT_CONSTRUCT_FORMAT = i;
    }
    
    // no type was given at creation
    static int type(Mappings m) {
        Integer type = UNDEF_FORMAT;
        if (m.getQuery().isTemplate()) {
            return TEXT_FORMAT;
        }
        ASTQuery ast = (ASTQuery) m.getAST();
        if (ast != null && ast.hasMetadata(Metadata.DISPLAY)) {
            String val = ast.getMetadata().getValue(Metadata.DISPLAY);
            type = table.get(val);
            if (type == null){
                type = UNDEF_FORMAT;
            }
        }
        return type;
    }
    
    // str = application/sparql-results+json OR json
    public static int getFormat(String str) {
        if (str != null && format.containsKey(str)) {
            return format.get(str);
        }
        return DEFAULT_SELECT_FORMAT;
    }
    
    public static String getFormat(int type) {
        String ft = content.get(type);
        if (ft == null) {
            return getFormat(DEFAULT_SELECT_FORMAT);
        }
        return ft;
    }
    
    static int getType(String ft) {
        return getFormat(ft);
    }
       

    @Override
    public String toString() {
        if (map == null){
            return graphToString();
        }
        else {
           return mapToString(); 
        }
    }
    
    public String toString(IDatatype dt) {
        Node node = graph.getNode(dt);
        if (node == null) {
            return dt.toString();
        }
        return graphToString(node);
    }
    
    static int getSyntax(String syntax) {
        if (syntax.equals(Transformer.RDFXML)) {
            return ResultFormat.RDF_XML_FORMAT;           
        }
        return ResultFormat.TURTLE_FORMAT; 
    }
       
    String graphToString() {
        return graphToString(null);
    }
        
    String graphToString(Node node){
        if (type() == UNDEF_FORMAT){
            setType(getConstructFormat());
        }
        switch (type){
            case RDF_XML_FORMAT:
                return  RDFFormat.create(graph).toString();
            case TURTLE_FORMAT:
                return TripleFormat.create(graph).toString(node);
            case TRIG_FORMAT:
                return TripleFormat.create(graph, true).toString(node);    
            case JSON_LD_FORMAT:
                return JSONLDFormat.create(graph).toString();               
        }
        return null;
    }   
    
    String mapToString(){
        Query q = map.getQuery();
        if (q == null) {
            return "";
        }
        
        if (q.isTemplate()) {
            return map.getTemplateStringResult();
        } 
        else if (q.hasPragma(Pragma.TEMPLATE) && map.getGraph() != null) {
            return TemplateFormat.create(map).toString();
        } 
        else {
            if (type() == UNDEF_FORMAT) {
                if (q.isConstruct()) {
                    setType(getConstructFormat());
                } else {
                    setType(getSelectFormat());
                }
            }

            return process(map);
        }
    }
    
    boolean isGraphFormat(int type) {
        switch (type) {
            case RDF_XML_FORMAT:
            case TURTLE_FORMAT:
            case TRIG_FORMAT:
            case JSON_LD_FORMAT:
            case RDF_FORMAT: return true;
            default: return false;
        }
    }
        
    /**
     * Tune the format
     */
    String process(Mappings map) {
        int mytype = type();
        if (isGraphFormat(type) && map.getGraph() == null) {
            // return Mappings as W3C RDF Graph Mappings
            map.setGraph(MappingsGraph.create(map).getGraph());
        }
        else if (mytype == TEXT_FORMAT) {
            // Chose appropriate format
            // Content-Type remains text/plain, do not setType()
            mytype = defaultType(map);
        }
//        else if (!isGraphFormat(mytype) && map.getGraph() != null) {
//            // display graph but format is not graph
//            // select approximate format
//            switch (mytype) {
//                case XML_FORMAT:  mytype = setType(RDF_XML_FORMAT); break;
//                case JSON_FORMAT: mytype = setType(JSON_LD_FORMAT); break;
//            }
//        }
        return processBasic(map, mytype);
    }
       
    
    /**
     * Main function
     * map may contain a graph (construct OR W3C RDF graph format for Mappings)
     */  
    String processBasic(Mappings map, int type) {
        switch (type) {                         
            // map is graph
            case RDF_XML_FORMAT:
                return RDFFormat.create(map).toString();
            case TURTLE_FORMAT:
                return TripleFormat.create(map).toString();
            case RDF_FORMAT:
                // W3C RDF Graph Mappings, graph has been set above
                return TripleFormat.create(map).toString();
            case TRIG_FORMAT:
                return TripleFormat.create(map, true).toString();                
            case JSON_LD_FORMAT:
                return JSONLDFormat.create(map).toString();
                            
                
            // map is query result
            case XML_FORMAT:
                XMLFormat ft = XMLFormat.create(map);
                ft.setSelectAll(isSelectAll());
                ft.setNbResult(nbResult);
                return ft.toString();

            case JSON_FORMAT:
                return JSONFormat.create(map).toString();

            case CSV_FORMAT:                
                return CSVFormat.create(map).toString();
                
            case TSV_FORMAT:
                return TSVFormat.create(map).toString();

        }
        return null;
    }

    public void write(String name) throws IOException {
        FileWriter fw = new FileWriter(name);
        String str = toString();
        fw.write(str);
        fw.flush();
        fw.close();
    }

    /**
     * @return the construct_format
     */
    public int getConstructFormat() {
        return construct_format;
    }

    /**
     * @param construct_format the construct_format to set
     */
    public void setConstructFormat(int construct_format) {
        this.construct_format = construct_format;
    }

    /**
     * @return the select_format
     */
    public int getSelectFormat() {
        return select_format;
    }

    /**
     * @param select_format the select_format to set
     */
    public void setSelectFormat(int select_format) {
        this.select_format = select_format;
    }

    /**
     * @return the nbResult
     */
    public long getNbResult() {
        return nbResult;
    }

    /**
     * @param nbResult the nbResult to set
     */
    public void setNbResult(long nbResult) {
        this.nbResult = nbResult;
    }
    
    public int type() {
        return type;
    }
    
    int setType(int t) {
        type = t;
        return t;
    }
    
    public String getContentType() {
        String ct = content.get(type());
        if (ct == null) {
            ct = content.get(DEFAULT_SELECT_FORMAT);
        }
        return ct;
    }

    /**
     * @param contentType the contentType to set
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * @return the selectAll
     */
    public boolean isSelectAll() {
        return selectAll;
    }

    /**
     * @param selectAll the selectAll to set
     */
    public void setSelectAll(boolean selectAll) {
        this.selectAll = selectAll;
    }
    
    
}
