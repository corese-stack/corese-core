package fr.inria.corese.core.next.data.impl.io.parser.support;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.storage.impl.model.StorageModel;
import fr.inria.corese.core.next.data.impl.adapter.CoreseValueFactory;
import fr.inria.corese.core.next.storage.Storages;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for parser and circular (round-trip) integration tests.
 */
public abstract class ParserTestBase {

    protected ValueFactory valueFactory;

    @BeforeEach
    void setUpBase() {
        valueFactory = new CoreseValueFactory();
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
