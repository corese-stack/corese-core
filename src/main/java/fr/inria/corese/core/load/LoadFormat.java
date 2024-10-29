package fr.inria.corese.core.load;

import fr.inria.corese.core.api.Loader;
import fr.inria.corese.core.transform.Transformer;
import fr.inria.corese.core.sparql.triple.parser.NSManager;
import java.util.HashMap;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2016
 *
 */
public class LoadFormat {
    
    public static final String RULE = ".rul";
    static final String BRULE = ".brul";
    static final String IRULE = ".rl";
    static final String QUERY = ".rq";
    static final String UPDATE = ".ru";
    static final String TURTLE = ".ttl";
    static final String NT = ".nt";
    static final String N3 = ".n3";
    static final String TRIG = ".trig";
    static final String NQUADS = ".nq";
    static final String HTML = ".html";
    static final String XHTML = ".xhtml";
    static final String SVG = ".svg";
    static final String XML = ".xml";
    static final String EXT_RDF = ".rdf";
    static final String EXT_RDFS = ".rdfs";
    static final String EXT_OWL = ".owl";
    static final String OWL_RDFXML = ".owx";
    static final String JSONLD = ".jsonld";
    static final String JSON = ".json";
    static final String SWF = ".sw";
    
    static final String NT_FORMAT = NSManager.STL + "nt";

    static HashMap<String, Loader.format> ptable, utable, dtable;
    static HashMap<Loader.format, String> ftable;
    
    static {
        init();
    }
    
    static void init(){
        ptable = new HashMap<>();
        define(BRULE,   Loader.format.RULE_FORMAT);
        define(IRULE,   Loader.format.RULE_FORMAT);
        define(RULE,    Loader.format.RULE_FORMAT);
        define(QUERY,   Loader.format.QUERY_FORMAT);
        define(UPDATE,  Loader.format.QUERY_FORMAT);
        define(TURTLE,  Loader.format.TURTLE_FORMAT);
        define(NT,      Loader.format.NT_FORMAT);
        define(N3,      Loader.format.NT_FORMAT);
        define(TRIG,    Loader.format.TRIG_FORMAT);
        define(NQUADS,  Loader.format.NQUADS_FORMAT);
        define(HTML,    Loader.format.RDFA_FORMAT);
        define(XHTML,   Loader.format.RDFA_FORMAT);
        define(XML,     Loader.format.XML_FORMAT);
        define(SVG,     Loader.format.RDFA_FORMAT);
        define(EXT_RDF, Loader.format.RDFXML_FORMAT);
        define(EXT_RDFS,Loader.format.RDFXML_FORMAT);
        define(OWL_RDFXML, Loader.format.RDFXML_FORMAT);
        define(EXT_OWL, Loader.format.OWL_FORMAT);
        define(JSONLD,  Loader.format.JSONLD_FORMAT);
        define(JSON,    Loader.format.JSON_FORMAT);
        define(SWF,     Loader.format.WORKFLOW_FORMAT);
        
        utable = new HashMap<>();
        ftable = new HashMap<>();
        
        udefine(Loader.HTML_FORMAT_STR,   Loader.format.RDFA_FORMAT);
        udefine(Loader.TURTLE_FORMAT_STR, Loader.format.TURTLE_FORMAT);
        udefine(Loader.NT_FORMAT_STR,     Loader.format.NT_FORMAT);
        udefine(Loader.TRIG_FORMAT_STR,   Loader.format.TRIG_FORMAT);
        udefine(Loader.NQUADS_FORMAT_STR, Loader.format.NQUADS_FORMAT);
        udefine(Loader.RDFXML_FORMAT_STR, Loader.format.RDFXML_FORMAT);
        udefine(Loader.JSON_FORMAT_STR,   Loader.format.JSONLD_FORMAT);
        udefine(Loader.JSONLD_FORMAT_STR, Loader.format.JSONLD_FORMAT);
        udefine(Loader.ALL_FORMAT_STR,    Loader.format.OWL_FORMAT);
        
        
        dtable = new HashMap<String, Loader.format>();
        ddefine(Transformer.TURTLE, Loader.format.TURTLE_FORMAT);
        ddefine(NT_FORMAT,          Loader.format.NT_FORMAT);
        ddefine(Transformer.RDFXML, Loader.format.RDFXML_FORMAT);
        ddefine(Transformer.JSON,   Loader.format.JSONLD_FORMAT);
        
   }
      
    static void define(String extension, Loader.format format){
        ptable.put(extension, format);
    }
    
    static void udefine(String extension, Loader.format format){
        utable.put(extension, format);
        ftable.put(format, extension);
    }
     
    static void ddefine(String extension, Loader.format format){
        dtable.put(extension, format);
    }
    
    public static String getFormat(Loader.format format) {
        return ftable.get(format);
    }
    
    public static Loader.format getFormat(String path){
        if (path == null){
            return Loader.format.UNDEF_FORMAT;
        }
        int index = path.lastIndexOf(".");
        if (index == -1){
            return Loader.format.UNDEF_FORMAT;
        }
        String ext = path.substring(index);
        Loader.format format = ptable.get(ext);
        if (format == null){
           return Loader.format.UNDEF_FORMAT;
        }
        return format;
    }
    
    static Loader.format getTypeFormat(String contentType, Loader.format format) {
        for (String key : utable.keySet()) {
            if (contentType.startsWith(key)) {
                return utable.get(key);
            }
        }
        return format;
    }
    
    public static Loader.format getDTFormat(String format){
        Loader.format ft = dtable.get(format);
        if (ft == null){
            return Loader.format.UNDEF_FORMAT;
        }
        return ft;
    }
      
}
