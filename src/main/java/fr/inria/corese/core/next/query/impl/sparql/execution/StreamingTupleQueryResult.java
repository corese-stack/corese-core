package fr.inria.corese.core.next.query.impl.sparql.execution;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.QueryTimeoutException;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.impl.engine.diagnostics.ExecutionDiagnostics;
import fr.inria.corese.core.next.query.impl.engine.eval.SelectCursor;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.pattern.QueryPlan;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;
import fr.inria.corese.core.next.query.impl.engine.storage.StorageManagerProducer;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.exception.StorageException;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Caller-thread SELECT result owning its complete read view. Advancing is confined
 * to the creating thread; close may cancel it from another thread. A deadline spans
 * preparation, traversal and client idle time, but interrupts the owner only while
 * it is advancing this cursor. Expiration releases snapshots even for an idle client.
 */
final class StreamingTupleQueryResult implements TupleQueryResult {
    /** Shared daemon timers; cancelled tasks must not retain closed query snapshots. */
    private static final ScheduledThreadPoolExecutor DEADLINES = deadlines();

    private static ScheduledThreadPoolExecutor deadlines() {
        var timers = new ScheduledThreadPoolExecutor(2,
                Thread.ofPlatform().daemon(true).name("sparql-cursor-timeout-", 0).factory());
        timers.setRemoveOnCancelPolicy(true);
        return timers;
    }

    private final Object cancellation = new Object();
    private final Thread owner = Thread.currentThread();
    private final SelectCursor cursor;
    private final StorageManagerProducer producer;
    private final StorageManager snapshot;
    private final ExecutionDiagnostics diagnostics;
    private final List<String> names;
    private final Set<String> projected;
    private final long timeoutMillis;
    private final long started;
    private final ScheduledFuture<?> deadline;
    private boolean advancing;
    private boolean viewReleased;
    private volatile boolean timedOut;
    private volatile boolean closed;
    private boolean exhausted;
    private BindingSet delivered;
    private boolean unwound;

    StreamingTupleQueryResult(SelectCursor cursor, QueryPlan query,
            StorageManagerProducer producer, StorageManager snapshot, ExecutionDiagnostics diagnostics,
            long timeoutMillis, long started) {
        this.cursor = cursor;
        this.producer = producer;
        this.snapshot = snapshot;
        this.diagnostics = diagnostics;
        this.timeoutMillis = timeoutMillis;
        this.started = started;
        names = query.getSelect().stream().map(Node::getLabel).toList();
        projected = Collections.unmodifiableSet(new LinkedHashSet<>(names));
        long remaining = TimeUnit.MILLISECONDS.toNanos(timeoutMillis) - (System.nanoTime() - started);
        deadline = timeoutMillis <= 0 ? null
                : DEADLINES.schedule(this::expire, Math.max(0, remaining), TimeUnit.NANOSECONDS);
    }

    private void expire() {
        synchronized (cancellation) {
            if (closed || exhausted || timedOut) return;
            timedOut = true;
            if (advancing) owner.interrupt();
            cursor.cancel();
            releaseView();
        }
    }

    @Override
    public List<String> getBindingNames() {
        return names;
    }

    @Override
    public boolean hasNext() {
        return advance(false);
    }

    @SuppressWarnings("java:S1181") // Release owned snapshots even on fatal traversal errors, then rethrow unchanged.
    private boolean advance(boolean consume) {
        checkOwner();
        if (diagnostics != null) diagnostics.beginWork();
        try {
            if (!beginAdvance()) return false;
            boolean more = cursor.hasNext();
            if (more && consume) delivered = new CoreseBindingSet(cursor.next(), projected);
            endAdvance();
            if (!more || cursor.isFinished()) finish();
            if (diagnostics != null) diagnostics.firstRow(more);
            return more;
        } catch (RuntimeException | Error failure) {
            traversalFailed(failure);
            throw failure;
        } finally {
            if (diagnostics != null) diagnostics.evaluationTime(diagnostics.endWork());
        }
    }

    /**
     * Consumes unsliced ordered input under the same snapshot and deadline as a
     * progressive read. Only the bounded selection is materialized, not all rows.
     *
     * @param selector evaluation-owned collector using Corese's ordering comparator
     * @return ordered final mappings after LIMIT and OFFSET
     */
    @SuppressWarnings("java:S1181") // Fatal traversal failures must release the same owned resources as ordinary reads.
    Mappings collectOrdered(TopKSelector selector) {
        checkOwner();
        long startedWork = diagnostics == null ? 0 : System.nanoTime();
        if (diagnostics != null) diagnostics.beginWork();
        try {
            beginAdvance();
            if (selector.needsInput()) {
                while (cursor.hasNext()) selector.offer(cursor.next());
            }
            Mappings selected = selector.finish();
            endAdvance();
            finish();
            if (diagnostics != null) diagnostics.firstRow(!selected.isEmpty());
            return selected;
        } catch (RuntimeException | Error failure) {
            traversalFailed(failure);
            throw failure;
        } finally {
            if (diagnostics != null) {
                diagnostics.endWork();
                diagnostics.evaluationTime(System.nanoTime() - startedWork);
            }
        }
    }

    private void traversalFailed(Throwable failure) {
        failedAdvance(failure);
        if (timedOut && !(failure instanceof Error)) throw new QueryTimeoutException(timeoutMillis);
        if (failure instanceof StorageException) {
            throw new QueryEvaluationException("Storage access failed during query evaluation", failure);
        }
    }

    private boolean beginAdvance() {
        synchronized (cancellation) {
            if (!closed && !exhausted && timeoutMillis > 0
                    && System.nanoTime() - started >= TimeUnit.MILLISECONDS.toNanos(timeoutMillis)) expire();
            if (timedOut) throw new QueryTimeoutException(timeoutMillis);
            if (closed) throw new IllegalStateException("This tuple query result is closed.");
            if (exhausted) return false;
            if (owner.isInterrupted()) throw new QueryEvaluationException("QueryPlan interrupted");
            advancing = true;
            return true;
        }
    }

    private void endAdvance() {
        synchronized (cancellation) {
            advancing = false;
            if (timedOut) {
                Thread.interrupted();
                throw new QueryTimeoutException(timeoutMillis);
            }
            if (closed) throw new QueryEvaluationException("QueryPlan cursor was cancelled");
        }
    }

    @SuppressWarnings("java:S1181") // Preserve the traversal failure if releasing resources also fails.
    private void failedAdvance(Throwable failure) {
        synchronized (cancellation) {
            if (timedOut && advancing) Thread.interrupted();
            advancing = false;
        }
        try {
            close(false);
        } catch (RuntimeException | Error cleanup) {
            if (cleanup != failure) failure.addSuppressed(cleanup);
        }
    }

    @Override
    public BindingSet next() {
        if (!advance(true)) throw new NoSuchElementException();
        BindingSet row = delivered;
        delivered = null;
        return row;
    }

    private void checkOwner() {
        if (Thread.currentThread() != owner) {
            throw new IllegalStateException("QueryPlan results must be consumed on their creating thread");
        }
    }

    private void finish() {
        synchronized (cancellation) {
            exhausted = true;
            if (deadline != null) deadline.cancel(false);
            releaseView();
        }
    }

    /** Called with the cancellation monitor; read-snapshot close is cross-thread safe. */
    private void releaseView() {
        if (viewReleased) return;
        viewReleased = true;
        try {
            producer.close();
        } finally {
            snapshot.close();
        }
    }

    @Override
    public void close() {
        close(true);
    }

    private void close(boolean measureClose) {
        boolean onOwner = Thread.currentThread() == owner;
        boolean measure = measureClose && onOwner && !unwound && diagnostics != null;
        if (measure) diagnostics.beginWork();
        try {
            synchronized (cancellation) {
                closed = true;
                if (advancing && Thread.currentThread() != owner) owner.interrupt();
                if (deadline != null) deadline.cancel(false);
                cursor.cancel();
                releaseView();
            }
        } finally {
            // Only the owner unwinds bindings. Cancellation already releases storage.
            if (onOwner) {
                delivered = null;
                unwound = true;
                try {
                    cursor.close();
                } finally {
                    if (measure) diagnostics.evaluationTime(diagnostics.endWork());
                }
            }
        }
    }
}
