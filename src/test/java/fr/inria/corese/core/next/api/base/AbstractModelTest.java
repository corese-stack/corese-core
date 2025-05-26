package fr.inria.corese.core.next.api.base;

import static org.junit.jupiter.api.Assertions.*;


import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.base.model.AbstractModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 * Classe de test pour AbstractModel.
 * Utilise une implémentation concrète interne pour tester les fonctionnalités
 * par défaut fournies par AbstractModel.
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
     * Méthode d'initialisation exécutée avant chaque test.
     * Crée une nouvelle instance de ConcreteModel et initialise les objets de test (sujets, prédicats, object.).
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
     * Teste la méthode unmodifiable().
     */
    @Test
    @DisplayName("unmodifiable() doit retourner un modèle en lecture seule")
    void testUnmodifiable() {
        Model unmodifiable = model.unmodifiable();
        assertNotNull(unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.add(subject1, predicate1, object1));
    }

    /**
     * Teste la méthode setNamespace(String prefix, String name).
     */
    @Test
    @DisplayName("setNamespace(String, String) doit ajouter ou mettre à jour un namespace")
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
     * Teste la méthode setNamespace(Namespace namespace).
     */
    @Test
    @DisplayName("setNamespace(Namespace) doit ajouter un namespace")
    void testSetNamespaceWithNamespaceObject() {
        Namespace newNs = new CreateNamespace("geosparql", "http://example.org/ont/geosparql#");
        model.setNamespace(newNs);

        Optional<Namespace> fetchedNs = model.getNamespace("geosparql");
        assertTrue(fetchedNs.isPresent());
        assertEquals("http://example.org/ont/geosparql#", fetchedNs.get().getName());
        assertEquals(1, model.getNamespaces().size());
    }

    /**
     * Teste la méthode getNamespaces().
     */
    @Test
    @DisplayName("getNamespaces() doit retourner tous les namespaces définis")
    void testGetNamespaces() {
        model.setNamespace("ex1", "http://example.org/ns1/");
        model.setNamespace("ex2", "http://example.org/ns2/");

        Set<Namespace> namespaces = model.getNamespaces();
        assertEquals(2, namespaces.size());

        assertTrue(namespaces.stream().anyMatch(n -> n.getPrefix().equals("ex1") && n.getName().equals("http://example.org/ns1/")));
        assertTrue(namespaces.stream().anyMatch(n -> n.getPrefix().equals("ex2") && n.getName().equals("http://example.org/ns2/")));
    }

    /**
     * Teste la méthode removeNamespace().
     */
    @Test
    @DisplayName("removeNamespace() doit supprimer un namespace et retourner un Optional du namespace supprimé")
    void testRemoveNamespace() {
        Namespace testNs = new CreateNamespace("ex", "http://example.org/ns/");
        model.setNamespace("ex", "http://example.org/ns/");
        assertEquals(1, model.getNamespaces().size());

        Optional<Namespace> removedNs = model.removeNamespace("ex");
        assertTrue(removedNs.isPresent());

        // Comparaison
        assertEquals(testNs.getPrefix(), removedNs.get().getPrefix());
        assertEquals(testNs.getName(), removedNs.get().getName());

        assertEquals(0, model.getNamespaces().size());
        assertFalse(model.getNamespace("ex").isPresent());

        Optional<Namespace> nonExistentNs = model.removeNamespace("nonExistent");
        assertFalse(nonExistentNs.isPresent());
    }

    /**
     * Teste la méthode clearNamespaces().
     */
    @Test
    @DisplayName("clearNamespaces() doit supprimer tous les namespaces")
    void testClearNamespaces() {
        model.setNamespace("ex1", "http://example.org/ns1/");
        model.setNamespace("ex2", "http://example.org/ns2/");
        assertEquals(2, model.getNamespaces().size());


        model.clear();
        assertTrue(model.getNamespaces().isEmpty());
    }

    /**
     * Teste l'ajout d'une déclaration et sa présence via contains(Statement).
     */
    @Test
    @DisplayName("add() et contains() doivent fonctionner pour les déclarations")
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
     * Teste la suppression d'une déclaration via remove(Object).
     */
    @Test
    @DisplayName("remove() doit supprimer une déclaration existante")
    void testRemoveStatement() {
        Statement stmt = new SimpleStatement(subject1, predicate1, object1, null);
        model.add(stmt);

        assertTrue(model.remove(stmt));
        assertFalse(model.contains(stmt));
        assertEquals(0, model.size());
        assertFalse(model.remove(stmt));
    }

    /**
     * Teste la méthode isEmpty().
     */
    @Test
    @DisplayName("isEmpty() doit refléter l'état vide ou non vide du modèle")
    void testIsEmpty() {
        assertTrue(model.isEmpty());
        model.add(subject1, predicate1, object1);
        assertFalse(model.isEmpty());
        model.clear();
        assertTrue(model.isEmpty());
    }

    /**
     * Teste la vue subjects().
     */
    @Test
    @DisplayName("subjects() doit retourner un ensemble unique de sujets et gérer les suppressions")
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
     * Teste la vue predicates().
     */
    @Test
    @DisplayName("predicates() doit retourner un ensemble unique de prédicats")
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
     * Teste la vue objects().
     */
    @Test
    @DisplayName("objects() doit retourner un ensemble unique d'objets")
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
     * Teste la vue contexts().
     */
    @Test
    @DisplayName("contexts() doit retourner un ensemble unique de contextes (y compris null)")
    void testContextsView() {
        model.add(subject1, predicate1, object1, context1);
        model.add(subject1, predicate1, object1, context2);
        model.add(subject2, predicate2, object2, null);

        Set<Resource> contexts = model.contexts();
        assertEquals(3, contexts.size());
        assertTrue(contexts.contains(context1));
        assertTrue(contexts.contains(context2));
        assertTrue(contexts.contains(null));
    }

    /**
     * Teste la méthode getStatements() pour filtrer les déclarations.
     */
    @Test
    @DisplayName("getStatements() doit filtrer les déclarations par critère")
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

        Iterable<Statement> filteredByNullContext = model.getStatements(null, null, null, null);
        Set<Statement> collected3 = new HashSet<>();
        filteredByNullContext.forEach(collected3::add);
        assertEquals(1, collected3.size());
        assertFalse(collected3.contains(stmt4));
    }

    /**
     * Teste la méthode clear() avec un ou plusieurs contextes spécifiés.
     */
    @Test
    @DisplayName("clear(contextes) doit supprimer les déclarations dans les contextes spécifiés")
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
     * Teste les méthodes addAll() et removeAll().
     */
    @Test
    @DisplayName("addAll() et removeAll() doivent gérer les collections de déclarations")
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
     * Teste la méthode retainAll().
     */
    @Test
    @DisplayName("retainAll() doit conserver uniquement les déclarations spécifiées")
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
     * Teste les méthodes toArray() et toArray(T[]).
     */
    @Test
    @DisplayName("toArray() doit retourner un tableau des déclarations")
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
        assertNotSame(smallArray, result);
        assertEquals(2, result.length);
    }

    /**
     * Teste la méthode getStatements() pour filtrer les déclarations.
     * Vérifie que le filtrage fonctionne correctement avec différents critères.
     */
    @Test
    @DisplayName("getStatements() doit filtrer les déclarations par critère (y compris filtrage par objet seul)")
    void testFilterStatementsExtended() {
        Statement stmt1 = new SimpleStatement(subject1, predicate1, object1, context1);
        Statement stmt2 = new SimpleStatement(subject1, predicate2, object2, context1);
        Statement stmt3 = new SimpleStatement(subject2, predicate1, object1, context2);
        Statement stmt4 = new SimpleStatement(subject1, predicate1, object1, null);

        model.add(stmt1);
        model.add(stmt2);
        model.add(stmt3);
        model.add(stmt4);

        // Filtrage par sujet et contexte
        Model filteredBySubjectAndContext = model.filter(subject1, null, null, context1);
        assertEquals(2, filteredBySubjectAndContext.size());
        assertTrue(filteredBySubjectAndContext.contains(stmt1));
        assertTrue(filteredBySubjectAndContext.contains(stmt2));

        // Filtrage par sujet et prédicat
        Model filteredBySubjectAndPredicate = model.filter(subject1, predicate1, null);
        assertEquals(1, filteredBySubjectAndPredicate.size());
        assertFalse(filteredBySubjectAndPredicate.contains(stmt1));
        assertTrue(filteredBySubjectAndPredicate.contains(stmt4));

        // Filtrage par contexte null
        Model filteredByNullContext = model.filter(null, null, null, null);
        assertEquals(1, filteredByNullContext.size());
        assertTrue(filteredByNullContext.contains(stmt4));

        // NOUVEAU TEST: Filtrage par objet seul
        Model filteredByObject = model.filter(null, null, object1);
        assertEquals(1, filteredByObject.size());
        assertFalse(filteredByObject.contains(stmt1));
        assertFalse(filteredByObject.contains(stmt3));
        assertTrue(filteredByObject.contains(stmt4));
        assertFalse(filteredByObject.contains(stmt2));
    }

    /**
     * Classe concrète interne qui hérite de AbstractModel.
     * Cette implémentation est nécessaire car AbstractModel est abstraite et ne peut pas être instanciée directement.
     * Elle utilise un LinkedHashSet pour stocker les déclarations, ce qui est simple et préserve l'ordre d'insertion.
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

        // Crée une nouvelle instance de ConcreteModel pour contenir les résultats filtrés
        @Override
        public Model filter(Resource subject, IRI predicate, Value object, Resource... contexts) {

            ConcreteModel filteredModel = new ConcreteModel();
            Resource ctx = contexts != null && contexts.length > 0 ? contexts[0] : null;

            for (Statement stmt : statements) {
                if ((subject == null || stmt.getSubject().equals(subject)) && (predicate == null || stmt.getPredicate().equals(predicate)) && (object == null || stmt.getObject().equals(object)) && (ctx == null ? stmt.getContext() == null : ctx.equals(stmt.getContext()))) {
                    filteredModel.add(stmt);
                }
            }
            // Retourne le nouveau modèle filtré
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

            // Vide les statements
            statements.clear();

            // Vide les namespaces
            namespaces.clear();
        }

    }

    /**
     * Implémentation simplifiée de l'interface Statement pour les tests.
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
     * Implémentation simplifiée des interfaces Resource, IRI, Value, Namespace pour les tests.
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
     * Implémentation simplifiée de l'interface IRI pour les tests.
     * Hérite de Resource et Value, et implémente les méthodes spécifiques à IRI.
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
            // Logique pour extraire la partie "namespace" de l'IRI
            // Cherche le dernier '#' ou '/' comme délimiteur
            int hashIdx = uriString.lastIndexOf('#');
            int slashIdx = uriString.lastIndexOf('/');
            int splitIdx = Math.max(hashIdx, slashIdx);

            if (splitIdx > -1) {
                return uriString.substring(0, splitIdx + 1);
            }
            // Retourne une chaîne vide si aucun délimiteur commun n'est trouvé,
            // ou si l'IRI est juste un nom local sans préfixe de namespace évident.
            return "";
        }

        @Override
        public String getLocalName() {
            // Logique pour extraire la partie "nom local" de l'IRI
            // Cherche le dernier '#' ou '/' comme délimiteur
            int hashIdx = uriString.lastIndexOf('#');
            int slashIdx = uriString.lastIndexOf('/');
            int splitIdx = Math.max(hashIdx, slashIdx);

            if (splitIdx > -1) {
                return uriString.substring(splitIdx + 1);
            }
            // Si pas de délimiteur, l'URI entière est considérée comme le nom local
            return uriString;
        }

    }

    /**
     * Implémentation simplifiée de l'interface Value pour les tests, représentant un littéral.
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
     * Implémentation simplifiée de l'interface Namespace pour les tests.
     * Implémente Comparable pour permettre le tri et la comparaison.
     */
    private static class CreateNamespace implements Namespace {
        private final String prefix;
        // L'URI du namespace
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
            // Compare d'abord par le préfixe pour un ordre stable
            int prefixComparison = this.getPrefix().compareTo(other.getPrefix());
            if (prefixComparison != 0) {
                return prefixComparison;
            }
            // Si les préfixes sont égaux, compare par le nom (URI)
            return this.getName().compareTo(other.getName());
        }
    }
}