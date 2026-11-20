package fr.inria.corese.core.next.data.impl.io.serializer.ntriples;

import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.impl.io.serializer.support.MockStatementFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that sort granularity is respected: enabling only sortSubjects
 * should not reorder triples that share the same subject.
 */
class NTriplesSerializerSortGranularityTest {

    private MockStatementFactory factory;
    private Resource subject;

    @BeforeEach
    void setUp() {
        factory = new MockStatementFactory();
        subject = factory.createIRI("http://example.org/s");
    }

    @Test
    @DisplayName("sortSubjects only should preserve insertion order within the same subject group")
    void sortSubjectsOnlyDoesNotReorderSameSubjectTriples() {
        // Two triples with the same subject but predicates inserted in reverse order.
        // With sortSubjects only (not sortPredicates), insertion order within
        // the same subject group must be preserved.
        IRI predB = factory.createIRI("http://example.org/bbb");
        IRI predA = factory.createIRI("http://example.org/aaa");
        IRI obj   = factory.createIRI("http://example.org/o");

        List<Statement> stmts = List.of(
                factory.createStatement(subject, predB, obj),
                factory.createStatement(subject, predA, obj)
        );

        NTriplesSerializerOptions opts = new NTriplesSerializerOptions.Builder()
                .sortSubjects(true)
                .sortPredicates(false)
                .sortObjects(false)
                .build();

        StringWriter writer = new StringWriter();
        new NTriplesSerializer(stmts, opts).write(writer);
        String output = writer.toString();

        // bbb was inserted first, so it should still appear first
        assertTrue(output.indexOf("bbb") < output.indexOf("aaa"),
                "insertion order within same subject should be preserved when only sortSubjects is enabled");
    }

    @Test
    @DisplayName("sortSubjects + sortPredicates should reorder predicates lexicographically")
    void sortSubjectsAndPredicatesReordersPredicates() {
        IRI predB = factory.createIRI("http://example.org/bbb");
        IRI predA = factory.createIRI("http://example.org/aaa");
        IRI obj   = factory.createIRI("http://example.org/o");

        // Insert bbb predicate first
        List<Statement> stmts = List.of(
                factory.createStatement(subject, predB, obj),
                factory.createStatement(subject, predA, obj)
        );

        NTriplesSerializerOptions opts = new NTriplesSerializerOptions.Builder()
                .sortSubjects(true)
                .sortPredicates(true)
                .sortObjects(false)
                .build();

        StringWriter writer = new StringWriter();
        new NTriplesSerializer(stmts, opts).write(writer);
        String output = writer.toString();

        assertTrue(output.indexOf("aaa") < output.indexOf("bbb"),
                "aaa predicate should appear before bbb when sortPredicates is enabled");
    }
}
