package fr.inria.corese.core.next.query.impl.parser;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.antlr.v4.runtime.misc.ParseCancellationException;

import fr.inria.corese.core.next.data.impl.io.common.IOConstants;
import fr.inria.corese.core.next.query.api.base.io.AbstractQueryParser;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.api.io.parser.QueryOptions;
import fr.inria.corese.core.next.query.api.sparql.options.BaseIRIOptions;
import fr.inria.corese.core.next.query.api.validation.QueryTextValidator;
import fr.inria.corese.core.next.query.api.validation.QueryValidationResult;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;

/**
 * SPARQL parser exposing both AST parsing and non-throwing validation entry points.
 *
 * <p>{@code parse(...)} keeps the traditional parser contract: syntax and
 * semantic query errors are reported with exceptions and a valid query returns
 * a {@link QueryAst}. By contrast, {@code validate(...)} is intended for
 * linter-style usage and reports query problems through
 * {@link QueryValidationResult} diagnostics. Both families may still throw on
 * technical failures unrelated to the query validity itself.</p>
 */
public class SparqlParser extends AbstractQueryParser implements QueryTextValidator {

    private final SparqlQueryAnalyzer analyzer = new SparqlQueryAnalyzer();

    public SparqlParser() {
        this(new SparqlParserOptions.Builder().build());
    }

    public SparqlParser(QueryOptions config) {
        super(config);
    }
    @Override
    public QueryAst parse(InputStream in) {
        String baseIri = IOConstants.getDefaultBaseURI();
        if (getConfig() instanceof BaseIRIOptions baseIRIOptions) {
            baseIri = baseIRIOptions.getBaseIRI();
        }
        return parse(new java.io.InputStreamReader(in, StandardCharsets.UTF_8), baseIri);
    }

    @Override
    public QueryAst parse(InputStream in, String baseIRI) {
        return parse(new java.io.InputStreamReader(in, StandardCharsets.UTF_8), baseIRI);
    }

    @Override
    public QueryAst parse(Reader reader) {
        return parse(reader, getBaseIRIFromConfig());
    }

    @Override
    public QueryAst parse(Reader reader, String baseIRI) {
        try {
            return analyzer.parse(reader, baseIRI, getEffectiveConfig());
        } catch (QuerySyntaxException | QueryValidationException | QueryEvaluationException e) {
            throw e;
        } catch (Exception e) {
            throw new QuerySyntaxException("Unexpected error during SPARQL parsing: " + e.getMessage(), e);
        }
    }

    @Override
    public QueryAst parse(String queryString) {
        return parse(new StringReader(queryString), getBaseIRIFromConfig());
    }

    @Override
    public QueryAst parse(String queryString, String baseIRI) {
        return parse(new StringReader(queryString), baseIRI);
    }

    @Override
    public QueryValidationResult validate(InputStream in) {
        String baseIri = IOConstants.getDefaultBaseURI();
        if (getConfig() instanceof BaseIRIOptions baseIRIOptions) {
            baseIri = baseIRIOptions.getBaseIRI();
        }
        return validate(new java.io.InputStreamReader(in, StandardCharsets.UTF_8), baseIri);
    }

    @Override
    public QueryValidationResult validate(InputStream in, String baseIRI) {
        return validate(new java.io.InputStreamReader(in, StandardCharsets.UTF_8), baseIRI);
    }

    @Override
    public QueryValidationResult validate(Reader reader) {
        return validate(reader, getBaseIRIFromConfig());
    }

    @Override
    public QueryValidationResult validate(Reader reader, String baseIRI) {
        return analyzer.validate(reader, baseIRI, getEffectiveConfig());
    }

    @Override
    public QueryValidationResult validate(String queryString) {
        return validate(new StringReader(queryString), getBaseIRIFromConfig());
    }

    @Override
    public QueryValidationResult validate(String queryString, String baseIRI) {
        return validate(new StringReader(queryString), baseIRI);
    }

    private String getBaseIRIFromConfig() {
        SparqlParserOptions opts = getEffectiveConfig();
        return opts.getBaseIRI();
    }

    /**
     * Normalizes ANTLR fail-fast parse cancellations into a Corese syntax exception.
     * Reuses an existing QuerySyntaxException when available, otherwise prefers
     * diagnostics collected by the error listener before falling back to the
     * cancellation or cause message.
     * 
     * @param e the original ParseCancellationException thrown by ANTLR
     * @param errorListener the error listener that may have collected syntax errors
     * @return a QuerySyntaxException with an informative message and the original exception as cause
     */
    static QuerySyntaxException toQuerySyntaxException(ParseCancellationException e, SparqlErrorListener errorListener) {
        if (e.getCause() instanceof QuerySyntaxException querySyntaxException) {
            return querySyntaxException;
        }

        String errorMsg = null;
        if (errorListener != null && errorListener.hasErrors()) {
            errorMsg = errorListener.getErrorMessage();
        } else if (e.getCause() != null && e.getCause().getMessage() != null && !e.getCause().getMessage().trim().isEmpty()) {
            errorMsg = e.getCause().getMessage();
        } else if (e.getMessage() != null && !e.getMessage().trim().isEmpty()) {
            errorMsg = e.getMessage();
        }

        if (errorMsg == null || errorMsg.trim().isEmpty()) {
            errorMsg = "Parsing cancelled due to a syntax error";
        }

        return new QuerySyntaxException(errorMsg, e);
    }

    /** Returns config as SparqlParserOptions, or default options if null or wrong type. */
    private SparqlParserOptions getEffectiveConfig() {
        QueryOptions opts = getConfig();
        if (opts instanceof SparqlParserOptions spo) {
            return spo;
        }
        return new SparqlParserOptions.Builder().build();
    }
}
