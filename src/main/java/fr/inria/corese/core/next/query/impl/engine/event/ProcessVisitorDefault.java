package fr.inria.corese.core.next.query.impl.engine.event;

import fr.inria.corese.core.next.query.impl.engine.eval.Eval;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;

/**
 * Default process visitor that forwards reporting events to the query binding context
 * when query report is enabled.
 */
public final class ProcessVisitorDefault implements ProcessVisitor {

    void visit(Eval eval, Node g, Exp e, Mappings m1, Mappings m2) {
        if (eval.getQuery().isReportEnabled()) {
            eval.getBind().visit(e, g, m1, m2);
        }
    }

    @Override
    public void graph(Eval eval, Node g, Exp e, Mappings m1) {
        visit(eval, g, e, m1, null);
    }

    @Override
    public void query(Eval eval, Node g, Exp e, Mappings m1) {
        visit(eval, g, e, m1, null);
    }

    @Override
    public void service(Eval eval, Node g, Exp e, Mappings m1) {
        visit(eval, g, e, m1, null);
    }

    @Override
    public void optional(Eval eval, Node g, Exp e, Mappings m1, Mappings m2) {
        visit(eval, g, e, m1, m2);
    }

    @Override
    public void minus(Eval eval, Node g, Exp e, Mappings m1, Mappings m2) {
        visit(eval, g, e, m1, m2);
    }

    @Override
    public void union(Eval eval, Node g, Exp e, Mappings m1, Mappings m2) {
        visit(eval, g, e, m1, m2);
    }

    @Override
    public void join(Eval eval, Node g, Exp e, Mappings m1, Mappings m2) {
        visit(eval, g, e, m1, m2);
    }

}
