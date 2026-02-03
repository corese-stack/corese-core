package fr.inria.corese.core.next.datamanager.impl.operations;

import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.Model;
import fr.inria.corese.core.next.data.api.Resource;
import fr.inria.corese.core.next.data.api.Value;
import fr.inria.corese.core.next.datamanager.api.support.exception.DataManagerException;
import fr.inria.corese.core.next.datamanager.api.support.model.ModelStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MetadataOperationsImpl.
 */
@DisplayName("MetadataOperationsImpl Tests")
class MetadataOperationsImplTest {

    @Mock
    private Model model;

    @Mock
    private IRI pred1, pred2;

    @Mock
    private Resource subj1, subj2, ctx1, ctx2;

    @Mock
    private Value obj1, obj2;

    private MetadataOperationsImpl metadataOps;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        metadataOps = new MetadataOperationsImpl(model);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when constructed with null model")
    void testConstructorNullModel() {
        assertThrows(IllegalArgumentException.class,
                () -> new MetadataOperationsImpl(null));
    }

    @Test
    @DisplayName("Should retrieve all unique predicates from model")
    void testGetPredicates() throws DataManagerException {
        Set<IRI> predicates = new HashSet<>();
        predicates.add(pred1);
        predicates.add(pred2);

        when(model.predicates()).thenReturn(predicates);

        Set<IRI> result = metadataOps.getPredicates();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(pred1));
        assertTrue(result.contains(pred2));

        verify(model).predicates();
    }

    @Test
    @DisplayName("Should return empty set when model has no predicates")
    void testGetPredicatesEmpty() throws DataManagerException {
        when(model.predicates()).thenReturn(new HashSet<>());

        Set<IRI> result = metadataOps.getPredicates();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return unmodifiable set of predicates")
    void testGetPredicatesUnmodifiable() throws DataManagerException {
        Set<IRI> predicates = new HashSet<>();
        predicates.add(pred1);

        when(model.predicates()).thenReturn(predicates);

        Set<IRI> result = metadataOps.getPredicates();

        assertThrows(UnsupportedOperationException.class,
                () -> result.add(pred2));
    }

    @Test
    @DisplayName("Should retrieve all unique subjects from model")
    void testGetSubjects() throws DataManagerException {
        Set<Resource> subjects = new HashSet<>();
        subjects.add(subj1);
        subjects.add(subj2);

        when(model.subjects()).thenReturn(subjects);

        Set<Resource> result = metadataOps.getSubjects();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(subj1));
        assertTrue(result.contains(subj2));

        verify(model).subjects();
    }

    @Test
    @DisplayName("Should retrieve all unique objects from model")
    void testGetObjects() throws DataManagerException {
        Set<Value> objects = new HashSet<>();
        objects.add(obj1);
        objects.add(obj2);

        when(model.objects()).thenReturn(objects);

        Set<Value> result = metadataOps.getObjects();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(obj1));
        assertTrue(result.contains(obj2));

        verify(model).objects();
    }

    @Test
    @DisplayName("Should retrieve all unique contexts from model")
    void testGetContexts() throws DataManagerException {
        Set<Resource> contexts = new HashSet<>();
        contexts.add(ctx1);
        contexts.add(ctx2);

        when(model.contexts()).thenReturn(contexts);

        Set<Resource> result = metadataOps.getContexts();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(ctx1));
        assertTrue(result.contains(ctx2));

        verify(model).contexts();
    }

    @Test
    @DisplayName("Should calculate model statistics with correct counts")
    void testGetStatistics() throws DataManagerException {
        Set<Resource> subjects = new HashSet<>();
        subjects.add(subj1);
        subjects.add(subj2);
        when(model.subjects()).thenReturn(subjects);

        Set<IRI> predicates = new HashSet<>();
        predicates.add(pred1);
        when(model.predicates()).thenReturn(predicates);

        Set<Value> objects = new HashSet<>();
        objects.add(obj1);
        objects.add(obj2);
        when(model.objects()).thenReturn(objects);

        Set<Resource> contexts = new HashSet<>();
        contexts.add(ctx1);
        when(model.contexts()).thenReturn(contexts);

        ModelStatistics stats = metadataOps.getStatistics();

        assertNotNull(stats);
        assertEquals(0, stats.statementCount());
        assertEquals(2, stats.subjectCount());
        assertEquals(1, stats.predicateCount());
        assertEquals(2, stats.objectCount());
        assertEquals(1, stats.contextCount());
    }

    @Test
    @DisplayName("Should return empty statistics for empty model")
    void testGetStatisticsEmptyModel() throws DataManagerException {
        when(model.subjects()).thenReturn(new HashSet<>());
        when(model.predicates()).thenReturn(new HashSet<>());
        when(model.objects()).thenReturn(new HashSet<>());
        when(model.contexts()).thenReturn(new HashSet<>());

        ModelStatistics stats = metadataOps.getStatistics();

        assertNotNull(stats);
        assertEquals(0, stats.statementCount());
        assertTrue(stats.isEmpty());
    }

    @Test
    @DisplayName("Should throw DataManagerException when predicates() fails")
    void testGetPredicatesWithException() {
        when(model.predicates()).thenThrow(new RuntimeException("Test error"));

        assertThrows(DataManagerException.class,
                () -> metadataOps.getPredicates());
    }

    @Test
    @DisplayName("Should throw DataManagerException when subjects() fails")
    void testGetSubjectsWithException() {
        when(model.subjects()).thenThrow(new RuntimeException("Test error"));

        assertThrows(DataManagerException.class,
                () -> metadataOps.getSubjects());
    }

    @Test
    @DisplayName("Should throw DataManagerException when objects() fails")
    void testGetObjectsWithException() {
        when(model.objects()).thenThrow(new RuntimeException("Test error"));

        assertThrows(DataManagerException.class,
                () -> metadataOps.getObjects());
    }

    @Test
    @DisplayName("Should throw DataManagerException when contexts() fails")
    void testGetContextsWithException() {
        when(model.contexts()).thenThrow(new RuntimeException("Test error"));

        assertThrows(DataManagerException.class,
                () -> metadataOps.getContexts());
    }

    @Test
    @DisplayName("Should throw DataManagerException when getStatistics() fails")
    void testGetStatisticsWithException() {
        when(model.size()).thenThrow(new RuntimeException("Test error"));

        assertThrows(DataManagerException.class,
                () -> metadataOps.getStatistics());
    }
}