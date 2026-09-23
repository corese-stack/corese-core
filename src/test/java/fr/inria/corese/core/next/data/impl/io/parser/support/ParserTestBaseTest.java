package fr.inria.corese.core.next.data.impl.io.parser.support;

import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.term.IRI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ParserTestBaseTest extends ParserTestBase {

    private IRI iri(String name) {
        return valueFactory.createIRI("urn:" + name);
    }

    @Test
    void acceptsRenamedBlankNodesAcrossSubjectObjectAndGraph() {
        Model original = createTestModel();
        Model renamed = createTestModel();
        var a = valueFactory.createBNode("a");
        var b = valueFactory.createBNode("b");
        var x = valueFactory.createBNode("x");
        var y = valueFactory.createBNode("y");
        original.add(a, iri("p"), b, a);
        original.add(b, iri("p"), a);
        renamed.add(y, iri("p"), x);
        renamed.add(x, iri("p"), y, x);

        assertModelsIsomorphic(original, renamed);
    }

    @Test
    void rejectsChangedBlankNodeTriples() {
        Model original = createTestModel();
        Model corrupted = createTestModel();
        original.add(valueFactory.createBNode("a"), iri("p"), valueFactory.createLiteral("original"));
        corrupted.add(valueFactory.createBNode("b"), iri("q"), valueFactory.createLiteral("corrupted"));

        assertThrows(AssertionError.class, () -> assertModelsIsomorphic(original, corrupted));
    }

    @Test
    void rejectsSplitSharedBlankNode() {
        Model original = createTestModel();
        Model corrupted = createTestModel();
        var shared = valueFactory.createBNode("shared");
        original.add(iri("s"), iri("p"), shared);
        original.add(shared, iri("q"), iri("o"));
        corrupted.add(iri("s"), iri("p"), valueFactory.createBNode("x"));
        corrupted.add(valueFactory.createBNode("y"), iri("q"), iri("o"));

        assertThrows(AssertionError.class, () -> assertModelsIsomorphic(original, corrupted));
    }

    @Test
    void rejectsChangedNamedGraph() {
        Model original = createTestModel();
        Model corrupted = createTestModel();
        original.add(iri("s"), iri("p"), iri("o"), iri("g1"));
        corrupted.add(iri("s"), iri("p"), iri("o"), iri("g2"));

        assertThrows(AssertionError.class, () -> assertModelsIsomorphic(original, corrupted));
    }

    @Test
    void rejectsNamedGraphMovedToDefaultGraph() {
        Model original = createTestModel();
        Model corrupted = createTestModel();
        original.add(iri("s"), iri("p"), iri("o"), iri("g"));
        corrupted.add(iri("s"), iri("p"), iri("o"));

        assertThrows(AssertionError.class, () -> assertModelsIsomorphic(original, corrupted));
    }
}
