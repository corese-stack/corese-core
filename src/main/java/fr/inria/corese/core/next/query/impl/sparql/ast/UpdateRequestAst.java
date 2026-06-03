package fr.inria.corese.core.next.query.impl.sparql.ast;

import java.util.List;

/**
 * Represents a series of update operations sharing a prologue
 */
public record UpdateRequestAst(QueryPrologueAst prologue, List<UpdateRequestUnitAst> operations)
        implements QueryAst {
    public UpdateRequestAst {
        prologue = prologue != null ? prologue : QueryPrologueAst.empty();
        operations = operations != null ? List.copyOf(operations) : List.of();
    }

    public void addQuery(UpdateRequestUnitAst updateQuery) {
        this.operations.add(updateQuery);
    }
}
