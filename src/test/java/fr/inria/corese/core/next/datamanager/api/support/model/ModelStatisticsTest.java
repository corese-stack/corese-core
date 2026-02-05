package fr.inria.corese.core.next.datamanager.api.support.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ModelStatistics Record.
 */
@DisplayName("ModelStatistics Record Tests")
class ModelStatisticsTest {

    @Test
    @DisplayName("Should create record with all field values correctly assigned")
    void testRecordCreation() {
        ModelStatistics stats = new ModelStatistics(1000, 250, 50, 400, 5);

        assertEquals(1000, stats.statementCount());
        assertEquals(250, stats.subjectCount());
        assertEquals(50, stats.predicateCount());
        assertEquals(400, stats.objectCount());
        assertEquals(5, stats.contextCount());
    }

    @Test
    @DisplayName("Should calculate correct average statements per subject")
    void testAverageStatementsPerSubject() {
        ModelStatistics stats = new ModelStatistics(1000, 250, 50, 400, 5);

        assertEquals(4.0, stats.getAverageStatementsPerSubject(), 0.01);
    }

    @Test
    @DisplayName("Should calculate correct average statements per predicate")
    void testAverageStatementsPerPredicate() {
        ModelStatistics stats = new ModelStatistics(1000, 250, 50, 400, 5);

        assertEquals(20.0, stats.getAverageStatementsPerPredicate(), 0.01);
    }

    @Test
    @DisplayName("Should calculate correct average statements per context")
    void testAverageStatementsPerContext() {
        ModelStatistics stats = new ModelStatistics(1000, 250, 50, 400, 5);

        assertEquals(200.0, stats.getAverageStatementsPerContext(), 0.01);
    }

    @Test
    @DisplayName("Should return 0.0 for averages when divisor is zero")
    void testAverageWithZeroDivisor() {
        ModelStatistics stats = new ModelStatistics(1000, 0, 0, 400, 0);

        assertEquals(0.0, stats.getAverageStatementsPerSubject());
        assertEquals(0.0, stats.getAverageStatementsPerPredicate());
        assertEquals(0.0, stats.getAverageStatementsPerContext());
    }

    @Test
    @DisplayName("Should correctly identify empty and non-empty statistics")
    void testIsEmpty() {
        ModelStatistics empty = new ModelStatistics(0, 0, 0, 0, 0);
        ModelStatistics notEmpty = new ModelStatistics(1, 1, 1, 1, 1);

        assertTrue(empty.isEmpty());
        assertFalse(notEmpty.isEmpty());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for negative values in any field")
    void testNegativeValueValidation() {
        assertThrows(IllegalArgumentException.class,
                () -> new ModelStatistics(-1, 250, 50, 400, 5),
                "Negative statementCount should throw exception");

        assertThrows(IllegalArgumentException.class,
                () -> new ModelStatistics(1000, -1, 50, 400, 5),
                "Negative subjectCount should throw exception");

        assertThrows(IllegalArgumentException.class,
                () -> new ModelStatistics(1000, 250, -1, 400, 5),
                "Negative predicateCount should throw exception");

        assertThrows(IllegalArgumentException.class,
                () -> new ModelStatistics(1000, 250, 50, -1, 5),
                "Negative objectCount should throw exception");

        assertThrows(IllegalArgumentException.class,
                () -> new ModelStatistics(1000, 250, 50, 400, -1),
                "Negative contextCount should throw exception");
    }

    @Test
    @DisplayName("Should implement equals() correctly for Record")
    void testEquality() {
        ModelStatistics stats1 = new ModelStatistics(1000, 250, 50, 400, 5);
        ModelStatistics stats2 = new ModelStatistics(1000, 250, 50, 400, 5);
        ModelStatistics stats3 = new ModelStatistics(2000, 250, 50, 400, 5);

        assertEquals(stats1, stats2);
        assertNotEquals(stats1, stats3);
    }

    @Test
    @DisplayName("Should generate consistent hashCode() for equal records")
    void testHashCode() {
        ModelStatistics stats1 = new ModelStatistics(1000, 250, 50, 400, 5);
        ModelStatistics stats2 = new ModelStatistics(1000, 250, 50, 400, 5);

        assertEquals(stats1.hashCode(), stats2.hashCode());
    }

    @Test
    @DisplayName("Should include field values and class name in toString() output")
    void testToString() {
        ModelStatistics stats = new ModelStatistics(1000, 250, 50, 400, 5);
        String str = stats.toString();

        assertTrue(str.contains("1000"));
        assertTrue(str.contains("250"));
        assertTrue(str.contains("ModelStatistics"));
    }
}