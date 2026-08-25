package fr.inria.corese.core.next.query.api.dataset;

import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.impl.adapter.CoreseValueFactory;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatasetTest {

    private final ValueFactory values = new CoreseValueFactory();

    @Test
    void emptyDatasetIsSharedAndImmutable() {
        assertSame(Dataset.empty(), Dataset.builder().build());
        assertTrue(Dataset.empty().isEmpty());
        Set<IRI> defaultGraphs = Dataset.empty().getDefaultGraphs();
        IRI graphIri = iri("urn:graph");
        assertThrows(UnsupportedOperationException.class,
                () -> defaultGraphs.add(graphIri));
    }

    @Test
    void builderCreatesAnImmutableOrderedSnapshot() {
        IRI defaultGraph = iri("urn:default");
        IRI namedGraph = iri("urn:named");

        Dataset dataset = Dataset.builder()
                .defaultGraph(defaultGraph)
                .defaultGraph(defaultGraph)
                .namedGraph(namedGraph)
                .build();

        assertEquals(Set.of(defaultGraph), dataset.getDefaultGraphs());
        assertEquals(Set.of(namedGraph), dataset.getNamedGraphs());
        Set<IRI> namedGraphs = dataset.getNamedGraphs();
        assertThrows(UnsupportedOperationException.class,
                namedGraphs::clear);
    }

    private IRI iri(String value) {
        return values.createIRI(value);
    }
}
