package fr.inria.corese.core.kgram.api.core;

/**
 * Types of expression of KGRAM query language
 * 
 * @author Olivier Corby, Edelweiss, INRIA 2010
 *
 */
public interface ExpType {
	
	//public static final String KGRAM  = "http://ns.inria.fr/edelweiss/2010/kgram/";
    String KGRAM  = "http://ns.inria.fr/corese.core.kgram/";
	String DEFAULT_GRAPH = KGRAM + "default";
        String SKOLEM_MARKER = "/.well-known/genid/";
        String SKOLEM = "http://ns.inria.fr" + SKOLEM_MARKER ;
	String SPARQL = "http://ns.inria.fr/sparql-function/";
	String STL    = "http://ns.inria.fr/sparql-template/";
	String EXT    = "http://ns.inria.fr/sparql-extension/";
	String DOM    = "http://ns.inria.fr/sparql-extension/dom/";
	String CUSTOM = "http://ns.inria.fr/sparql-custom/";
	String BNODE  = EXT + "bnode";
	String UXT    = "http://ns.inria.fr/sparql-extension/user/";
	String DT     = "http://ns.inria.fr/sparql-datatype/";
	String SWL    = "http://ns.inria.fr/sparql-workflow/";
        String TYPE_METADATA = "@type";


	String KPREF = "kg";

	int EMPTY 	= 0;
	int AND 	= 1;
	int UNION 	= 2;
	int GRAPH 	= 3;
	int OPTION = 4;
	int EDGE 	= 5;
	int FILTER = 6;
	int NODE 	= 7;
	
	int BGP 		= 8;
	int WATCH 		= 9;
	int CONTINUE 	= 10;
	int BACKJUMP 	= 11;
	int EXTERN 	= 12;
	int QUERY 		= 13;
	int FORALL 	= 14;
	int EXIST  	= 15;
	int GRAPHNODE 	= 16;
	int OPTIONAL 	= 17;
	int SCAN 		= 18;
	int IF 		= 19;
	int PATH 		= 20;
	int XPATH 		= 21;
	int ACCEPT 	= 22;
	int BIND           = 23;
	int EVAL 		= 24;
	int SCOPE 		= 25;
	int TEST 		= 26;
	int NEXT 		= 27;
	int MINUS 		= 28;
	int POP 		= 29;
	int SERVICE 	= 30;
	int RESTORE 	= 31;
	int JOIN 		= 32;
	int VALUES 	= 33;
	int OPT_BIND 	= 34;
        


	
	String[] TITLE = {
		"EMPTY", "AND", "UNION", "GRAPH", "OPTION", "EDGE", "FILTER", "NODE", 
		"BGP", "WATCH", "CONTINUE", "BACKJUMP", "EXTERN", "QUERY", "FORALL", "EXIST",
		"GRAPHNODE", "OPTIONAL", "SCAN", "IF", "PATH", "XPATH", "ACCEPT", "BIND", 
		"EVAL", "SCOPE", "TEST", "NEXT", "MINUS", "POP", "SERVICE", "RESTORE", "JOIN", "VALUES", 
                "OPT_BIND"
	};
	
}
