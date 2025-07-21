package fr.inria.corese.core.next.api.base;

import static org.junit.jupiter.api.Assertions.*;


import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.base.model.AbstractModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 * Test class for AbstractModel.
 * Uses an internal concrete implementation to test the default functionalities
 * provided by AbstractModel.
 */
class AbstractModelTest {

    private AbstractModel model;

    private Resource subject1;
    private Resource subject2;

    private IRI predicate1;
    private IRI predicate2;

    private Value object1;
    private Value object2;

    private Resource context1;
    private Resource context2;

    /**
     * Initialization method executed before each test.
     * Creates a new instance of ConcreteModel and initializes test objects (subjects, predicates, objects).
     */
    @BeforeEach
    void setUp() {
        model = new ConcreteModel();

        subject1 = new CreateResource("http://example.org/subject1");
        subject2 = new CreateResource("http://example.org/subject2");

        predicate1 = new CreateIRI("http://example.org/predicate1");
        predicate2 = new CreateIRI("http://example.org/predicate2");

        object1 = new CreateResource("http://example.org/object1");
        object2 = new CreateValue("literalValue");

        context1 = new CreateResource("http://example.org/context1");
        context2 = new CreateResource("http://example.org/context2");
    }


    /**
     * Tests the unmodifiable() method.
     */
    @Test
    @DisplayName("unmodifiable() should return a read-only model")
    void testUnmodifiable() {
        Model unmodifiable = model.unmodifiable();
        assertNotNull(unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.add(subject1, predicate1, object1));
    }

    /**
     * Tests the setNamespace(String prefix, String name) method.
     */
    @Test
    @DisplayName("setNamespace(String, String) should add or update a namespace")
    void testSetNamespaceWithStringArgs() {
        model.setNamespace("ex", "http://example.org/ns/");
        Optional<Namespace> ns = model.getNamespace("ex");
        assertTrue(ns.isPresent());
        assertEquals("ex", ns.get().getPrefix());
        assertEquals("http://example.org/ns/", ns.get().getName());

        Namespace existingNs = model.setNamespace("ex", "http://example.org/ns/");
        assertEquals("http://example.org/ns/", existingNs.getName());
        assertEquals(1, model.getNamespaces().size());

        model.setNamespace("ex", "http://example.com/newns/");
        Optional<Namespace> updatedNs = model.getNamespace("ex");
        assertTrue(updatedNs.isPresent());
        assertEquals("http://example.com/newns/", updatedNs.get().getName());
        assertEquals(1, model.getNamespaces().size());
    }

    /**
     * Tests the setNamespace(Namespace namespace) method.
     */
    @Test
    @DisplayName("setNamespace(Namespace) should add a namespace")
    void testSetNamespaceWithNamespaceObject() {
        Namespace newNs = new CreateNamespace("geosparql", "http://example.org/ont/geosparql#");
        model.setNamespace(newNs);

        Optional<Namespace> fetchedNs = model.getNamespace("geosparql");
        assertTrue(fetchedNs.isPresent());
        assertEquals("http://example.org/ont/geosparql#", fetchedNs.get().getName());
        assertEquals(1, model.getNamespaces().size());
    }

    /**
     * Tests the getNamespaces() method.
     */
    @Test
    @DisplayName("getNamespaces() should return all defined namespaces")
    void testGetNamespaces() {
        model.setNamespace("ex1", "http://example.org/ns1/");
        model.setNamespace("ex2", "http://example.org/ns2/");

        Set<Namespace> namespaces = model.getNamespaces();
        assertEquals(2, namespaces.size());

        assertTrue(namespaces.stream().anyMatch(n -> n.getPrefix().equals("ex1") && n.getName().equals("http://example.org/ns1/")));
        assertTrue(namespaces.stream().anyMatch(n -> n.getPrefix().equals("ex2") && n.getName().equals("http://example.org/ns2/")));
    }

    /**
     * Tests the removeNamespace() method.
     */
    @Test
    @DisplayName("removeNamespace() should remove a namespace and return an Optional of the removed namespace")
    void testRemoveNamespace() {
        Namespace testNs = new CreateNamespace("ex", "http://example.org/ns/");
        model.setNamespace("ex", "http://example.org/ns/");
        assertEquals(1, model.getNamespaces().size());

        Optional<Namespace> removedNs = model.removeNamespace("ex");
        assertTrue(removedNs.isPresent());

        // Comparison
        assertEquals(testNs.getPrefix(), removedNs.get().getPrefix());
        assertEquals(testNs.getName(), removedNs.get().getName());

        assertEquals(0, model.getNamespaces().size());
        assertFalse(model.getNamespace("ex").isPresent());

        Optional<Namespace> nonExistentNs = model.removeNamespace("nonExistent");
        assertFalse(nonExistentNs.isPresent());
    }

    /**
     * Tests the clearNamespaces() method.
     */
    @Test
    @DisplayName("clearNamespaces() should remove all namespaces")
    void testClearNamespaces() {
        model.setNamespace("ex1", "http://example.org/ns1/");
        model.setNamespace("ex2", "http://example.org/ns2/");
        assertEquals(2, model.getNamespaces().size());


        model.clear();
        assertTrue(model.getNamespaces().isEmpty());
    }

    /**
     * Tests adding a statement and its presence via contains(Statement).
     */
    @Test
    @DisplayName("add() and contains() should work for statements")
    void testAddAndContainsStatement() {
        Statement stmt = new SimpleStatement(subject1, predicate1, object1, null);

        assertFalse(model.contains(stmt));
        assertTrue(model.add(stmt));
        assertTrue(model.contains(stmt));
        assertEquals(1, model.size());
        assertTrue(model.add(stmt));
        assertEquals(2, model.size());
    }

    /**
     * Tests removing a statement via remove(Object).
     */
    @Test
    @DisplayName("remove() should remove an existing statement")
    void testRemoveStatement() {
        Statement stmt = new SimpleStatement(subject1, predicate1, object1, null);
        model.add(stmt);

        assertTrue(model.remove(stmt));
        assertFalse(model.contains(stmt));
        assertEquals(0, model.size());
        assertFalse(model.remove(stmt));
    }

    /**
     * Tests the isEmpty() method.
     */
    @Test
    @DisplayName("isEmpty() should reflect the empty or non-empty state of the model")
    void testIsEmpty() {
        assertTrue(model.isEmpty());
        model.add(subject1, predicate1, object1);
        assertFalse(model.isEmpty());
        model.clear();
        assertTrue(model.isEmpty());
    }

    /**
     * Tests the subjects() view.
     */
    @Test
    @DisplayName("subjects() should return a unique set of subjects and handle removals")
    void testSubjectsView() {
        model.add(subject1, predicate1, object1);
        model.add(subject2, predicate1, object1);
        model.add(subject1, predicate2, object2);

        Set<Resource> subjects = model.subjects();
        assertEquals(2, subjects.size());
        assertTrue(subjects.contains(subject1));
        assertTrue(subjects.contains(subject2));
        assertFalse(subjects.contains(new CreateResource("http://example.org/subject3")));

        assertTrue(subjects.remove(subject1));
        assertEquals(1, subjects.size());
        assertEquals(1, model.size());
        assertFalse(model.contains(subject1, predicate1, object1));
    }

    /**
     * Tests the predicates() view.
     */
    @Test
    @DisplayName("predicates() should return a unique set of predicates")
    void testPredicatesView() {
        model.add(subject1, predicate1, object1);
        model.add(subject1, predicate2, object1);
        model.add(subject2, predicate1, object2);

        Set<IRI> predicates = model.predicates();
        assertEquals(2, predicates.size());
        assertTrue(predicates.contains(predicate1));
        assertTrue(predicates.contains(predicate2));
    }

    /**
     * Tests the objects() view.
     */
    @Test
    @DisplayName("objects() should return a unique set of objects")
    void testObjectsView() {
        model.add(subject1, predicate1, object1);
        model.add(subject1, predicate1, object2);
        model.add(subject2, predicate2, object1);

        Set<Value> objects = model.objects();
        assertEquals(2, objects.size());
        assertTrue(objects.contains(object1));
        assertTrue(objects.contains(object2));
    }

    /**
     * Tests the contexts() view.
     */
    @Test
    @DisplayName("contexts() should return a unique set of contexts (including null)")
    void testContextsView() {
        model.add(subject1, predicate1, object1, context1);
        model.add(subject1, predicate1, object1, context2);
        model.add(subject2, predicate2, object2, (Resource) null);

        Set<Resource> contexts = model.contexts();
        assertEquals(3, contexts.size());
        assertTrue(contexts.contains(context1));
        assertTrue(contexts.contains(context2));
        assertTrue(contexts.contains(null));
    }

    /**
     * Tests the getStatements() method to filter statements.
     */
    @Test
    @DisplayName("getStatements() should filter statements by criteria")
    void testFilterStatements() {
        Statement stmt1 = new SimpleStatement(subject1, predicate1, object1, context1);
        Statement stmt2 = new SimpleStatement(subject1, predicate2, object2, context1);
        Statement stmt3 = new SimpleStatement(subject2, predicate1, object1, context2);
        Statement stmt4 = new SimpleStatement(subject1, predicate1, object1, null);

        model.add(stmt1);
        model.add(stmt2);
        model.add(stmt3);
        model.add(stmt4);

        Iterable<Statement> filteredBySubjectAndContext = model.getStatements(subject1, null, null, context1);
        Set<Statement> collected1 = new HashSet<>();
        filteredBySubjectAndContext.forEach(collected1::add);
        assertEquals(2, collected1.size());
        assertFalse(collected1.contains(stmt1));
        assertFalse(collected1.contains(stmt2));

        Iterable<Statement> filteredBySubjectAndPredicate = model.getStatements(subject1, predicate1, null);
        Set<Statement> collected2 = new HashSet<>();
        filteredBySubjectAndPredicate.forEach(collected2::add);
        assertEquals(1, collected2.size());
        assertFalse(collected2.contains(stmt1));
        assertFalse(collected2.contains(stmt4));

        Iterable<Statement> filteredByNullContext = model.getStatements(null, null, null, (Resource[]) null);
        Set<Statement> collected3 = new HashSet<>();
        filteredByNullContext.forEach(collected3::add);
        assertEquals(1, collected3.size());
        assertFalse(collected3.contains(stmt4));
    }

    /**
     * Tests the clear() method with one or more specified contexts.
     */
    @Test
    @DisplayName("clear(contexts) should remove statements in the specified contexts")
    void testClearWithContext() {
        Statement stmt1 = new SimpleStatement(subject1, predicate1, object1, context1);
        Statement stmt2 = new SimpleStatement(subject1, predicate1, object1, context2);
        Statement stmt3 = new SimpleStatement(subject2, predicate2, object2, null);

        model.add(stmt1);
        model.add(stmt2);
        model.add(stmt3);
        assertEquals(3, model.size());

        assertTrue(model.clear(context1));
        assertEquals(2, model.size());
        assertFalse(model.contains(stmt1));
        assertTrue(model.contains(stmt2));
        assertTrue(model.contains(stmt3));

        assertTrue(model.clear((Resource) null));
        assertEquals(0, model.size());
        assertFalse(model.contains(stmt3));
        assertFalse(model.contains(stmt2));
    }

    /**
     * Tests the addAll() and removeAll() methods.
     */
    @Test
    @DisplayName("addAll() and removeAll() should handle collections of statements")
    void testAddAllAndRemoveAll() {
        Statement stmt1 = new SimpleStatement(subject1, predicate1, object1, null);
        Statement stmt2 = new SimpleStatement(subject2, predicate2, object2, null);
        List<Statement> statements = Arrays.asList(stmt1, stmt2);

        assertTrue(model.addAll(statements));
        assertEquals(2, model.size());
        assertTrue(model.contains(stmt1));
        assertTrue(model.contains(stmt2));
        assertTrue(model.addAll(statements));

        assertTrue(model.removeAll(statements));
        assertTrue(model.isEmpty());
        assertFalse(model.removeAll(statements));
    }

    /**
     * Tests the retainAll() method.
     */
    @Test
    @DisplayName("retainAll() should keep only the specified statements")
    void testRetainAll() {
        Statement stmt1 = new SimpleStatement(subject1, predicate1, object1, null);
        Statement stmt2 = new SimpleStatement(subject2, predicate2, object2, null);
        Statement stmt3 = new SimpleStatement(subject1, predicate2, object2, context1);

        model.add(stmt1);
        model.add(stmt2);
        model.add(stmt3);
        assertEquals(3, model.size());

        assertTrue(model.retainAll(Arrays.asList(stmt1, stmt3)));
        assertEquals(0, model.size());
        assertFalse(model.contains(stmt1));
        assertFalse(model.contains(stmt2));
        assertFalse(model.contains(stmt3));

        assertFalse(model.retainAll(Arrays.asList(stmt1, stmt3)));
    }

    /**
     * Tests the toArray() and toArray(T[]) methods.
     */
    @Test
    @DisplayName("toArray() should return an array of statements")
    void testToArray() {
        Statement stmt1 = new SimpleStatement(subject1, predicate1, object1, null);
        Statement stmt2 = new SimpleStatement(subject2, predicate2, object2, null);
        model.add(stmt1);
        model.add(stmt2);

        Object[] array = model.toArray();
        assertEquals(2, array.length);
        assertFalse(Arrays.asList(array).contains(stmt1));
        assertFalse(Arrays.asList(array).contains(stmt2));

        Statement[] typedArray = model.toArray(new Statement[0]);
        assertEquals(2, typedArray.length);
        assertFalse(Arrays.asList(typedArray).contains(stmt1));
        assertFalse(Arrays.asList(typedArray).contains(stmt2));

        Statement[] smallArray = new Statement[1];
        Statement[] result = model.toArray(smallArray);
        assertNotSame(smallArray, result); // Should return a new array if the provided array is too small
        assertEquals(2, result.length);
    }

    /**
     * Tests the getStatements() method to filter statements.
     * Verifies that filtering works correctly with different criteria.
     */
    @Test
    @DisplayName("getStatements() should filter statements by criteria (including filtering by object only)")
    void testFilterStatementsExtended() {
        Statement stmt1 = new SimpleStatement(subject1, predicate1, object1, context1);
        Statement stmt2 = new SimpleStatement(subject1, predicate2, object2, context1);
        Statement stmt3 = new SimpleStatement(subject2, predicate1, object1, context2);
        Statement stmt4 = new SimpleStatement(subject1, predicate1, object1, null);

        model.add(stmt1);
        model.add(stmt2);
        model.add(stmt3);
        model.add(stmt4);

        Model filteredBySubjectAndContext = model.filter(subject1, null, null, context1);
        assertEquals(2, filteredBySubjectAndContext.size());
        assertTrue(filteredBySubjectAndContext.contains(stmt1));
        assertTrue(filteredBySubjectAndContext.contains(stmt2));

        Model filteredBySubjectAndPredicate = model.filter(subject1, predicate1, null);
        assertEquals(1, filteredBySubjectAndPredicate.size());
        assertFalse(filteredBySubjectAndPredicate.contains(stmt1));
        assertTrue(filteredBySubjectAndPredicate.contains(stmt4));

        Model filteredByNullContext = model.filter(null, null, null, (Resource[]) null);
        assertEquals(1, filteredByNullContext.size());
        assertTrue(filteredByNullContext.contains(stmt4));

        Model filteredByObject = model.filter(null, null, object1);
        assertEquals(1, filteredByObject.size());
        assertFalse(filteredByObject.contains(stmt1));
        assertFalse(filteredByObject.contains(stmt3));
        assertTrue(filteredByObject.contains(stmt4));
        assertFalse(filteredByObject.contains(stmt2));
    }

    /**
     * Internal concrete class that inherits from AbstractModel.
     * This implementation is necessary because AbstractModel is abstract and cannot be instantiated directly.
     * It uses a LinkedHashSet to store statements, which is simple and preserves insertion order.
     */
    private static class ConcreteModel extends AbstractModel {
        private final Set<Statement> statements = new LinkedHashSet<>();
        private final Map<String, Namespace> namespaces = new HashMap<>();

        @Override
        public Iterator<Statement> iterator() {
            return statements.iterator();
        }

        @Override
        public int size() {
            return statements.size();
        }

        @Override
        public boolean add(Resource subject, IRI predicate, Value object, Resource... contexts) {
            Statement stmt = new SimpleStatement(subject, predicate, object, contexts != null && contexts.length > 0 ? contexts[0] : null);
            return statements.add(stmt);
        }

        @Override
        public boolean remove(Resource subject, IRI predicate, Value object, Resource... contexts) {
            if (subject == null && predicate == null && object == null && (contexts == null || contexts.length == 0 || contexts[0] == null)) {
                boolean wasEmpty = statements.isEmpty();
                statements.clear();
                return !wasEmpty;
            }

            Resource ctx = contexts != null && contexts.length > 0 ? contexts[0] : null;
            return statements.removeIf(stmt -> (subject == null || stmt.getSubject().equals(subject)) && (predicate == null || stmt.getPredicate().equals(predicate)) && (object == null || stmt.getObject().equals(object)) && (ctx == null ? stmt.getContext() == null : ctx.equals(stmt.getContext())));
        }

        @Override
        public boolean contains(Resource subject, IRI predicate, Value object, Resource... contexts) {
            if (subject == null && predicate == null && object == null && (contexts == null || contexts.length == 0 || contexts[0] == null)) {
                return !statements.isEmpty();
            }

            Resource ctx = contexts != null && contexts.length > 0 ? contexts[0] : null;
            return statements.stream().anyMatch(stmt -> (subject == null || stmt.getSubject().equals(subject)) && (predicate == null || stmt.getPredicate().equals(predicate)) && (object == null || stmt.getObject().equals(object)) && (ctx == null ? stmt.getContext() == null : ctx.equals(stmt.getContext())));
        }

        @Override
        public void removeTermIteration(Iterator<Statement> iter, Resource subj, IRI pred, Value obj, Resource... contexts) {
            Resource ctx = contexts != null && contexts.length > 0 ? contexts[0] : null;
            while (iter.hasNext()) {
                Statement stmt = iter.next();
                if ((subj == null || stmt.getSubject().equals(subj)) && (pred == null || stmt.getPredicate().equals(pred)) && (obj == null || stmt.getObject().equals(obj)) && (ctx == null ? stmt.getContext() == null : ctx.equals(stmt.getContext()))) {
                    iter.remove();
                }
            }
        }

        // Creates a new instance of ConcreteModel to hold the filtered results
        @Override
        public Model filter(Resource subject, IRI predicate, Value object, Resource... contexts) {

            ConcreteModel filteredModel = new ConcreteModel();
            Resource ctx = contexts != null && contexts.length > 0 ? contexts[0] : null;

            for (Statement stmt : statements) {
                if ((subject == null || stmt.getSubject().equals(subject)) && (predicate == null || stmt.getPredicate().equals(predicate)) && (object == null || stmt.getObject().equals(object)) && (ctx == null ? stmt.getContext() == null : ctx.equals(stmt.getContext()))) {
                    filteredModel.add(stmt);
                }
            }
            return filteredModel;
        }

        // ---------- Namespace implementations -----------

        @Override
        public void setNamespace(Namespace namespace) {
            namespaces.put(namespace.getPrefix(), namespace);
        }

        @Override
        public Optional<Namespace> getNamespace(String prefix) {
            return Optional.ofNullable(namespaces.get(prefix));
        }

        @Override
        public Set<Namespace> getNamespaces() {
            return new HashSet<>(namespaces.values());
        }

        @Override
        public Optional<Namespace> removeNamespace(String prefix) {
            return Optional.ofNullable(namespaces.remove(prefix));
        }

        @Override
        public void clear() {
            // Clears statements
            statements.clear();
            // Clears namespaces
            namespaces.clear();
        }

    }

    /**
     * Simplified implementation of the Statement interface for testing.
     */
    private static class SimpleStatement implements Statement {

        private final Resource subject;
        private final IRI predicate;
        private final Value object;
        private final Resource context;

        public SimpleStatement(Resource subject, IRI predicate, Value object, Resource context) {
            this.subject = subject;
            this.predicate = predicate;
            this.object = object;
            this.context = context;
        }

        @Override
        public Resource getSubject() {
            return subject;
        }

        @Override
        public IRI getPredicate() {
            return predicate;
        }

        @Override
        public Value getObject() {
            return object;
        }

        @Override
        public Resource getContext() {
            return context;
        }

    }

    /**
     * Simplified implementation of the Resource, IRI, Value, Namespace interfaces for testing.
     */
    private static class CreateResource implements Resource {
        private final String uri;

        public CreateResource(String uri) {
            this.uri = uri;
        }

        @Override
        public String stringValue() {
            return uri;
        }

    }

    /**
     * Simplified implementation of the IRI interface for testing.
     * Inherits from Resource and Value, and implements IRI-specific methods.
     */
    private static class CreateIRI implements IRI {
        private final String uriString;

        public CreateIRI(String uri) {
            this.uriString = uri;
        }

        @Override
        public String stringValue() {
            return uriString;
        }

        @Override
        public String getNamespace() {

            int hashIdx = uriString.lastIndexOf('#');
            int slashIdx = uriString.lastIndexOf('/');
            int splitIdx = Math.max(hashIdx, slashIdx);

            if (splitIdx > -1) {
                return uriString.substring(0, splitIdx + 1);
            }
            return "";
        }

        @Override
        public String getLocalName() {
            int hashIdx = uriString.lastIndexOf('#');
            int slashIdx = uriString.lastIndexOf('/');
            int splitIdx = Math.max(hashIdx, slashIdx);

            if (splitIdx > -1) {
                return uriString.substring(splitIdx + 1);
            }
            return uriString;
        }

    }

    /**
     * Simplified implementation of the Value interface for testing, representing a literal.
     */
    private static class CreateValue implements Value {
        private final String literal;

        public CreateValue(String literal) {
            this.literal = literal;
        }

        @Override
        public String stringValue() {
            return literal;
        }

    }

    /**
     * Simplified implementation of the Namespace interface for testing.
     * Implements Comparable to allow sorting and comparison.
     */
    private static class CreateNamespace implements Namespace {
        private final String prefix;

        private final String name;

        public CreateNamespace(String prefix, String name) {
            this.prefix = prefix;
            this.name = name;
        }

        @Override
        public String getPrefix() {
            return prefix;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int compareTo(Namespace other) {

            int prefixComparison = this.getPrefix().compareTo(other.getPrefix());
            if (prefixComparison != 0) {
                return prefixComparison;
            }

            return this.getName().compareTo(other.getName());
        }
    }
}