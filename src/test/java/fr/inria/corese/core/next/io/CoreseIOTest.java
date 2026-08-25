package fr.inria.corese.core.next.io;

import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.io.parser.RDFParserOptions;
import fr.inria.corese.core.next.data.api.io.serializer.RDFSerializerOptions;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.query.api.io.ResultFormat;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializerOptions;
import fr.inria.corese.core.next.query.api.result.Binding;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.api.result.GraphQueryResult;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.impl.repository.CoreseRepository;
import fr.inria.corese.core.next.storage.impl.memory.MemoryStorageManager;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreseIOTest {

    @Test
    void readsAndWritesRdfWithoutImplementationImports() {
        Model model = CoreseIO.read(
                new StringReader("<http://example/s> <http://example/p> <http://example/o> ."),
                RDFFormat.TURTLE);

        assertEquals(1, model.size());
        assertEquals(
                "<http://example/s> <http://example/p> <http://example/o> .\n",
                CoreseIO.writeToString(model, RDFFormat.NTRIPLES));
    }

    @Test
    void readsRdfWithPublicSharedOptions() {
        Model model = CoreseIO.read(
                new StringReader("<subject> <predicate> <object> ."),
                RDFFormat.TURTLE,
                RDFParserOptions.builder().baseIRI("http://example/").build());

        assertEquals(1, model.size());
        assertTrue(model.subjects().stream()
                .anyMatch(subject -> subject.stringValue().equals("http://example/subject")));
    }

    @Test
    void exportsTupleResultsWithoutClosingResultOrWriter() {
        TestTupleResult result = tupleResult();
        TrackingWriter writer = new TrackingWriter();

        CoreseIO.write(result, ResultFormat.JSON, writer);

        assertEquals(
                "{\"head\":{\"vars\":[\"value\"]},\"results\":{\"bindings\":[{\"value\":{\"type\":\"literal\",\"value\":\"hello\"}}]}}",
                writer.toString());
        assertFalse(result.closed);
        assertFalse(writer.closed);
    }

    @Test
    void exportsBooleanResultsWithRequiredHeaders() {
        assertEquals(
                "{\"head\":{},\"boolean\":true}",
                CoreseIO.writeToString(true, ResultFormat.JSON));
        assertTrue(CoreseIO.writeToString(false, ResultFormat.XML).contains("<head/><boolean>false</boolean>"));
    }

    @Test
    void appliesPublicOptionsAndRejectsIncompatibleOnes() {
        ResultSerializerOptions links = ResultSerializerOptions.builder()
                .addLink("http://example/metadata")
                .build();
        assertTrue(CoreseIO.writeToString(tupleResult(), ResultFormat.JSON, links)
                .contains("\"link\":[\"http://example/metadata\"]"));

        ResultSerializerOptions csv = ResultSerializerOptions.builder()
                .lineEnding("\r\n")
                .build();
        assertTrue(CoreseIO.writeToString(tupleResult(), ResultFormat.CSV, csv).endsWith("\r\n"));
        TestTupleResult result = tupleResult();
        assertThrows(IllegalArgumentException.class,
                () -> CoreseIO.writeToString(result, ResultFormat.CSV, links));

        Model model = CoreseIO.read(
                new StringReader("<http://example/s> <http://example/p> \"é\" ."),
                RDFFormat.TURTLE);
        RDFSerializerOptions rdfOptions = RDFSerializerOptions.builder().lineEnding("\r\n").build();
        assertTrue(CoreseIO.writeToString(model, RDFFormat.NTRIPLES, rdfOptions).endsWith("\r\n"));
    }

    @Test
    void exportsGraphQueryResultsDirectlyWithoutClosingThem() {
        Statement statement = CoreseIO.valueFactory().createStatement(
                CoreseIO.valueFactory().createIRI("http://example/s"),
                CoreseIO.valueFactory().createIRI("http://example/p"),
                CoreseIO.valueFactory().createIRI("http://example/o"));
        TestGraphResult result = new TestGraphResult(List.of(statement));

        String output = CoreseIO.writeToString(result, RDFFormat.NTRIPLES);

        assertEquals("<http://example/s> <http://example/p> <http://example/o> .\n", output);
        assertFalse(result.closed);
    }

    @Test
    void acceptsEquivalentStandardFormatDescriptors() {
        ResultFormat equivalentJson = new ResultFormat(
                "json",
                List.of("srj", "json"),
                List.of("application/sparql-results+json"));

        assertTrue(CoreseIO.writeToString(tupleResult(), equivalentJson).startsWith("{\"head\""));
    }

    @Test
    void evaluatesAndExportsRepositoryQueriesInOneCall() throws Exception {
        try (CoreseRepository repository = new CoreseRepository(MemoryStorageManager.builder().build())) {
            repository.init();
            repository.update("INSERT DATA { <http://example/s> <http://example/p> <http://example/o> }");

            StringWriter select = new StringWriter();
            CoreseIO.writeSelect(
                    repository,
                    "SELECT ?s WHERE { ?s <http://example/p> ?o }",
                    ResultFormat.JSON,
                    select);
            assertTrue(select.toString().contains("http://example/s"));

            StringWriter graph = new StringWriter();
            CoreseIO.writeGraph(
                    repository,
                    "CONSTRUCT { ?s <http://example/p> ?o } WHERE { ?s <http://example/p> ?o }",
                    RDFFormat.NTRIPLES,
                    graph);
            assertEquals("<http://example/s> <http://example/p> <http://example/o> .\n", graph.toString());
        }
    }

    private static TestTupleResult tupleResult() {
        Map<String, Value> values = new LinkedHashMap<>();
        values.put("value", CoreseIO.valueFactory().createLiteral("hello"));
        return new TestTupleResult(List.of("value"), List.of(new TestBindingSet(values)));
    }

    private static final class TrackingWriter extends StringWriter {
        private boolean closed;

        @Override
        public void close() {
            closed = true;
        }
    }

    private static final class TestTupleResult implements TupleQueryResult {
        private final List<String> names;
        private final Iterator<BindingSet> rows;
        private boolean closed;

        private TestTupleResult(List<String> names, List<BindingSet> rows) {
            this.names = names;
            this.rows = rows.iterator();
        }

        @Override
        public List<String> getBindingNames() {
            return names;
        }

        @Override
        public boolean hasNext() {
            return rows.hasNext();
        }

        @Override
        public BindingSet next() {
            return rows.next();
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private record TestBindingSet(Map<String, Value> values) implements BindingSet {
        @Override
        public Set<String> getBindingNames() {
            return values.keySet();
        }

        @Override
        public boolean hasBinding(String name) {
            return values.containsKey(name);
        }

        @Override
        public Value getValue(String name) {
            return values.get(name);
        }

        @Override
        public Iterator<Binding> iterator() {
            return values.entrySet().stream()
                    .<Binding>map(entry -> new TestBinding(entry.getKey(), entry.getValue()))
                    .iterator();
        }
    }

    private record TestBinding(String name, Value value) implements Binding {
    }

    private static final class TestGraphResult implements GraphQueryResult {
        private final Iterator<Statement> statements;
        private boolean closed;

        private TestGraphResult(List<Statement> statements) {
            this.statements = statements.iterator();
        }

        @Override
        public boolean hasNext() {
            return statements.hasNext();
        }

        @Override
        public Statement next() {
            return statements.next();
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
