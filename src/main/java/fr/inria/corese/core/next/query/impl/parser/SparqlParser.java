package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.data.impl.io.common.IOConstants;
import fr.inria.corese.core.next.data.impl.io.parser.util.ParserConstants;
import fr.inria.corese.core.next.impl.parser.antlr.SparqlLexer;
import fr.inria.corese.core.next.query.api.base.io.AbstractQueryParser;
import fr.inria.corese.core.next.query.api.exception.QueryException;
import fr.inria.corese.core.next.query.api.io.parser.QueryOptions;
import fr.inria.corese.core.next.query.api.sparql.options.BaseIRIOptions;
import fr.inria.corese.core.next.query.impl.parser.listener.AskQueryFeature;
import fr.inria.corese.core.next.query.impl.parser.listener.BgpFeature;
import fr.inria.corese.core.next.query.impl.parser.listener.DescribeQueryFeature;
import fr.inria.corese.core.next.query.impl.parser.listener.SelectQueryFeature;
import fr.inria.corese.core.next.query.impl.parser.listener.ConstructQueryFeature;
import fr.inria.corese.core.next.query.impl.parser.listener.UnionFeature;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
                tree= parser.query();
                if (errorListener.hasErrors()) {
                    String errorMsg = errorListener.getErrorMessage();
                    if (errorMsg == null || errorMsg.trim().isEmpty()) {
                        errorMsg = "Unknown syntax error detected";
                    }
                    throw new QueryException("Syntax error in Sparql query: " + errorMsg);
                }
            } catch (RecognitionException e) {
                throw new QueryException("Recognition error in Sparql query: " + e.getMessage(), e);
            }


            SparqlAstBuilder builder = new SparqlAstBuilder(sparqlParserOptions);

            SparqlListener listener = new SparqlListener(List.of(
                    new BgpFeature(builder),
                    new AskQueryFeature(builder),
                    new SelectQueryFeature(builder),
                    new SolutionModifierFeature(builder),
                    new UnionFeature(builder),
                    new SelectQueryFeature(builder),
                    new DescribeQueryFeature(builder),
                    new ConstructQueryFeature(builder)
            ));

            walker.walk(listener, tree);

            return builder.getResult();

        } catch (QueryException e) {
            throw e;
        }
        catch (IOException e) {
            throw new QueryException("Failed to parse SPARQL query: " + e.getMessage(), e);

        }
        catch (Exception e) {
            throw new QueryException("Unexpected error during SPARQL parsing: " + e.getMessage(), e);
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

    /** Returns config as SparqlParserOptions, or default options if null or wrong type. */
    private SparqlParserOptions getEffectiveConfig() {
        QueryOptions opts = getConfig();
        if (opts instanceof SparqlParserOptions spo) {
            return spo;
        }
        return new SparqlParserOptions.Builder().build();
    }
}
