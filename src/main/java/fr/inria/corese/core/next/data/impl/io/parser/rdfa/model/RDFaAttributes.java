package fr.inria.corese.core.next.data.impl.io.parser.rdfa.model;

/**
 * Enumeration of standard RDFa 1.1 attributes used during parsing.
 *
 * @see <a href="https://www.w3.org/TR/rdfa-core/">RDFa Core 1.1 Specification</a>
 */
public enum RDFaAttributes {
    ABOUT("about"),
    BASE("base"),
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
