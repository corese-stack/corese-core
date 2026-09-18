package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.AstVisitor;

/** DELETE/INSERT templates evaluated against one snapshot of the WHERE solutions. */
public record ModifyRequestAst(IriAst withGraph, QuadsAst deleteTemplate, QuadsAst insertTemplate,
        DatasetClauseAst using, SelectQueryAst query) implements UpdateRequestUnitAst {
    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        if (withGraph != null) {
            withGraph.accept(visitor);
        }
        deleteTemplate.accept(visitor);
        insertTemplate.accept(visitor);
        using.accept(visitor);
        query.accept(visitor);
    }
}
