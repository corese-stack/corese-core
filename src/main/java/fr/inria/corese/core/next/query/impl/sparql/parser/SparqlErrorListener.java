package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import org.antlr.v4.runtime.*;

import java.util.ArrayList;
import java.util.List;

/**
 * ANTLR error listener that collects parse errors as {@link QueryDiagnostic}
 * and optionally feeds them into {@link SparqlParserOptions}.
 */
public class SparqlErrorListener extends BaseErrorListener {

    private final SparqlParserOptions options;
    private final List<QueryDiagnostic> local = new ArrayList<>();

    public SparqlErrorListener(SparqlParserOptions options) {
        this.options = options;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line,
                            int charPositionInLine,
                            String msg,
                            RecognitionException e) {

        String offendingText = null;
        if (offendingSymbol instanceof Token t) {
            offendingText = t.getText();
        }

        QueryDiagnostic.Kind kind = (recognizer instanceof Lexer)
                ? QueryDiagnostic.Kind.LEXER_ERROR
                : QueryDiagnostic.Kind.SYNTAX_ERROR;

        QueryDiagnostic diag = new QueryDiagnostic(
                kind,
                QueryDiagnostic.Severity.ERROR,
                msg != null ? msg : "Unknown parse error",
                line,
                Math.max(0, charPositionInLine),
                offendingText,
                recognizer != null ? recognizer.getClass().getSimpleName() : null
        );

        local.add(diag);

        if (options != null) {
            options.addDiagnostic(diag);
        }

        if (options != null && options.isFailFast()) {
            throw new QuerySyntaxException(diag.format(), line, charPositionInLine, e);
        }
    }

    /**
     * Returns true if at least one error was recorded by this listener.
     */
    public boolean hasErrors() {
        return !local.isEmpty();
    }

    /**
     * Returns a formatted message containing all errors recorded by this listener.
     */
    public String getErrorMessage() {
        if (local.isEmpty()) return "Unknown parsing error";
        return local.stream().map(QueryDiagnostic::format).reduce((a, b) -> a + "; " + b).orElse("Unknown parsing error");
    }

    /**
     * Returns all structured diagnostics recorded by this listener.
     */
    public List<QueryDiagnostic> getDiagnostics() {
        return List.copyOf(local);
    }
}
