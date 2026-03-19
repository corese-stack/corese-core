package fr.inria.corese.core.next.data.impl.io.util;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.next.data.api.Model;
import fr.inria.corese.core.next.data.api.ValueFactory;
import fr.inria.corese.core.next.data.impl.StorageModel;
import fr.inria.corese.core.next.data.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.storagemanager.api.plugin.StoragePluginManager;
import fr.inria.corese.core.next.storagemanager.api.support.config.StorageConfig;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for parser and circular (round-trip) integration tests.
 */
public abstract class ParserTestBase {

    protected ValueFactory valueFactory;

    @BeforeEach
    void setUpBase() {
        valueFactory = new CoreseAdaptedValueFactory();
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
                .storage(StoragePluginManager.create(config))
                .valueFactory(valueFactory)
                .build();
    }

}