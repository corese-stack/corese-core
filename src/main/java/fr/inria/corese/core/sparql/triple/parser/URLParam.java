package fr.inria.corese.core.sparql.triple.parser;

/**
 *
 */
public interface URLParam {
    
    String STAR = "*";
    String USER = "user";
    String SEPARATOR = "~";
    String CLIENT = "cn:";
    String SERVER = "sv:";

    String MODE = "mode";
    String QUERY = "query";
    String ACCESS = "access";
    String FORMAT = "format";
    String DECODE = "decode";
    String PARAM = "param";
    String ARG = "arg";
    String URI = "uri";
    String URL = "url";
    String DEFAULT_GRAPH = "default-graph-uri";
    String NAMED_GRAPH = "named-graph-uri";

    // specific for service clause
    String LOOP = "loop";
    String START = "start";
    String UNTIL = "until";
    String LIMIT = "limit";
    String SLICE = "slice";
    String TIMEOUT = "timeout";
    String TIME = "time";
    String BINDING = "binding";
    String FOCUS = "focus";
    String SKIP = "skip";
    String HEADER = "header";
    String COOKIE = "cookie";
    String HEADER_ACCEPT = "Accept";
    // service clause does not return variable in service result
    String UNSELECT = "unselect";
    String LOCAL = "local";
    
    String FED_SUCCESS  = "federateSuccess";
    String FED_LENGTH   = "federateLength";
    String FED_INCLUDE  = "include";
    
    // value of mode=
    String DISPLAY = "display";
    String TRACE = "trace";
    String DEBUG = "debug";
    String SHOW = "show";
    String TRAP = "trap";
    String SPARQL = "sparql";
    String PROVENANCE = "provenance";
    String DETAIL = "detail";
    String LOG = "log";
    String LOG_QUERY = "logquery";
    String NBVAR = "nbvar";
    String NBRESULT = "nboutput";
    String NBINPUT = "nbinput";
    String RESULT = "result";
    String RESULT_TEXT = "text";
    String FEDERATE = "federate";
    String FEDERATION = "federation";
    String SHACL = "shacl";
    String REQUEST = "request";
    String BEFORE = "before";
    String AFTER = "after";
    String SHARE = "share";
    String EXPORT = "export";
    String COUNT = "count";
    String PLATFORM = "platform";
    String CORESE = "corese";
    String CONSTRUCT = "construct";
    String WRAPPER = "wrapper";
    String ACCEPT = "accept";
    String REJECT = "reject";
    String DOCUMENT = "document";
    String LINK = "link";
    // display first transform, link other transform
    String LINK_REST = "linkrest";
    String TRANSFORM = "transform";
    String PARSE = "parse";
    String COMPILE = "compile";
    String PROPERTY = "property";
    String PREFIX = "prefix";
    String TO_SPIN = "spin";
    String EXPLAIN = "explain";
    String WHY = "why";
    String INPUT = "input";
    String OUTPUT = "output";
    String OUTPUT_INDEX = "outputIndex";
    String SEQUENCE = "sequence";

    // values of binding=
    String VALUES = "values";
    String FILTER = "filter";
    
    String REW = "rewrite";
    String INDEX = "index";
    String SEL = "select";
    String SRC = "source";
    String INFO = "info";
    String MES = "message";
    String ALL = "all";
    String WORKFLOW = "workflow";
    String TEST = "test";
    String DISTANCE = "distance";
    String ERROR = "error";
    String STATUS = "status";
    String CATCH = "catch";
    String UNDEF = "undefined";
    String DATE = "date";
    String ENDPOINT = "endpoint";
    String FAIL = "fail";
    String HISTORY = "history";
    String CARDINALITY = "cardinality";
    String GRAPH_SIZE = "graphSize";
    String MERGE = "merge";
    String SIZE = "size";
    String FULL_SIZE = "fullSize";
    String FULL_TIME = "fullTime";
    String CALL = "call";
    String NB_CALL = "nbCall";
    String NUMBER = "number";
    String LENGTH = "length";
    String SERVER_NAME = "server";
    String LOCATION = "location";
    String REPORT = "report";
    

}
