package fr.inria.corese.core.next.query.impl.engine.model;

/**
 * Expression types for SPARQL query algebra in KGRAM engine.
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
        QUERY("QUERY"),
        EXIST("EXIST"),
        GRAPHNODE("GRAPHNODE"),
        OPTIONAL("OPTIONAL"),
        ACCEPT("ACCEPT"),
        BIND("BIND"),
        TEST("TEST"),
        MINUS("MINUS"),
        SERVICE("SERVICE"),
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
