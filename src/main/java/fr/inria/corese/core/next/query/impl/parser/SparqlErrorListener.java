package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.api.sparql.options.SparqlAstError;
import org.antlr.v4.runtime.*;

import java.util.ArrayList;
import java.util.List;

/**
 * ANTLR error listener that collects parse errors as {@link SparqlAstError}
 * and optionally feeds them into {@link SparqlParserOptions}.
 */
public class SparqlErrorListener extends BaseErrorListener {

    private final SparqlParserOptions options;
    private final List<SparqlAstError> local = new ArrayList<>();

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

        SparqlAstError.Kind kind = (recognizer instanceof Lexer)
                ? SparqlAstError.Kind.LEXER_ERROR
                : SparqlAstError.Kind.SYNTAX_ERROR;

        SparqlAstError diag = new SparqlAstError(
                kind,
                SparqlAstError.Severity.ERROR,
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
        return local.stream().map(SparqlAstError::format).reduce((a, b) -> a + "; " + b).orElse("Unknown parsing error");
    }

    /**
     * Returns all structured diagnostics recorded by this listener.
     */
    public List<SparqlAstError> getDiagnostics() {
        return List.copyOf(local);
    }
}
