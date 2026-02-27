package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.data.impl.io.common.IOConstants;
import fr.inria.corese.core.next.impl.parser.antlr.SparqlLexer;
import fr.inria.corese.core.next.query.api.exception.QueryException;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.data.impl.io.parser.util.ParserConstants;
import fr.inria.corese.core.next.query.api.base.io.AbstractQueryParser;
import fr.inria.corese.core.next.query.api.io.parser.QueryOptions;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.parser.listener.BgpFeature;
import fr.inria.corese.core.next.query.impl.sparql.options.BaseIRIOptions;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
                    new BgpFeature(builder)
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

    /**
     * A custom error listener to collect errors from the lexer and parser.
     */
    private static final class SparqlErrorListener extends BaseErrorListener {
        private final List<String> errors = new ArrayList<>();

        /**
         * Records syntax errors generated by ANTLR.
         *
         * @param recognizer         The recognizer that detected the error.
         * @param offendingSymbol    The symbol that caused the error.
         * @param line               The line number where the error occurred.
         * @param charPositionInLine The character position on the line.
         * @param msg                The error message.
         * @param e                  The recognition exception.
         */
        private final SparqlParserOptions options;

        private SparqlErrorListener(SparqlParserOptions options) {
            this.options = options;
        }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer,
                                Object offendingSymbol,
                                int line, int charPositionInLine,
                                String msg, RecognitionException e) {

            String error = "line " + line + ":" + charPositionInLine + " " + msg;
            errors.add(error);
            options.addError(error);

            if (options.isFailFast()) {
                throw new QuerySyntaxException(error, line, charPositionInLine, e);
            }
        }

        /**
         * Checks if parsing errors have been found.
         *
         * @return `true` if the error list is not empty, otherwise `false`.
         */
        boolean hasErrors() { return !errors.isEmpty(); }

        /**
         * Returns a formatted error message containing all found errors.
         *
         * @return A {@link String} containing the error messages.
         */
        public String getErrorMessage() {
            if (errors.isEmpty()) {
                return "Unknown parsing error";
            }
            return String.join("; ", errors);
        }
    }
}
