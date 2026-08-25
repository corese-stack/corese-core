package fr.inria.corese.core.next.storage;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.lifecycle.LifecycleState;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StorageEntryPointsTest {

    @Test
    void createsOwnedReadyToUseStorage() {
        StorageManager storage = Storages.create();

        assertTrue(storage.isOpen());
        storage.close();
        storage.close();
        assertFalse(storage.isOpen());
        assertEquals(LifecycleState.SHUTDOWN, storage.lifecycle().getState());
    }

    @Test
    void batchDefaultsHaveSetSemantics() {
        ValueFactory values = Values.factory();
        IRI predicate = values.createIRI("urn:p");
        Statement first = values.createStatement(
                values.createIRI("urn:s1"), predicate, values.createLiteral("one"));
        Statement second = values.createStatement(
                values.createIRI("urn:s2"), predicate, values.createLiteral("two"));

        try (StorageManager storage = Storages.create()) {
            assertEquals(2, storage.mutations().addAll(List.of(first, second, first)));
            assertEquals(2, storage.queries().count(StatementPattern.matchAll()));
            assertEquals(2, storage.mutations().removeAll(List.of(first, second, first)));
            assertEquals(0, storage.queries().count(StatementPattern.matchAll()));
        }
    }

    @Test
    void rejectsMissingConfiguration() {
        assertThrows(NullPointerException.class, () -> Storages.create(null));
    }
}
