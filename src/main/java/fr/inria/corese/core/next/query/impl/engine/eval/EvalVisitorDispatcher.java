package fr.inria.corese.core.next.query.impl.engine.eval;

import fr.inria.corese.core.next.query.impl.engine.event.ProcessVisitor;
import fr.inria.corese.core.next.query.impl.engine.event.ProcessVisitorDefault;

import java.util.Objects;

/**
 * Coordinates and dispatches execution lifecycle visitors for SPARQL evaluation in {@link Eval}.
 *
 * <p>Encapsulates the active {@link ProcessVisitor} for query observation and reporting.</p>
 */
public final class EvalVisitorDispatcher {

    private ProcessVisitor visitor;

    private boolean hasStatement = false;
    private boolean hasCandidate = false;
    private boolean hasFilter = false;

    public EvalVisitorDispatcher() {
        setVisitor(new ProcessVisitorDefault());
    }

    // --- Visitor ---

    public ProcessVisitor getVisitor() {
        return visitor;
    }

    public void setVisitor(ProcessVisitor visitor) {
        this.visitor = Objects.requireNonNullElseGet(visitor, ProcessVisitorDefault::new);
        updateVisitorFlags();
    }

    public void updateVisitorFlags() {
        this.hasStatement = visitor.statement();
        this.hasCandidate = visitor.candidate();
        this.hasFilter = visitor.filter();
    }

    public boolean hasStatement() {
        return hasStatement;
    }

    public boolean hasCandidate() {
        return hasCandidate;
    }

    public boolean hasFilter() {
        return hasFilter;
    }
}
