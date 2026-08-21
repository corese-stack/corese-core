package fr.inria.corese.core.next.storage.api.plugin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StoragePluginManager.
 */
class StoragePluginManagerTest {

    @BeforeEach
    void setUp() {
        StoragePluginManager.reload();
    }

    @Test
    @DisplayName("Should discover available plugins")
    void shouldDiscoverAvailablePlugins() {
        List<StoragePlugin> plugins = StoragePluginManager.getAvailablePlugins();

        assertNotNull(plugins);
        assertTrue(plugins.size() >= 2);
    }

    @Test
    @DisplayName("Should find Graph plugin")
    void shouldFindGraphPlugin() {
        Optional<StoragePlugin> plugin = StoragePluginManager.findPlugin("graph");

        assertTrue(plugin.isPresent());
        assertEquals("graph", plugin.get().getName());
    }

    @Test
    @DisplayName("Should find Memory plugin")
    void shouldFindMemoryPlugin() {
        Optional<StoragePlugin> plugin = StoragePluginManager.findPlugin("memory");

        assertTrue(plugin.isPresent());
        assertEquals("memory", plugin.get().getName());
    }
}
