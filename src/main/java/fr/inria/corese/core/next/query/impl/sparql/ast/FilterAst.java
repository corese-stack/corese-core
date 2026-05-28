package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;

/**
 * Filter clause in a SPARQL query
 * @param operator The operator whose evaluation will make a filter a createFunCall on the query results
 */
public record FilterAst(TermAst operator) implements PatternAst {
    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.operator.accept(visitor);
    }
}
