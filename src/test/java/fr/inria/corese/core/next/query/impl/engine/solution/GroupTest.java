package fr.inria.corese.core.next.query.impl.engine.solution;

import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Group — regroupement par clés de tuples immuables")
class GroupTest {

    private static final String XSD_INTEGER =
            "http://www.w3.org/2001/XMLSchema#integer";

    private static final Node X = NodeImpl.forVariable("?x");
    private static final Node Y = NodeImpl.forVariable("?y");
    private static final Node L = NodeImpl.forVariable("?l");

    enum Mode {
        NORMAL, DISTINCT, EXTEND
    }

    @ParameterizedTest(name = "{0} variable(s) de groupement")
    @ValueSource(ints = {1, 2, 5})
    void groupsTuplesUsingEveryComponent(int arity) {
        List<Node> variables = variables(arity);
        Group group = Group.create(variables);

        Node[] values = new Node[arity];
        Node[] equivalentValues = new Node[arity];
        for (int i = 0; i < arity; i++) {
            values[i] = iri("value-" + i);
            equivalentValues[i] = iri("value-" + i);
        }

        Mapping first = mapping(variables, values);
        Mapping equivalent = mapping(variables, equivalentValues);

        assertTrue(group.add(first));
        assertTrue(group.add(equivalent));

        // Chaque composante, y compris la dernière, participe à la clé.
        List<Mapping> differentTuples = new ArrayList<>();
        for (int i = 0; i < arity; i++) {
            Node[] changed = equivalentValues.clone();
            changed[i] = iri("different-" + i);
            Mapping different = mapping(variables, changed);
            differentTuples.add(different);
            assertTrue(group.add(different));
        }

        List<Mappings> result = groups(group);
        assertEquals(arity + 1, result.size());
        assertMembers(result.get(0), first, equivalent);
        for (int i = 0; i < arity; i++) {
            assertMembers(result.get(i + 1), differentTuples.get(i));
        }
    }

    @Test
    void preservesGroupInsertionOrderAndMappingInsertionOrder() {
        Group group = Group.create(List.of(X));

        Mapping b1 = mapping(List.of(X), iri("b"));
        Mapping a1 = mapping(List.of(X), iri("a"));
        Mapping b2 = mapping(List.of(X), iri("b"));
        Mapping c1 = mapping(List.of(X), iri("c"));
        Mapping a2 = mapping(List.of(X), iri("a"));

        group.add(b1);
        group.add(a1);
        group.add(b2);
        group.add(c1);
        group.add(a2);

        List<Mappings> result = groups(group);
        assertEquals(3, result.size());
        assertMembers(result.get(0), b1, b2);
        assertMembers(result.get(1), a1, a2);
        assertMembers(result.get(2), c1);
    }

    @Test
    void emptyGroupHasNoValues() {
        assertTrue(groups(Group.create(List.of(X))).isEmpty());
    }

    @Test
    void emptyGroupingTupleProducesOneGlobalGroup() {
        Group group = new Group();
        Mapping first = mapping(List.of(X), iri("a"));
        Mapping second = mapping(List.of(X), iri("b"));

        group.add(first);
        group.add(second);

        assertEquals(1, groups(group).size());
        assertMembers(groups(group).get(0), first, second);
    }

    @Test
    void normalModeUsesComputedGroupByValues() {
        Group group = Group.create(List.of(X));

        // Les bindings diffèrent, mais l'expression GROUP BY vaut pareil.
        Mapping first = mapping(List.of(X), iri("binding-a"));
        first.setGroupBy(new Node[]{literal("same")});

        Mapping second = mapping(List.of(X), iri("binding-b"));
        second.setGroupBy(new Node[]{literal("same")});

        // Même binding que first, mais autre résultat de GROUP BY.
        Mapping third = mapping(List.of(X), iri("binding-a"));
        third.setGroupBy(new Node[]{literal("other")});

        group.add(first);
        group.add(second);
        group.add(third);

        List<Mappings> result = groups(group);
        assertEquals(2, result.size());
        assertMembers(result.get(0), first, second);
        assertMembers(result.get(1), third);
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void groupsUnboundVariablesByTheirPosition(Mode mode) {
        List<Node> variables = List.of(X, Y);
        Group group = group(mode, variables);

        Mapping first = mapping(variables, null, iri("a"));
        Mapping equivalent = mapping(variables, null, iri("a"));
        Mapping otherValue = mapping(variables, null, iri("b"));
        Mapping otherPosition = mapping(variables, iri("a"), null);
        Mapping allNull = mapping(variables, null, null);
        Mapping allNullAgain = mapping(variables, null, null);

        group.add(first);
        group.add(equivalent);
        group.add(otherValue);
        group.add(otherPosition);
        group.add(allNull);
        group.add(allNullAgain);

        List<Mappings> result = groups(group);
        assertEquals(4, result.size());
        assertMembers(result.get(0), first, equivalent);
        assertMembers(result.get(1), otherValue);
        assertMembers(result.get(2), otherPosition);
        assertMembers(result.get(3), allNull, allNullAgain);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void propagatesFakeFlagToEveryCreatedMappings(boolean fake) {
        Group group = Group.create(List.of(X));
        assertFalse(group.isFake());
        group.setFake(fake);
        assertEquals(fake, group.isFake());

        Mapping first = mapping(List.of(X), iri("a"));
        Mapping duplicate = mapping(List.of(X), iri("a"));
        Mapping other = mapping(List.of(X), iri("b"));

        group.add(first);
        group.add(duplicate);
        group.add(other);

        List<Mappings> result = groups(group);
        assertEquals(2, result.size());
        for (Mappings mappings : result) {
            assertEquals(fake, mappings.isFake());
        }
        assertMembers(result.get(0), first, duplicate);
        assertMembers(result.get(1), other);
    }

    @Test
    void distinctAcceptsOnlyFirstOccurrenceOfSelectedTuple() {
        Group distinct = Group.create(List.of(X));
        distinct.setDistinct(true);

        // ?y ne participe pas à COUNT(DISTINCT ?x) / AVG(DISTINCT ?x).
        assertTrue(distinct.isDistinct(
                mapping(List.of(X, Y), integer("1"), iri("a"))));
        assertFalse(distinct.isDistinct(
                mapping(List.of(X, Y), integer("1"), iri("b"))));
        assertTrue(distinct.isDistinct(
                mapping(List.of(X, Y), integer("2"), iri("a"))));
        assertFalse(distinct.isDistinct(
                mapping(List.of(X, Y), integer("2"), iri("c"))));

        // Vérifie que la première clé reste présente après d'autres insertions.
        assertFalse(distinct.isDistinct(
                mapping(List.of(X, Y), integer("1"), iri("d"))));
    }

    @Test
    void distinctTracksWholeTupleIncludingUnboundComponents() {
        Group distinct = Group.create(List.of(X, Y));

        assertTrue(distinct.isDistinct(
                mapping(List.of(X, Y), iri("a"), null)));
        assertFalse(distinct.isDistinct(
                mapping(List.of(X, Y), iri("a"), null)));
        assertTrue(distinct.isDistinct(
                mapping(List.of(X, Y), iri("b"), null)));
        assertTrue(distinct.isDistinct(
                mapping(List.of(X, Y), null, iri("a"))));
        assertTrue(distinct.isDistinct(
                mapping(List.of(X, Y), null, null)));
        assertFalse(distinct.isDistinct(
                mapping(List.of(X, Y), null, null)));
    }

    @Test
    void distinctWithEmptyTupleAcceptsExactlyOneOccurrence() {
        Group distinct = Group.create(List.of());

        assertTrue(distinct.isDistinct(mapping(List.of(X), iri("a"))));
        assertFalse(distinct.isDistinct(mapping(List.of(X), iri("b"))));
    }

    @Test
    void distinctAddUsesBindingsInsteadOfComputedGroupByValues() {
        Group group = group(Mode.DISTINCT, List.of(X));

        Mapping first = mapping(List.of(X), integer("1"));
        first.setGroupBy(new Node[]{iri("computed-a")});

        Mapping equivalent = mapping(List.of(X), integer("1"));
        equivalent.setGroupBy(new Node[]{iri("computed-b")});

        Mapping other = mapping(List.of(X), integer("2"));
        other.setGroupBy(new Node[]{iri("computed-a")});

        group.add(first);
        group.add(equivalent);
        group.add(other);

        List<Mappings> result = groups(group);
        assertEquals(2, result.size());

        // add() stocke les mappings ; isDistinct() filtre les occurrences.
        assertMembers(result.get(0), first, equivalent);
        assertMembers(result.get(1), other);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void extendGroupsByRequestedBindingsAndIgnoresAggregateValue(
            boolean alsoDistinct) {
        Group group = group(Mode.EXTEND, List.of(X, Y));
        group.setDistinct(alsoDistinct);

        Mapping first = mapping(
                List.of(X, Y, L), iri("a"), iri("b"), integer("10"));
        Mapping sameGroup = mapping(
                List.of(X, Y, L), iri("a"), iri("b"), integer("3"));
        Mapping otherGroup = mapping(
                List.of(X, Y, L), iri("a"), iri("c"), integer("1"));

        // Ces valeurs ne doivent pas servir à min(?l, groupBy(?x, ?y)).
        first.setGroupBy(new Node[]{iri("ignored-1"), iri("ignored-2")});
        sameGroup.setGroupBy(new Node[]{iri("ignored-3"), iri("ignored-4")});
        otherGroup.setGroupBy(new Node[]{iri("ignored-1"), iri("ignored-2")});

        group.add(first);
        group.add(sameGroup);
        group.add(otherGroup);

        List<Mappings> result = groups(group);
        assertEquals(2, result.size());
        assertMembers(result.get(0), first, sameGroup);
        assertMembers(result.get(1), otherGroup);

        // Vérifie également le chemin setGroup(), prioritaire sur DISTINCT.
        assertArrayEquals(
                new Node[]{iri("a"), iri("b")}, first.getGroupNodes());
        assertArrayEquals(
                new Node[]{iri("a"), iri("b")}, sameGroup.getGroupNodes());
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void modifyingMappingKeyArrayDoesNotCorruptStoredGroup(Mode mode) {
        List<Node> variables = List.of(X, Y);
        Group group = group(mode, variables);
        Mapping original = mapping(variables, iri("a"), iri("b"));

        if (mode == Mode.NORMAL) {
            original.setGroupBy(new Node[]{iri("a"), iri("b")});
        }
        group.add(original);

        Node[] exposedArray = switch (mode) {
            case NORMAL -> original.getGroupByNodes();
            case DISTINCT -> original.getDistinctNodes();
            case EXTEND -> original.getGroupNodes();
        };

        exposedArray[0] = iri("changed");
        exposedArray[1] = null;

        Mapping originalTuple = mapping(variables, iri("a"), iri("b"));
        Mapping changedTuple = mapping(variables, iri("changed"), null);

        group.add(originalTuple);
        group.add(changedTuple);

        List<Mappings> result = groups(group);
        assertEquals(2, result.size());
        assertMembers(result.get(0), original, originalTuple);
        assertMembers(result.get(1), changedTuple);
    }

    @Test
    void modifyingDistinctArrayDoesNotCorruptMembership() {
        Group distinct = Group.create(List.of(X, Y));
        Mapping first = mapping(List.of(X, Y), iri("a"), iri("b"));

        assertTrue(distinct.isDistinct(first));
        first.getDistinctNodes()[0] = iri("changed");
        first.getDistinctNodes()[1] = null;

        assertFalse(distinct.isDistinct(
                mapping(List.of(X, Y), iri("a"), iri("b"))));
        assertTrue(distinct.isDistinct(
                mapping(List.of(X, Y), iri("changed"), null)));
        assertFalse(distinct.isDistinct(
                mapping(List.of(X, Y), iri("changed"), null)));
    }

    @Test
    void groupKeyDefensivelyCopiesInputAndKeepsStableHash() {
        Node[] source = {iri("a"), null, literal("bonjour", "fr")};
        Object key = key(source);
        Object expected = key(iri("a"), null, literal("bonjour", "fr"));
        int initialHash = key.hashCode();

        Map<Object, String> table = new HashMap<>();
        table.put(key, "stored");

        source[0] = iri("changed");
        source[1] = integer("42");
        source[2] = literal("hello", "en");

        assertEquals(initialHash, key.hashCode());
        assertEquals(expected, key);
        assertEquals(expected.hashCode(), key.hashCode());
        assertEquals("stored", table.get(expected));
        assertNotEquals(key(source), key);
        assertNull(table.get(key(source)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("equalRdfTerms")
    void equalRdfTermsProduceEqualKeysAndOneGroup(
            String description, Node left, Node right) {
        assertNotSame(left, right);
        assertEquals(0, left.compare(right));

        Object firstKey = key(left);
        Object secondKey = key(right);
        Object thirdKey = key(right);

        assertEquals(firstKey, firstKey);
        assertEquals(firstKey, secondKey);
        assertEquals(secondKey, firstKey);
        assertEquals(secondKey, thirdKey);
        assertEquals(firstKey, thirdKey);
        assertEquals(firstKey.hashCode(), secondKey.hashCode());

        Group group = Group.create(List.of(X));
        Mapping first = mapping(List.of(X), left);
        Mapping second = mapping(List.of(X), right);
        group.add(first);
        group.add(second);

        assertEquals(1, groups(group).size());
        assertMembers(groups(group).get(0), first, second);

        Group distinct = Group.create(List.of(X));
        assertTrue(distinct.isDistinct(mapping(List.of(X), left)));
        assertFalse(distinct.isDistinct(mapping(List.of(X), right)));
    }

    static Stream<Arguments> equalRdfTerms() {
        return Stream.of(
                Arguments.of("URI identiques",
                        iri("resource"), iri("resource")),
                Arguments.of("Littéraux simples identiques",
                        literal("hello"), literal("hello")),
                Arguments.of("Littéraux typés identiques",
                        integer("42"), integer("42")),
                Arguments.of("Littéraux de même langue",
                        literal("bonjour", "fr"), literal("bonjour", "fr")),
                Arguments.of("Blank nodes de même identifiant",
                        NodeImpl.forBlank("b1"), NodeImpl.forBlank("b1")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("differentRdfTerms")
    void differentRdfTermsRemainInSeparateGroups(
            String description, Node left, Node right) {
        Object firstKey = key(left);
        Object secondKey = key(right);

        assertNotEquals(firstKey, secondKey);
        assertNotEquals(secondKey, firstKey);

        // Des clés différentes peuvent légalement avoir le même hash.
        Group group = Group.create(List.of(X));
        Mapping first = mapping(List.of(X), left);
        Mapping second = mapping(List.of(X), right);
        group.add(first);
        group.add(second);

        List<Mappings> result = groups(group);
        assertEquals(2, result.size());
        assertMembers(result.get(0), first);
        assertMembers(result.get(1), second);
    }

    static Stream<Arguments> differentRdfTerms() {
        return Stream.of(
                Arguments.of("URI différentes", iri("a"), iri("b")),
                Arguments.of("Valeurs littérales différentes",
                        literal("a"), literal("b")),
                Arguments.of("Langues différentes",
                        literal("chat", "fr"), literal("chat", "en")),
                Arguments.of("Avec et sans langue",
                        literal("chat", "fr"), literal("chat")),
                Arguments.of("Types RDF différents avec même label",
                        iri("same"), literal("urn:test:same")),
                Arguments.of("Blank nodes différents",
                        NodeImpl.forBlank("b1"), NodeImpl.forBlank("b2")));
    }

    @Test
    void groupKeyEqualityIncludesArityPositionAndNulls() {
        Object tuple = key(iri("a"), null);

        assertEquals(tuple, key(iri("a"), null));
        assertEquals(tuple.hashCode(), key(iri("a"), null).hashCode());
        assertNotEquals(tuple, key(null, iri("a")));
        assertNotEquals(tuple, key(iri("a")));
        assertNotEquals(tuple, key(iri("a"), null, null));
        assertNotEquals(tuple, key());
        assertNotEquals(tuple, null);
        assertNotEquals(tuple, "not a GroupKey");

        assertNotEquals(
                key(iri("a"), iri("b")),
                key(iri("b"), iri("a")));
        assertEquals(key(), key());
        assertEquals(key().hashCode(), key().hashCode());
    }

    private static Group group(Mode mode, List<Node> variables) {
        Group group = Group.create(variables);
        group.setDistinct(mode == Mode.DISTINCT);
        group.setExtend(mode == Mode.EXTEND);
        return group;
    }

    private static Mapping mapping(List<Node> variables, Node... values) {
        assertEquals(variables.size(), values.length, "Tuple de test invalide");
        Mapping map = Mapping.create(variables.toArray(Node[]::new), values);
        map.setGroupBy(new Node[0]);
        return map;
    }

    private static List<Node> variables(int count) {
        List<Node> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(NodeImpl.forVariable("?v" + i));
        }
        return result;
    }

    private static List<Mappings> groups(Group group) {
        List<Mappings> result = new ArrayList<>();
        group.getValues().forEach(result::add);
        return result;
    }

    private static void assertMembers(
            Mappings actual, Mapping... expected) {
        assertEquals(expected.length, actual.size());
        for (int i = 0; i < expected.length; i++) {
            assertSame(expected[i], actual.get(i),
                    "Mapping incorrect à la position " + i);
        }
    }

    private static Node iri(String localName) {
        return NodeImpl.forIRI("urn:test:" + localName);
    }

    private static Node literal(String label) {
        return NodeImpl.forLiteral(label, null, null);
    }

    private static Node literal(String label, String language) {
        return NodeImpl.forLiteral(label, null, language);
    }

    private static Node integer(String lexicalValue) {
        return NodeImpl.forLiteral(lexicalValue, XSD_INTEGER, null);
    }

    /**
     * Accès limité à la construction de la classe privée, pour vérifier
     * directement son contrat equals/hashCode et la copie du tableau.
     */
    @SuppressWarnings("java:S3011") // Reflective inspection of package-private GroupKey for equality verification.
    private static Object key(Node... values) {
        try {
            Class<?> keyClass = Class.forName(
                    Group.class.getName() + "$GroupKey");
            Constructor<?> constructor =
                    keyClass.getDeclaredConstructor(Node[].class);
            constructor.setAccessible(true);
            return constructor.newInstance((Object) values);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(
                    "Impossible de construire Group.GroupKey", exception);
        }
    }
}
