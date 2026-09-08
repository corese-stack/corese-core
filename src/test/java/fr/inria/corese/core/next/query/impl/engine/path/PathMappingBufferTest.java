package fr.inria.corese.core.next.query.impl.engine.path;

import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(5)
class PathMappingBufferTest {
    @Test
    void consumesProducedMappingsAndKeepsTheEndOfStream() throws Exception {
        PathMappingBuffer buffer = new PathMappingBuffer();
        Mapping mapping = Mapping.create();
        try (var executor = Executors.newSingleThreadExecutor()) {
            var producer = executor.submit(() -> {
                buffer.put(mapping, true);
                buffer.put(null, false);
            });
            assertTrue(buffer.hasNext());
            assertSame(mapping, buffer.next());
            assertFalse(buffer.hasNext());
            assertThrows(NoSuchElementException.class, buffer::next);
            assertFalse(buffer.hasNext());
            producer.get(2, TimeUnit.SECONDS);
        }
    }

    @Test
    void interruptionEndsIterationAndPreservesTheInterruptFlag() {
        PathMappingBuffer buffer = new PathMappingBuffer();
        Thread.currentThread().interrupt();
        try {
            assertThrows(NoSuchElementException.class, buffer::next);
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
