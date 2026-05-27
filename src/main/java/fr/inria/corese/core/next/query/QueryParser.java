package fr.inria.corese.core.next.query;

import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;

import java.io.InputStream;
import java.io.Reader;

/**
 * A parser for SPARQL operations.
 *
 * <p>This interface defines methods for parsing SPARQL operations from
 * various input sources (InputStream, Reader, or String) and producing
 * a {@link QueryAst} representation.</p>
 *
 * <p>Implementations may support configurable parsing behavior via
 * {@link QueryOptions}, such as base IRI handling, strict mode,
 * or error handling strategy.</p>
 */
public interface QueryParser {

    /**
     * Sets the configuration options used by this parser.
     *
     * @param options the parsing configuration
     */
    void setConfig(QueryOptions options);

    /**
     * Returns the current configuration options used by this parser.
     *
     * @return the parsing configuration
     */
    QueryOptions getConfig();

    /** Parse a SPARQL query from an InputStream (UTF-8) */
    QueryAst parse(InputStream in);

    /** Parse a SPARQL query from an InputStream with a base IRI */
    QueryAst parse(InputStream in, String baseIRI);

    /** Parse a SPARQL query from a Reader */
    QueryAst parse(Reader reader);

    /** Parse a SPARQL query from a Reader with a base IRI */
    QueryAst parse(Reader reader, String baseIRI);

    /** Parse a SPARQL string query */
    QueryAst parse(String queryString);

    /** Parse a SPARQL string with a base IRI */
    QueryAst parse(String queryString, String baseIRI);
}
