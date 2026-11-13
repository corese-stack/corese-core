package fr.inria.corese.core.next.query.impl.sparql.ast;

import java.util.List;

import fr.inria.corese.core.next.query.impl.sparql.ast.visitor.AstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.ast.visitor.VisitableAst;

/**
 * SPARQL {@code HAVING} clause: boolean constraints evaluated after grouping.
 */
public record HavingAst(
        List<TermAst> conditions,
        int line,
        int column,
        List<SourceLocation> conditionLocations) implements VisitableAst {
    public HavingAst(List<TermAst> conditions) {
        this(conditions, -1, -1, List.of());
    }

    public HavingAst {
        conditions = conditions != null ? List.copyOf(conditions) : List.of();
        conditionLocations = conditionLocations != null ? List.copyOf(conditionLocations) : List.of();
    }

    public boolean isEmpty() {
        return conditions.isEmpty();
    }

    public static HavingAst empty() {
        return new HavingAst(List.of());
    }

    public record SourceLocation(int line, int column) {
        public SourceLocation {
            if (line < -1 || column < -1) {
                throw new IllegalArgumentException("source location must be >= -1");
            }
        }
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.conditions.forEach(conditionAst -> conditionAst.accept(visitor));
    }
}
