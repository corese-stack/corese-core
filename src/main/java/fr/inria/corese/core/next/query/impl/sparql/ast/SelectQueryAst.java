package fr.inria.corese.core.next.query.impl.sparql.ast;

import java.util.List;

/**
 * Abstract Syntax Tree (AST) representation of a SPARQL {@code SELECT} query.
 * Holds the projection (SELECT * or SELECT ?v1 ?v2 ...) and the WHERE clause.
 * <p>
 * {@link #prologue()} captures PREFIX/BASE for SELECT;
 * for {@link QueryAst} compatibility.
 */
public record SelectQueryAst(ProjectionAst projection, DatasetClauseAst datasetClause, GroupGraphPatternAst whereClause, SolutionModifierAst solutionModifier, QueryPrologueAst prologue, ValuesAst valuesClause) implements QueryAst {

    /** Constructor with default projection SELECT *. */
    public SelectQueryAst(GroupGraphPatternAst whereClause) {
        this(ProjectionAsts.selectAll(), DatasetClauseAst.none(), whereClause);
    }

    /** Constructor with default solution modifier (no DISTINCT/REDUCED/ORDER BY/LIMIT/OFFSET) and default prologue. */
    public SelectQueryAst(ProjectionAst projection, DatasetClauseAst datasetClause, GroupGraphPatternAst whereClause) {
        this(projection, datasetClause, whereClause, null, null, null);
    }

    /** Constructor with default prologue */
    public SelectQueryAst(ProjectionAst projection, DatasetClauseAst datasetClause, GroupGraphPatternAst whereClause, SolutionModifierAst solutionModifier) {
        this(projection, datasetClause, whereClause, solutionModifier, null, null);
    }

    public SelectQueryAst {
        if (projection == null) {
            projection = ProjectionAsts.selectAll();
        }
        if (datasetClause == null) {
            datasetClause = DatasetClauseAst.none();
        }
        if (whereClause == null) {
            whereClause = new GroupGraphPatternAst(List.of());
        }
        if (solutionModifier == null) {
            solutionModifier = SolutionModifierAst.empty();
        }
        if (prologue == null) {
            prologue = QueryPrologueAst.empty();
        }
        if(valuesClause == null) {
            valuesClause = ValuesAst.none();
        }
    }
}
