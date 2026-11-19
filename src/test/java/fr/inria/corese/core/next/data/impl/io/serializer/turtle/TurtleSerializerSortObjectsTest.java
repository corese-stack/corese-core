package fr.inria.corese.core.next.data.impl.io.serializer.turtle;

import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.impl.io.serializer.support.MockStatementFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TurtleSerializerSortObjectsTest {

    private MockStatementFactory factory;
    private Model mockModel;
    private Resource subject;
    private IRI predicate;

    @BeforeEach
    void setUp() {
        factory = new MockStatementFactory();
        mockModel = mock(Model.class);
        subject = factory.createIRI("http://example.org/s");
        predicate = factory.createIRI("http://example.org/p");
    }

    @Test
    void sortObjectsShouldOutputObjectsLexicographically() {
        IRI objA = factory.createIRI("http://example.org/aaa");
        IRI objB = factory.createIRI("http://example.org/bbb");
        IRI objC = factory.createIRI("http://example.org/ccc");

        // Insert in reverse order: C, A, B
        Statement s1 = factory.createStatement(subject, predicate, objC);
        Statement s2 = factory.createStatement(subject, predicate, objA);
        Statement s3 = factory.createStatement(subject, predicate, objB);

        List<Statement> stmts = Arrays.asList(s1, s2, s3);
        when(mockModel.iterator()).thenAnswer(inv -> stmts.iterator());
        when(mockModel.stream()).thenAnswer(inv -> stmts.stream());
        when(mockModel.getNamespaces()).thenReturn(java.util.Set.of());

        TurtleSerializerOptions opts = new TurtleSerializerOptions.Builder()
                .sortSubjects(true)
                .sortPredicates(true)
                .sortObjects(true)
                .usePrefixes(false)
                .useCompactTriples(true)
                .prettyPrint(false)
                .build();

        StringWriter writer = new StringWriter();
        new TurtleSerializer(mockModel, opts).write(writer);
        String output = writer.toString();

        int posA = output.indexOf("aaa");
        int posB = output.indexOf("bbb");
        int posC = output.indexOf("ccc");

        assertTrue(posA < posB, "aaa should appear before bbb");
        assertTrue(posB < posC, "bbb should appear before ccc");
    }

    @Test
    void noSortObjectsShouldPreserveInsertionOrder() {
        IRI objA = factory.createIRI("http://example.org/aaa");
        IRI objC = factory.createIRI("http://example.org/ccc");

        // Insert C then A
        Statement s1 = factory.createStatement(subject, predicate, objC);
        Statement s2 = factory.createStatement(subject, predicate, objA);

        List<Statement> stmts = Arrays.asList(s1, s2);
        when(mockModel.iterator()).thenAnswer(inv -> stmts.iterator());
        when(mockModel.stream()).thenAnswer(inv -> stmts.stream());
        when(mockModel.getNamespaces()).thenReturn(java.util.Set.of());

        TurtleSerializerOptions opts = new TurtleSerializerOptions.Builder()
                .sortSubjects(false)
                .sortPredicates(false)
                .sortObjects(false)
                .usePrefixes(false)
                .useCompactTriples(true)
                .prettyPrint(false)
                .build();

        StringWriter writer = new StringWriter();
        new TurtleSerializer(mockModel, opts).write(writer);
        String output = writer.toString();

        int posA = output.indexOf("aaa");
        int posC = output.indexOf("ccc");

        assertTrue(posC < posA, "ccc should appear before aaa (insertion order preserved)");
    }
}
