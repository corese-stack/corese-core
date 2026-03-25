package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.data.api.IPrefixHandler;
import fr.inria.corese.core.next.data.impl.common.prefix.PrefixHandler;
import fr.inria.corese.core.next.data.impl.io.common.IOConstants;

import java.util.List;

/**
 * Abstract Syntax Tree (AST) representation of a SPARQL {@code SELECT} query.
 * Holds the projection (SELECT * or SELECT ?v1 ?v2 ...) and the WHERE clause.
 */
public record SelectQueryAst(ProjectionAst projection, DatasetClauseAst datasetClause, GroupGraphPatternAst whereClause, SolutionModifierAst solutionModifier, PrefixHandler prefixHandler) implements QueryAst {

    /** Constructor with default projection SELECT *. */
    public SelectQueryAst(GroupGraphPatternAst whereClause) {
        this(ProjectionAsts.selectAll(), DatasetClauseAst.none(), whereClause);
    }

    /** Constructor with default solution modifier (no DISTINCT/REDUCED/ORDER BY/LIMIT/OFFSET) and default prefix handler. */
    public SelectQueryAst(ProjectionAst projection, DatasetClauseAst datasetClause, GroupGraphPatternAst whereClause) {
        this(projection, datasetClause, whereClause, null, null);
    }

    /** Constructor with default prefix handler */
    public SelectQueryAst(ProjectionAst projection, DatasetClauseAst datasetClause, GroupGraphPatternAst whereClause, SolutionModifierAst solutionModifier) {
        this(projection, datasetClause, whereClause, solutionModifier, null);
    }

    public SelectQueryAst {
        if (projection == null) {
            projection = ProjectionAsts.selectAll();
        }
        if(datasetClause == null) {
            datasetClause = DatasetClauseAst.none();
        }
        if (whereClause == null) {
            whereClause = new GroupGraphPatternAst(List.of());
        }
        if (solutionModifier == null) {
            solutionModifier = SolutionModifierAst.empty();
        }
        if (prefixHandler == null) {
            prefixHandler = new PrefixHandler();
            prefixHandler.setDefaultNamespace(IOConstants.getDefaultBaseURI());
        }
    }
}