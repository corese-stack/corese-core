package fr.inria.corese.core.next.query.impl.dataset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CoreseDataset Tests")
class CoreseDatasetTest {

    @Test
    @DisplayName("Should start empty")
    void testInitialState() {
        CoreseDataset dataset = new CoreseDataset();

        assertTrue(dataset.getDefaultGraphs().isEmpty());
        assertTrue(dataset.getNamedGraphs().isEmpty());
    }

    @Test
    @DisplayName("Should add default and named graphs and return unmodifiable views")
    void testAddGraphs() {
        CoreseDataset dataset = new CoreseDataset();
        dataset.addDefaultGraph("http://example.org/default1");
        dataset.addNamedGraph("http://example.org/named1");

        Set<String> defaultGraphs = dataset.getDefaultGraphs();
        Set<String> namedGraphs = dataset.getNamedGraphs();

        assertEquals(1, defaultGraphs.size());
        assertTrue(defaultGraphs.contains("http://example.org/default1"));

        assertEquals(1, namedGraphs.size());
        assertTrue(namedGraphs.contains("http://example.org/named1"));

        assertThrows(UnsupportedOperationException.class, () -> defaultGraphs.add("http://example.org/default2"));
        assertThrows(UnsupportedOperationException.class, () -> namedGraphs.add("http://example.org/named2"));
    }

    @Test
    @DisplayName("clear() should remove all default and named graphs")
    void testClear() {
        CoreseDataset dataset = new CoreseDataset();
        dataset.addDefaultGraph("http://example.org/default1");
        dataset.addNamedGraph("http://example.org/named1");

        dataset.clear();

        assertTrue(dataset.getDefaultGraphs().isEmpty());
        assertTrue(dataset.getNamedGraphs().isEmpty());
    }
}
