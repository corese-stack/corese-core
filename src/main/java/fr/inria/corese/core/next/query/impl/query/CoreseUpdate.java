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

/** Prepared SPARQL 1.1 update request, with connection-owned transaction execution. */
public final class CoreseUpdate implements Update {
    private final String updateString;
    private final StorageManager storage;
    private final SparqlParser parser;
    private final Runnable executionGuard;
    private final Consumer<Runnable> transaction;

    public CoreseUpdate(String updateString, StorageManager storage, SparqlParser parser, Runnable executionGuard) {
        this(updateString, storage, parser, executionGuard, action -> UpdateTransaction.execute(storage, action));
    }

    public CoreseUpdate(String updateString, StorageManager storage, SparqlParser parser,
            Runnable executionGuard, Consumer<Runnable> transaction) {
        this.updateString = Objects.requireNonNull(updateString, "updateString");
        this.storage = Objects.requireNonNull(storage, "storage");
        this.parser = Objects.requireNonNull(parser, "parser");
        this.executionGuard = Objects.requireNonNull(executionGuard, "executionGuard");
        this.transaction = Objects.requireNonNull(transaction, "transaction");
    }

    @Override
    public void execute() throws QueryEvaluationException {
        executionGuard.run();
        UpdateRequestAst request = (UpdateRequestAst) parser.parse(updateString);
        NextSparqlUpdateExecutor executor = new NextSparqlUpdateExecutor(storage, request.prologue());
        transaction.accept(() -> executor.execute(request));
    }
}
