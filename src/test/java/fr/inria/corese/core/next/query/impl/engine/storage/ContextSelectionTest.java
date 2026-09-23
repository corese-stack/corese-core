package fr.inria.corese.core.next.query.impl.engine.storage;

import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.engine.spi.Environment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link StoragePatternTranslator.ContextSelection#defaultContext()} and
 * {@link StoragePatternTranslator#selectDatasetContexts}.
 *
 * <p>The key invariant: when no FROM clause is present and no explicit dataset is declared,
 * triple patterns must target only the null/default context — named-graph triples must not
 * bleed into default-graph patterns.</p>
 */
@DisplayName("StoragePatternTranslator - ContextSelection and selectDatasetContexts")
class ContextSelectionTest {

    private static Environment envWith(boolean datasetSpecified) {
        Environment env = mock(Environment.class);
        Query query = mock(Query.class);
        when(env.getQuery()).thenReturn(query);
        when(query.isDatasetSpecified()).thenReturn(datasetSpecified);
        return env;
    }

    @Test
    @DisplayName("defaultContext() contains exactly one null element and noMatch=false")
    void defaultContextHasNullElementAndNoMatchFalse() {
        StoragePatternTranslator.ContextSelection sel =
                StoragePatternTranslator.ContextSelection.defaultContext();

        assertFalse(sel.noMatch(), "defaultContext must not signal an impossible match");
        assertEquals(1, sel.contexts().size(), "defaultContext must contain exactly one entry");
        assertNull(sel.contexts().getFirst(), "the single entry must be null (default-graph sentinel)");
    }

    @Test
    @DisplayName("null activeGraphs, no explicit dataset → defaultContext (isolates named graphs)")
    void nullActiveGraphsNoExplicitDatasetReturnsDefaultContext() {
        StoragePatternTranslator.ContextSelection sel =
                StoragePatternTranslator.selectDatasetContexts(null, envWith(false));

        assertFalse(sel.noMatch());
        assertEquals(1, sel.contexts().size());
        assertNull(sel.contexts().getFirst(),
                "without FROM, only the null/default context must be queried");
    }

    @Test
    @DisplayName("empty activeGraphs, no explicit dataset → defaultContext")
    void emptyActiveGraphsNoExplicitDatasetReturnsDefaultContext() {
        StoragePatternTranslator.ContextSelection sel =
                StoragePatternTranslator.selectDatasetContexts(List.of(), envWith(false));

        assertFalse(sel.noMatch());
        assertEquals(1, sel.contexts().size());
        assertNull(sel.contexts().getFirst());
    }


    @Test
    @DisplayName("null activeGraphs, explicit dataset → emptyResult (FROM with no graphs)")
    void nullActiveGraphsExplicitDatasetReturnsEmptyResult() {
        StoragePatternTranslator.ContextSelection sel =
                StoragePatternTranslator.selectDatasetContexts(null, envWith(true));

        assertTrue(sel.noMatch(),
                "an explicit FROM clause that names no graphs must yield no results");
    }

    @Test
    @DisplayName("empty activeGraphs, explicit dataset → emptyResult")
    void emptyActiveGraphsExplicitDatasetReturnsEmptyResult() {
        StoragePatternTranslator.ContextSelection sel =
                StoragePatternTranslator.selectDatasetContexts(List.of(), envWith(true));

        assertTrue(sel.noMatch());
    }
}
