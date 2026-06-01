package fr.inria.corese.core.next.query.impl.sparql.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a series of update operations sharing a prologue
 */
public final class UpdateRequestAst implements QueryAst {
    private QueryPrologueAst prologue;
    private final List<UpdateRequestUnitAst> operations;

    public UpdateRequestAst(QueryPrologueAst prologue, List<UpdateRequestUnitAst> operations) {
        this.prologue = prologue == null ? QueryPrologueAst.empty() : prologue;
        this.operations = new ArrayList<>();
        if (operations != null) {
            this.operations.addAll(operations);
        }
    }

    @Override
    public QueryPrologueAst prologue() {
        return this.prologue;
    }

    public void addQuery(UpdateRequestUnitAst updateQuery) {
        this.operations.add(updateQuery);
    }

    public List<UpdateRequestUnitAst> operations() {
        return operations;
    }
}
