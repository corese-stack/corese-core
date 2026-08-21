package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.AstVisitor;

import java.util.List;

/**
 * Abstract Syntax Tree (AST) representation of a SPARQL {@code DESCRIBE} query.
 * DESCRIBE (var|uri)* WHERE { pattern } or DESCRIBE (var|uri)*.
 *
 * <p>
 * Examples:
 * </p>
 *
 * <pre>{@code
 * DESCRIBE <http://example.org/>
 * }</pre>
 *
 * <pre>{@code
 * PREFIX foaf: <http://xmlns.com/foaf/0.1/>
 *
 * DESCRIBE ?x
 * WHERE {
 *   ?x foaf:mbox <mailto:alice@org>
 * }
 * }</pre>
 */
public record DescribeQueryAst(
        DatasetClauseAst datasetClause,
        List<TermAst> described,
        GroupGraphPatternAst whereClause,
        SolutionModifierAst solutionModifier,
        QueryPrologueAst prologue,
        ValuesAst valuesClause) implements SparqlQueryAst {
    public DescribeQueryAst {
        described = described != null ? List.copyOf(described) : List.of();
        if (whereClause == null) {
            whereClause = new GroupGraphPatternAst(List.of());
        }
        if (datasetClause == null) {
            datasetClause = DatasetClauseAst.none();
        }
        if (solutionModifier == null) {
            solutionModifier = SolutionModifierAst.empty();
        }
        if (prologue == null) {
            prologue = QueryPrologueAst.empty();
        }
        if (valuesClause == null) {
            valuesClause = ValuesAst.none();
        }
    }

    /**
     * constructor with default prefix handler
     */
    public DescribeQueryAst(
            DatasetClauseAst datasetClause,
            List<TermAst> described,
            GroupGraphPatternAst whereClause,
            SolutionModifierAst solutionModifier) {
        this(datasetClause, described, whereClause, solutionModifier, null, null);
    }

    /**
     * constructor with default prefix handler and default solution modifier
     */
    public DescribeQueryAst(DatasetClauseAst datasetClause, List<TermAst> described, GroupGraphPatternAst whereClause) {
        this(datasetClause, described, whereClause, null, null, null);
    }

    /**
     * Returns {@code true} if this is a {@code DESCRIBE *} query
     * (no explicit resources specified).
     */
    public boolean isDescribeAll() {
        return described.isEmpty();
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.prologue.accept(visitor);
        this.datasetClause.accept(visitor);
        this.described.forEach(termAst -> {
            termAst.accept(visitor);
        });
        this.whereClause.accept(visitor);
        this.valuesClause.accept(visitor);
        this.solutionModifier.accept(visitor);
    }
}
