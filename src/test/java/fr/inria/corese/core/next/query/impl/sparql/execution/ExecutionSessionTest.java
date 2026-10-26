package fr.inria.corese.core.next.query.impl.sparql.execution;

import fr.inria.corese.core.next.storage.api.StorageManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class ExecutionSessionTest {

    @Test
    void sessionInitializesProducerAndEvalAndClosesProducer() {
        StorageManager storage = mock(StorageManager.class);
        ExecutionSession session = new ExecutionSession(storage, null);

        assertNotNull(session.producer());
        assertNotNull(session.eval());

        assertDoesNotThrow(session::close);
    }

    @Test
    void rejectsNullStorageManager() {
        assertThrows(NullPointerException.class, () -> new ExecutionSession(null, null));
    }
}
