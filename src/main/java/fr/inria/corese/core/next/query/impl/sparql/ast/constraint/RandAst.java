package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;

/**
 * Function {@code RAND()} in SPARQL 1.1.
 */
public record RandAst() implements NumericExpressionAst {

    @Override
    public String getName() {
        return "RAND";
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}
