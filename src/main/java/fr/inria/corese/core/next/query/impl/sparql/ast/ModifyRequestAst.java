package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.AstVisitor;

/**
 * DELETE/INSERT templates evaluated against one snapshot of the WHERE solutions.
 *
 * @param withGraph      the graph specified by WITH, or {@code null} if absent
 * @param deleteTemplate quad template of triples to delete
 * @param insertTemplate quad template of triples to insert
 * @param using          dataset clause specified by USING / USING NAMED
 * @param query          the underlying SELECT query representing WHERE pattern and modifiers
 */
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
