package fr.inria.corese.core.kgram.api.core;

/**
 * Types of expression of KGRAM query language
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 */
public interface ExpType {

    String KGRAM = "http://ns.inria.fr/corese.core.kgram/";
    String DEFAULT_GRAPH = KGRAM + "default";
    String SKOLEM_MARKER = "/.well-known/genid/";
    String SKOLEM = "http://ns.inria.fr" + SKOLEM_MARKER;
    String SPARQL = "http://ns.inria.fr/sparql-function/";
    String STL = "http://ns.inria.fr/sparql-template/";
    String EXT = "http://ns.inria.fr/sparql-extension/";
    String DOM = "http://ns.inria.fr/sparql-extension/dom/";
    String CUSTOM = "http://ns.inria.fr/sparql-custom/";
    String BNODE = EXT + "bnode";
    String UXT = "http://ns.inria.fr/sparql-extension/user/";
    String DT = "http://ns.inria.fr/sparql-datatype/";
    String SWL = "http://ns.inria.fr/sparql-workflow/";
    String TYPE_METADATA = "@type";


    String KPREF = "kg";

    public enum Title {
    	EMPTY("EMPTY"),
		AND("AND"),
		UNION("UNION"),
		GRAPH("GRAPH"),
		OPTION("OPTION"),
		EDGE("EDGE"),
		FILTER("FILTER"),
		NODE("NODE"),
		BGP("BGP"),
		WATCH("WATCH"),
		CONTINUE("CONTINUE"),
		BACKJUMP("BACKJUMP"),
		EXTERN("EXTERN"),
		QUERY("QUERY"),
		FORALL("FORALL"),
		EXIST("EXIST"),
		GRAPHNODE("GRAPHNODE"),
		OPTIONAL("OPTIONAL"),
		SCAN("SCAN"),
		IF("IF"),
		PATH("PATH"),
		XPATH("XPATH"),
		ACCEPT("ACCEPT"),
		BIND("BIND"),
		EVAL("EVAL"),
		SCOPE("SCOPE"),
		TEST("TEST"),
		NEXT("NEXT"),
		MINUS("MINUS"),
		POP("POP"),
		SERVICE("POP"),
		RESTORE("RESTORE"),
		JOIN("JOIN"),
		VALUES("VALUES"),
		OPT_BIND("OPT_BIND");
		private final String title;
		Title(String title) {
			this.title = title;
		}

		public String getTitle() {
			return title;
		}
	}

}
