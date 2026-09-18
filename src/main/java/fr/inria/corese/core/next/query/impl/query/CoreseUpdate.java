package fr.inria.corese.core.next.query.impl.query;

import fr.inria.corese.core.next.query.api.Update;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.UpdateRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlParser;
import fr.inria.corese.core.next.query.impl.sparql.update.NextSparqlUpdateExecutor;
import fr.inria.corese.core.next.query.impl.sparql.update.UpdateTransaction;
import fr.inria.corese.core.next.storage.api.StorageManager;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Prepared SPARQL 1.1 update request, with connection-owned transaction execution.
 */
public final class CoreseUpdate implements Update {
    private final String updateString;
    private final StorageManager storage;
    private final SparqlParser parser;
    private final Runnable executionGuard;
    private final Consumer<Runnable> transaction;

    /**
     * Constructs a prepared SPARQL update request with default transactional execution.
     *
     * @param updateString   the SPARQL update query string (must not be {@code null})
     * @param storage        the storage manager to execute updates against (must not be {@code null})
     * @param parser         the SPARQL parser (must not be {@code null})
     * @param executionGuard guard runnable executed before update execution (must not be {@code null})
     * @throws NullPointerException if any argument is {@code null}
     */
    public CoreseUpdate(String updateString, StorageManager storage, SparqlParser parser, Runnable executionGuard) {
        this(updateString, storage, parser, executionGuard, action -> UpdateTransaction.execute(storage, action));
    }

    /**
     * Constructs a prepared SPARQL update request with a custom transaction coordinator.
     *
     * @param updateString   the SPARQL update query string (must not be {@code null})
     * @param storage        the storage manager to execute updates against (must not be {@code null})
     * @param parser         the SPARQL parser (must not be {@code null})
     * @param executionGuard guard runnable executed before update execution (must not be {@code null})
     * @param transaction    transaction coordinator accepting update execution runnable (must not be {@code null})
     * @throws NullPointerException if any argument is {@code null}
     */
    public CoreseUpdate(String updateString, StorageManager storage, SparqlParser parser,
            Runnable executionGuard, Consumer<Runnable> transaction) {
        this.updateString = Objects.requireNonNull(updateString, "updateString");
        this.storage = Objects.requireNonNull(storage, "storage");
        this.parser = Objects.requireNonNull(parser, "parser");
        this.executionGuard = Objects.requireNonNull(executionGuard, "executionGuard");
        this.transaction = Objects.requireNonNull(transaction, "transaction");
    }

    /**
     * {@inheritDoc}
     *
     * @throws QueryEvaluationException if update parsing or execution fails
     */
    @Override
    public void execute() throws QueryEvaluationException {
        executionGuard.run();
        UpdateRequestAst request = (UpdateRequestAst) parser.parse(updateString);
        NextSparqlUpdateExecutor executor = new NextSparqlUpdateExecutor(storage, request.prologue());
        transaction.accept(() -> executor.execute(request));
    }
}
