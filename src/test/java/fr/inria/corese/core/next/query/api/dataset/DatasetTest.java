package fr.inria.corese.core.next.query.api.dataset;

import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.Values;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatasetTest {

    private final ValueFactory values = Values.factory();

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

    @Test
    void ofFactoryAndCollectionBuilderMethodsCreateExpectedDatasets() {
        IRI g1 = iri("urn:g1");
        IRI g2 = iri("urn:g2");
        IRI n1 = iri("urn:n1");

        Dataset datasetOf = Dataset.of(g1, g2);
        assertEquals(Set.of(g1, g2), datasetOf.getDefaultGraphs());
        assertTrue(datasetOf.getNamedGraphs().isEmpty());

        Dataset datasetEmpty = Dataset.of();
        assertSame(Dataset.empty(), datasetEmpty);

        Dataset datasetCollections = Dataset.builder()
                .defaultGraphs(java.util.List.of(g1, g2))
                .namedGraphs(java.util.List.of(n1))
                .build();
        assertEquals(Set.of(g1, g2), datasetCollections.getDefaultGraphs());
        assertEquals(Set.of(n1), datasetCollections.getNamedGraphs());
    }

    private IRI iri(String value) {
        return values.createIRI(value);
    }
}
