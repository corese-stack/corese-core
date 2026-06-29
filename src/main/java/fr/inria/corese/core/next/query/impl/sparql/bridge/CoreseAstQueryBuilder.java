package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.sparql.ast.AskQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ConstraintAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.DatasetClauseAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.FilterAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SolutionModifierAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.kgram.api.core.Filter;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.next.query.kgram.core.Query;
import fr.inria.corese.core.next.query.kgram.tool.NodeImpl;
import fr.inria.corese.core.sparql.triple.parser.Atom;
import fr.inria.corese.core.sparql.triple.parser.Expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds KGRAM {@code Exp} / {@code Query} structures from Corese-next query AST nodes.
 */
public final class CoreseAstQueryBuilder {

    private final WhereCompiler whereCompiler = new WhereCompiler();

    /**
     * Builds a KGRAM {@link Query} from a SPARQL {@code ASK} query AST.
     */
    public Query toNextQuery(AskQueryAst askQueryAst) {
        Objects.requireNonNull(askQueryAst, "askQueryAst");
        rejectUnsupportedClauses(askQueryAst);

        Query query = Query.create(whereCompiler.compile(askQueryAst.whereClause()));
        applyDataset(query, askQueryAst.datasetClause());
        applySolutionModifier(query, askQueryAst.solutionModifier());
        query.setAsk(true);
        return query;
    }

    private static void rejectUnsupportedClauses(AskQueryAst askQueryAst) {
        if (!askQueryAst.valuesClause().mappings().isEmpty()) {
            throw new UnsupportedOperationException(
                    "Inline VALUES is not supported yet for ASK (values handling is a follow-up)");
        }
        SolutionModifierAst mod = askQueryAst.solutionModifier();
        if (mod.hasOrderBy() || mod.hasGroupBy() || mod.hasHaving()
                || mod.distinct() || mod.reduced()) {
            throw new UnsupportedOperationException(
                    "Solution modifiers (ORDER BY, GROUP BY, HAVING, DISTINCT, REDUCED) are not supported for ASK");
        }
    }

    private void applyDataset(Query query, DatasetClauseAst datasetClause) {
        query.setFrom(toNodeList(datasetClause.graphs()));
        query.setNamed(toNodeList(datasetClause.namedGraphs()));
    }

    private List<Node> toNodeList(Iterable<IriAst> iris) {
        List<Node> nodes = new ArrayList<>();
        for (IriAst iri : iris) {
            nodes.add(toNode(iri));
        }
        return nodes;
    }

    private void applySolutionModifier(Query query, SolutionModifierAst solutionModifier) {
        if (solutionModifier.hasLimit()) {
            query.setLimit(Math.toIntExact(solutionModifier.limit()));
        }
        if (solutionModifier.hasOffset()) {
            query.setOffset(Math.toIntExact(solutionModifier.offset()));
        }
    }

    static Node toNode(TermAst term) {
        Expression expression = SparqlAstToExpression.convert(term);
        if (expression instanceof Atom atom) {
            return new NodeImpl(atom);
        }
        throw new IllegalArgumentException(
                "A query term must be a variable, IRI or literal, got: "
                        + term.getClass().getSimpleName());
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
