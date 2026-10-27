package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;
import fr.inria.corese.core.next.storage.api.operations.StatementSpliterator;

import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Single-consumer cursor owning a captured read state until close, exhaustion or failure.
 *
 * <p>
 * Known-size traversal can release ownership before delivering the last immutable statement. Close is idempotent
 * and clears traversal references under the monitor. No database monitor is held while visiting rows or invoking
 * consumer code. Session and transaction closure do not invalidate this independent read snapshot.
 * </p>
 */
final class SnapshotStatementSpliterator implements StatementSpliterator, AutoCloseable {

    private final Object monitor;
    private PartitionState snapshot;
    private Iterator<GraphPartition> partitions;
    private TripleIndexCursor rows;
    private final StatementPattern pattern;
    private final TermKey subject;
    private final TermKey predicate;
    private final TermKey object;
    private final boolean sized;
    private final boolean distinctTriples;
    private long remaining;

    SnapshotStatementSpliterator(final Object monitor, final PartitionState snapshot, final StatementPattern pattern) {
        this.monitor = Objects.requireNonNull(monitor, "monitor");
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        this.pattern = Objects.requireNonNull(pattern, "pattern");
        this.distinctTriples = hasAtMostOnePopulatedPartition(snapshot);
        this.subject = TermKey.nullable(pattern.getSubject());
        this.predicate = TermKey.nullable(pattern.getPredicate());
        this.object = TermKey.nullable(pattern.getObject());
        this.sized = subject == null && predicate == null && object == null;
        this.remaining = sized ? snapshot.size : Long.MAX_VALUE;
        this.partitions = snapshot.graphs.values().iterator();
    }

    private static boolean hasAtMostOnePopulatedPartition(final PartitionState snapshot) {
        if (snapshot.graphs.size() <= 1) {
            return true;
        }
        boolean populated = false;
        for (final GraphPartition partition : snapshot.graphs.values()) {
            if (partition.spo.size() == 0) {
                continue;
            }
            if (populated) {
                return false;
            }
            populated = true;
        }
        return true;
    }

    @SuppressWarnings("java:S1181")
    @Override
    public boolean tryAdvance(final Consumer<? super Statement> action) {
        Objects.requireNonNull(action, "action");
        if (snapshot == null) {
            return false;
        }
        try {
            for (;;) {
                final Statement statement = rows == null ? null : rows.next();
                if (statement != null) {
                    if (sized && --remaining == 0) {
                        close();
                    }
                    action.accept(statement);
                    return true;
                }
                if (!partitions.hasNext()) {
                    close();
                    return false;
                }
                rows = partitions.next().index(pattern).cursor(subject, predicate, object);
            }
        } catch (final RuntimeException | Error failure) {
            close();
            throw failure;
        }
    }

    @Override
    public void forEachRemaining(final Consumer<? super Statement> action) {
        Objects.requireNonNull(action, "action");
        if (snapshot == null) {
            return;
        }
        if (rows != null) {
            StatementSpliterator.super.forEachRemaining(action);
            return;
        }
        try {
            final Predicate<Statement> visitor = statement -> {
                if (sized && --remaining == 0) {
                    close();
                }
                action.accept(statement);
                return snapshot == null;
            };
            while (snapshot != null && partitions.hasNext()) {
                partitions.next().index(pattern).visit(subject, predicate, object, visitor);
            }
        } finally {
            close();
        }
    }

    @Override
    public boolean hasDistinctTriples() {
        return distinctTriples;
    }

    @Override
    public Spliterator<Statement> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return snapshot == null ? 0 : remaining;
    }

    @Override
    public int characteristics() {
        return ORDERED | NONNULL | IMMUTABLE | (sized ? SIZED | SUBSIZED : 0);
    }

    @Override
    public void close() {
        synchronized (monitor) {
            if (snapshot == null) {
                return;
            }
            snapshot.release();
            snapshot = null;
            partitions = null;
            rows = null;
        }
    }
}
