package fr.inria.corese.core.next.data.impl.io.parser.support;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.BNode;
import fr.inria.corese.core.next.storage.Storages;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import fr.inria.corese.core.next.storage.impl.model.StorageModel;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Base class for parser and circular (round-trip) integration tests.
 */
public abstract class ParserTestBase {

    protected ValueFactory valueFactory;

    @BeforeEach
    void setUpBase() {
        valueFactory = Values.factory();
    }

    /**
     * Asserts that two models are graph-isomorphic: same size, all non-bnode triples
     * match exactly, and bnode-involving triples are accounted for by size equality.
     *
     * <p>Use this instead of {@code assertEquals(model1, model2)} when models may
     * contain blank nodes, since re-parsed blank nodes receive fresh IDs that differ
     * from the originals even when the graph structure is identical.</p>
     */
    protected void assertModelsIsomorphic(Model original, Model deserialized) {
        assertEquals(original.size(), deserialized.size(), "Model sizes must match");
        for (Statement stmt : original) {
            boolean subjectIsBNode = stmt.getSubject() instanceof BNode;
            boolean objectIsBNode = stmt.getObject() instanceof BNode;
            if (!subjectIsBNode && !objectIsBNode) {
                assertTrue(
                        deserialized.contains(stmt.getSubject(), stmt.getPredicate(), stmt.getObject()),
                        "Non-bnode triple missing from deserialized model: " + stmt);
            }
        }
    }

    /**
     * Creates a test model using the production Graph backend.
     *
     * @return a new Model instance backed by Graph storage
     */
    protected Model createTestModel() {
        Graph graph = Graph.create();

        StorageConfig config = StorageConfig.builder()
                .property("type", "graph")
                .property("graph", graph)
                .property("valueFactory", valueFactory)
                .build();

        return StorageModel.builder()
                .storage(Storages.create(config))
                .valueFactory(valueFactory)
                .build();
    }

}
