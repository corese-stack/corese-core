package fr.inria.corese.core.query;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import fr.inria.corese.core.sparql.triple.parser.Dataset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the QueryEngine class, focusing on the new caching mechanism
 * and the 'clean' method fix.
 */
class QueryEngineTest {

    private QueryEngine queryEngine;
    private QueryProcess mockQueryProcess;
    private Graph mockGraph;
    private Dataset mockDataset;

    @BeforeEach
    void setUp() {
        mockGraph = Mockito.mock(Graph.class);
        mockQueryProcess = Mockito.mock(QueryProcess.class);
        mockDataset = Mockito.mock(Dataset.class);

        queryEngine = Mockito.spy(new QueryEngine(mockGraph));

        try {
            java.lang.reflect.Field execField = QueryEngine.class.getDeclaredField("exec");
            execField.setAccessible(true);
            execField.set(queryEngine, mockQueryProcess);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to inject mock QueryProcess: " + e.getMessage());
        }

        doReturn(mockDataset).when(queryEngine).getDataset();
    }

    @Test
    void testDefQuery_CacheHit() throws EngineException {
        String queryString = "SELECT * WHERE { ?s ?p ?o }";
        Query mockQuery = Mockito.mock(Query.class);
        ASTQuery mockASTQuery = Mockito.mock(ASTQuery.class);
        when(mockQuery.isTemplate()).thenReturn(false);
        when(mockQuery.toString()).thenReturn(queryString);
        when(mockQuery.getAST()).thenReturn(mockASTQuery);
        when(mockQueryProcess.compile(eq(queryString), any(Dataset.class))).thenReturn(mockQuery);

        queryEngine.defQuery(queryString);
        verify(mockQueryProcess, times(1)).compile(eq(queryString), any(Dataset.class));
        assertEquals(0, queryEngine.getCacheStats().getHits());

        queryEngine.defQuery(queryString);
        verify(mockQueryProcess, times(1)).compile(eq(queryString), any(Dataset.class));
        assertEquals(1, queryEngine.getCacheStats().getHits());
        assertEquals(1, queryEngine.getCacheStats().getMisses());
        assertEquals(1, queryEngine.getCacheStats().getCurrentSize());
    }

    @Test
    void testDefQuery_CacheMiss_DifferentQuery() throws EngineException {
        String queryString1 = "SELECT * WHERE { ?s ?p ?o }";
        String queryString2 = "SELECT DISTINCT ?s WHERE { ?s a :Class }";

        Query mockQuery1 = Mockito.mock(Query.class);
        ASTQuery mockASTQuery1 = Mockito.mock(ASTQuery.class);
        when(mockQuery1.isTemplate()).thenReturn(false);
        when(mockQuery1.toString()).thenReturn(queryString1);
        when(mockQuery1.getAST()).thenReturn(mockASTQuery1);

        Query mockQuery2 = Mockito.mock(Query.class);
        ASTQuery mockASTQuery2 = Mockito.mock(ASTQuery.class);
        when(mockQuery2.isTemplate()).thenReturn(false);
        when(mockQuery2.toString()).thenReturn(queryString2);
        when(mockQuery2.getAST()).thenReturn(mockASTQuery2);

        when(mockQueryProcess.compile(eq(queryString1), any(Dataset.class))).thenReturn(mockQuery1);
        when(mockQueryProcess.compile(eq(queryString2), any(Dataset.class))).thenReturn(mockQuery2);


        queryEngine.defQuery(queryString1);
        verify(mockQueryProcess, times(1)).compile(eq(queryString1), any(Dataset.class));
        assertEquals(0, queryEngine.getCacheStats().getHits());
        assertEquals(1, queryEngine.getCacheStats().getMisses());
        assertEquals(1, queryEngine.getCacheStats().getCurrentSize());

        queryEngine.defQuery(queryString2);
        verify(mockQueryProcess, times(1)).compile(eq(queryString2), any(Dataset.class));
        assertEquals(0, queryEngine.getCacheStats().getHits());
        assertEquals(2, queryEngine.getCacheStats().getMisses());
        assertEquals(2, queryEngine.getCacheStats().getCurrentSize());
    }

    @Test
    void testDefQuery_CacheEviction() throws EngineException {
        queryEngine = Mockito.spy(new QueryEngine(mockGraph, 1));
        try {
            java.lang.reflect.Field execField = QueryEngine.class.getDeclaredField("exec");
            execField.setAccessible(true);
            execField.set(queryEngine, mockQueryProcess);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to inject mock QueryProcess: " + e.getMessage());
        }
        doReturn(mockDataset).when(queryEngine).getDataset();


        String queryString1 = "SELECT * WHERE { ?s ?p ?o }";
        String queryString2 = "ASK { ?s ?p ?o }";

        Query mockQuery1 = Mockito.mock(Query.class);
        ASTQuery mockASTQuery1 = Mockito.mock(ASTQuery.class);
        when(mockQuery1.isTemplate()).thenReturn(false);
        when(mockQuery1.toString()).thenReturn(queryString1);
        when(mockQuery1.getAST()).thenReturn(mockASTQuery1);

        Query mockQuery2 = Mockito.mock(Query.class);
        ASTQuery mockASTQuery2 = Mockito.mock(ASTQuery.class);
        when(mockQuery2.isTemplate()).thenReturn(false);
        when(mockQuery2.toString()).thenReturn(queryString2);
        when(mockQuery2.getAST()).thenReturn(mockASTQuery2);

        when(mockQueryProcess.compile(eq(queryString1), any(Dataset.class))).thenReturn(mockQuery1);
        when(mockQueryProcess.compile(eq(queryString2), any(Dataset.class))).thenReturn(mockQuery2);


        queryEngine.defQuery(queryString1);
        assertEquals(1, queryEngine.getCacheStats().getCurrentSize());
        assertEquals(1, queryEngine.getCacheStats().getMisses());

        queryEngine.defQuery(queryString2);
        assertEquals(1, queryEngine.getCacheStats().getCurrentSize());
        assertEquals(2, queryEngine.getCacheStats().getMisses());

        queryEngine.defQuery(queryString1);
        verify(mockQueryProcess, times(2)).compile(eq(queryString1), any(Dataset.class));
        assertEquals(0, queryEngine.getCacheStats().getHits());
        assertEquals(3, queryEngine.getCacheStats().getMisses());
    }

    @Test
    void testDefQuery_NoCacheForUpdateQueries() throws EngineException {
        String updateQuery = "INSERT DATA { :s :p :o }";
        Query mockQuery = Mockito.mock(Query.class);
        ASTQuery mockASTQuery = Mockito.mock(ASTQuery.class);
        when(mockQuery.isTemplate()).thenReturn(false);
        when(mockQuery.toString()).thenReturn(updateQuery);
        when(mockQuery.getAST()).thenReturn(mockASTQuery);
        when(mockQueryProcess.compile(eq(updateQuery), any(Dataset.class))).thenReturn(mockQuery);

        queryEngine.defQuery(updateQuery);
        verify(mockQueryProcess, times(1)).compile(eq(updateQuery), any(Dataset.class));
        assertEquals(0, queryEngine.getCacheStats().getHits());
        assertEquals(1, queryEngine.getCacheStats().getMisses());
        assertEquals(0, queryEngine.getCacheStats().getCurrentSize());
    }

    @Test
    void testClearCompilationCache() throws EngineException {
        String queryString = "SELECT * WHERE { ?s ?p ?o }";
        Query mockQuery = Mockito.mock(Query.class);
        ASTQuery mockASTQuery = Mockito.mock(ASTQuery.class);
        when(mockQuery.isTemplate()).thenReturn(false);
        when(mockQuery.getAST()).thenReturn(mockASTQuery);
        when(mockQueryProcess.compile(eq(queryString), any(Dataset.class))).thenReturn(mockQuery);

        queryEngine.defQuery(queryString);
        assertEquals(1, queryEngine.getCacheStats().getCurrentSize());

        queryEngine.clearCompilationCache();
        assertEquals(0, queryEngine.getCacheStats().getCurrentSize());
        assertEquals(0, queryEngine.getCacheStats().getHits());
        assertEquals(0, queryEngine.getCacheStats().getMisses());
    }

    @Test
    void testCleanMethod() {
        ArrayList<Query> internalList = null;
        try {
            java.lang.reflect.Field listField = QueryEngine.class.getDeclaredField("list");
            listField.setAccessible(true);
            internalList = (ArrayList<Query>) listField.get(queryEngine);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to access private 'list' field: " + e.getMessage());
        }

        Query query1 = Mockito.mock(Query.class);
        when(query1.isFail()).thenReturn(false);
        Query query2 = Mockito.mock(Query.class);
        when(query2.isFail()).thenReturn(true);
        Query query3 = Mockito.mock(Query.class);
        when(query3.isFail()).thenReturn(false);

        internalList.add(query1);
        internalList.add(query2);
        internalList.add(query3);

        assertEquals(3, internalList.size());

        queryEngine.clean();

        assertEquals(2, internalList.size());
        assertTrue(internalList.contains(query1));
        assertFalse(internalList.contains(query2));
        assertTrue(internalList.contains(query3));
    }


}
