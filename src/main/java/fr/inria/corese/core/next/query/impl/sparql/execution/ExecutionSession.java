package fr.inria.corese.core.next.query.impl.sparql.execution;

import fr.inria.corese.core.next.query.impl.engine.diagnostics.DiagnosticProducer;
import fr.inria.corese.core.next.query.impl.engine.diagnostics.ExecutionDiagnostics;
import fr.inria.corese.core.next.query.impl.engine.eval.Eval;
import fr.inria.corese.core.next.query.impl.engine.eval.RdfTermMatcher;
import fr.inria.corese.core.next.query.impl.engine.eval.SparqlKgramEvaluator;
import fr.inria.corese.core.next.query.impl.engine.storage.StorageManagerProducer;
import fr.inria.corese.core.next.storage.api.StorageManager;

import java.util.Objects;

/**
 * Encapsulates the execution resources and lifecycle (storage producer and engine evaluator)
 * for a single query execution.
 */
final class ExecutionSession implements AutoCloseable {

    private final StorageManagerProducer producer;
    private final Eval eval;

    ExecutionSession(StorageManager storage, ExecutionDiagnostics diagnostics) {
        Objects.requireNonNull(storage, "storage");
        StorageManagerProducer p = diagnostics == null
                ? new StorageManagerProducer(storage)
                : new DiagnosticProducer(storage, diagnostics);
        try {
            this.eval = Eval.create(p, new SparqlKgramEvaluator(), new RdfTermMatcher());
            this.producer = p;
        } catch (RuntimeException | Error failure) {
            p.close();
            throw failure;
        }
    }

    StorageManagerProducer producer() {
        return producer;
    }

    Eval eval() {
        return eval;
    }

    @Override
    public void close() {
        producer.close();
    }
}
