package fr.inria.corese.core.next.data.impl.io.parser.support;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.impl.io.serializer.rdfc10.RDFC10Canonicalizer;
import fr.inria.corese.core.next.data.impl.io.serializer.rdfc10.RDFC10SerializerOptions;
import fr.inria.corese.core.next.storage.Storages;
import fr.inria.corese.core.next.storage.impl.model.StorageModel;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
     * Compares canonical RDF datasets, preserving triples, graph contexts and blank-node
     * structure while allowing blank-node identifiers to differ after parsing.
     */
    protected void assertModelsIsomorphic(Model original, Model deserialized) {
        assertEquals(original.size(), deserialized.size(), "Model sizes must match");
        RDFC10SerializerOptions options = RDFC10SerializerOptions.defaultConfig();
        RDFC10Canonicalizer canonicalizer = new RDFC10Canonicalizer(
                options.getHashAlgorithm(), options.getPermutationLimit(),
                options.getDepthFactor(), Values.factory());
        assertEquals(canonicalizer.canonicalize(original), canonicalizer.canonicalize(deserialized),
                "Models must preserve RDF dataset structure, including graph contexts");
    }

    /**
     * Creates a test model using the default native memory backend.
     *
     * @return a new Model instance backed by memory storage
     */
    protected Model createTestModel() {
        return StorageModel.builder()
                .storage(Storages.create())
                .valueFactory(valueFactory)
                .build();
    }

}
