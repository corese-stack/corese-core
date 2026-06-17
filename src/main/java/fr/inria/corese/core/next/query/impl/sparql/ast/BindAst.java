package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;

/**
 * BIND(expression AS ?var) clause in SPARQL 1.1
 */
public record BindAst(TermAst expression, VarAst variable) implements PatternAst {
    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        if(this.expression != null) {
            expression.accept(visitor);
        }
        if(this.variable != null) {
            variable.accept(visitor);
        }
    }
}