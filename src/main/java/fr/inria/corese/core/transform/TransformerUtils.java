package fr.inria.corese.core.transform;

import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import fr.inria.corese.core.sparql.triple.parser.Context;
import fr.inria.corese.core.sparql.triple.parser.NSManager;
import fr.inria.corese.core.sparql.triple.parser.Processor;

public class TransformerUtils {

    public static final String NULL = "";
    public static final String STL = NSManager.STL;
    public static final String SQL = STL + "sql";
    public static final String SPIN = STL + "spin";
    public static final String TOSPIN = STL + "tospin";
    public static final String OWL = STL + "owl";
    public static final String OWLRL = STL + "owlrl";
    public static final String OWL_RL = STL + "owlrl";
    public static final String OWL_EL = STL + "owleltc";
    public static final String OWL_QL = STL + "owlqltc";
    public static final String OWL_TC = STL + "owltc";

    public static final String OWL_MAIN = STL + "main";

    public static final String PP_ERROR = STL + "pperror";
    public static final String PP_ERROR_MAIN = STL + "main";
    public static final String PP_ERROR_DISPLAY = STL + "display";
    public static final String DATASHAPE = STL + "dsmain";
    public static final String TEXT = STL + "text";
    public static final String TURTLE = STL + "turtle";
    public static final String TURTLE_HTML = STL + "hturtle";
    public static final String RDFXML = STL + "rdfxml";
    public static final String ALL = STL + "all";
    public static final String XML = STL + "xml";
    public static final String RDF = STL + "rdf";
    public static final String JSON = STL + "json";
    public static final String JSON_LD = STL + "jsonld";
    public static final String TRIG = STL + "trig";
    public static final String TABLE = STL + "table";
    public static final String HTML = STL + "html";
    public static final String SPARQL = STL + "sparql";
    public static final String RDFRESULT = STL + "result";
    public static final String NAVLAB = STL + "navlab";
    public static final String RDFTYPECHECK = STL + "rdftypecheck";
    public static final String SPINTYPECHECK = STL + "spintypecheck";
    public static final String STL_PROFILE = STL + "profile";
    public static final String STL_START = STL + "start";
    public static final String STL_MAIN = STL + "main";
    public static final String STL_TRACE = STL + "trace";
    public static final String STL_DEFAULT = Processor.STL_DEFAULT;
    public static final String STL_DEFAULT_NAMED = STL + "defaultNamed";
    public static final String STL_OPTIMIZE = STL + "optimize";
    public static final String STL_IMPORT = STL + "import";
    public static final String STL_PROCESS = Processor.STL_PROCESS;
    public static final String STL_AGGREGATE = Processor.STL_AGGREGATE;
    public static final String STL_TRANSFORM = Context.STL_TRANSFORM;
    public static final String STL_PREFIX = Context.STL_PREFIX;
    public static final String D3 = NSManager.D3;
    public static final String D3_ALL = D3 + "all";

    public static final String[] RESULT_FORMAT = { XML, JSON, RDF };
    public static final String[] GRAPHIC_FORMAT = { D3 + "graphic", D3 + "hierarchy" };

    // default
    public static final String PPRINTER = TURTLE;
    public static final String OUT = ASTQuery.OUT;
    public static final String IN = ASTQuery.IN;
    public static final String IN2 = ASTQuery.IN2;



    private TransformerUtils() {}
}
