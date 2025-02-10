package fr.inria.corese.core.sparql.triple.cst;

/**
 *
 */
public interface LogKey {

    String NS = "http://ns.inria.fr/corese/log/";
    String HEADER_NS = NS+"header/";
    String PREF = "ns:";
    String HEADER_PREF = "hd:";
    String SUBJECT = NS+"report" ;
    String EVALUATION_REPORT = "ns:EvaluationReport";
    String REPORT = "ns:ServiceReport";
    
    // initial service URL
    String SERVICE_URL = "ns:serviceURL";
    // initial service AST
    String SERVICE_AST = "ns:serviceAST";
    // initial service result
    String SERVICE_OUTPUT = "ns:serviceOutput";
    // list of endpoints
    String ENDPOINT = "ns:endpoint";
    String INDEX = "ns:index";
    String ENDPOINT_CALL = "ns:endpointCall";
    String ENDPOINT_NUMBER = "ns:endpointNumber";
    String ENDPOINT_URL    = "ns:endpointURL";
    // query that generated an exception 
    String QUERY = "ns:query";
    // federate query result of federate rewrite
    String AST = "ns:ast";
    // federate source selection
    String AST_SELECT = "ns:astSelect";
    String AST_INDEX = "ns:astIndex";
    // intermediate service call:
    String AST_SERVICE = "ns:astService";
    String INPUT_SIZE = "ns:astServiceLength";
    String MESSAGE = "ns:message";
    String DATE = "ns:date";
    String SERVER = "ns:server";
    String STATUS = "ns:status";
    String INFO = "ns:info";
    String URL = "ns:url";
    String URL_PARAM = "ns:urlParam";
    String LOG = "ns:log";
    String LINK = "ns:link";
    String INPUT_CARD = "ns:inputCard";
    String OUTPUT_CARD = "ns:outputCard";
    String OUTPUT_SIZE = "ns:outputLength";
    String TIME = "ns:time";
    String NBCALL = "ns:nbcall";
    String INPUT = "ns:input";
    String OUTPUT = "ns:output";
    String RESULT = OUTPUT;
    String RESULT_TEXT = "ns:outputText";
    String RESULT_SELECT = "ns:outputSelect";
    String RESULT_INDEX = "ns:outputIndex";
    String BNODE = "ns:bnode";
    String NL = System.getProperty("line.separator");

}
