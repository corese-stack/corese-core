package fr.inria.corese.core.next.query.impl.engine.eval;

import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.query.impl.engine.event.ProcessVisitor;
import fr.inria.corese.core.next.query.impl.engine.event.ProcessVisitorDefault;
import fr.inria.corese.core.next.query.impl.engine.event.ResultListener;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Expr;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;

import java.util.Objects;

/**
 * Coordinates and dispatches execution lifecycle visitors and result listeners
 * for SPARQL evaluation in {@link Eval}.
 *
 * <p>Encapsulates the active {@link ProcessVisitor} (for query observation and reporting)
 * and the optional {@link ResultListener} (for inference rule execution).</p>
 */
public final class EvalVisitorDispatcher {

    private ResultListener listener;
    private ProcessVisitor visitor;

    private boolean hasListener = false;
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
        if (visitor != null) {
            this.hasStatement = visitor.statement();
            this.hasCandidate = visitor.candidate();
            this.hasFilter = visitor.filter();
        } else {
            this.hasStatement = false;
            this.hasCandidate = false;
            this.hasFilter = false;
        }
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

    // --- Listener ---

    public ResultListener getListener() {
        return listener;
    }

    public void setListener(ResultListener listener) {
        this.listener = listener;
        this.hasListener = (listener != null);
    }

    public boolean hasListener() {
        return hasListener;
    }

    public Exp listen(Exp exp, int n) {
        if (hasListener) {
            return listener.listen(exp, n);
        }
        return exp;
    }

    public void listen(Expr exp) {
        if (hasListener) {
            listener.listen(exp);
        }
    }

    public boolean listen(Exp exp, Edge qEdge, Edge entity) {
        if (hasListener) {
            return listener.listen(exp, qEdge, entity);
        }
        return true;
    }
}
