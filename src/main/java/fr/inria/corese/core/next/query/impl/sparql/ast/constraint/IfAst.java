package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.AstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.ast.ConstraintAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Function {@code IF(condition, thenExpr, elseExpr)} in SPARQL 1.1
 * Evaluates condition; if true returns thenExpr, otherwise elseExpr.
 */
public record IfAst(TermAst condition, TermAst thenExpr, TermAst elseExpr) implements ConstraintAst {

    @Override
    public String getName() {
        return "IF";
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.condition.accept(visitor);
        this.thenExpr.accept(visitor);
        this.elseExpr.accept(visitor);
    }
}