package fr.inria.corese.core.next.query.impl.engine.model;

/**
 * Types of expression of KGRAM query language
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 */
public interface ExpType {

    enum Type {
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
        QUERY("QUERY"),
        FORALL("FORALL"),
        EXIST("EXIST"),
        GRAPHNODE("GRAPHNODE"),
        OPTIONAL("OPTIONAL"),
        SCAN("SCAN"),
        IF("IF"),
        XPATH("XPATH"),
        ACCEPT("ACCEPT"),
        BIND("BIND"),
        EVAL("EVAL"),
        SCOPE("SCOPE"),
        TEST("TEST"),
        NEXT("NEXT"),
        MINUS("MINUS"),
        POP("POP"),
        SERVICE("SERVICE"),
        RESTORE("RESTORE"),
        JOIN("JOIN"),
        VALUES("VALUES"),
        OPT_BIND("OPT_BIND");
        private final String title;
        Type(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }
    }

}
