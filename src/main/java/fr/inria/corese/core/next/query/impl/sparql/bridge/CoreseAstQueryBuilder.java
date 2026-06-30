package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.sparql.ast.AskQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ASTConstants;
import fr.inria.corese.core.next.query.impl.sparql.ast.ConstraintAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.DatasetClauseAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.FilterAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.GroupGraphPatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.OrderConditionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ProjectionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SolutionModifierAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.kgram.api.core.ExpType.Type;
import fr.inria.corese.core.next.query.kgram.api.core.Filter;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.next.query.kgram.core.Exp;
import fr.inria.corese.core.next.query.kgram.core.Query;
import fr.inria.corese.core.next.query.kgram.tool.NodeImpl;
import fr.inria.corese.core.sparql.triple.parser.Atom;
import fr.inria.corese.core.sparql.triple.parser.Expression;
import fr.inria.corese.core.sparql.triple.parser.Variable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Builds KGRAM {@code Exp} / {@code Query} structures from Corese-next query AST nodes.
 *
 * <p>This bridge sits between the parsed SPARQL AST and the KGRAM runtime query model:
 * it does not execute queries, it translates syntax-level query forms ({@code ASK}, {@code SELECT})
 * into runtime-ready {@link Query}/{@link Exp} structures.</p>
 */
public final class CoreseAstQueryBuilder {

    private final WhereCompiler whereCompiler;

    /**
     * Creates a bridge backed by the default {@link WhereCompiler}.
     */
    public CoreseAstQueryBuilder() {
        this(new WhereCompiler());
    }

    /**
     * Creates a bridge with an explicit {@link WhereCompiler}.
     *
     * <p>This constructor stays package-visible so tests and same-package bridge code
     * can inject a specialized compiler without widening the public API surface.</p>
     */
    CoreseAstQueryBuilder(WhereCompiler whereCompiler) {
        this.whereCompiler = Objects.requireNonNull(whereCompiler, "whereCompiler");
    }

    /**
     * Builds a KGRAM {@link Query} from a SPARQL {@code ASK} query AST.
     *
     * <p>The bridge maps the query body and the modifiers that already have a direct
     * runtime representation ({@code FROM}, {@code FROM NAMED}, {@code ORDER BY},
     * {@code LIMIT}, {@code OFFSET}). Clauses that require dedicated aggregate or
     * values handling are rejected explicitly.</p>
     */
    public Query toNextQuery(AskQueryAst askQueryAst) {
        Objects.requireNonNull(askQueryAst, "askQueryAst");
        rejectUnsupportedAskClauses(askQueryAst);

        Query query = createQuery(
                askQueryAst.whereClause(),
                askQueryAst.datasetClause(),
                askQueryAst.solutionModifier());
        applyOrderBy(query, askQueryAst.solutionModifier());
        query.setAsk(true);
        return query;
    }

    /**
     * Builds a KGRAM {@link Query} from a {@link SelectQueryAst}.
     *
     * <p>The bridge maps the compiled {@code WHERE} body, plain projection,
     * dataset clauses, and solution modifiers that already have a direct runtime
     * representation ({@code DISTINCT}, {@code ORDER BY}, {@code LIMIT}, {@code OFFSET}).
     * Clauses that require dedicated aggregate, alias, or values handling are rejected
     * explicitly.</p>
     */
    public Query toNextQuery(SelectQueryAst selectQueryAst) {
        Objects.requireNonNull(selectQueryAst, "selectQueryAst");
        rejectUnsupportedSelectClauses(selectQueryAst);

        Query query = createQuery(
                selectQueryAst.whereClause(),
                selectQueryAst.datasetClause(),
                selectQueryAst.solutionModifier());
        applyProjection(query, selectQueryAst.projection());
        query.setDistinct(selectQueryAst.solutionModifier().distinct());
        applyOrderBy(query, selectQueryAst.solutionModifier());
        return query;
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

    /**
     * Converts a query term used as subject, predicate, object, or variable reference
     * into a runtime {@link Node}.
     *
     * <p>This helper is package-visible because both the query builder and the
     * {@link WhereCompiler} need a single shared term-to-node conversion rule.</p>
     */
    static Node toNode(TermAst term) {
        Expression expression = SparqlAstToExpression.convert(term);
        if (expression instanceof Atom atom) {
            return new NodeImpl(atom);
        }
        throw new IllegalArgumentException(
                "A query term must be a variable, IRI or literal, got: "
                        + term.getClass().getSimpleName());
    }

    private static void rejectUnsupportedAskClauses(AskQueryAst askQueryAst) {
        if (!askQueryAst.valuesClause().mappings().isEmpty()) {
            throw new UnsupportedOperationException(
                    "Inline VALUES is not supported yet for ASK (values handling is a follow-up)");
        }
        SolutionModifierAst mod = askQueryAst.solutionModifier();
        if (mod.hasGroupBy() || mod.hasHaving() || mod.distinct() || mod.reduced()) {
            throw new UnsupportedOperationException(
                    "Solution modifiers (GROUP BY, HAVING, DISTINCT, REDUCED) are not supported for ASK");
        }
        // TODO(#388): inline VALUES needs a dedicated runtime mapping.
        // TODO(#388): GROUP BY / HAVING would require aggregate-aware ASK semantics, not just field copying.
        // TODO(#388): REDUCED support should be aligned with the final query-form policy for the next pipeline.
    }

    private static void rejectUnsupportedSelectClauses(SelectQueryAst selectQueryAst) {
        if (!selectQueryAst.valuesClause().mappings().isEmpty()) {
            throw new UnsupportedOperationException("VALUES is not supported yet when building a next Query");
        }

        ProjectionAst projection = selectQueryAst.projection();
        if (!projection.expressionTerms().isEmpty() || !projection.expressionBoundVariables().isEmpty()) {
            throw new UnsupportedOperationException(
                    "SELECT expressions are not supported yet when building a next Query");
        }

        SolutionModifierAst solutionModifier = selectQueryAst.solutionModifier();
        if (solutionModifier.reduced()) {
            throw new UnsupportedOperationException("REDUCED is not supported yet when building a next Query");
        }
        if (solutionModifier.hasGroupBy()) {
            throw new UnsupportedOperationException("GROUP BY is not supported yet when building a next Query");
        }
        if (solutionModifier.hasHaving()) {
            throw new UnsupportedOperationException("HAVING is not supported yet when building a next Query");
        }
        // TODO(#387): inline VALUES needs a dedicated runtime mapping.
        // TODO(#387): SELECT expressions need a clear runtime story for aliases and reuse in later clauses.
        // TODO(#387): GROUP BY / HAVING require aggregate semantics, not only AST field propagation.
        // TODO(#387): REDUCED support should be aligned with the final next-pipeline query-form policy.
    }

    /**
     * Creates the runtime {@link Query} shell shared by every query form handled here.
     *
     * <p>The compiled {@code WHERE} body comes first, then the builder copies dataset
     * and limit/offset information, and finally collects visible nodes so later clauses
     * can resolve variables against the runtime body.</p>
     */
    private Query createQuery(
            GroupGraphPatternAst whereClause,
            DatasetClauseAst datasetClause,
            SolutionModifierAst solutionModifier) {
        Query query = Query.create(whereCompiler.compile(whereClause));
        // Collect visible nodes once so later clauses (projection, ORDER BY, GROUP BY)
        // can resolve variables against the compiled runtime body.
        query.collect();
        applyDataset(query, datasetClause);
        applyLimitOffset(query, solutionModifier);
        return query;
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

    /**
     * Maps the {@code SELECT} projection onto the runtime query.
     *
     * <p>{@code SELECT *} reuses the visible nodes collected from the compiled
     * query body. An explicit projection reuses these same runtime nodes and fails
     * fast when a projected variable is not visible in the body.</p>
     */
    private void applyProjection(Query query, ProjectionAst projection) {
        List<Exp> selectExpressions;
        if (projection.selectAll()) {
            selectExpressions = toNodeExpressions(query.selectNodesFromPattern());
        } else {
            selectExpressions = new ArrayList<>();
            for (VarAst variable : projection.variables()) {
                Node node = query.getExtNode(variable.name());
                if (node == null) {
                    throw new IllegalArgumentException(
                            "Projected variable ?" + variable.name() + " is not visible in the compiled query body");
                }
                selectExpressions.add(Exp.create(Type.NODE, node));
            }
        }

        query.setSelectFun(selectExpressions);
        query.setSelect(selectNodeList(selectExpressions));
    }

    /**
     * Maps {@code ORDER BY} conditions that are already expressible in runtime KGRAM terms.
     *
     * <p>Variables reuse already-visible query nodes. Other expressions are wrapped as runtime
     * filters and attached to synthetic internal nodes, just like the historical pipeline does.</p>
     */
    private void applyOrderBy(Query query, SolutionModifierAst solutionModifier) {
        if (!solutionModifier.hasOrderBy()) {
            return;
        }

        List<Exp> orderByExpressions = new ArrayList<>();
        int syntheticIndex = 0;
        for (OrderConditionAst orderCondition : solutionModifier.orderBy()) {
            Exp orderExpression = toOrderByExpression(query, orderCondition, syntheticIndex++);
            orderExpression.status(orderCondition.orderDirection() == ASTConstants.OrderDirection.DESC);
            orderByExpressions.add(orderExpression);
        }
        query.setOrderBy(orderByExpressions);
    }

    private Exp toOrderByExpression(Query query, OrderConditionAst orderCondition, int syntheticIndex) {
        TermAst expression = orderCondition.expression();
        if (expression instanceof VarAst(String name)) {
            Exp selectExpression = query.getSelectExp(name);
            Node node = selectExpression != null
                    ? selectExpression.getNode()
                    : query.getProperAndSubSelectNode(name);
            if (node == null) {
                throw new IllegalArgumentException(
                        "ORDER BY variable ?" + name + " is not visible in the compiled query");
            }
            return Exp.create(Type.NODE, node);
        }

        Filter filter = SparqlAstToExpression.toNextFilter(expression);
        Exp exp = Exp.create(Type.NODE, createSyntheticOrderNode(syntheticIndex));
        exp.setFilter(filter);
        return exp;
    }

    private Node createSyntheticOrderNode(int syntheticIndex) {
        return new NodeImpl(Variable.create("__order_by_" + syntheticIndex));
    }

    private List<Exp> toNodeExpressions(List<Node> nodes) {
        List<Exp> expressions = new ArrayList<>();
        for (Node node : nodes) {
            expressions.add(Exp.create(Type.NODE, node));
        }
        return expressions;
    }

    private List<Node> selectNodeList(List<Exp> selectExpressions) {
        LinkedHashSet<Node> selectNodes = new LinkedHashSet<>();
        for (Exp selectExpression : selectExpressions) {
            selectNodes.add(selectExpression.getNode());
        }
        return new ArrayList<>(selectNodes);
    }

    private void applyLimitOffset(Query query, SolutionModifierAst solutionModifier) {
        if (solutionModifier.hasLimit()) {
            query.setLimit(Math.toIntExact(solutionModifier.limit()));
        }
        if (solutionModifier.hasOffset()) {
            query.setOffset(Math.toIntExact(solutionModifier.offset()));
        }
    }
}
