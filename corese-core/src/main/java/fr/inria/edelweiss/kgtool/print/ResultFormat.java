package fr.inria.edelweiss.kgtool.print;

import fr.inria.acacia.corese.triple.parser.ASTQuery;
import fr.inria.acacia.corese.triple.parser.Metadata;
import java.io.FileWriter;
import java.io.IOException;

import fr.inria.edelweiss.kgenv.parser.Pragma;
import fr.inria.edelweiss.kgram.core.Mappings;
import fr.inria.edelweiss.kgram.core.Query;
import fr.inria.edelweiss.kgraph.core.Graph;
import fr.inria.edelweiss.kgtool.util.MappingsGraph;
import java.util.HashMap;

/**
 * Select Result format according to query form and @display annotation.,
 * Olivier Corby Edelweiss INRIA 2011 - Wimmics 2016
 */
public class ResultFormat {

    public static final int UNDEF_FORMAT = -1;
    
    public static final int RDF_XML_FORMAT = 1;
    public static final int TURTLE_FORMAT  = 2;
    public static final int JSON_LD_FORMAT = 3;
    
    public static final int XML_FORMAT     = 11;
    public static final int RDF_FORMAT     = 12;
    public static final int JSON_FORMAT    = 13;
    
    public static int DEFAULT_SELECT_FORMAT = XML_FORMAT;
    public static int DEFAULT_CONSTRUCT_FORMAT = RDF_XML_FORMAT;
    
    Mappings map;
    Graph graph;
    int type = UNDEF_FORMAT;
    private int construct_format = DEFAULT_CONSTRUCT_FORMAT;
    private int select_format = DEFAULT_SELECT_FORMAT;
    private long nbResult = Long.MAX_VALUE;
    
    static HashMap<String, Integer> table;
    
    static {
        init();
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

    public static void setDefaultSelectFormat(int i) {
        DEFAULT_SELECT_FORMAT = i;
    }

    public static void setDefaultConstructFormat(int i) {
        DEFAULT_CONSTRUCT_FORMAT = i;
    }
    
    static int type(Mappings m) {
        Integer type = UNDEF_FORMAT;
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

    @Override
    public String toString() {
        if (map == null){
            return graphToString();
        }
        else {
           return mapToString(); 
        }
    }
        
    String graphToString(){
        if (type == UNDEF_FORMAT){
            type = getConstructFormat();
        }
        switch (type){
            case RDF_XML_FORMAT:
                return  RDFFormat.create(graph).toString();
            case TURTLE_FORMAT:
                return TripleFormat.create(graph).toString();
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
        
        ASTQuery ast = (ASTQuery) q.getAST();

        if (q.isTemplate()
                || (q.hasPragma(Pragma.TEMPLATE) && map.getGraph() != null)) {
            return TemplateFormat.create(map).toString();
        } else {
            if (type == UNDEF_FORMAT) {
                if (q.isConstruct()) {
                    type = getConstructFormat();
                } 
                else {
                    type = getSelectFormat();               
                }
            }
            
            return process(map, type);
        }
    }
    
    

    String process(Mappings map, int type) {
        switch (type) {
            case RDF_XML_FORMAT: return RDFFormat.create(map).toString();
            case TURTLE_FORMAT:  return TripleFormat.create(map).toString();
            case JSON_LD_FORMAT: return JSONLDFormat.create(map).toString();

            case XML_FORMAT: 
                XMLFormat ft =  XMLFormat.create(map); 
                ft.setNbResult(nbResult);
                return ft.toString();
                
            case RDF_FORMAT: 
                Graph g = MappingsGraph.create(map).getGraph();
                return TripleFormat.create(g).toString();
            case JSON_FORMAT: return JSONFormat.create(map).toString();
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
}
