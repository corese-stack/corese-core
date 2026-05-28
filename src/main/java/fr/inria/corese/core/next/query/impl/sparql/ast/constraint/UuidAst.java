package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.ast.ConstraintAst;

/**
 * Function {@code UUID()} in SPARQL 1.1
 * Returns a fresh IRI from the UUID URN scheme.
 */
public record UuidAst() implements ConstraintAst {

    @Override
    public String getName() {
        return "UUID";
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}