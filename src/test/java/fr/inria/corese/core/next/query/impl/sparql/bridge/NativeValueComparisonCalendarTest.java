package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.Repositories;
import fr.inria.corese.core.next.storage.impl.memory.MemoryStorageManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for cross-type calendar equality in {@code NativeValueComparison}.
 *
 * <p>Two calendar literals with <em>different</em> XSD types (e.g., {@code xsd:date}
 * vs {@code xsd:dateTime}) are distinct RDF terms and must compare as {@code false},
 * not throw a type error.</p>
 */
@DisplayName("NativeValueComparison - cross-type calendar equality")
class NativeValueComparisonCalendarTest {

    private static final String PREFIX =
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

    private boolean ask(String filter) {
        try (var repo = Repositories.create(MemoryStorageManager.builder().build());
             var conn = repo.getConnection()) {
            return conn.prepareBooleanQuery(PREFIX + "ASK { FILTER(" + filter + ") }").evaluate();
        }
    }

    @Test
    @DisplayName("xsd:date = xsd:dateTime returns false")
    void dateDifferentFromDateTime() {
        assertFalse(ask(
                "\"2024-01-01\"^^xsd:date = \"2024-01-01T00:00:00\"^^xsd:dateTime"),
                "xsd:date and xsd:dateTime have different XSD types and must not be equal");
    }

    @Test
    @DisplayName("xsd:date = xsd:time returns false")
    void dateDifferentFromTime() {
        assertFalse(ask(
                "\"2024-01-01\"^^xsd:date = \"10:00:00\"^^xsd:time"));
    }

    @Test
    @DisplayName("xsd:dateTime = xsd:time returns false")
    void dateTimeDifferentFromTime() {
        assertFalse(ask(
                "\"2024-01-01T00:00:00\"^^xsd:dateTime = \"00:00:00\"^^xsd:time"));
    }

    @Test
    @DisplayName("identical xsd:date values are equal")
    void sameDateEquality() {
        assertTrue(ask(
                "\"2024-01-01\"^^xsd:date = \"2024-01-01\"^^xsd:date"));
    }

    @Test
    @DisplayName("different xsd:date values are not equal")
    void differentDatesNotEqual() {
        assertFalse(ask(
                "\"2024-01-01\"^^xsd:date = \"2024-01-02\"^^xsd:date"));
    }

    @Test
    @DisplayName("identical xsd:dateTime values are equal")
    void sameDateTimeEquality() {
        assertTrue(ask(
                "\"2024-01-01T12:00:00\"^^xsd:dateTime = \"2024-01-01T12:00:00\"^^xsd:dateTime"));
    }
}
