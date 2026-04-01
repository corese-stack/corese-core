package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.data.impl.io.common.IOConstants;
import fr.inria.corese.core.next.impl.parser.antlr.SparqlLexer;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.api.sparql.options.SparqlAstError;
import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.api.validation.QueryValidationResult;
import fr.inria.corese.core.next.query.impl.parser.listener.AskQueryFeature;
import fr.inria.corese.core.next.query.impl.parser.listener.BgpFeature;
import fr.inria.corese.core.next.query.impl.parser.listener.ConstructQueryFeature;
import fr.inria.corese.core.next.query.impl.parser.listener.DatasetClauseFeature;
import fr.inria.corese.core.next.query.impl.parser.listener.DescribeQueryFeature;
import fr.inria.corese.core.next.query.impl.parser.listener.FilterFeature;
import fr.inria.corese.core.next.query.impl.parser.listener.PrologueFeature;
import fr.inria.corese.core.next.query.impl.parser.listener.SelectQueryFeature;
import fr.inria.corese.core.next.query.impl.parser.listener.SolutionModifierFeature;
import fr.inria.corese.core.next.query.impl.parser.listener.UnionFeature;
import fr.inria.corese.core.next.query.impl.parser.semantic.validator.SparqlQuerySemanticValidator;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

/** Shared SPARQL analysis pipeline used by both parsing and validation entry points. */
final class SparqlQueryAnalyzer {

    private final SparqlQuerySemanticValidator semanticValidator = new SparqlQuerySemanticValidator();

    /**
     * Parses a SPARQL query and keeps the historical parser contract:
     * syntax problems raise {@link QuerySyntaxException}, semantic problems
     * raise {@link QueryValidationException}.
     */
    QueryAst parse(Reader reader, String baseIRI, SparqlParserOptions config) {
        AnalysisResult analysisResult = analyze(reader, baseIRI, config, false);
        if (!analysisResult.validationResult().isValid()) {
            QueryDiagnostic firstError = firstErrorDiagnostic(analysisResult.validationResult());

            if (firstError.kind() == QueryDiagnostic.Kind.SEMANTIC_ERROR) {
                throw new QueryValidationException(firstError.message());
            }
            throw buildSyntaxException(analysisResult.validationResult());
        }

        if (analysisResult.ast() == null) {
            throw new QueryEvaluationException("SPARQL analysis completed without producing an AST");
        }

        return analysisResult.ast();
    }

    /**
     * Validates a SPARQL query and returns structured diagnostics instead of
     * throwing on syntax or semantic issues.
     */
    QueryValidationResult validate(Reader reader, String baseIRI, SparqlParserOptions config) {
        return analyze(reader, baseIRI, config, true).validationResult();
    }

    private AnalysisResult analyze(
            Reader reader,
            String baseIRI,
            SparqlParserOptions config,
            boolean validationMode
    ) {
        SparqlParserOptions effectiveOptions = buildEffectiveOptions(baseIRI, config, validationMode);
        SyntaxAnalysis syntaxAnalysis = analyzeSyntax(reader, effectiveOptions);

        if (!syntaxAnalysis.validationResult().isValid()) {
            return new AnalysisResult(null, syntaxAnalysis.validationResult());
        }

        try {
            QueryAst ast = buildAst(syntaxAnalysis.parseTree(), effectiveOptions);
            QueryValidationResult semanticValidation = semanticValidator.validate(ast);
            return new AnalysisResult(ast, semanticValidation);
        } catch (QuerySyntaxException e) {
            return new AnalysisResult(null, new QueryValidationResult(List.of(toSyntaxDiagnostic(e))));
        }
    }

    private SparqlParserOptions buildEffectiveOptions(
            String baseIRI,
            SparqlParserOptions config,
            boolean validationMode
    ) {
        String effectiveBaseIRI = baseIRI != null ? baseIRI : IOConstants.getDefaultBaseURI();
        return new SparqlParserOptions.Builder()
                .baseIRI(effectiveBaseIRI)
                .strictMode(config.isStrictMode())
                .failFast(!validationMode && config.isFailFast())
                .collectErrors(validationMode || config.isCollectErrors())
                .build();
    }

    private SyntaxAnalysis analyzeSyntax(Reader reader, SparqlParserOptions options) {
        SparqlErrorListener errorListener = null;
        try {
            CharStream charStream = CharStreams.fromReader(reader);
            SparqlLexer lexer = new SparqlLexer(charStream);

            errorListener = new SparqlErrorListener(options);

            lexer.removeErrorListeners();
            lexer.addErrorListener(errorListener);

            CommonTokenStream tokens = new CommonTokenStream(lexer);
            fr.inria.corese.core.next.impl.parser.antlr.SparqlParser parser =
                    new fr.inria.corese.core.next.impl.parser.antlr.SparqlParser(tokens);

            parser.removeErrorListeners();
            parser.addErrorListener(errorListener);

            if (options.isFailFast()) {
                parser.setErrorHandler(new BailErrorStrategy());
            } else {
                parser.setErrorHandler(new DefaultErrorStrategy());
            }

            ParseTree tree = parser.query();
            if (errorListener.hasErrors()) {
                return new SyntaxAnalysis(
                        null,
                        new QueryValidationResult(mapSyntaxDiagnostics(errorListener.getDiagnostics())));
            }

            return new SyntaxAnalysis(tree, new QueryValidationResult(List.of()));
        } catch (RecognitionException e) {
            return new SyntaxAnalysis(null, new QueryValidationResult(List.of(
                    new QueryDiagnostic(
                            QueryDiagnostic.Kind.SYNTAX_ERROR,
                            QueryDiagnostic.Severity.ERROR,
                            "Recognition error in SPARQL query: " + e.getMessage(),
                            -1,
                            -1,
                            null,
                            e.getClass().getSimpleName()))));
        } catch (ParseCancellationException e) {
            QuerySyntaxException syntaxException = SparqlParser.toQuerySyntaxException(e, errorListener);
            return new SyntaxAnalysis(null, new QueryValidationResult(List.of(toSyntaxDiagnostic(syntaxException))));
        } catch (QuerySyntaxException e) {
            return new SyntaxAnalysis(null, new QueryValidationResult(List.of(toSyntaxDiagnostic(e))));
        } catch (IOException e) {
            throw new QueryEvaluationException("Failed to validate SPARQL query: " + e.getMessage(), e);
        }
    }

    private QueryAst buildAst(ParseTree tree, SparqlParserOptions options) {
        SparqlAstBuilder builder = new SparqlAstBuilder(options);

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

        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(listener, tree);
        return builder.getResult();
    }

    private List<QueryDiagnostic> mapSyntaxDiagnostics(List<SparqlAstError> diagnostics) {
        return diagnostics.stream()
                .map(SparqlQueryAnalyzer::toQueryDiagnostic)
                .toList();
    }

    static QueryDiagnostic toQueryDiagnostic(SparqlAstError diagnostic) {
        QueryDiagnostic.Kind kind = switch (diagnostic.kind()) {
            case LEXER_ERROR -> QueryDiagnostic.Kind.LEXER_ERROR;
            case SYNTAX_ERROR -> QueryDiagnostic.Kind.SYNTAX_ERROR;
            case STRICT_MODE_ERROR -> QueryDiagnostic.Kind.SEMANTIC_ERROR;
        };

        QueryDiagnostic.Severity severity = switch (diagnostic.severity()) {
            case INFO -> QueryDiagnostic.Severity.INFO;
            case WARNING -> QueryDiagnostic.Severity.WARNING;
            case ERROR -> QueryDiagnostic.Severity.ERROR;
        };

        return new QueryDiagnostic(
                kind,
                severity,
                diagnostic.message(),
                diagnostic.line(),
                diagnostic.column(),
                diagnostic.offendingText(),
                diagnostic.source());
    }

    private QueryDiagnostic toSyntaxDiagnostic(QuerySyntaxException exception) {
        return new QueryDiagnostic(
                QueryDiagnostic.Kind.SYNTAX_ERROR,
                QueryDiagnostic.Severity.ERROR,
                exception.getMessage(),
                exception.getLine(),
                exception.getColumn(),
                null,
                exception.getClass().getSimpleName());
    }

    private QueryDiagnostic firstErrorDiagnostic(QueryValidationResult validationResult) {
        return validationResult.diagnostics().stream()
                .filter(diagnostic -> diagnostic.severity() == QueryDiagnostic.Severity.ERROR)
                .findFirst()
                .orElse(validationResult.diagnostics().getFirst());
    }

    private QuerySyntaxException buildSyntaxException(QueryValidationResult validationResult) {
        List<QueryDiagnostic> diagnostics = validationResult.diagnostics();
        QueryDiagnostic firstDiagnostic = diagnostics.getFirst();
        String formattedDiagnostics = diagnostics.stream()
                .map(QueryDiagnostic::format)
                .reduce((left, right) -> left + "; " + right)
                .orElse("Unknown syntax error detected");

        String message = "Syntax error in SPARQL query: " + formattedDiagnostics;
        if (firstDiagnostic.line() >= 1 && firstDiagnostic.column() >= 0) {
            return new QuerySyntaxException(message, firstDiagnostic.line(), firstDiagnostic.column());
        }
        return new QuerySyntaxException(message);
    }

    private record SyntaxAnalysis(ParseTree parseTree, QueryValidationResult validationResult) {
    }

    private record AnalysisResult(QueryAst ast, QueryValidationResult validationResult) {
    }
}
