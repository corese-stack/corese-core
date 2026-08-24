package fr.inria.corese.core.next.query.api.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("QueryDiagnostic Tests")
class QueryDiagnosticTest {

    @Test
    @DisplayName("Should create QueryDiagnostic with valid fields")
    void testValidConstruction() {
        QueryDiagnostic diagnostic = new QueryDiagnostic(
                QueryDiagnostic.Kind.SYNTAX_ERROR,
                QueryDiagnostic.Severity.ERROR,
                "Syntax error near SELECT",
                5,
                10,
                "SELECT",
                "sparql-parser"
        );

        assertEquals(QueryDiagnostic.Kind.SYNTAX_ERROR, diagnostic.kind());
        assertEquals(QueryDiagnostic.Severity.ERROR, diagnostic.severity());
        assertEquals("Syntax error near SELECT", diagnostic.message());
        assertEquals(5, diagnostic.line());
        assertEquals(10, diagnostic.column());
        assertEquals("SELECT", diagnostic.offendingText());
        assertEquals("sparql-parser", diagnostic.source());
    }

    @Test
    @DisplayName("Should throw NullPointerException when required fields are null")
    void testNullChecks() {
        assertThrows(NullPointerException.class, () -> new QueryDiagnostic(
                null, QueryDiagnostic.Severity.ERROR, "msg", 1, 1, null, null));

        assertThrows(NullPointerException.class, () -> new QueryDiagnostic(
                QueryDiagnostic.Kind.SYNTAX_ERROR, null, "msg", 1, 1, null, null));

        assertThrows(NullPointerException.class, () -> new QueryDiagnostic(
                QueryDiagnostic.Kind.SYNTAX_ERROR, QueryDiagnostic.Severity.ERROR, null, 1, 1, null, null));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when line or column is < -1")
    void testInvalidLineAndColumn() {
        assertThrows(IllegalArgumentException.class, () -> new QueryDiagnostic(
                QueryDiagnostic.Kind.SYNTAX_ERROR, QueryDiagnostic.Severity.ERROR, "msg", -2, 1, null, null));

        assertThrows(IllegalArgumentException.class, () -> new QueryDiagnostic(
                QueryDiagnostic.Kind.SYNTAX_ERROR, QueryDiagnostic.Severity.ERROR, "msg", 1, -2, null, null));
    }

    @Test
    @DisplayName("format() should produce clean diagnostic representation")
    void testFormat() {
        QueryDiagnostic diagWithPos = new QueryDiagnostic(
                QueryDiagnostic.Kind.SYNTAX_ERROR,
                QueryDiagnostic.Severity.ERROR,
                "Unexpected token",
                12,
                4,
                "FOO",
                "antlr"
        );

        assertEquals("line 12:4 Unexpected token (offending: FOO) [antlr]", diagWithPos.format());

        QueryDiagnostic diagWithoutPos = new QueryDiagnostic(
                QueryDiagnostic.Kind.SEMANTIC_ERROR,
                QueryDiagnostic.Severity.WARNING,
                "Unused variable",
                -1,
                -1,
                null,
                null
        );

        assertEquals("Unused variable", diagWithoutPos.format());
    }
}
