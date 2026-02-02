package fr.inria.corese.core.next.datamanager.impl.operations;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.datamanager.api.support.exception.DataManagerException;
import fr.inria.corese.core.next.datamanager.api.support.model.StatementPattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for QueryOperationsImpl.
 */
@DisplayName("QueryOperationsImpl Tests")
class QueryOperationsImplTest {

    @Mock
    private Model model;

    @Mock
    private Statement stmt1, stmt2, stmt3;

    @Mock
    private Resource subject, context;

    @Mock
    private IRI predicate;

    @Mock
    private Value object;

    private QueryOperationsImpl queryOps;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        queryOps = new QueryOperationsImpl(model);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when constructed with null model")
    void testConstructorNullModel() {
        assertThrows(IllegalArgumentException.class,
                () -> new QueryOperationsImpl(null));
    }

    @Test
    @DisplayName("Should query statements matching the given pattern")
    void testQueryWithPattern() throws DataManagerException {
        StatementPattern pattern = StatementPattern.builder()
                .subject(subject)
                .predicate(predicate)
                .object(object)
                .contexts(context)
                .build();

        Iterable<Statement> iterable = Arrays.asList(stmt1, stmt2, stmt3);
        when(model.getStatements(subject, predicate, object, context))
                .thenReturn(iterable);

        Stream<Statement> stream = queryOps.query(pattern);

        assertNotNull(stream);
        assertEquals(3, stream.count());

        verify(model).getStatements(subject, predicate, object, context);
    }

    @Test
    @DisplayName("Should query all statements when using matchAll pattern")
    void testQueryMatchAll() throws DataManagerException {
        StatementPattern pattern = StatementPattern.matchAll();

        Iterable<Statement> iterable = Arrays.asList(stmt1, stmt2);
        when(model.getStatements(null, null, null))
                .thenReturn(iterable);

        Stream<Statement> stream = queryOps.query(pattern);

        assertNotNull(stream);
        assertEquals(2, stream.count());
    }

    @Test
    @DisplayName("Should return empty stream when no statements match pattern")
    void testQueryEmpty() throws DataManagerException {
        StatementPattern pattern = StatementPattern.matchAll();

        when(model.getStatements(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        Stream<Statement> stream = queryOps.query(pattern);

        assertNotNull(stream);
        assertEquals(0, stream.count());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when query called with null pattern")
    void testQueryNullPattern() {
        assertThrows(IllegalArgumentException.class,
                () -> queryOps.query(null));
    }

    @Test
    @DisplayName("Should count statements matching the given pattern")
    void testCountWithPattern() throws DataManagerException {
        StatementPattern pattern = StatementPattern.builder()
                .predicate(predicate)
                .build();

        Iterable<Statement> iterable = Arrays.asList(stmt1, stmt2, stmt3);
        when(model.getStatements(null, predicate, null))
                .thenReturn(iterable);

        long count = queryOps.count(pattern);

        assertEquals(3, count);
    }

    @Test
    @DisplayName("Should return zero when no statements match count pattern")
    void testCountEmpty() throws DataManagerException {
        StatementPattern pattern = StatementPattern.builder()
                .subject(subject)
                .build();

        when(model.getStatements(subject, null, null))
                .thenReturn(Collections.emptyList());

        long count = queryOps.count(pattern);

        assertEquals(0, count);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when count called with null pattern")
    void testCountNullPattern() {
        assertThrows(IllegalArgumentException.class,
                () -> queryOps.count(null));
    }

    @Test
    @DisplayName("Should return true when statement matching pattern exists")
    void testExistsTrue() throws DataManagerException {
        StatementPattern pattern = StatementPattern.builder()
                .subject(subject)
                .predicate(predicate)
                .build();

        when(model.contains(subject, predicate, null))
                .thenReturn(true);

        boolean exists = queryOps.exists(pattern);

        assertTrue(exists);
        verify(model).contains(subject, predicate, null);
    }

    @Test
    @DisplayName("Should return false when no statement matching pattern exists")
    void testExistsFalse() throws DataManagerException {
        StatementPattern pattern = StatementPattern.builder()
                .subject(subject)
                .build();

        when(model.contains(subject, null, null))
                .thenReturn(false);

        boolean exists = queryOps.exists(pattern);

        assertFalse(exists);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when exists called with null pattern")
    void testExistsNullPattern() {
        assertThrows(IllegalArgumentException.class,
                () -> queryOps.exists(null));
    }

    @Test
    @DisplayName("Should filter model and return filtered subset")
    void testFilter() throws DataManagerException {
        StatementPattern pattern = StatementPattern.builder()
                .predicate(predicate)
                .build();

        Model filteredModel = mock(Model.class);
        when(model.filter(null, predicate, null))
                .thenReturn(filteredModel);

        Model result = queryOps.filter(pattern);

        assertNotNull(result);
        assertEquals(filteredModel, result);
        verify(model).filter(null, predicate, null);
    }

    @Test
    @DisplayName("Should filter with matchAll pattern")
    void testFilterMatchAll() throws DataManagerException {
        StatementPattern pattern = StatementPattern.matchAll();

        Model filteredModel = mock(Model.class);
        when(model.filter(null, null, null))
                .thenReturn(filteredModel);

        Model result = queryOps.filter(pattern);

        assertNotNull(result);
        verify(model).filter(null, null, null);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when filter called with null pattern")
    void testFilterNullPattern() {
        assertThrows(IllegalArgumentException.class,
                () -> queryOps.filter(null));
    }

    @Test
    @DisplayName("Should throw DataManagerException when filter operation fails")
    void testFilterWithException() {
        StatementPattern pattern = StatementPattern.matchAll();

        when(model.filter(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Test error"));

        assertThrows(DataManagerException.class,
                () -> queryOps.filter(pattern));
    }
}