package fr.inria.corese.core.next.data.impl.model;

import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.namespace.Namespace;
import fr.inria.corese.core.next.data.api.support.model.AbstractModel;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.impl.adapter.CoreseValueFactory;
import fr.inria.corese.core.next.data.impl.io.parser.support.ParserTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FilteredModelTest extends ParserTestBase {

    private final ValueFactory vf = new CoreseValueFactory();
    private AbstractModel baseModel;
    private Resource subjectFilter;
    private IRI predicateFilter;
    private Value objectFilter;
    private TestFilteredModel filteredModel;

    private static class TestFilteredModel extends FilteredModel {
        boolean removeCalled = false;

        TestFilteredModel(AbstractModel model, Resource subjectFilter, IRI predicateFilter, Value objectFilter, Resource... contextFilters) {
            super(model, subjectFilter, predicateFilter, objectFilter, contextFilters);
        }

        @Override
        public Iterator<Statement> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        protected void removeFilteredTermIteration(Iterator<Statement> iterator, Resource subject, IRI predicate, Value object, Resource... contexts) {
            removeCalled = true;
        }
    }

    @BeforeEach
    void setUp() {
        baseModel = (AbstractModel) createTestModel();
        subjectFilter = vf.createIRI("http://example.org/s1");
        predicateFilter = vf.createIRI("http://example.org/p1");
        objectFilter = vf.createLiteral("o1");
        filteredModel = new TestFilteredModel(baseModel, subjectFilter, predicateFilter, objectFilter);
    }

    @Test
    void testConstructorNullChecks() {
        assertThrows(NullPointerException.class, () -> new TestFilteredModel(null, subjectFilter, predicateFilter, objectFilter));
        assertThrows(NullPointerException.class, () -> new TestFilteredModel(baseModel, subjectFilter, predicateFilter, objectFilter, (Resource[]) null));
    }

    @Test
    void testAddMatchingStatement() {
        baseModel.add(subjectFilter, predicateFilter, objectFilter);

        assertTrue(filteredModel.add(subjectFilter, predicateFilter, objectFilter));
        assertTrue(baseModel.contains(subjectFilter, predicateFilter, objectFilter));
    }

    @Test
    void testAddNonMatchingStatementThrows() {
        Resource otherSubject = vf.createIRI("http://example.org/other");

        assertThrows(IllegalArgumentException.class, () -> filteredModel.add(otherSubject, predicateFilter, objectFilter));
    }

    @Test
    void testContainsMatchingAndNonMatching() {
        baseModel.add(subjectFilter, predicateFilter, objectFilter);
        Resource otherSubject = vf.createIRI("http://example.org/other");

        assertTrue(filteredModel.contains(subjectFilter, predicateFilter, objectFilter));
        assertFalse(filteredModel.contains(otherSubject, predicateFilter, objectFilter));
    }

    @Test
    void testRemoveMatchingAndNonMatching() {
        baseModel.add(subjectFilter, predicateFilter, objectFilter);
        Resource otherSubject = vf.createIRI("http://example.org/other");

        assertFalse(filteredModel.remove(otherSubject, predicateFilter, objectFilter));
        assertTrue(filteredModel.remove(subjectFilter, predicateFilter, objectFilter));
        assertFalse(baseModel.contains(subjectFilter, predicateFilter, objectFilter));
    }

    @Test
    void testFilterNonMatchingReturnsEmptyModel() {
        Resource otherSubject = vf.createIRI("http://example.org/other");

        assertInstanceOf(EmptyModel.class, filteredModel.filter(otherSubject, predicateFilter, objectFilter));
    }

    @Test
    void testRemoveTermIterationValidation() {
        Resource otherSubject = vf.createIRI("http://example.org/other");
        Iterator<Statement> dummyIterator = Collections.emptyIterator();

        assertThrows(IllegalStateException.class, () -> filteredModel.removeTermIteration(dummyIterator, otherSubject, predicateFilter, objectFilter));

        assertDoesNotThrow(() -> filteredModel.removeTermIteration(dummyIterator, subjectFilter, predicateFilter, objectFilter));
        assertTrue(filteredModel.removeCalled);
    }

    @Test
    void testNamespaceOperationsDelegate() {
        filteredModel.setNamespace("ex", "http://example.org/");
        Optional<Namespace> ns = filteredModel.getNamespace("ex");

        assertTrue(ns.isPresent());
        assertEquals("ex", ns.get().getPrefix());
        assertEquals("http://example.org/", ns.get().getNamespace());

        assertEquals(baseModel.getNamespaces(), filteredModel.getNamespaces());

        Optional<Namespace> removed = filteredModel.removeNamespace("ex");
        assertTrue(removed.isPresent());
        assertFalse(filteredModel.getNamespace("ex").isPresent());
    }

    @Test
    void testSizeOnEmptyIterator() {
        assertEquals(0, filteredModel.size());
    }
}
