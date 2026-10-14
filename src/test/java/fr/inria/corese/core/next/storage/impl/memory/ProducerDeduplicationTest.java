package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.SimpleIRI;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.engine.spi.Environment;
import fr.inria.corese.core.next.query.impl.engine.storage.StorageManagerEdge;
import fr.inria.corese.core.next.query.impl.engine.storage.StorageManagerProducer;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.bridge.CoreseAstQueryBuilder;
import fr.inria.corese.core.next.query.impl.sparql.execution.NextSparqlPipelineExecutor;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlParser;
import fr.inria.corese.core.next.storage.Storages;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;
import fr.inria.corese.core.next.storage.api.operations.QueryOperations;
import fr.inria.corese.core.next.storage.api.operations.StatementSpliterator;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProducerDeduplicationTest {
    private static final Resource G = new SimpleIRI("urn:g");
    private static final Resource H = new SimpleIRI("urn:h");
    private static final Resource EMPTY = new SimpleIRI("urn:empty");
    private static StorageManager storage() { return Storages.create(StorageConfig.memory()); }
    private static Statement row(int i, Resource graph) {
        return Values.factory().createStatement(new SimpleIRI("urn:s" + i), new SimpleIRI("urn:p"), new SimpleIRI("urn:o"), graph);
    }
    private static Query query(String dataset) {
        return new CoreseAstQueryBuilder().toNextQuery((SelectQueryAst) new SparqlParser()
                .parse("SELECT * " + dataset + " WHERE { ?s ?p ?o }"));
    }
    private static Iterable<Edge> edges(StorageManagerProducer producer, Query query) {
        Environment env = mock(Environment.class); when(env.getQuery()).thenReturn(query);
        return producer.getEdges(null, query.getFrom(), query.getBody().first().first().getEdge(), env);
    }
    @SuppressWarnings("java:S3011")
    private static Object seen(Iterator<Edge> iterator) throws ReflectiveOperationException {
        var field = iterator.getClass().getDeclaredField("seen");
        field.setAccessible(true);
        return field.get(iterator);
    }
    private static List<Statement> rows(Iterator<Edge> iterator) {
        List<Statement> result = new ArrayList<>();
        while (iterator.hasNext()) result.add(((StorageManagerEdge) iterator.next()).getSourceStatement());
        return result;
    }
    private static StorageManager observed(StorageManager root, java.util.function.Function<StatementPattern, Stream<Statement>> find) {
        StorageManager wrapper = mock(StorageManager.class);
        QueryOperations queries = mock(QueryOperations.class);
        when(wrapper.queries()).thenReturn(queries); when(wrapper.metadata()).thenReturn(root.metadata());
        when(queries.find(any())).thenAnswer(call -> find.apply(call.getArgument(0))); return wrapper;
    }
    private static StatementSpliterator traversal(Stream<Statement> stream) {
        return assertInstanceOf(StatementSpliterator.class, stream.spliterator());
    }

    @Test void uniquenessMetadataReflectsTheSelectedSnapshotAndIgnoresEmptyPartitions() {
        try (var root = storage()) {
            root.mutations().add(row(0, null)); root.mutations().add(row(1, G)); root.mutations().createGraph(EMPTY);
            try (var all = root.queries().find(StatementPattern.matchAll());
                 var one = root.queries().find(StatementPattern.of(null, null, null, G, EMPTY));
                 var absent = root.queries().find(StatementPattern.of(null, null, null, H))) {
                var multi = traversal(all);
                var single = traversal(one);
                assertFalse(multi.hasDistinctTriples()); assertTrue(single.hasDistinctTriples());
                assertTrue(traversal(absent).hasDistinctTriples());
                root.mutations().clear(); root.mutations().add(row(2, H));
                assertFalse(multi.hasDistinctTriples()); assertTrue(single.hasDistinctTriples());
                List<Statement> old = new ArrayList<>(); multi.forEachRemaining(old::add);
                assertEquals(List.of(row(0, null), row(1, G)), old);
                old.clear(); single.forEachRemaining(old::add); assertEquals(List.of(row(1, G)), old);
            }
        }
    }

    @Test void singleDefaultExplicitFromAndNamedGraphAllocateNoDeduplicationSet() throws Exception {
        try (var root = storage()) {
            root.mutations().add(row(0, null)); root.mutations().add(row(1, G)); root.mutations().add(row(1, H));
            var producer = new StorageManagerProducer(root);
            for (String dataset : List.of("", "FROM <urn:g>", "FROM <urn:g> FROM <urn:g>")) {
                var q = query(dataset);
                var iterable = edges(producer, q);
                var iterator = iterable.iterator();
                try { assertNull(seen(iterator)); assertEquals(1, rows(iterator).size()); }
                finally { producer.closeEdges(iterable); }
            }
            var q = query("");
            var iterable = producer.getEdges(NodeImpl.forValue(G), List.of(), q.getBody().first().first().getEdge(), null);
            var iterator = iterable.iterator();
            try { assertNull(seen(iterator)); assertEquals(List.of(row(1, G)), rows(iterator)); }
            finally { producer.closeEdges(iterable); producer.close(); }
        }
    }

    @Test void multipleFromContextsWithOnlyOnePopulatedPartitionNeedNoSet() throws Exception {
        try (var root = storage()) {
            root.mutations().add(row(0, G)); root.mutations().add(row(1, H)); root.mutations().createGraph(EMPTY);
            var producer = new StorageManagerProducer(root);
            var iterable = edges(producer, query("FROM <urn:g> FROM <urn:empty> FROM <urn:missing>"));
            var iterator = iterable.iterator(); try { assertNull(seen(iterator)); assertEquals(List.of(row(0, G)), rows(iterator)); }
            finally { producer.closeEdges(iterable); producer.close(); }
        }
    }

    @Test void mergedContextsDeduplicateTriplesInFirstEncounterOrderWithoutLosingStatements() throws Exception {
        try (var root = storage()) {
            // Ordered roots, a removal hole, and overlapping triples in both named graphs.
            for (Resource graph : new Resource[]{G, H}) for (int i = 0; i < 12; i++) root.mutations().add(row(i, graph));
            root.mutations().remove(row(4, G)); root.mutations().add(row(20, H));
            List<Statement> oracle = new ArrayList<>();
            var keys = new LinkedHashSet<List<Object>>();
            try (var stream = root.queries().find(StatementPattern.of(null, null, null, G, H))) {
                stream.forEach(s -> { if (keys.add(List.of(s.getSubject(), s.getPredicate(), s.getObject()))) oracle.add(s); });
            }
            var producer = new StorageManagerProducer(root);
            var iterable = edges(producer, query("FROM <urn:h> FROM <urn:g> FROM <urn:g>"));
            var iterator = iterable.iterator();
            try {
                assertNotNull(seen(iterator)); var actual = rows(iterator); assertEquals(oracle, actual);
                for (int i = 0; i < oracle.size(); i++) assertSame(oracle.get(i), actual.get(i));
            } finally { producer.closeEdges(iterable); producer.close(); }
        }
    }

    @Test void writerAddingAPartitionBetweenFindAndMetadataInspectionKeepsTheSingleSnapshot() throws Exception {
        try (var root = storage()) {
            root.mutations().add(row(0, G));
            var wrapper = observed(root, pattern -> {
                var stream = root.queries().find(pattern);
                root.mutations().add(row(0, H)); return stream;
            });
            var producer = new StorageManagerProducer(wrapper);
            var iterable = edges(producer, query("FROM <urn:g> FROM <urn:h>"));
            var iterator = iterable.iterator();
            try { assertNull(seen(iterator)); assertEquals(List.of(row(0, G)), rows(iterator)); }
            finally { producer.closeEdges(iterable); producer.close(); }
        }
    }

    @Test void writerDroppingAPartitionBetweenFindAndMetadataInspectionStillDeduplicatesTheOldUnion() throws Exception {
        try (var root = storage()) {
            root.mutations().add(row(0, G)); root.mutations().add(row(0, H));
            var wrapper = observed(root, pattern -> {
                var stream = root.queries().find(pattern);
                root.mutations().dropGraph(H); return stream;
            });
            var producer = new StorageManagerProducer(wrapper);
            var iterable = edges(producer, query("FROM <urn:g> FROM <urn:h>"));
            var iterator = iterable.iterator();
            try { assertNotNull(seen(iterator)); assertEquals(List.of(row(0, G)), rows(iterator)); }
            finally { producer.closeEdges(iterable); producer.close(); }
        }
    }

    @Test void unknownWrappedTraversalConservativelyDeduplicatesMergedContexts() throws Exception {
        try (var root = storage()) {
            root.mutations().add(row(0, G)); root.mutations().add(row(0, H));
            var wrapper = observed(root, pattern -> root.queries().find(pattern).peek(s -> { }));
            var producer = new StorageManagerProducer(wrapper);
            var iterable = edges(producer, query("FROM <urn:g> FROM <urn:h>"));
            var iterator = iterable.iterator();
            try { assertNotNull(seen(iterator)); assertEquals(List.of(row(0, G)), rows(iterator)); }
            finally { producer.closeEdges(iterable); producer.close(); }
        }
    }

    @Test void sparqlProjectionBagsAndExplicitNamedGraphsKeepTheirMultiplicity() {
        try (var root = storage()) {
            root.mutations().add(row(0, G)); root.mutations().add(row(0, H));
            root.mutations().add(Values.factory().createStatement(new SimpleIRI("urn:s0"), new SimpleIRI("urn:p2"), new SimpleIRI("urn:o"), G));
            var executor = new NextSparqlPipelineExecutor(root);
            try (var result = executor.evaluateTuple("SELECT ?s FROM <urn:g> FROM <urn:h> { ?s ?p ?o }")) {
                assertEquals(List.of("urn:s0", "urn:s0"), result.stream().map(b -> b.getValue("s").stringValue()).toList());
            }
            try (var result = executor.evaluateTuple("SELECT ?s FROM <urn:g> { ?s ?p ?o }")) { assertEquals(2, result.stream().count()); }
            try (var result = executor.evaluateTuple("SELECT ?s FROM NAMED <urn:g> FROM NAMED <urn:h> { GRAPH ?g { ?s <urn:p> ?o } }")) {
                assertEquals(2, result.stream().count());
            }
        }
    }
}
