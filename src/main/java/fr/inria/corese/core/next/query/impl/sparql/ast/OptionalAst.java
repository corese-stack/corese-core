package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.AstVisitor;

/**
 * Optional can contain BGP, FILTER, UNION
 *
 * @param ast PatternAst
 */
public record OptionalAst(PatternAst ast) implements PatternAst {
    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.ast.accept(visitor);
    }
}