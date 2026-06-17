package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.ast.ConstraintAst;

/**
 * Function {@code STRUUID()} in SPARQL 1.1
 * Returns a string form of a UUID.
 */
public record StrUuidAst() implements ConstraintAst, SimpleLiteralExpressionAst {

    @Override
    public String getName() {
        return "STRUUID";
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}