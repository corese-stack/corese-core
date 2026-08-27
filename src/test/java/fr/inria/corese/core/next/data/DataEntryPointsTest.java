package fr.inria.corese.core.next.data;

import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.namespace.PrefixMapping;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.storage.Storages;
import fr.inria.corese.core.next.storage.StorageModels;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataEntryPointsTest {

    private final ValueFactory values = Values.factory();
    private final IRI subject = values.createIRI("urn:subject");
    private final IRI predicate = values.createIRI("urn:predicate");
    private final IRI object = values.createIRI("urn:object");
    private final IRI graph = values.createIRI("urn:graph");

    @Test
    void exposesAStableFactoryWithoutImplementationImports() {
        assertSame(values, Values.factory());
        assertEquals("urn:subject", values.createIRI("urn:subject").stringValue());
    }

    @Test
    void createsEmptyAndInitializedIndependentModels() {
        Statement statement = values.createStatement(subject, predicate, object);
        Model initialized = Models.create(List.of(statement));
        Model empty = Models.create();

        assertEquals(1, initialized.size());
        assertTrue(initialized.contains(statement));
        assertTrue(empty.isEmpty());

        initialized.clear();
        assertTrue(initialized.isEmpty());
        assertTrue(empty.isEmpty());
    }

    @Test
    void modelCopiesPreserveNamespaceMetadata() {
        Model source = Models.create();
        source.add(subject, predicate, object);
        source.setNamespace("ex", "https://example.org/");

        Model copy = Models.create(source);
        source.clear();
        source.removeNamespace("ex");

        assertEquals(1, copy.size());
        assertEquals("https://example.org/",
                copy.getNamespace("ex").orElseThrow().getNamespace());
    }

    @Test
    void modelContextSelectionDistinguishesAllGraphsFromTheDefaultGraph() {
        Model model = Models.create();
        model.add(subject, predicate, object);
        model.add(subject, predicate, object, graph);

        assertTrue(model.contains(subject, predicate, object));
        assertTrue(model.contains(subject, predicate, object, (Resource) null));
        assertEquals(2, model.filter(subject, predicate, object).size());
        assertEquals(1, model.filter(subject, predicate, object, (Resource) null).size());
    }

    @Test
    void termViewsSupportRemovalButRejectMeaninglessAdditions() {
        Model model = Models.create();
        model.add(subject, predicate, object);
        model.add(subject, predicate, values.createIRI("urn:other"), graph);

        List<?> invalidElements = List.of(values.createStatement(subject, predicate, object));
        assertFalse(model.subjects().containsAll(invalidElements));

        Set<Resource> subjects = model.subjects();
        IRI newSubject = values.createIRI("urn:new-subject");
        assertThrows(UnsupportedOperationException.class, () -> subjects.add(newSubject));

        Set<Resource> contexts = model.contexts();
        IRI newGraph = values.createIRI("urn:new-graph");
        assertThrows(UnsupportedOperationException.class, () -> contexts.add(newGraph));

        assertTrue(model.predicates().remove(predicate));
        assertTrue(model.isEmpty());
    }

    @Test
    void modelContractDoesNotPromiseJavaObjectSerialization() {
        assertFalse(Models.create() instanceof Serializable);
    }

    @Test
    void createsAModelBackedByCallerOwnedStorage() {
        try (var storage = Storages.create()) {
            Model model = StorageModels.create(storage);
            model.add(subject, predicate, object);

            assertTrue(model.contains(subject, predicate, object));
            assertTrue(storage.queries().contains(
                    fr.inria.corese.core.next.storage.api.model.StatementPattern.of(
                            subject, predicate, object)));
        }
    }

    @Test
    void canonicalizationIsAvailableWithoutImplementationImports() {
        Statement statement = values.createStatement(subject, predicate, object);

        assertEquals("<urn:subject> <urn:predicate> <urn:object> .",
                RdfCanonicalization.toNQuad(statement));
        assertEquals(List.of(statement), RdfCanonicalization.canonicalize(Models.create(List.of(statement))));
        assertEquals(List.of(statement), RdfCanonicalization.canonicalize(
                Models.create(List.of(statement)), RdfCanonicalization.HashAlgorithm.SHA_384));
    }

    @Test
    void createsEmptyDefaultAndIndependentPrefixMappings() {
        PrefixMapping empty = Prefixes.create();
        PrefixMapping defaults = Prefixes.createWithDefaults();

        assertTrue(empty.isEmpty());
        assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#",
                defaults.getNamespace("rdf"));

        defaults.setPrefix("ex", "https://example.org/");
        PrefixMapping copy = defaults.copy();
        copy.removePrefix("ex");

        assertTrue(defaults.hasPrefix("ex"));
        assertFalse(copy.hasPrefix("ex"));

        Set<?> namespaces = defaults.getNamespaceObjects();
        assertThrows(UnsupportedOperationException.class, namespaces::clear);
    }
}
