package fr.inria.corese.core.next.query.impl.parser;

public class AbstractSparqlParserFeatureTest {

    /**
     * create a Default Sparql Parser with default Config
     * @return SparqlParser
     */
    protected SparqlParser newParserDefault() {
        return new SparqlParser(new SparqlParserOptions.Builder().build());
    }

    /**
     *  create a Sparql Parser with Option
     * @param failFast option to make the Parsing fail if it encountered an error
     * @param collectErrors option to collect error encountered when parsing
     * @return SparqlParser
     */
    protected SparqlParser newParser(boolean failFast, boolean collectErrors) {
        return new SparqlParser(new SparqlParserOptions.Builder()
                .failFast(failFast)
                .collectErrors(collectErrors)
                .build());
    }
}
