package fr.inria.corese.core.next.query.impl.engine.event;

import fr.inria.corese.core.next.query.impl.engine.eval.Eval;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;

import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.event.ProcessVisitor;
import fr.inria.corese.core.next.data.api.model.DatatypeValue;

/** No-op process visitor that forwards reporting events to the query binding context. */
@SuppressWarnings("java:S4144") // Distinct visitor callbacks intentionally share no-op behavior.
public final class ProcessVisitorDefault implements ProcessVisitor {

    @Override
    public int slice() {
        return ProcessVisitor.SLICE_DEFAULT;
    }


    @Override
    public DatatypeValue defaultValue() {
        return null;
    }


    void visit(Eval eval, Node g, Exp e, Mappings m1, Mappings m2) {
        if (eval.getQuery().isReportEnabled()) {
            eval.getBind().visit(e, g, m1, m2);
        }
    }


    @Override
    public DatatypeValue graph(Eval eval, Node g, Exp e, Mappings m1) {
        visit(eval, g, e, m1, null);
        return defaultValue();
    }

    @Override
    public DatatypeValue query(Eval eval, Node g, Exp e, Mappings m1) {
        visit(eval, g, e, m1, null);
        return defaultValue();
    }

    @Override
    public DatatypeValue service(Eval eval, Node g, Exp e, Mappings m1) {
        visit(eval, g, e, m1, null);
        return defaultValue();
    }

    @Override
    public DatatypeValue optional(Eval eval, Node g, Exp e, Mappings m1, Mappings m2) {
        visit(eval, g, e, m1, m2);
        return defaultValue();
    }

    @Override
    public DatatypeValue minus(Eval eval, Node g, Exp e, Mappings m1, Mappings m2) {
        visit(eval, g, e, m1, m2);
        return defaultValue();
    }

    @Override
    public DatatypeValue union(Eval eval, Node g, Exp e, Mappings m1, Mappings m2) {
        visit(eval, g, e, m1, m2);
        return defaultValue();
    }

    @Override
    public DatatypeValue join(Eval eval, Node g, Exp e, Mappings m1, Mappings m2) {
        visit(eval, g, e, m1, m2);
        return defaultValue();
    }
}
