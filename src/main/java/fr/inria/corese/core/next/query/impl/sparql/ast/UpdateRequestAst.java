package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;

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

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this.prologue);
        this.operations.forEach(updateRequestUnitAst -> {
            updateRequestUnitAst.accept(visitor);
        });
    }
}
