package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.impl.parser.options.SparqlAstError;
import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SparqlQueryAnalyzerTest {

    @Test
    void toQueryDiagnosticPreservesInfoSeverity() {
        QueryDiagnostic diagnostic = SparqlQueryAnalyzer.toQueryDiagnostic(new SparqlAstError(
                SparqlAstError.Kind.SYNTAX_ERROR,
                SparqlAstError.Severity.INFO,
                "Informational syntax diagnostic",
                1,
                2,
                "token",
                "source"));

        assertEquals(QueryDiagnostic.Kind.SYNTAX_ERROR, diagnostic.kind());
        assertEquals(QueryDiagnostic.Severity.INFO, diagnostic.severity());
    }

    @Test
    void toQueryDiagnosticPreservesWarningSeverity() {
        QueryDiagnostic diagnostic = SparqlQueryAnalyzer.toQueryDiagnostic(new SparqlAstError(
                SparqlAstError.Kind.LEXER_ERROR,
                SparqlAstError.Severity.WARNING,
                "Warning lexer diagnostic",
                1,
                2,
                "token",
                "source"));

        assertEquals(QueryDiagnostic.Kind.LEXER_ERROR, diagnostic.kind());
        assertEquals(QueryDiagnostic.Severity.WARNING, diagnostic.severity());
    }

    @Test
    void toQueryDiagnosticMapsStrictModeErrorsToSemanticKind() {
        QueryDiagnostic diagnostic = SparqlQueryAnalyzer.toQueryDiagnostic(new SparqlAstError(
                SparqlAstError.Kind.STRICT_MODE_ERROR,
                SparqlAstError.Severity.ERROR,
                "Strict mode diagnostic",
                1,
                2,
                "token",
                "source"));

        assertEquals(QueryDiagnostic.Kind.SEMANTIC_ERROR, diagnostic.kind());
        assertEquals(QueryDiagnostic.Severity.ERROR, diagnostic.severity());
    }
}
