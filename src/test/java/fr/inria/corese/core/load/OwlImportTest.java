package fr.inria.corese.core.load;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.util.Property;
import fr.inria.corese.core.util.Property.Value;

/**
 * Unit tests for OWL imports functionality across different RDF formats.
 * 
 * These tests verify that the owl:imports statement is correctly processed
 * when OWL auto-import is enabled or disabled.
 */
public class OwlImportTest {

    private static final String RESOURCE_FOLDER = OwlImportTest.class.getResource("/data-test/owl_imports/").getPath();

    @Before
    public void setUp() {
        // Ensure OWL auto-import is disabled before starting tests
        Property.set(Value.OWL_AUTO_IMPORT, false);
    }

    /**
     * Test OWL imports handling in Rdf when auto-import is disabled.
     */
    @Test
    public void testRdfImportDisabled() throws LoadException {
        int graphSize = testImport("test_owl_import.rdf", false);
        assertEquals(8, graphSize);
    }

    /**
     * Test OWL imports handling in Rdf when auto-import is enabled.
     */
    @Test
    public void testRdfImportEnabled() throws LoadException {
        int graphSize = testImport("test_owl_import.rdf", true);
        assertEquals(628, graphSize);
    }

    /**
     * Test OWL imports handling in Turtle when auto-import is disabled.
     */
    @Test
    public void testTurtleImportDisabled() throws LoadException {
        int graphSize = testImport("test_owl_import.ttl", false);
        assertEquals(8, graphSize);
    }

    /**
     * Test OWL imports handling in Turtle when auto-import is enabled.
     */
    @Test
    public void testTurtleImportEnabled() throws LoadException {
        int graphSize = testImport("test_owl_import.ttl", true);
        assertEquals(628, graphSize);
    }

    /**
     * Test OWL imports handling in TriG when auto-import is disabled.
     */
    @Test
    public void testTrigImportDisabled() throws LoadException {
        int graphSize = testImport("test_owl_import.trig", false);
        assertEquals(8, graphSize);
    }

    /**
     * Test OWL imports handling in TriG when auto-import is enabled.
     */
    @Test
    public void testTrigImportEnabled() throws LoadException {
        int graphSize = testImport("test_owl_import.trig", true);
        assertEquals(628, graphSize);
    }

    /**
     * Test OWL imports handling in N-Triples when auto-import is disabled.
     */
    @Test
    public void testNTriplesImportDisabled() throws LoadException {
        int graphSize = testImport("test_owl_import.nt", false);
        assertEquals(8, graphSize);
    }

    /**
     * Test OWL imports handling in N-Triples when auto-import is enabled.
     */
    @Test
    public void testNTriplesImportEnabled() throws LoadException {
        int graphSize = testImport("test_owl_import.nt", true);
        assertEquals(628, graphSize);
    }

    /**
     * Test OWL imports handling in N-Quads when auto-import is disabled.
     */
    @Test
    public void testNQuadsImportDisabled() throws LoadException {
        int graphSize = testImport("test_owl_import.nq", false);
        assertEquals(8, graphSize);
    }

    /**
     * Test OWL imports handling in N-Quads when auto-import is enabled.
     */
    @Test
    public void testNQuadsImportEnabled() throws LoadException {
        int graphSize = testImport("test_owl_import.nq", true);
        assertEquals(628, graphSize);
    }

    /**
     * Test OWL imports handling in JSON-LD when auto-import is disabled.
     */
    @Test
    public void testJsonLdImportDisabled() throws LoadException {
        int graphSize = testImport("test_owl_import.jsonld", false);
        assertEquals(8, graphSize);
    }

    /**
     * Test OWL imports handling in JSON-LD when auto-import is enabled.
     */
    @Test
    public void testJsonLdImportEnabled() throws LoadException {
        int graphSize = testImport("test_owl_import.jsonld", true);
        assertEquals(628, graphSize);
    }

    /**
     * Test OWL imports handling in RDFa when auto-import is disabled.
     */
    @Test
    public void testRdfaImportDisabled() throws LoadException {
        int graphSize = testImport("test_owl_import.html", false);
        assertEquals(8, graphSize);
    }

    /**
     * Test OWL imports handling in RDFa when auto-import is enabled.
     */
    @Test
    public void testRdfaImportEnabled() throws LoadException {
        int graphSize = testImport("test_owl_import.html", true);
        assertEquals(628, graphSize);
    }

    /**
     * Helper method to load an RDF file with OWL imports and verify the number of
     * triples in the graph.
     *
     * @param fileName     The name of the test file.
     * @param enableImport Whether OWL auto-import should be enabled.
     * @return The number of triples in the graph after loading.
     * @throws LoadException If loading fails.
     */
    private int testImport(String fileName, boolean enableImport) throws LoadException {
        Property.set(Value.OWL_AUTO_IMPORT, enableImport);

        Graph graph = Graph.create();
        Load loader = Load.create(graph);

        String resourcePath = RESOURCE_FOLDER + fileName;

        loader.parse(resourcePath);
        return graph.size();
    }
}
