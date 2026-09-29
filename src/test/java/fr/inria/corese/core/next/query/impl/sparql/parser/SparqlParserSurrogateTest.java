package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests that surrogate code points (U+D800–U+DFFF) are rejected when they appear
 * as Unicode escape sequences in SPARQL
 * string literals.
 *
 */
@DisplayName("SPARQL parser - surrogate code point rejection in literals")
class SparqlParserSurrogateTest extends AbstractSparqlParserFeatureTest {

    private static final String BASE = "SELECT * WHERE { ?s ?p ";
    private static final String END  = " }";

    @ParameterizedTest(name = "literal \\u{0} is rejected")
    @ValueSource(strings = {"D800", "DBFF", "DC00", "DFFF"})
    @DisplayName("literal with \\uXXXX surrogate is rejected")
    void literalWithSurrogateUcharIsRejected(String hex) {
        String query = BASE + "\"\\u" + hex + "\"" + END;
        assertThrows(QuerySyntaxException.class,
                () -> newParserDefault().parse(query),
                "Surrogate U+" + hex + " in a literal must be rejected");
    }

    @ParameterizedTest(name = "literal \\U0000{0} is rejected")
    @ValueSource(strings = {"D800", "DBFF", "DC00", "DFFF"})
    @DisplayName("literal with \\UXXXXXXXX surrogate is rejected")
    void literalWithLongSurrogateUcharIsRejected(String hex) {
        String query = BASE + "\"\\U0000" + hex + "\"" + END;
        assertThrows(QuerySyntaxException.class,
                () -> newParserDefault().parse(query),
                "Surrogate U+" + hex + " (long form) in a literal must be rejected");
    }

    @Test
    @DisplayName("literal with valid \\u0041 ('A') is accepted")
    void literalWithValidShortEscapeIsAccepted() {
        assertDoesNotThrow(() -> newParserDefault().parse(
                BASE + "\"\\u0041\"" + END));
    }

    @Test
    @DisplayName("literal with valid \\U0001F600 (emoji) is accepted")
    void literalWithValidLongEscapeIsAccepted() {
        assertDoesNotThrow(() -> newParserDefault().parse(
                BASE + "\"\\U0001F600\"" + END));
    }
}
