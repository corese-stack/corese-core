package fr.inria.corese.core.next.query.impl.repository;

import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.io.CoreseIO;
import fr.inria.corese.core.next.query.Repositories;
import fr.inria.corese.core.next.query.api.exception.RepositoryException;
import fr.inria.corese.core.next.query.api.repository.Repository;
import fr.inria.corese.core.next.query.api.repository.RepositoryConnection;
import fr.inria.corese.core.next.query.api.result.StatementResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreseRepositoryConnectionDataTest {

    private Repository repository;
    private ValueFactory valueFactory;
    private IRI subject;
    private IRI predicate;
    private IRI object;
    private IRI firstGraph;
    private IRI secondGraph;

    @BeforeEach
    void setUp() {
        repository = Repositories.create();
        valueFactory = repository.getValueFactory();
        subject = valueFactory.createIRI("urn:subject");
        predicate = valueFactory.createIRI("urn:predicate");
        object = valueFactory.createIRI("urn:object");
        firstGraph = valueFactory.createIRI("urn:graph:first");
        secondGraph = valueFactory.createIRI("urn:graph:second");
    }

    @AfterEach
    void tearDown() {
        repository.close();
    }

    @Test
    void addsAndReadsStatementsAcrossContexts() {
        try (RepositoryConnection connection = repository.getConnection()) {
            Statement defaultStatement = valueFactory.createStatement(subject, predicate, object);

            connection.add(defaultStatement);
            connection.add(defaultStatement);
            connection.add(subject, predicate, object, firstGraph, secondGraph);

            assertEquals(3, connection.size());
            assertEquals(1, connection.size((Resource) null));
            assertEquals(1, connection.size(firstGraph));
            assertEquals(2, connection.size(firstGraph, secondGraph));
            assertTrue(connection.hasStatement(subject, predicate, object));
            assertTrue(connection.hasStatement(subject, predicate, object, (Resource) null));
            assertFalse(connection.hasStatement(subject, predicate, object,
                    valueFactory.createIRI("urn:graph:missing")));

            try (StatementResult result = connection.getStatements(
                    subject, predicate, object, firstGraph, secondGraph)) {
                assertEquals(2, result.stream().count());
            }
        }
    }

    @Test
    void removesStatementsIdempotentlyByValueAndPattern() {
        Statement defaultStatement = valueFactory.createStatement(subject, predicate, object);
        Statement missingStatement = valueFactory.createStatement(
                valueFactory.createIRI("urn:missing"), predicate, object);

        try (RepositoryConnection connection = repository.getConnection()) {
            connection.add(defaultStatement);
            connection.add(subject, predicate, object, firstGraph, secondGraph);

            connection.remove(missingStatement);
            connection.remove(subject, predicate, object, firstGraph);
            assertEquals(2, connection.size());

            Statement secondGraphStatement =
                    valueFactory.createStatement(subject, predicate, object, secondGraph);
            connection.remove(List.of(defaultStatement, secondGraphStatement));
            assertEquals(0, connection.size());
        }
    }

    @Test
    void clearsDefaultNamedAndAllGraphsPrecisely() {
        try (RepositoryConnection connection = repository.getConnection()) {
            connection.add(subject, predicate, object, (Resource) null, firstGraph, secondGraph);

            connection.clear((Resource) null);
            assertEquals(2, connection.size());
            assertEquals(0, connection.size((Resource) null));

            connection.clear(firstGraph);
            assertEquals(1, connection.size());
            assertEquals(1, connection.size(secondGraph));

            connection.clear();
            assertEquals(0, connection.size());
        }
    }

    @Test
    void statementResultRequiresBothItselfAndItsConnectionToRemainOpen() {
        RepositoryConnection connection = repository.getConnection();
        connection.add(subject, predicate, object);

        StatementResult closedResult = connection.getStatements(null, null, null);
        closedResult.close();
        assertThrows(IllegalStateException.class, closedResult::hasNext);

        StatementResult detachedResult = connection.getStatements(null, null, null);
        connection.close();
        assertThrows(RepositoryException.class, detachedResult::hasNext);
        detachedResult.close();
    }

    @Test
    void loadsParsedRdfAndExportsRepositoryStatementsThroughTheSameIoFacade() {
        Model parsed = CoreseIO.read(
                new StringReader("<urn:s> <urn:p> <urn:o> ."),
                RDFFormat.TURTLE);

        try (RepositoryConnection connection = repository.getConnection()) {
            connection.add(parsed);

            assertTrue(connection.hasStatement(
                    valueFactory.createIRI("urn:s"),
                    valueFactory.createIRI("urn:p"),
                    valueFactory.createIRI("urn:o")));

            try (StatementResult result = connection.getStatements(null, null, null)) {
                assertEquals(
                        "<urn:s> <urn:p> <urn:o> .\n",
                        CoreseIO.writeToString(result, RDFFormat.NTRIPLES));
            }
        }
    }
}
