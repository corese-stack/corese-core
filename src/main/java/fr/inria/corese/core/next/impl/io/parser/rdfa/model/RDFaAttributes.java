package fr.inria.corese.core.next.impl.io.parser.rdfa.model;

public enum RDFaAttributes {
    ABOUT("about"),
    CONTENT("content"),
    DATATYPE("datatype"),
    HREF("href"),
    INLIST("inlist"),
    PREFIX("prefix"),
    PROPERTY("property"),
    REL("rel"),
    RESOURCE("resource"),
    REV("rev"),
    SRC("src"),
    TYPEOF("typeof"),
    VOCAB("vocab"),
    LANG("lang"),
    LANG_ALT("xml:lang");

    private final String name;

    RDFaAttributes(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
