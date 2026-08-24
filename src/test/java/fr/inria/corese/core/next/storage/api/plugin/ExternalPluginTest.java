package fr.inria.corese.core.next.storage.api.plugin;

import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.impl.adapter.CoreseValueFactory;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for loading and using external plugins from JAR resources.
 */
class ExternalPluginTest {

    private static final String PLUGIN_JAR_PATH = "storage-plugin/demo-storage-plugin-1.0.0.jar";

    @Test
    @DisplayName("External plugin should load from JAR and perform storage operations")
    void testExternalPluginLoadingAndUsage() throws Exception {
        // 1. Locate and load the external plugin from resources
        File jarFile = getResourceFile();
        assertTrue(jarFile.exists(), "Plugin JAR file should exist");

        int countLoaded = ExternalPluginLoader.loadPluginsFromJar(jarFile);
        assertTrue(countLoaded > 0, "At least one plugin should be loaded from the JAR");

        // 2. Create StorageConfig with type="demo"
        StorageConfig config = StorageConfig.builder()
                .property("type", "memory")
                .build();

        // 3. Create StorageManager via StoragePluginManager
        StorageManager storage = StoragePluginManager.create(config);
        assertNotNull(storage, "StorageManager created by plugin should not be null");

        try {
            // 4. Create test statement
            ValueFactory vf = new CoreseValueFactory();
            IRI alice = vf.createIRI("http://example.org/Alice");
            IRI knows = vf.createIRI("http://xmlns.com/foaf/0.1/knows");
            IRI bob = vf.createIRI("http://example.org/Bob");
            Statement stmt = vf.createStatement(alice, knows, bob);

            // 5. Insert statement
            storage.getMutationOperations().insertStatement(stmt);

            // 6. Query count and existence
            long count = storage.getQueryOperations().count(StatementPattern.builder().build());
            assertEquals(1, count, "Expected exactly 1 statement in storage");

            boolean exists = storage.getQueryOperations().exists(StatementPattern.builder().subject(alice).build());
            assertTrue(exists, "Statement with subject Alice should exist");

        } finally {
            // 7. Gracefully shutdown the storage
            storage.getLifecycle().shutdown();
        }
    }

    private File getResourceFile() {
        URL resourceUrl = getClass().getClassLoader().getResource(PLUGIN_JAR_PATH);
        assertNotNull(resourceUrl, "Resource should be found at: " + PLUGIN_JAR_PATH);
        try {
            return new File(resourceUrl.toURI());
        } catch (Exception e) {
            fail("Failed to resolve resource URL to URI", e);
            return null;
        }
    }
}
