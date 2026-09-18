package fr.inria.corese.core.next.query.impl.repository;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.query.Repositories;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.api.repository.Repository;
import fr.inria.corese.core.next.query.api.repository.RepositoryConnection;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.impl.graph.GraphStorageManager;
import fr.inria.corese.core.next.storage.impl.memory.MemoryStorageManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreseUpdateExecutionTest {
    private static final String PREFIX = "PREFIX ex: <http://example.org/> ";
    private StorageManager storage;
    private Repository repository;
    private RepositoryConnection connection;
    @TempDir
    Path directory;

    @BeforeEach
    void setUp() {
        storage = MemoryStorageManager.builder().build();
        repository = Repositories.create(storage);
        connection = repository.getConnection();
    }

    @AfterEach
    void close() {
        connection.close();
        repository.close();
    }

    @Test
    void dataAndDeleteWhereRespectDefaultAndNamedGraphs() {
        update("INSERT DATA { ex:s ex:p ex:o . GRAPH ex:g { ex:s ex:p ex:o } }");
        update("DELETE WHERE { ?s ?p ?o }");
        assertEquals(0, connection.size((Resource) null));
        assertEquals(1, connection.size(iri("g")));
        update("DELETE DATA { GRAPH ex:g { ex:s ex:p ex:o } }");
        assertEquals(0, connection.size());
    }

    @Test
    void deleteAndInsertUseOneSnapshotOfAllSolutions() {
        update("INSERT DATA { ex:a ex:p ex:b . ex:b ex:p ex:c }");
        update("DELETE { ?s ex:p ?o } INSERT { ?o ex:p ?s } WHERE { ?s ex:p ?o }");
        assertEquals(2, connection.size());
        assertTrue(connection.hasStatement(iri("b"), iri("p"), iri("a")));
        assertTrue(connection.hasStatement(iri("c"), iri("p"), iri("b")));
    }

    @Test
    void withSelectsTheDefaultGraphAndUsingOverridesItsReadSideOnly() {
        update("INSERT DATA { GRAPH ex:g { ex:a ex:p ex:b } GRAPH ex:h { ex:c ex:p ex:d } }");
        update("WITH ex:g DELETE { ?s ex:p ?o } INSERT { ?s ex:q ?o } WHERE { ?s ex:p ?o }");
        assertTrue(connection.hasStatement(iri("a"), iri("q"), iri("b"), iri("g")));
        update("WITH ex:g INSERT { ?s ex:r ?o } USING ex:h WHERE { ?s ex:p ?o }");
        assertTrue(connection.hasStatement(iri("c"), iri("r"), iri("d"), iri("g")));
        assertEquals(1, connection.size(iri("h")));
    }

    @Test
    void usingNamedRestrictsGraphsAndLeavesAnEmptyDefaultGraph() {
        update("INSERT DATA { ex:a ex:p ex:b . GRAPH ex:g { ex:c ex:p ex:d } GRAPH ex:h { ex:e ex:p ex:f } }");
        update("INSERT { ?s ex:q ?o } USING NAMED ex:g WHERE { GRAPH ?g { ?s ex:p ?o } }");
        update("INSERT { ?s ex:r ?o } USING NAMED ex:g WHERE { ?s ex:p ?o }");
        assertEquals(4, connection.size());
        assertTrue(connection.hasStatement(iri("c"), iri("q"), iri("d"), (Resource) null));
    }

    @Test
    void whereSupportsSubqueriesValuesOptionalAndPropertyPaths() {
        update("INSERT DATA { ex:a ex:p ex:b . ex:b ex:p ex:c }");
        update("INSERT { ?s ex:reachable ?o } WHERE { { SELECT ?s WHERE { VALUES ?s { ex:a } } } "
                + "?s ex:p+ ?o OPTIONAL { ?o ex:missing ?unused } }");
        assertEquals(4, connection.size());
    }

    @Test
    void unboundAndInvalidTemplateTriplesAreOmitted() {
        update("INSERT { ?s ex:p ex:o . ex:s ?p ex:o . GRAPH ?g { ex:s ex:p ex:o } } "
                + "WHERE { VALUES (?s ?p ?g) { (12 13 14) (UNDEF UNDEF UNDEF) } }");
        assertEquals(0, connection.size());
    }

    @Test
    void templateBlankNodesAreFreshPerSolutionAndSharedAcrossGraphs() {
        update("INSERT { _:b ex:p ?o . GRAPH ex:g { _:b ex:q ?o } } WHERE { VALUES ?o { ex:a ex:b } }");
        assertEquals(4, connection.size());
        try (var result = connection.prepareTupleQuery(PREFIX +
                "SELECT ?s WHERE { GRAPH ex:g { ?s ex:q ?o } }").evaluate()) {
            var first = result.next().getValue("s");
            var second = result.next().getValue("s");
            assertNotEquals(first, second);
            assertFalse(result.hasNext());
            assertTrue(connection.hasStatement((Resource) first, iri("p"), null, (Resource) null));
        }
    }

    @Test
    void nestedBlankNodesAndCollectionsAreExpandedInQuadTemplates() {
        update("INSERT DATA { ex:s ex:p [ ex:q (ex:a ex:b) ] }");
        assertEquals(6, connection.size());
        assertTrue(connection.prepareBooleanQuery(PREFIX +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "ASK { ex:s ex:p/ex:q/rdf:rest/rdf:first ex:b }").evaluate());
        update("INSERT { GRAPH ex:g { [ ex:p (?o) ] ex:q ex:c } } WHERE { VALUES ?o { ex:a ex:b } }");
        assertEquals(8, connection.size(iri("g")));
    }

    @Test
    void createClearAndDropPreserveTheEmptyGraphDistinction() {
        update("CREATE GRAPH ex:g");
        assertEquals(Set.of(iri("g")), storage.metadata().getContexts());
        assertTrue(connection.prepareBooleanQuery(PREFIX + "ASK { GRAPH ex:g {} }").evaluate());
        assertThrows(QueryEvaluationException.class, () -> update("CREATE GRAPH ex:g"));
        update("INSERT DATA { GRAPH ex:g { ex:a ex:p ex:b } }; CLEAR GRAPH ex:g");
        assertEquals(0, connection.size());
        assertEquals(Set.of(iri("g")), storage.metadata().getContexts());
        update("DROP GRAPH ex:g");
        assertTrue(storage.metadata().getContexts().isEmpty());
        assertThrows(QueryEvaluationException.class, () -> update("CLEAR GRAPH ex:g"));
    }

    @Test
    void allAndNamedGraphCommandsSelectTheRightTargets() {
        update("INSERT DATA { ex:s ex:p ex:o . GRAPH ex:g { ex:s ex:p ex:o } }; CREATE GRAPH ex:h");
        update("CLEAR NAMED");
        assertEquals(1, connection.size());
        assertEquals(Set.of(iri("g"), iri("h")), storage.metadata().getContexts());
        update("CLEAR ALL");
        assertEquals(0, connection.size());
        update("DROP NAMED");
        assertTrue(storage.metadata().getContexts().isEmpty());
    }

    @Test
    void copyMoveAndAddHandleIdentityAndEmptyGraphs() {
        update("INSERT DATA { ex:s ex:p ex:o . GRAPH ex:g { ex:a ex:p ex:b } }");
        update("ADD DEFAULT TO GRAPH ex:g");
        assertEquals(2, connection.size(iri("g")));
        update("COPY GRAPH ex:g TO DEFAULT; MOVE DEFAULT TO GRAPH ex:h");
        assertEquals(0, connection.size((Resource) null));
        assertEquals(2, connection.size(iri("h")));
        update("MOVE GRAPH ex:g TO GRAPH ex:g");
        assertEquals(2, connection.size(iri("g")));
        update("CREATE GRAPH ex:empty; COPY GRAPH ex:empty TO GRAPH ex:h");
        assertEquals(0, connection.size(iri("h")));
        assertTrue(storage.metadata().getContexts().contains(iri("h")));
    }

    @Test
    void failureRollsBackTheEntireRequestIncludingGraphNames() {
        update("INSERT DATA { ex:s ex:p ex:o }");
        assertThrows(QueryEvaluationException.class, () -> update(
                "CLEAR DEFAULT; CREATE GRAPH ex:g; INSERT DATA { GRAPH ex:g { ex:a ex:p ex:b } }; COPY GRAPH ex:missing TO DEFAULT"));
        assertEquals(1, connection.size());
        assertTrue(storage.metadata().getContexts().isEmpty());
    }

    @Test
    void silentFailureAllowsFollowingOperations() {
        update("CLEAR SILENT GRAPH ex:missing; DROP SILENT GRAPH ex:missing; "
                + "COPY SILENT GRAPH ex:missing TO DEFAULT; CREATE GRAPH ex:g; CREATE SILENT GRAPH ex:g; "
                + "INSERT DATA { ex:s ex:p ex:o }");
        assertEquals(1, connection.size());
        assertEquals(Set.of(iri("g")), storage.metadata().getContexts());
    }

    @Test
    void explicitTransactionIsIsolatedFromOtherConnectionsOnTheSameThread() {
        try (var other = repository.getConnection()) {
            connection.begin();
            update("INSERT DATA { ex:s ex:p ex:o }; CREATE GRAPH ex:g");
            assertTrue(connection.isActive());
            assertEquals(1, connection.size());
            assertEquals(0, other.size());
            assertTrue(storage.metadata().getContexts().isEmpty());
            connection.commit();
            assertEquals(1, other.size());
            assertEquals(Set.of(iri("g")), storage.metadata().getContexts());
        }
    }

    @Test
    void failedUpdateRestoresItsSavepointInsideAnExplicitTransaction() {
        connection.begin();
        update("INSERT DATA { ex:s ex:p ex:o }");
        assertThrows(QueryEvaluationException.class, () -> update("CLEAR ALL; COPY GRAPH ex:missing TO DEFAULT"));
        assertTrue(connection.isActive());
        assertEquals(1, connection.size());
        connection.rollback();
        assertEquals(0, connection.size());
    }

    @Test
    void loadUsesSourceBaseAndFreshBlankNodesAndHonorsSilent() throws IOException {
        Path file = directory.resolve("data.ttl");
        Files.writeString(file, "<relative> <urn:p> _:b . _:b <urn:p> <urn:o> .");
        update("LOAD <" + file.toUri() + "> INTO GRAPH ex:g; LOAD <" + file.toUri() + "> INTO GRAPH ex:h");
        assertEquals(4, connection.size());
        IRI subject = Values.factory().createIRI(directory.resolve("relative").toUri().toString());
        assertTrue(connection.hasStatement(subject, null, null, iri("g")),
                () -> "Expected " + subject + "; got " + storage.metadata().getSubjects());
        update("LOAD SILENT <" + directory.resolve("missing.ttl").toUri() + ">; INSERT DATA { ex:s ex:p ex:o }");
        assertEquals(5, connection.size());
        Files.writeString(file, "<urn:s> <urn:p> <urn:o> . <broken>");
        String malformedLoad = "LOAD <" + file.toUri() + ">";
        assertThrows(QueryEvaluationException.class, () -> update(malformedLoad));
        assertEquals(5, connection.size());
    }

    @Test
    void illegalDataAndDeleteTemplatesAreRejectedBeforeExecution() {
        assertThrows(QuerySyntaxException.class, () -> update("INSERT DATA { ?s ex:p ex:o }"));
        assertThrows(QuerySyntaxException.class, () -> update("DELETE { ex:s ex:p [ ex:q ex:o ] } WHERE {}"));
        assertThrows(QuerySyntaxException.class, () -> update("DELETE WHERE { _:b ex:p ex:o }"));
        assertThrows(QuerySyntaxException.class, () -> update("INSERT DATA { _:b ex:p ex:o }; INSERT DATA { _:b ex:p ex:o }"));
        assertEquals(0, connection.size());
    }

    @Test
    void legacyGraphBackendSupportsUpdatesAndFailureRecovery() {
        StorageManager legacy = GraphStorageManager.builder().graph(Graph.create()).valueFactory(Values.factory()).build();
        try (Repository repo = Repositories.create(legacy); var conn = repo.getConnection()) {
            conn.prepareUpdate(PREFIX + "CREATE GRAPH ex:g; INSERT DATA { GRAPH ex:g { ex:s ex:p ex:o } }").execute();
            assertEquals(1, conn.size());
            var failingUpdate = conn.prepareUpdate(PREFIX +
                    "CLEAR ALL; DROP GRAPH ex:g; COPY GRAPH ex:missing TO DEFAULT");
            assertThrows(QueryEvaluationException.class, failingUpdate::execute);
            assertEquals(1, conn.size());
            assertTrue(legacy.metadata().getContexts().contains(iri("g")));
            conn.prepareUpdate(PREFIX + "DROP ALL").execute();
            assertEquals(0, conn.size());
        }
    }

    @Test
    void eachOperationRetainsItsPrefixAndBaseScope() {
        connection.prepareUpdate("BASE <http://first.example/> PREFIX ex: <http://first.example/> "
                + "INSERT DATA { ex:s ex:p <object> }; "
                + "BASE <http://second.example/> PREFIX ex: <http://second.example/> "
                + "INSERT { ex:s ex:p <object> } WHERE {}").execute();
        assertEquals(2, connection.size());
        assertTrue(connection.hasStatement(Values.factory().createIRI("http://first.example/s"), null,
                Values.factory().createIRI("http://first.example/object")));
        assertTrue(connection.hasStatement(Values.factory().createIRI("http://second.example/s"), null,
                Values.factory().createIRI("http://second.example/object")));
    }

    private void update(String text) {
        connection.prepareUpdate(PREFIX + text).execute();
    }

    private static IRI iri(String local) {
        return Values.factory().createIRI("http://example.org/" + local);
    }
}
