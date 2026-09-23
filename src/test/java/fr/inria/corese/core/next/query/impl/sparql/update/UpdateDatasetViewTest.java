package fr.inria.corese.core.next.query.impl.sparql.update;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;
import fr.inria.corese.core.next.storage.api.operations.QueryOperations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UpdateDatasetView#find(StatementPattern)}.
 *
 * <p>Verifies that patterns targeting the default graph (no contexts, or a single null context)
 * are redirected to the {@code defaultGraph} resource, while patterns carrying an explicit named
 * context are forwarded to the underlying storage unchanged.</p>
 */
@DisplayName("UpdateDatasetView - default-graph routing in find()")
class UpdateDatasetViewTest {

    private static final ValueFactory FACTORY = Values.factory();
    private static final String EX = "http://example.org/";

    private QueryOperations queryOps;
    private IRI defaultGraph;
    private UpdateDatasetView view;

    @BeforeEach
    void setUp() {
        StorageManager storage = mock(StorageManager.class);
        queryOps = mock(QueryOperations.class);
        when(storage.queries()).thenReturn(queryOps);
        when(queryOps.find(any())).thenReturn(Stream.empty());

        defaultGraph = FACTORY.createIRI(EX + "default");
        view = new UpdateDatasetView(storage, defaultGraph);
    }

    @Test
    @DisplayName("pattern with no contexts is redirected to the WITH/default graph")
    void noContextsAreRedirectedToDefaultGraph() {
        StatementPattern pattern = StatementPattern.matchAll(); // empty contexts

        view.find(pattern);

        ArgumentCaptor<StatementPattern> captor = ArgumentCaptor.forClass(StatementPattern.class);
        verify(queryOps).find(captor.capture());
        Resource[] forwarded = captor.getValue().getContexts();
        assertEquals(1, forwarded.length);
        assertEquals(defaultGraph, forwarded[0],
                "pattern without contexts must be forwarded targeting the default graph");
    }


    @Test
    @DisplayName("null context is redirected to the WITH graph without changing triple terms")
    void nullContextIsRedirectedToDefaultGraph() {
        IRI subject = FACTORY.createIRI(EX + "s");
        IRI predicate = FACTORY.createIRI(EX + "p");
        IRI object = FACTORY.createIRI(EX + "o");
        StatementPattern pattern = StatementPattern.of(subject, predicate, object, (Resource) null);

        try (var statements = view.find(pattern)) {
            assertEquals(0, statements.count());
        }

        ArgumentCaptor<StatementPattern> captor = ArgumentCaptor.forClass(StatementPattern.class);
        verify(queryOps).find(captor.capture());
        StatementPattern forwarded = captor.getValue();
        assertArrayEquals(new Resource[]{defaultGraph}, forwarded.getContexts());
        assertEquals(subject, forwarded.getSubject());
        assertEquals(predicate, forwarded.getPredicate());
        assertEquals(object, forwarded.getObject());
    }

    @Test
    @DisplayName("pattern with a named context is passed through unchanged")
    void namedContextIsPassedThroughUnchanged() {
        IRI namedGraph = FACTORY.createIRI(EX + "named");
        StatementPattern pattern = StatementPattern.of(null, null, null, namedGraph);

        view.find(pattern);

        ArgumentCaptor<StatementPattern> captor = ArgumentCaptor.forClass(StatementPattern.class);
        verify(queryOps).find(captor.capture());
        assertSame(pattern, captor.getValue(),
                "pattern with a named context must be forwarded as-is without rewriting");
    }
}
