package fr.inria.corese.core.next.query.impl.sparql.update;

import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.lifecycle.StorageLifecycle;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;
import fr.inria.corese.core.next.storage.api.operations.MetadataOperations;
import fr.inria.corese.core.next.storage.api.operations.MutationOperations;
import fr.inria.corese.core.next.storage.api.operations.QueryOperations;
import fr.inria.corese.core.next.storage.api.transaction.TransactionManager;

import java.util.stream.Stream;

/**
 * Scoped storage view for SPARQL update WHERE clauses.
 *
 * <p>Ensures that queries matching the default graph target the store's unnamed graph
 * or the graph specified by the {@code WITH} clause, never the union graph.</p>
 *
 * @param storage      the underlying storage manager
 * @param defaultGraph the effective default graph resource (or {@code null} for the unnamed default graph)
 */
record UpdateDatasetView(StorageManager storage, Resource defaultGraph) implements StorageManager, QueryOperations {
    @Override
    public Stream<Statement> find(StatementPattern pattern) {
        if (pattern.getContexts().length == 0) {
            return storage.queries().find(StatementPattern.of(pattern.getSubject(), pattern.getPredicate(),
                    pattern.getObject(), defaultGraph));
        }
        return storage.queries().find(pattern);
    }

    @Override
    public long count(StatementPattern pattern) {
        try (Stream<Statement> statements = find(pattern)) {
            return statements.count();
        }
    }

    @Override
    public boolean contains(StatementPattern pattern) {
        try (Stream<Statement> statements = find(pattern)) {
            return statements.findAny().isPresent();
        }
    }

    @Override
    public QueryOperations queries() {
        return this;
    }

    @Override
    public MutationOperations mutations() {
        return storage.mutations();
    }

    @Override
    public MetadataOperations metadata() {
        return storage.metadata();
    }

    @Override
    public TransactionManager transactions() {
        return storage.transactions();
    }

    @Override
    public StorageLifecycle lifecycle() {
        return storage.lifecycle();
    }
}
