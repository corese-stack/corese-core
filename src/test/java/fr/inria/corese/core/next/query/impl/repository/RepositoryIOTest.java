package fr.inria.corese.core.next.query.impl.repository;

import fr.inria.corese.core.next.data.api.exception.ParsingException;
import fr.inria.corese.core.next.data.api.exception.SerializationException;
import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.term.BNode;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.query.Repositories;
import fr.inria.corese.core.next.query.api.exception.RepositoryException;
import fr.inria.corese.core.next.query.api.repository.RepositoryConnection;
import fr.inria.corese.core.next.storage.Storages;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.operations.QueryOperations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.AdditionalAnswers.delegatesTo;

class RepositoryIOTest {
    private static final String TRIPLE = "<urn:s> <urn:p> <urn:o> .\n";
    private static final String QUAD = "<urn:s> <urn:p> <urn:o> <urn:g> .\n";

    @TempDir
    Path directory;

    @Test
    void fileImportsDetectFormatsAndRespectExplicitOverrides() throws IOException {
        Path file = directory.resolve("input.NT");
        Files.writeString(file, TRIPLE);
        try (var repository = Repositories.create()) {
            repository.load(file);
            assertTrue(repository.ask("ASK { <urn:s> <urn:p> <urn:o> }"));
            Path unknown = directory.resolve("input.unknown");
            Files.writeString(unknown, QUAD);
            assertThrows(IllegalArgumentException.class, () -> repository.load(unknown));
            repository.load(unknown, RDFFormat.NQUADS);
            assertTrue(repository.ask("ASK { GRAPH <urn:g> { <urn:s> <urn:p> <urn:o> } }"));
        }
    }

    @Test
    void fileUriAndExplicitBaseResolveRelativeTurtleIris() throws IOException {
        Path file = directory.resolve("input.ttl");
        Files.writeString(file, "<subject> <urn:p> <object> .");
        try (var repository = Repositories.create(); var connection = repository.getConnection()) {
            connection.load(file);
            assertTrue(connection.hasStatement(Values.factory().createIRI(directory.resolve("subject").toUri().toString()),
                    Values.factory().createIRI("urn:p"), Values.factory().createIRI(directory.resolve("object").toUri().toString())));
            connection.load(new StringReader("<s> <urn:p> <o> ."), RDFFormat.TURTLE, "https://example.org/base/");
            assertTrue(connection.hasStatement(Values.factory().createIRI("https://example.org/base/s"),
                    Values.factory().createIRI("urn:p"), Values.factory().createIRI("https://example.org/base/o")));
        }
    }

    @Test
    void ownedImportRollsBackValidPrefixOnMalformedInput() {
        try (var repository = Repositories.create(); var connection = repository.getConnection()) {
            connection.load(new StringReader(TRIPLE), RDFFormat.NTRIPLES);
            assertThrows(ParsingException.class, () -> connection.load(
                    new StringReader("<urn:added> <urn:p> <urn:o> .\ninvalid"), RDFFormat.NTRIPLES));
            assertEquals(1, connection.size());
            assertFalse(connection.isActive());
            connection.load(new StringReader(QUAD), RDFFormat.NQUADS);
            assertEquals(2, connection.size());
        }
    }

    @Test
    void importJoinsActiveTransactionAndLeavesItsFailureToTheCaller() {
        try (var repository = Repositories.create(); var writer = repository.getConnection();
                var reader = repository.getConnection()) {
            writer.begin();
            writer.load(new StringReader(TRIPLE), RDFFormat.NTRIPLES);
            assertEquals(0, reader.size());
            assertEquals(1, writer.size());
            assertThrows(ParsingException.class, () -> writer.load(new StringReader(QUAD + "invalid"), RDFFormat.NQUADS));
            assertTrue(writer.isActive());
            assertEquals(2, writer.size());
            writer.rollback();
            assertEquals(0, reader.size());
            writer.inTransaction(connection -> connection.load(new StringReader(TRIPLE), RDFFormat.NTRIPLES));
            assertEquals(1, reader.size());
        }
    }

    @Test
    void importRetargetsContextsIncludingExplicitDefaultGraph() {
        try (var repository = Repositories.create(); var connection = repository.getConnection()) {
            connection.load(new StringReader(QUAD), RDFFormat.NQUADS,
                    Values.factory().createIRI("urn:target"), (Resource) null);
            assertEquals(2, connection.size());
            assertEquals(0, connection.size(Values.factory().createIRI("urn:g")));
            assertEquals(1, connection.size(Values.factory().createIRI("urn:target")));
            assertEquals(1, connection.size((Resource) null));
        }
    }

    @Test
    void blankNodeLabelsAreSharedWithinOneImportAndSeparatedBetweenImports() {
        String input = "_:a <urn:p> _:b _:a .\n_:b <urn:p> _:a _:a .\n";
        try (var repository = Repositories.create(); var connection = repository.getConnection()) {
            connection.load(new StringReader(input), RDFFormat.NQUADS);
            connection.load(new StringReader(input), RDFFormat.NQUADS);
            assertEquals(4, connection.size());
            try (var statements = connection.getStatements(null, null, null)) {
                var first = statements.next();
                assertInstanceOf(BNode.class, first.getSubject());
                assertEquals(first.getSubject(), first.getContext());
                assertEquals(first.getObject(), statements.next().getSubject());
            }
        }
    }

    @Test
    void borrowedInputsAndOutputsRemainOpen() {
        try (var repository = Repositories.create()) {
            var input = new ByteArrayInputStream(TRIPLE.getBytes(StandardCharsets.UTF_8)) {
                @Override
                public void close() { fail("Caller-owned input was closed"); }
            };
            repository.load(input, RDFFormat.NTRIPLES);
            var reader = new StringReader(TRIPLE) {
                @Override
                public void close() { fail("Caller-owned reader was closed"); }
            };
            repository.load(reader, RDFFormat.NTRIPLES);
            var output = new ByteArrayOutputStream() {
                @Override
                public void close() { fail("Caller-owned output was closed"); }
            };
            repository.export(output, RDFFormat.NTRIPLES);
            assertTrue(output.toString(StandardCharsets.UTF_8).contains("urn:s"));
            var writer = new StringWriter() {
                @Override
                public void close() { fail("Caller-owned writer was closed"); }
            };
            repository.export(writer, RDFFormat.NTRIPLES);
            assertTrue(writer.toString().contains("urn:s"));
        }
    }

    @Test
    void failedReadsRollBackAndDoNotCloseBorrowedInput() {
        try (var repository = Repositories.create(); var connection = repository.getConnection()) {
            var reader = new StringReader(TRIPLE) {
                @Override
                public int read(char[] buffer, int offset, int length) throws IOException {
                    throw new IOException("input unavailable");
                }

                @Override
                public void close() { fail("Caller-owned input was closed"); }
            };
            assertThrows(ParsingException.class, () -> connection.load(reader, RDFFormat.NTRIPLES));
            assertFalse(connection.isActive());
            assertEquals(0, connection.size());
        }
    }

    @Test
    void exportRoundTripsDatasetAndSupportsContextSelection() throws IOException {
        Path file = directory.resolve("backup.nq");
        try (var repository = Repositories.create(); var connection = repository.getConnection()) {
            connection.load(new StringReader(TRIPLE + QUAD), RDFFormat.NQUADS);
            repository.export(file, RDFFormat.NQUADS);
            connection.clear();
            repository.load(file);
            assertEquals(2, connection.size());
            StringWriter writer = new StringWriter();
            connection.export(writer, RDFFormat.NTRIPLES, (Resource) null);
            assertEquals(TRIPLE.trim(), writer.toString().trim());
            assertThrows(SerializationException.class, () -> connection.export(new StringWriter(), RDFFormat.TURTLE));
            assertThrows(SerializationException.class, () -> connection.export(new StringWriter(), RDFFormat.NTRIPLES));
        }
    }

    @Test
    void failedExportClosesTheCapturedCursor() {
        var storage = Storages.create();
        var session = storage.openSession();
        var closed = new AtomicBoolean();
        var queries = mock(QueryOperations.class, delegatesTo(session.queries()));
        doAnswer(call -> session.queries().find(call.getArgument(0)).onClose(() -> closed.set(true)))
                .when(queries).find(any());
        var wrapped = mock(StorageManager.class, delegatesTo(session));
        when(wrapped.queries()).thenReturn(queries);
        try (var repository = Repositories.create(storage);
                var real = new CoreseRepositoryConnection(repository, wrapped)) {
            real.load(new StringReader(TRIPLE), RDFFormat.NTRIPLES);
            var writer = new StringWriter() {
                @Override
                public void write(String text) { throw new IllegalStateException("output unavailable"); }

                @Override
                public void write(char[] buffer, int offset, int length) {
                    throw new IllegalStateException("output unavailable");
                }
            };
            assertThrows(RuntimeException.class, () -> real.export(writer, RDFFormat.NTRIPLES));
            assertTrue(closed.get(), "Export failure must close the captured storage stream");
            real.clear();
            assertEquals(0, real.size());
        }
    }

    @Test
    void functionalTransactionsCommitResultsAndRollBackCallbackFailures() {
        try (var repository = Repositories.create(); var connection = repository.getConnection()) {
            long count = repository.computeInTransaction(current -> {
                current.load(new StringReader(TRIPLE), RDFFormat.NTRIPLES);
                return current.size();
            });
            assertEquals(1, count);
            IllegalStateException original = new IllegalStateException("callback failed");
            assertSame(original, assertThrows(IllegalStateException.class, () -> connection.inTransaction(current -> {
                current.clear();
                throw original;
            })));
            assertEquals(1, connection.size());
            assertFalse(connection.isActive());
            connection.begin();
            assertThrows(RepositoryException.class, () -> connection.inTransaction(current -> fail("Nested callback ran")));
            assertTrue(connection.isActive());
            connection.rollback();
        }
    }

    @Test
    void exportRetainsItsReadVersionDuringWriterMutations() {
        try (var repository = Repositories.create(); var connection = repository.getConnection()) {
            StringBuilder input = new StringBuilder();
            for (int index = 0; index < 1_000; index++) {
                input.append("<urn:s:").append(index).append("> <urn:p> <urn:o> .\n");
            }
            connection.load(new StringReader(input.toString()), RDFFormat.NTRIPLES);
            var mutated = new AtomicBoolean();
            var writer = new StringWriter() {
                @Override
                public void write(char[] buffer, int offset, int length) {
                    if (mutated.compareAndSet(false, true)) {
                        connection.add(Values.factory().createIRI("urn:late"),
                                Values.factory().createIRI("urn:p"), Values.factory().createIRI("urn:o"));
                    }
                    super.write(buffer, offset, length);
                }
            };
            connection.export(writer, RDFFormat.NTRIPLES);
            assertTrue(mutated.get());
            assertEquals(1_000, writer.toString().lines().count());
            assertFalse(writer.toString().contains("urn:late"));
            assertEquals(1_001, connection.size());
        }
    }

    @Test
    void callbackErrorsAlsoRollBackAndReleaseTheOwnedTransaction() {
        try (var repository = Repositories.create(); var connection = repository.getConnection()) {
            AssertionError original = new AssertionError("callback failed");
            assertSame(original, assertThrows(AssertionError.class, () -> connection.inTransaction(current -> {
                current.load(new StringReader(TRIPLE), RDFFormat.NTRIPLES);
                throw original;
            })));
            assertFalse(connection.isActive());
            assertEquals(0, connection.size());
        }
    }

    @Test
    void failedCommitKeepsOriginalFailureAndSuppressesRollbackFailure() {
        var connection = mock(RepositoryConnection.class, CALLS_REAL_METHODS);
        var commitFailure = new RepositoryException("commit failed");
        var rollbackFailure = new RepositoryException("rollback failed");
        when(connection.isActive()).thenReturn(true);
        doThrow(commitFailure).when(connection).commit();
        doThrow(rollbackFailure).when(connection).rollback();
        assertSame(commitFailure, assertThrows(RepositoryException.class,
                () -> connection.computeInTransaction(current -> 42)));
        assertArrayEquals(new Throwable[] { rollbackFailure }, commitFailure.getSuppressed());
        verify(connection).begin();
        verify(connection).rollback();
    }
}
