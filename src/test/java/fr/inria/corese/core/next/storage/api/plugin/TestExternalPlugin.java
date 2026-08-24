package fr.inria.corese.core.next.storage.api.plugin;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.impl.adapter.CoreseValueFactory;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;

/**
 * Test class for loading and using external plugins from resources.
 *
 */
public class TestExternalPlugin {

    private static final Logger logger = LoggerFactory.getLogger(TestExternalPlugin.class);

    /**
     * Path to the plugin JAR file in test resources.
     */
    private static final String PLUGIN_JAR_PATH = "storage-plugin/demo-storage-plugin-1.0.0.jar";

    /**
     * Main entry point for the test.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        logger.info("=== External Plugin Test (from resources) ===");

        try {
            // Load the external plugin from resources
            loadExternalPlugin();

            // Test the plugin using StoragePluginManager
            testDemoPlugin();

            logger.info("TEST PASSED");

        } catch (Exception e) {
            logger.error("TEST FAILED", e);
            System.exit(1);
        }
    }

    /**
     * Loads the demo plugin from the test resources directory.
     *
     * <p>This method locates the JAR file in {@code src/test/resources/storage-plugin/}
     * and loads it using {@link ExternalPluginLoader}.</p>
     *
     * @throws Exception if loading fails
     */
    private static void loadExternalPlugin() throws Exception {
        logger.info("Loading plugin from resources: {}", PLUGIN_JAR_PATH);

        // Get the JAR file from resources
        File jarFile = getResourceFile();

        logger.info("Found JAR: {} ({} bytes)", jarFile.getName(), jarFile.length());

        // Load using ExternalPluginLoader utility
        int count = ExternalPluginLoader.loadPluginsFromJar(jarFile);

        logger.info("Loaded {} plugin(s) from JAR", count);

        if (count == 0) {
            throw new IllegalStateException("No plugins found in JAR: " + jarFile.getName());
        }
    }

    /**
     * Gets a file from the test resources directory.
     *
     * @return the File object
     * @throws IllegalStateException if the resource is not found
     */
    private static File getResourceFile() {
        URL resourceUrl = TestExternalPlugin.class.getClassLoader().getResource(TestExternalPlugin.PLUGIN_JAR_PATH);

        if (resourceUrl == null) {
            throw new IllegalStateException(
                    "Resource not found: " + TestExternalPlugin.PLUGIN_JAR_PATH + "\n" +
                            "Expected location: src/test/resources/" + TestExternalPlugin.PLUGIN_JAR_PATH
            );
        }

        try {
            return new File(resourceUrl.toURI());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load resource: " + TestExternalPlugin.PLUGIN_JAR_PATH, e);
        }
    }

    /**
     * Tests the demo plugin with basic operations.
     *
     */
    private static void testDemoPlugin() {
        logger.info("Starting plugin tests");

        // 1. Create StorageConfig with type="demo"
        StorageConfig config = StorageConfig.builder()
                .property("type", "demo")
                .build();

        // 2. Create StorageManager via StoragePluginManager
        StorageManager storage = StoragePluginManager.create(config);
        logger.info("StorageManager created via StoragePluginManager");

        // 3. Create test data
        ValueFactory vf = new CoreseValueFactory();
        IRI alice = vf.createIRI("http://example.org/Alice");
        IRI knows = vf.createIRI("http://xmlns.com/foaf/0.1/knows");
        IRI bob = vf.createIRI("http://example.org/Bob");
        Statement stmt = vf.createStatement(alice, knows, bob);

        // 4. Insert statement
        storage.getMutationOperations().insertStatement(stmt);
        logger.info("Statement inserted");

        // 5. Count statements
        long count = storage.getQueryOperations().count(
                StatementPattern.builder().build()
        );
        logger.info("Count: {} statement(s)", count);

        // 6. Check if statement exists
        boolean exists = storage.getQueryOperations().exists(
                StatementPattern.builder().subject(alice).build()
        );
        logger.info("Exists (alice): {}", exists);

        // 7. Verify results
        if (count != 1) {
            throw new AssertionError("Expected count=1, got count=" + count);
        }
        if (!exists) {
            throw new AssertionError("Expected exists=true, got exists=false");
        }

        // 8. Shutdown the storage
        storage.getLifecycle().shutdown();
        logger.info("StorageManager shut down");

        logger.info("Tests completed successfully");
    }
}