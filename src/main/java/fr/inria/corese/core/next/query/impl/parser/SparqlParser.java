package fr.inria.corese.core.next.query.impl.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import fr.inria.corese.core.next.query.impl.parser.listener.*;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import fr.inria.corese.core.next.data.impl.io.common.IOConstants;
import fr.inria.corese.core.next.data.impl.io.parser.util.ParserConstants;
import fr.inria.corese.core.next.impl.parser.antlr.SparqlLexer;
import fr.inria.corese.core.next.query.api.base.io.AbstractQueryParser;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.api.io.parser.QueryOptions;
import fr.inria.corese.core.next.query.api.sparql.options.BaseIRIOptions;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;

public class SparqlParser extends AbstractQueryParser {

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
        SparqlParserOptions config = getEffectiveConfig();
        SparqlParserOptions sparqlParserOptions = new SparqlParserOptions.Builder()
                .baseIRI(baseIRI != null ? baseIRI : ParserConstants.getDefaultBaseURI())
                .failFast(config.isFailFast())
                .collectErrors(config.isCollectErrors())
                .build();

        try {
            CharStream charStream = CharStreams.fromReader(reader);
            SparqlLexer lexer = new SparqlLexer(charStream);

            SparqlErrorListener errorListener = new SparqlErrorListener(sparqlParserOptions);

            lexer.removeErrorListeners();
            lexer.addErrorListener(errorListener);

            CommonTokenStream tokens = new CommonTokenStream(lexer);
            fr.inria.corese.core.next.impl.parser.antlr.SparqlParser parser = new fr.inria.corese.core.next.impl.parser.antlr.SparqlParser(tokens);

            parser.removeErrorListeners();
            parser.addErrorListener(errorListener);

            if (sparqlParserOptions.isFailFast()) {
                parser.setErrorHandler(new BailErrorStrategy());
            } else {
                parser.setErrorHandler(new DefaultErrorStrategy());
            }

            ParseTreeWalker walker = new ParseTreeWalker();

            ParseTree tree;

            try {
                tree = parser.query();
                if (errorListener.hasErrors()) {
                    String errorMsg = errorListener.getErrorMessage();
                    if (errorMsg == null || errorMsg.trim().isEmpty()) {
                        errorMsg = "Unknown syntax error detected";
                    }
                    throw new QuerySyntaxException("Syntax error in SPARQL query: " + errorMsg);
                }
            } catch (RecognitionException e) {
                throw new QuerySyntaxException("Recognition error in SPARQL query: " + e.getMessage(), e);
            } catch (ParseCancellationException e) {
                throw toQuerySyntaxException(e, errorListener);
            }

            SparqlAstBuilder builder = new SparqlAstBuilder(sparqlParserOptions);

            SparqlListener listener = new SparqlListener(List.of(
                    new BgpFeature(builder),
                    new AskQueryFeature(builder),
                    new SelectQueryFeature(builder),
                    new ConstructQueryFeature(builder),
                    new SolutionModifierFeature(builder),
                    new FilterFeature(builder),
                    new UnionFeature(builder),
                    new DescribeQueryFeature(builder),
                    new DatasetClauseFeature(builder),
                    new PrologueFeature(builder)
            ));

            walker.walk(listener, tree);

            return builder.getResult();
        } catch (QuerySyntaxException | QueryValidationException | QueryEvaluationException e) {
            throw e;
        } catch (IOException e) {
            throw new QueryEvaluationException("Failed to parse SPARQL query: " + e.getMessage(), e);
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
