package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StorageDataVersionTest {
    @Test
    void graphRegistryChangesInvalidateEvenWhenThereAreNoStatements() {
        try (var storage = MemoryStorageManager.builder().build()) {
            long empty = storage.queries().getDataVersion();
            storage.mutations().clear();
            assertEquals(empty, storage.queries().getDataVersion());
            assertTrue(storage.mutations().createGraph(iri("graph")));
            long created = storage.queries().getDataVersion();
            assertNotEquals(empty, created);
            assertFalse(storage.mutations().createGraph(iri("graph")));
            assertEquals(created, storage.queries().getDataVersion());
            assertTrue(storage.mutations().dropGraph(iri("graph")));
            assertNotEquals(created, storage.queries().getDataVersion());
        }
    }

    @Test
    void effectiveBatchDeleteAndClearChangesHaveNewTokensButReadsDoNot() {
        try (var storage = MemoryStorageManager.builder().build()) {
            long empty = storage.queries().getDataVersion();
            storage.mutations().addAll(List.of(statement("a"), statement("b")));
            long loaded = storage.queries().getDataVersion();
            assertNotEquals(empty, loaded);
            storage.mutations().addAll(List.of(statement("a"), statement("b")));
            try (var reader = storage.queries().find(StatementPattern.matchAll())) {
                assertEquals(2, reader.count());
            }
            assertEquals(loaded, storage.queries().getDataVersion());
            assertEquals(1, storage.mutations().remove(StatementPattern.of(iri("a"), null, null)));
            long deleted = storage.queries().getDataVersion();
            assertNotEquals(loaded, deleted);
            assertEquals(0, storage.mutations().remove(StatementPattern.of(iri("absent"), null, null)));
            assertEquals(deleted, storage.queries().getDataVersion());
            storage.mutations().clear();
            assertNotEquals(deleted, storage.queries().getDataVersion());
        }
    }

    @Test
    void failedStreamingBatchPreservesTheTokenAndData() {
        try (var storage = MemoryStorageManager.builder().build()) {
            long version = storage.queries().getDataVersion();
            Iterable<Statement> failing = () -> new Iterator<>() {
                private boolean first = true;
                @Override public boolean hasNext() { return true; }
                @Override public Statement next() {
                    if (!first) throw new IllegalStateException("Broken source");
                    first = false;
                    return statement("staged");
                }
            };
            assertThrows(RuntimeException.class, () -> storage.mutations().addAll(failing));
            assertEquals(version, storage.queries().getDataVersion());
            assertEquals(0, storage.queries().count(StatementPattern.matchAll()));
        }
    }

    @Test
    void transactionVersionsDescribeItsOwnViewAndConflictingCommitsRemainRejected() {
        try (var root = MemoryStorageManager.builder().build();
             var a = root.openSession(); var b = root.openSession()) {
            long initial = root.queries().getDataVersion();
            try (var transaction = a.transactions().beginTransaction()) {
                assertEquals(initial, a.queries().getDataVersion());
                a.mutations().add(statement("uncommitted"));
                long ownWrite = a.queries().getDataVersion();
                assertNotEquals(initial, ownWrite);
                assertEquals(initial, b.queries().getDataVersion());
                b.mutations().add(statement("committed"));
                long committed = b.queries().getDataVersion();
                assertNotEquals(initial, committed);
                assertEquals(ownWrite, a.queries().getDataVersion());
                assertThrows(RuntimeException.class, transaction::commit);
                assertEquals(ownWrite, a.queries().getDataVersion());
                transaction.rollback();
                assertEquals(committed, a.queries().getDataVersion());
                assertEquals(1, a.queries().count(StatementPattern.matchAll()));
            }
        }
    }

    private static IRI iri(String name) {
        return Values.factory().createIRI("urn:version:" + name);
    }

    private static Statement statement(String name) {
        return Values.factory().createStatement(iri(name), iri("p"), iri("o"));
    }
}
