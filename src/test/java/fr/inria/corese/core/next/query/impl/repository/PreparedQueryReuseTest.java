package fr.inria.corese.core.next.query.impl.repository;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.BNode;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.query.Repositories;
import fr.inria.corese.core.next.query.api.GraphQuery;
import fr.inria.corese.core.next.query.api.TupleQuery;
import fr.inria.corese.core.next.query.api.dataset.Dataset;
import fr.inria.corese.core.next.query.api.exception.RepositoryException;
import fr.inria.corese.core.next.query.api.exception.QueryTimeoutException;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.storage.Storages;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class PreparedQueryReuseTest {
    private static IRI iri(String local) {
        return Values.factory().createIRI("https://example.org/" + local);
    }

    private static List<String> objects(TupleQuery query) {
        List<String> values = new ArrayList<>();
        try (var rows = query.evaluate()) {
            while (rows.hasNext()) values.add(rows.next().getValue("o").stringValue());
        }
        values.sort(String::compareTo);
        return values;
    }

    private static List<Statement> statements(GraphQuery query) {
        try (var rows = query.evaluate()) {
            return rows.stream().toList();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"memory"})
    void repeatedSelectUsesCurrentBindingsDatasetAndData(String backend) {
        try (var repository = Repositories.create(Storages.create(StorageConfig.builder().type(backend).build()));
             var connection = repository.getConnection()) {
            connection.add(iri("alice"), iri("p"), iri("bob"), iri("g1"));
            connection.add(iri("carol"), iri("p"), iri("dave"), iri("g2"));
            var query = connection.prepareTupleQuery(
                    "SELECT ?o FROM <g1> { ?s <p> ?o }", "https://example.org/query.rq");
            assertEquals(List.of(iri("bob").stringValue()), objects(query));
            query.setDataset(Dataset.builder().defaultGraph(iri("g2")).build());
            assertEquals(List.of(iri("dave").stringValue()), objects(query));
            query.setBinding("s", iri("alice"));
            assertTrue(objects(query).isEmpty());
            query.setBinding("s", iri("carol"));
            assertEquals(List.of(iri("dave").stringValue()), objects(query));
            connection.add(iri("carol"), iri("p"), iri("eve"), iri("g2"));
            assertEquals(List.of(iri("dave").stringValue(), iri("eve").stringValue()), objects(query));
            query.clearBindings();
            query.setDataset(null);
            assertEquals(List.of(iri("bob").stringValue()), objects(query));
            connection.close();
            assertThrows(RepositoryException.class, query::evaluate);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"memory"})
    void repeatedAskUsesCurrentBindingsDatasetAndData(String backend) {
        try (var repository = Repositories.create(Storages.create(StorageConfig.builder().type(backend).build()));
             var connection = repository.getConnection()) {
            connection.add(iri("alice"), iri("p"), iri("bob"), iri("g1"));
            var query = connection.prepareBooleanQuery(
                    "ASK FROM <g1> { ?s <p> ?o }", "https://example.org/query.rq");
            assertTrue(query.evaluate());
            query.setBinding("o", iri("eve"));
            assertFalse(query.evaluate());
            connection.add(iri("alice"), iri("p"), iri("eve"), iri("g1"));
            assertTrue(query.evaluate());
            query.setDataset(Dataset.empty());
            assertFalse(query.evaluate());
            query.clearBindings();
            query.setDataset(null);
            assertTrue(query.evaluate());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"memory"})
    void repeatedConstructCreatesFreshBlankNodesAndUsesCurrentOptions(String backend) {
        try (var repository = Repositories.create(Storages.create(StorageConfig.builder().type(backend).build()));
             var connection = repository.getConnection()) {
            connection.add(iri("alice"), iri("p"), iri("bob"), iri("g1"));
            connection.add(iri("alice"), iri("p"), iri("eve"), iri("g2"));
            var query = connection.prepareGraphQuery(
                    "CONSTRUCT { _:b <p> ?o } FROM <g1> WHERE { ?s <p> ?o }",
                    "https://example.org/query.rq");
            var first = statements(query);
            var second = statements(query);
            assertEquals(1, first.size());
            assertEquals(1, second.size());
            assertEquals(iri("bob"), first.getFirst().getObject());
            assertInstanceOf(BNode.class, first.getFirst().getSubject());
            assertNotEquals(first.getFirst().getSubject(), second.getFirst().getSubject());
            query.setDataset(Dataset.builder().defaultGraph(iri("g2")).build());
            assertEquals(iri("eve"), statements(query).getFirst().getObject());
            query.setBinding("o", iri("bob"));
            assertTrue(statements(query).isEmpty());
            query.clearBindings();
            query.setDataset(null);
            assertEquals(iri("bob"), statements(query).getFirst().getObject());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"memory"})
    void repeatedDescribeSeesMutationsAndDatasetOverrides(String backend) {
        try (var repository = Repositories.create(Storages.create(StorageConfig.builder().type(backend).build()));
             var connection = repository.getConnection()) {
            connection.add(iri("alice"), iri("p"), iri("bob"), iri("g1"));
            var query = connection.prepareGraphQuery(
                    "DESCRIBE <alice> FROM <g1>", "https://example.org/query.rq");
            assertEquals(1, statements(query).size());
            connection.add(iri("alice"), iri("p"), iri("eve"), iri("g1"));
            assertEquals(2, statements(query).size());
            query.setDataset(Dataset.empty());
            assertTrue(statements(query).isEmpty());
            query.setDataset(null);
            assertEquals(2, statements(query).size());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"memory"})
    void evaluationAfterTimeoutUsesTheNewDeadline(String backend) {
        try (var repository = Repositories.create(Storages.create(StorageConfig.builder().type(backend).build()));
             var connection = repository.getConnection()) {
            for (int i = 0; i < 201; i++) connection.add(iri("s" + i), iri("p"), iri("o" + i));
            var query = connection.prepareTupleQuery("SELECT * { ?s1 ?p1 ?o1 . ?s2 ?p2 ?o2 }");
            query.setTimeout(Duration.ofMillis(1));
            assertThrows(QueryTimeoutException.class, () -> {
                try (var rows = query.evaluate()) { rows.stream().count(); }
            });
            query.setTimeout(Duration.ofSeconds(30));
            try (var rows = query.evaluate()) { assertEquals(40_401, rows.stream().count()); }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"memory"})
    void repeatedEvaluationRecomputesVolatileExpressions(String backend) {
        try (var repository = Repositories.create(Storages.create(StorageConfig.builder().type(backend).build()));
             var connection = repository.getConnection()) {
            var query = connection.prepareTupleQuery("SELECT (UUID() AS ?o) WHERE {}");
            assertNotEquals(objects(query), objects(query));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"memory"})
    void evaluationWithTimeoutInsideActiveTransactionSucceeds(String backend) {
        try (var repository = Repositories.create(Storages.create(StorageConfig.builder().type(backend).build()));
             var connection = repository.getConnection()) {
            connection.begin();
            connection.add(iri("s"), iri("p"), iri("o"));
            var ask = connection.prepareBooleanQuery("ASK { ?s ?p ?o }");
            ask.setTimeout(Duration.ofSeconds(10));
            assertTrue(ask.evaluate());

            var tuple = connection.prepareTupleQuery("SELECT ?o { ?s ?p ?o }");
            tuple.setTimeout(Duration.ofSeconds(10));
            assertEquals(List.of(iri("o").stringValue()), objects(tuple));

            connection.rollback();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"memory"})
    void expiredQueryLeavesTransactionUsableAndCallerUninterrupted(String backend) {
        try (var repository = Repositories.create(Storages.create(StorageConfig.builder().type(backend).build()));
             var connection = repository.getConnection()) {
            connection.begin();
            for (int i = 0; i < 201; i++) {
                connection.add(iri("s" + i), iri("p"), iri("o" + i));
            }
            var query = connection.prepareTupleQuery("SELECT * { ?s1 ?p1 ?o1 . ?s2 ?p2 ?o2 }");
            query.setTimeout(Duration.ofMillis(1));
            assertThrows(QueryTimeoutException.class, () -> {
                try (var rows = query.evaluate()) {
                    rows.stream().count();
                }
            });
            assertFalse(Thread.currentThread().isInterrupted());
            assertTrue(connection.isActive());

            var ask = connection.prepareBooleanQuery("ASK { ?s ?p ?o }");
            ask.setTimeout(Duration.ofSeconds(10));
            assertTrue(ask.evaluate());
            connection.commit();
            assertEquals(201, connection.size());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"memory"})
    void callerInterruptionIsPreservedWhenEvaluationFails(String backend) {
        try (var repository = Repositories.create(Storages.create(StorageConfig.builder().type(backend).build()));
             var connection = repository.getConnection()) {
            connection.add(iri("s"), iri("p"), iri("o"));
            var ask = connection.prepareBooleanQuery("ASK { ?s ?p ?o }");
            ask.setTimeout(Duration.ofSeconds(10));
            try {
                Thread.currentThread().interrupt();
                assertThrows(QueryEvaluationException.class, ask::evaluate);
                assertTrue(Thread.currentThread().isInterrupted());
            } finally {
                Thread.interrupted();
            }
            assertTrue(ask.evaluate());
        }
    }
}
