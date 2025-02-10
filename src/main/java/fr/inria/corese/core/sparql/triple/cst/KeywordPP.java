package fr.inria.corese.core.sparql.triple.cst;

import fr.inria.corese.core.sparql.datatype.RDF;



public interface KeywordPP {

	String VAR1 = "?";
	String VAR2 = "$";
	String SDT = "^^";
	String LANG = "@";
    String QUOTE = "'";
    String DQUOTE = "\"";
    String TQUOTE = "\"\"\"";
    String BN = RDF.BLANKSEED ;

    String CORESE_PREFIX = "function://";
    
    /** Constants used for the pretty printer */
    String BASE = "base";
    String PREFIX = "prefix";
    String AS = "as";
    String SELECT = "select";
    String DELETE = "delete";
    String ASK = "ask";
    String CONSTRUCT = "construct";
    String INSERT = "insert";
    String DATA = "data";
    String DESCRIBE = "describe";
    String DEBUG = "debug";
    String NOSORT = "nosort";
    String ONE = "one";
    String MORE = "more";
    String LIST = "list";
    String MERGE = "merge";
    String GROUP = "group";
    String GROUPBY = "group by";
    String COUNT = "count";
    String SORT = "sort";
    String DISPLAY = "display";
    String TABLE = "table";
    String DRDF = "rdf";
    String FLAT = "flat";
    String ASQUERY = "asquery";
    String XML = "xml";
    String BLANK = "blank";
    String DISTINCT = "distinct";
    String REDUCED = "reduced";
    String SORTED = "sorted";
    String STAR = "*";
    String PROJECTION = "projection";
    String RESULT = "result";
    String THRESHOLD = "threshold";
    String FROM  = "from";
    String NAMED = "named";
    String WHERE = "where";
    String GRAPH = "graph";
    String STATE = "state";
    String LEAF  = "leaf";
    String TUPLE = "tuple";
    String JOIN  = "join";

    String SCORE = "score";
    String FILTER = "filter";
    String OPTIONAL = "optional";
    String UNION = "union";
    String MINUS = "minus";
    String DOT = " . ";
    String COMMA = ",";
    String ORDERBY = "order by";
    String DESC = "desc";
    String DISTANCE = "distance";
    String LIMIT = "limit";
    String HAVING = "having";
    String OFFSET = "offset";
    String BINDINGS = "values";
    String UNDEF = "UNDEF";
    String PRAGMA = "pragma";

    String SPACE = " ";
    String SPACE_LN = " " + System.getProperty("line.separator");
    
    String OPEN_BRACKET = "{";
    String CLOSE_BRACKET = "}";
    String OPEN_SQUARE_BRACKET = "[";
    String CLOSE_SQUARE_BRACKET = "]";
    String OPEN_PAREN = "(";
    String CLOSE_PAREN = ")";
    String OPEN = "<";
    String CLOSE = ">";
	
}
