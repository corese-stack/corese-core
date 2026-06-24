package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.kgram.api.core.Filter;
import fr.inria.corese.core.next.query.kgram.core.Exp;
import fr.inria.corese.core.next.query.kgram.core.Query;

import java.util.Objects;

/**
 * Builds KGRAM {@code Exp} / {@code Query} structures from Corese-next query AST nodes.
 */
public final class CoreseAstQueryBuilder {

    public CoreseAstQueryBuilder() {
    }

    private final WhereCompiler whereCompiler = new WhereCompiler();

    /**
     * Builds a KGRAM {@link Query} from a SPARQL {@code ASK} query AST.
     */
    public Query toQuery(AskQueryAst ask) {
        Objects.requireNonNull(ask, "ask");
        rejectUnsupportedClauses(ask);

        Exp body = whereCompiler.compile(ask.whereClause());
        Query query = Query.create(body);
        query.setAsk(true);

        return query;
    }

    private static void rejectUnsupportedClauses(AskQueryAst ask) {
        DatasetClauseAst dataset = ask.datasetClause();
        if (!dataset.graphs().isEmpty() || !dataset.namedGraphs().isEmpty()) {
            throw new UnsupportedOperationException(
                    "FROM / FROM NAMED is not supported yet for ASK (dataset handling is a follow-up)");
        }
        if (!ask.valuesClause().mappings().isEmpty()) {
            throw new UnsupportedOperationException(
                    "Inline VALUES is not supported yet for ASK (values handling is a follow-up)");
        }
        SolutionModifierAst mod = ask.solutionModifier();
        if (mod.hasOrderBy() || mod.hasGroupBy() || mod.hasHaving()
                || mod.hasLimit() || mod.hasOffset() || mod.distinct() || mod.reduced()) {
            throw new UnsupportedOperationException(
                    "Solution modifiers (ORDER BY, GROUP BY, HAVING, LIMIT, OFFSET, DISTINCT, REDUCED) are not supported for ASK");
        }
    }

    /**
     * Converts a SPARQL {@code FILTER} clause by converting {@link FilterAst#operator()} the same way as
     * {@link #toNextFilter(TermAst)}.
     */
    public Filter toNextFilter(FilterAst filterClause) {
        Objects.requireNonNull(filterClause, "filterClause");
        return toNextFilter(filterClause.operator());
    }

    /**
     * Converts a filter expression carried as {@link TermAst}: must be a {@link ConstraintAst}.
     */
    public Filter toNextFilter(TermAst filterExpression) {
        Objects.requireNonNull(filterExpression, "filterExpression");
        if (!(filterExpression instanceof ConstraintAst constraint)) {
            throw new IllegalArgumentException(
                    "FILTER expects a ConstraintAst, got: " + filterExpression.getClass().getName());
        }
        return toNextFilter(constraint);
    }

    /**
     * Converts a constraint tree (boolean filter expression) into a KGRAM {@link Filter}.
     */
    public Filter toNextFilter(ConstraintAst filterExpression) {
        Objects.requireNonNull(filterExpression, "filterExpression");
        return SparqlAstToExpression.toNextFilter(filterExpression);
    }

}