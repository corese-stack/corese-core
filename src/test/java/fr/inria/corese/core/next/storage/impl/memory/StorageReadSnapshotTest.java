package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class StorageReadSnapshotTest {
    @Test
    void snapshotsCaptureContentsGraphRegistryAndVersionUntilClosed() {
        try (var storage = MemoryStorageManager.builder().build()) {
            storage.mutations().addAll(StorageBenchmark.data(20, 0, false));
            long version = storage.queries().getDataVersion();
            try (var view = storage.openReadSnapshot()) {
                storage.mutations().clear();
                storage.mutations().createGraph(Values.factory().createIRI("urn:new-graph"));
                assertEquals(version, view.queries().getDataVersion());
                assertTrue(view.metadata().getContexts().isEmpty());
                assertEquals(120, view.queries().count(StatementPattern.matchAll()));
                try (var stream = view.queries().find(StatementPattern.matchAll())) {
                    assertEquals(120, stream.count());
                }
                assertThrows(IllegalStateException.class, () -> view.mutations().clear());
                assertThrows(IllegalStateException.class, () -> view.transactions().beginTransaction());
                try (var nested = view.openReadSnapshot()) {
                    assertEquals(120, nested.queries().count(StatementPattern.matchAll()));
                }
            }
            assertEquals(0, storage.queries().count(StatementPattern.matchAll()));
        }
    }

    @Test
    void querySnapshotClosureCannotFinishTheSourcesTransaction() {
        try (var storage = MemoryStorageManager.builder().build(); var transaction = storage.transactions().beginTransaction()) {
            storage.mutations().addAll(StorageBenchmark.data(20, 0, false));
            try (var snapshot = storage.openReadSnapshot()) {
                assertEquals(120, snapshot.queries().count(StatementPattern.matchAll()));
            }
            assertTrue(transaction.isActive());
            assertEquals(120, storage.queries().count(StatementPattern.matchAll()));
            transaction.rollback();
            assertEquals(0, storage.queries().count(StatementPattern.matchAll()));
        }
    }

    @Test
    void snapshotReadsAreThreadConfinedButCancellationCanReleaseThemElsewhere() throws InterruptedException {
        try (var storage = MemoryStorageManager.builder().build(); StorageManager snapshot = storage.openReadSnapshot()) {
            var failure = new AtomicReference<Throwable>();
            var reader = Thread.ofVirtual().start(() -> {
                try {
                    snapshot.queries().count(StatementPattern.matchAll());
                } catch (IllegalStateException expected) {
                    failure.set(expected);
                }
            });
            reader.join();
            assertInstanceOf(IllegalStateException.class, failure.get());
            var closer = Thread.ofVirtual().start(snapshot::close);
            closer.join();
            assertFalse(snapshot.isOpen());
            assertTrue(storage.isOpen());
        }
    }
}
