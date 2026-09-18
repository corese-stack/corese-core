package fr.inria.corese.core.next.query.impl.sparql.ast;

import java.util.List;

import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.AstVisitor;

/**
 * Represents ordered update operations and their effective PREFIX/BASE snapshots.
 *
 * @param prologue           the global query prologue
 * @param operations         the list of update operations
 * @param operationPrologues the prologue in effect for each operation
 */
public record UpdateRequestAst(QueryPrologueAst prologue, List<UpdateRequestUnitAst> operations,
        List<QueryPrologueAst> operationPrologues)
        implements QueryAst {

    /**
     * Constructs an update request where all operations share the same prologue.
     *
     * @param prologue   the query prologue
     * @param operations the list of update operations
     */
    public UpdateRequestAst(QueryPrologueAst prologue, List<UpdateRequestUnitAst> operations) {
        this(prologue, operations, List.of());
    }

    public UpdateRequestAst {
        prologue = prologue != null ? prologue : QueryPrologueAst.empty();
        operations = operations != null ? List.copyOf(operations) : List.of();
        operationPrologues = operationPrologues == null || operationPrologues.isEmpty()
                ? java.util.Collections.nCopies(operations.size(), prologue) : List.copyOf(operationPrologues);
        if (operationPrologues.size() != operations.size()) {
            throw new IllegalArgumentException("Each update operation requires its own prologue");
        }
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.prologue.accept(visitor);
        this.operations.forEach(updateRequestUnitAst -> updateRequestUnitAst.accept(visitor));
    }
}
