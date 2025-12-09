package fr.inria.corese.core.next.api.query;

/**
 * Query languages supported by the engine.
 */
public enum QueryLanguage {
    SPARQL("SPARQL"),
    LDSCRIPT("LDScript");

    private final String name;

    QueryLanguage(String name) { this.name = name; }

    public String getName() { return name; }

    @Override
    public String toString() { return name; }
}