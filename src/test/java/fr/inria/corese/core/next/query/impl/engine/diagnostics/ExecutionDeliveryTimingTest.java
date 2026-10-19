package fr.inria.corese.core.next.query.impl.engine.diagnostics;

import fr.inria.corese.core.next.query.api.diagnostics.QueryExecutionProfile.ExecutionMode;
import fr.inria.corese.core.next.query.api.diagnostics.QueryExecutionProfile.FallbackReason;
import fr.inria.corese.core.next.query.api.diagnostics.QueryExecutionProfile.FirstRowStatus;
import fr.inria.corese.core.next.query.api.diagnostics.QueryExecutionProfile.ResultDelivery;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/** Deterministic timing contracts: client pauses are not query execution time. */
class ExecutionDeliveryTimingTest {
    @Test
    void includesInitialWorkAndFirstAdvanceButExcludesClientIdleTime() {
        var time = new AtomicLong(10);
        var diagnostics = new ExecutionDiagnostics(time::get);
        diagnostics.selectDelivery(FallbackReason.NONE);
        diagnostics.beginWork();
        time.addAndGet(100);
        assertEquals(100, diagnostics.endWork());
        var before = diagnostics.snapshot();

        time.addAndGet(10_000);
        diagnostics.beginWork();
        time.addAndGet(40);
        diagnostics.firstRow(true);
        time.addAndGet(60);
        assertEquals(100, diagnostics.endWork());

        assertEquals(140, diagnostics.snapshot().delivery().firstRowNanos());
        assertEquals(FirstRowStatus.UNOBSERVED, before.delivery().firstRowStatus());
        assertEquals(-1, before.delivery().firstRowNanos());
        time.addAndGet(50_000);
        diagnostics.beginWork();
        time.addAndGet(80);
        diagnostics.firstRow(false);
        diagnostics.endWork();
        assertEquals(140, diagnostics.snapshot().delivery().firstRowNanos());
        assertEquals(FirstRowStatus.AVAILABLE, diagnostics.snapshot().delivery().firstRowStatus());
    }

    @Test
    void nestedCompilationOrCleanupScopesDoNotCountWorkTwice() {
        var time = new AtomicLong();
        var diagnostics = new ExecutionDiagnostics(time::get);
        diagnostics.selectDelivery(FallbackReason.NONE);
        diagnostics.beginWork();
        time.addAndGet(10);
        diagnostics.beginWork();
        time.addAndGet(20);
        assertEquals(0, diagnostics.endWork());
        time.addAndGet(10);
        diagnostics.firstRow(true);
        assertEquals(40, diagnostics.endWork());
        assertEquals(40, diagnostics.snapshot().delivery().firstRowNanos());
        assertThrows(IllegalStateException.class, diagnostics::endWork);
    }

    @Test
    void emptyResultsHaveAnExplicitStatusAndNoInventedFirstRowTime() {
        var time = new AtomicLong();
        var diagnostics = new ExecutionDiagnostics(time::get);
        diagnostics.selectDelivery(FallbackReason.ORDER_BY);
        diagnostics.beginWork();
        time.addAndGet(100);
        diagnostics.firstRow(false);
        diagnostics.endWork();
        diagnostics.firstRow(true);
        var delivery = diagnostics.snapshot().delivery();
        assertEquals(ExecutionMode.MATERIALIZED, delivery.mode());
        assertEquals(FallbackReason.ORDER_BY, delivery.fallbackReason());
        assertEquals(FirstRowStatus.EMPTY, delivery.firstRowStatus());
        assertEquals(-1, delivery.firstRowNanos());
    }

    @Test
    void aNanotimeWrapDoesNotChangeAnElapsedDuration() {
        var time = new AtomicLong(Long.MAX_VALUE - 20);
        var diagnostics = new ExecutionDiagnostics(time::get);
        diagnostics.selectDelivery(FallbackReason.NONE);
        diagnostics.beginWork();
        time.addAndGet(40);
        diagnostics.firstRow(true);
        diagnostics.endWork();
        assertEquals(40, diagnostics.snapshot().delivery().firstRowNanos());
    }

    @Test
    void nonSelectFormsHaveNoFirstRowMetric() {
        var diagnostics = new ExecutionDiagnostics();
        diagnostics.firstRow(true);
        var delivery = diagnostics.snapshot().delivery();
        assertEquals(FallbackReason.QUERY_FORM, delivery.fallbackReason());
        assertEquals(FirstRowStatus.NOT_APPLICABLE, delivery.firstRowStatus());
        assertEquals(-1, delivery.firstRowNanos());
        assertThrows(IllegalArgumentException.class, () -> diagnostics.selectDelivery(FallbackReason.QUERY_FORM));
    }

    @Test
    void deliveryCannotClaimStreamingWithAMaterializationReasonOrATimeWithoutARow() {
        assertThrows(IllegalArgumentException.class, () -> new ResultDelivery(ExecutionMode.STREAMING,
                FallbackReason.ORDER_BY, FirstRowStatus.UNOBSERVED, -1));
        assertThrows(IllegalArgumentException.class, () -> new ResultDelivery(ExecutionMode.MATERIALIZED,
                FallbackReason.NONE, FirstRowStatus.UNOBSERVED, -1));
        assertThrows(IllegalArgumentException.class, () -> new ResultDelivery(ExecutionMode.STREAMING,
                FallbackReason.NONE, FirstRowStatus.AVAILABLE, -1));
        assertThrows(IllegalArgumentException.class, () -> new ResultDelivery(ExecutionMode.STREAMING,
                FallbackReason.NONE, FirstRowStatus.EMPTY, 0));
        assertThrows(IllegalArgumentException.class, () -> new ResultDelivery(ExecutionMode.STREAMING,
                FallbackReason.NONE, FirstRowStatus.NOT_APPLICABLE, -1));
        assertThrows(IllegalArgumentException.class, () -> new ResultDelivery(ExecutionMode.MATERIALIZED,
                FallbackReason.QUERY_FORM, FirstRowStatus.AVAILABLE, 20));
    }
}
