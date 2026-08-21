package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.AstVisitor;

public record SubQueryAst(QueryAst query) implements PatternAst {
    public SubQueryAst {
        if (query == null) {
            throw new IllegalArgumentException("query is null");
        }
        if (!(query instanceof SelectQueryAst)) {
            throw new UnsupportedOperationException(
                    "Only SELECT subqueries are supported in W3C: " + query.getClass().getName());
        }
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.query.accept(visitor);
    }
}