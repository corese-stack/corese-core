package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlLexer;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.impl.parser.options.SparqlAstError;
import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.api.validation.QueryValidationResult;
import fr.inria.corese.core.next.query.impl.parser.listener.*;
import fr.inria.corese.core.next.query.impl.parser.semantic.validator.SparqlQuerySemanticValidator;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Performs SPARQL syntax, AST, and semantic analysis and returns the
 * collected result to the caller.
 */
final class SparqlQueryAnalyzer {

    private final SparqlQuerySemanticValidator semanticValidator = new SparqlQuerySemanticValidator();

    AnalysisResult analyze(Reader reader, SparqlParserOptions options) throws IOException {
        SyntaxAnalysis syntaxAnalysis = analyzeSyntax(reader, options);

        if (!syntaxAnalysis.validationResult().isValid()) {
            return new AnalysisResult(null, syntaxAnalysis.validationResult());
        }

        try {
            QueryAst ast = buildAst(syntaxAnalysis.parseTree(), options, syntaxAnalysis.blankNodeLabels());
            QueryValidationResult semanticValidation = semanticValidator.validate(ast);
            return new AnalysisResult(ast, semanticValidation);
        } catch (QuerySyntaxException e) {
            return new AnalysisResult(null, new QueryValidationResult(List.of(toSyntaxDiagnostic(e))));
        } catch (QueryValidationException e) {
            return new AnalysisResult(
                    null,
                    new QueryValidationResult(List.of(
                            new QueryDiagnostic(
                                    QueryDiagnostic.Kind.SEMANTIC_ERROR,
                                    QueryDiagnostic.Severity.ERROR,
                                    e.getMessage(),
                                    -1,
                                    -1,
                                    null,
                                    "SparqlAstBuilder"))));
        }
    }

    private SyntaxAnalysis analyzeSyntax(Reader reader, SparqlParserOptions options) throws IOException {
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
            Set<String> blankNodeLabels = blankNodeLabels(tokens);
            if (errorListener.hasErrors()) {
                return new SyntaxAnalysis(
                        null,
                        new QueryValidationResult(mapSyntaxDiagnostics(errorListener.getDiagnostics())),
                        blankNodeLabels);
            }

            return new SyntaxAnalysis(tree, new QueryValidationResult(List.of()), blankNodeLabels);
        } catch (RecognitionException e) {
            return new SyntaxAnalysis(null, new QueryValidationResult(List.of(
                    new QueryDiagnostic(
                            QueryDiagnostic.Kind.SYNTAX_ERROR,
                            QueryDiagnostic.Severity.ERROR,
                            "Recognition error in SPARQL query: " + e.getMessage(),
                            -1,
                            -1,
                            null,
                            e.getClass().getSimpleName()))), Set.of());
        } catch (ParseCancellationException e) {
            QuerySyntaxException syntaxException = SparqlParser.toQuerySyntaxException(e, errorListener);
            return new SyntaxAnalysis(
                    null,
                    new QueryValidationResult(List.of(toSyntaxDiagnostic(syntaxException))),
                    Set.of());
        } catch (QuerySyntaxException e) {
            return new SyntaxAnalysis(
                    null,
                    new QueryValidationResult(List.of(toSyntaxDiagnostic(e))),
                    Set.of());
        }
    }

    private QueryAst buildAst(
            ParseTree tree,
            SparqlParserOptions options,
            Collection<String> blankNodeLabels) {
        fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.QueryContext root =
                (fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.QueryContext) tree;
        ParseTreeWalker walker = new ParseTreeWalker();

        if (root.queryUnit() != null) {
            SparqlQueryAstBuilder queryBuilder = new SparqlQueryAstBuilder(options);
            queryBuilder.reserveBlankNodeLabels(blankNodeLabels);
            SparqlListenerDispatcher queryListener = new SparqlListenerDispatcher(List.of(
                    new AskQueryAstListener(queryBuilder),
                    new ConstructQueryAstListener(queryBuilder),
                    new DescribeQueryAstListener(queryBuilder),
                    new SelectQueryAstListener(queryBuilder),
                    new DatasetClauseAstListener(queryBuilder),
                    new HavingAstListener(queryBuilder),
                    new SolutionModifierAstListener(queryBuilder),
                    new ValuesAstListener(queryBuilder),
                    new BgpAstListener(queryBuilder),
                    new FilterAstListener(queryBuilder),
                    new UnionAstListener(queryBuilder),
                    new MinusAstListener(queryBuilder),
                    new PrologueAstListener(queryBuilder),
                    new BindAstListener(queryBuilder),
                    new ServiceAstListener(queryBuilder)
            ));
            walker.walk(queryListener, root.queryUnit());
            return queryBuilder.getResult();
        }

        if (root.updateUnit() != null) {
            SparqlUpdateAstBuilder updateBuilder = new SparqlUpdateAstBuilder(options);
            updateBuilder.reserveBlankNodeLabels(blankNodeLabels);
            SparqlListenerDispatcher updateListener = new SparqlListenerDispatcher(List.of(
                    new ClearUpdateAstListener(updateBuilder),
                    new CreateUpdateAstListener(updateBuilder),
                    new DropUpdateAstListener(updateBuilder),
                    new LoadUpdateAstListener(updateBuilder),
                    new BgpAstListener(updateBuilder),
                    new BindAstListener(updateBuilder),
                    new FilterAstListener(updateBuilder),
                    new MinusAstListener(updateBuilder),
                    new PrologueAstListener(updateBuilder),
                    new ServiceAstListener(updateBuilder),
                    new UnionAstListener(updateBuilder)
            ));
            walker.walk(updateListener, root.updateUnit());
            return updateBuilder.getResult();
        }

        throw new QueryEvaluationException(
                "Unknown parse root, could not determine if this is an update request or a query.");
    }

    private Set<String> blankNodeLabels(CommonTokenStream tokens) {
        tokens.fill();
        Set<String> labels = new LinkedHashSet<>();
        for (Token token : tokens.getTokens()) {
            if (token.getType() == SparqlLexer.BLANK_NODE_LABEL) {
                labels.add(token.getText());
            }
        }
        return labels;
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

    private record SyntaxAnalysis(
            ParseTree parseTree,
            QueryValidationResult validationResult,
            Set<String> blankNodeLabels) {
    }

    record AnalysisResult(QueryAst ast, QueryValidationResult validationResult) {
    }
}
