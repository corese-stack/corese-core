package fr.inria.corese.core.next.query.impl.sparql.bridge;


import fr.inria.corese.core.next.query.api.exception.UnsupportedQueryFeatureException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.engine.model.ExpType.Type;
import fr.inria.corese.core.next.query.impl.engine.model.Filter;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Builds KGRAM {@code Exp} / {@code Query} structures from Corese-next query AST nodes.
 *
 * <p>This bridge sits between the parsed SPARQL AST and the KGRAM runtime query model:
 * it does not execute queries, it translates syntax-level query forms ({@code ASK},
 * {@code SELECT}, {@code DESCRIBE}, {@code CONSTRUCT}) into runtime-ready
 * {@link Query}/{@link Exp} structures.</p>
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

        WhereCompiler compiler = whereCompiler.withPrologue(askQueryAst.prologue());
        Query query = createQuery(
                askQueryAst.whereClause(),
                askQueryAst.datasetClause(),
                askQueryAst.solutionModifier(),
                askQueryAst.valuesClause(),
                compiler);
        applyOrderBy(query, askQueryAst.solutionModifier(), compiler);
        query.setAsk(true);
        query.setAST(askQueryAst);
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

        WhereCompiler compiler = whereCompiler.withPrologue(selectQueryAst.prologue());
        Query query = createQuery(
                selectQueryAst.whereClause(),
                selectQueryAst.datasetClause(),
                selectQueryAst.solutionModifier(),
                selectQueryAst.valuesClause(),
                compiler);
        applyProjection(query, selectQueryAst.projection());
        query.setDistinct(selectQueryAst.solutionModifier().distinct());
        applyOrderBy(query, selectQueryAst.solutionModifier(), compiler);
        query.setAST(selectQueryAst);
        return query;
    }

    /**
     * Builds a KGRAM {@link Query} from a {@link DescribeQueryAst}.
     *
     * <p>Reuses the shared query shell (compiled {@code WHERE} body, dataset, LIMIT/OFFSET) and
     * {@code ORDER BY} like the other forms, then lowers {@code DESCRIBE} to the construct-like
     * shape expected by the current KGRAM runtime. A described variable reuses its runtime node,
     * a described IRI becomes a fresh constant node, and {@code DESCRIBE *} reuses the in-scope
     * nodes of the body, matching {@code SELECT *}. Clauses that require dedicated aggregate or
     * values handling are rejected explicitly.</p>
     */
    public Query toNextQuery(DescribeQueryAst describeQueryAst) {
        Objects.requireNonNull(describeQueryAst, "describeQueryAst");
        rejectUnsupportedDescribeClauses(describeQueryAst);

        WhereCompiler compiler = whereCompiler.withPrologue(describeQueryAst.prologue());
        Query query = createQuery(
                describeQueryAst.whereClause(),
                describeQueryAst.datasetClause(),
                describeQueryAst.solutionModifier(),
                describeQueryAst.valuesClause(),
                compiler);
        applyOrderBy(query, describeQueryAst.solutionModifier(), compiler);
        List<Node> describedNodes = describeNodes(query, describeQueryAst, compiler);
        lowerDescribeToConstructQuery(query, describedNodes);
        query.setAST(describeQueryAst);
        return query;
    }

    /**
     * Builds a KGRAM {@link Query} from a {@link ConstructQueryAst}.
     *
     * <p>Reuses the shared query shell (compiled {@code WHERE} body, dataset, LIMIT/OFFSET) and
     * {@code ORDER BY} like the other forms, then compiles the {@code CONSTRUCT} template into a
     * separate {@link Exp} carried by the query. Template variables reuse the runtime node bound by
     * the body when visible; a template-only variable stays fresh (an unbound template variable is
     * valid SPARQL, its triple is simply skipped at instantiation time). Clauses that require
     * dedicated aggregate or values handling are rejected explicitly.</p>
     */
    public Query toNextQuery(ConstructQueryAst constructQueryAst) {
        Objects.requireNonNull(constructQueryAst, "constructQueryAst");
        rejectUnsupportedConstructClauses(constructQueryAst);

        WhereCompiler compiler = whereCompiler.withPrologue(constructQueryAst.prologue());
        Query query = createQuery(
                constructQueryAst.whereClause(),
                constructQueryAst.datasetClause(),
                constructQueryAst.solutionModifier(),
                constructQueryAst.valuesClause(),
                compiler);
        applyOrderBy(query, constructQueryAst.solutionModifier(), compiler);
        Exp template = compileConstructTemplate(query, constructQueryAst.constructTemplate(), compiler);
        query.setConstruct(true);
        query.setConstruct(template);
        query.setConstructNodes(template.getNodes());
        query.setAST(constructQueryAst);
        return query;
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
        return new AstBackedExpr(filterExpression, whereCompiler).getFilter();
    }

    /**
     * Rejects unsupported clauses for {@code ASK} queries.
     *
     * <p>Planned roadmap items:
     * <ul>
     *   <li>Issue #388: {@code GROUP BY} / {@code HAVING} requires aggregate-aware ASK semantics.</li>
     *   <li>Issue #388: {@code REDUCED} support aligned with next-pipeline query-form policy.</li>
     * </ul>
     * </p>
     */
    private static void rejectUnsupportedAskClauses(AskQueryAst askQueryAst) {
        SolutionModifierAst mod = askQueryAst.solutionModifier();
        if (mod.hasGroupBy() || mod.hasHaving() || mod.distinct() || mod.reduced()) {
            throw new UnsupportedQueryFeatureException(
                    "GROUP BY, HAVING, DISTINCT and REDUCED are not supported yet by the next pipeline for ASK");
        }
    }

    /**
     * Rejects unsupported clauses for {@code SELECT} queries.
     *
     * <p>Planned roadmap items:
     * <ul>
     *   <li>Issue #387: {@code SELECT} expressions and aliases need a runtime story and reuse in later clauses.</li>
     *   <li>Issue #387: {@code GROUP BY} / {@code HAVING} require aggregate semantics, not only AST field propagation.</li>
     *   <li>Issue #387: {@code REDUCED} support aligned with next-pipeline query-form policy.</li>
     * </ul>
     * </p>
     */
    private static void rejectUnsupportedSelectClauses(SelectQueryAst selectQueryAst) {
        ProjectionAst projection = selectQueryAst.projection();
        if (!projection.expressionTerms().isEmpty() || !projection.expressionBoundVariables().isEmpty()) {
            throw new UnsupportedQueryFeatureException(
                    "SELECT expressions and aliases are not supported yet by the next pipeline");
        }
        SolutionModifierAst solutionModifier = selectQueryAst.solutionModifier();
        if (solutionModifier.reduced()) {
            throw new UnsupportedQueryFeatureException("REDUCED is not supported yet by the next pipeline for SELECT");
        }
        if (solutionModifier.hasGroupBy()) {
            throw new UnsupportedQueryFeatureException("GROUP BY is not supported yet by the next pipeline for SELECT");
        }
        if (solutionModifier.hasHaving()) {
            throw new UnsupportedQueryFeatureException("HAVING is not supported yet by the next pipeline for SELECT");
        }
    }

    /**
     * Rejects unsupported clauses for {@code DESCRIBE} queries.
     *
     * <p>Planned roadmap items:
     * <ul>
     *   <li>Issue #390: {@code GROUP BY} / {@code HAVING} requires aggregate-aware DESCRIBE semantics.</li>
     *   <li>Issue #390: {@code REDUCED} support aligned with next-pipeline query-form policy.</li>
     * </ul>
     * </p>
     */
    private static void rejectUnsupportedDescribeClauses(DescribeQueryAst describeQueryAst) {
        SolutionModifierAst mod = describeQueryAst.solutionModifier();
        if (mod.hasGroupBy() || mod.hasHaving() || mod.distinct() || mod.reduced()) {
            throw new UnsupportedQueryFeatureException(
                    "GROUP BY, HAVING, DISTINCT and REDUCED are not supported yet by the next pipeline for DESCRIBE");
        }
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
            SolutionModifierAst solutionModifier,
            ValuesAst valuesClause,
            WhereCompiler compiler) {
        Exp body = compiler.compile(whereClause);
        if (valuesClause.present()) {
            body = Exp.create(Type.JOIN,
                    body, compiler.compileValues(valuesClause));
        }
        Query query = Query.create(body);
        // Collect visible nodes once so later clauses (projection, ORDER BY, DESCRIBE)
        // can resolve variables against the compiled runtime body.
        query.collect();
        applyDataset(query, datasetClause, compiler);
        applyLimitOffset(query, solutionModifier);
        return query;
    }

    private void applyDataset(Query query, DatasetClauseAst datasetClause, WhereCompiler compiler) {
        query.setFrom(toNodeList(datasetClause.graphs(), compiler));
        query.setNamed(toNodeList(datasetClause.namedGraphs(), compiler));
        query.setDatasetSpecified(
                !datasetClause.graphs().isEmpty() || !datasetClause.namedGraphs().isEmpty());
    }

    private List<Node> toNodeList(Iterable<IriAst> iris, WhereCompiler compiler) {
        List<Node> nodes = new ArrayList<>();
        for (IriAst iri : iris) {
            nodes.add(compiler.termResolver().toNode(iri));
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
                Node node = visibleBodyNode(query, variable.name());
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
     * Resolves the resources of a {@code DESCRIBE} against the compiled body.
     *
     * <p>{@code DESCRIBE *} reuses the in-scope nodes of the body (like {@code SELECT *}).
     * A described variable reuses its runtime node (so it is the one bound by the body) and
     * fails fast when it is not visible; a described IRI becomes a fresh constant node.</p>
     */
    private List<Node> describeNodes(
            Query query, DescribeQueryAst describeQueryAst, WhereCompiler compiler) {
        if (describeQueryAst.isDescribeAll()) {
            return query.selectNodesFromPattern();
        }
        List<Node> nodes = new ArrayList<>();
        for (TermAst term : describeQueryAst.described()) {
            if (term instanceof VarAst(String name)) {
                Node node = visibleBodyNode(query, name);
                if (node == null) {
                    throw new IllegalArgumentException(
                            "DESCRIBE variable ?" + name + " is not visible in the compiled query body");
                }
                nodes.add(node);
            } else {
                nodes.add(compiler.termResolver().toNode(term));
            }
        }
        return nodes;
    }

    /**
     * Lowers {@code DESCRIBE} to the construct-like shape expected by the current
     * KGRAM-next runtime.
     *
     * <p>This keeps the current KGRAM contract, inherited from the historical pipeline:
     * {@code DESCRIBE} is executed through the construct runtime path.</p>
     */
    private void lowerDescribeToConstructQuery(Query query, List<Node> describedNodes) {
        Exp constructTemplate = Exp.create(Type.BGP);
        int syntheticIndex = 0;
        for (Node describedNode : describedNodes) {
            DescribePattern describePattern = describePattern(describedNode, syntheticIndex++);
            // KGRAM-next currently represents DESCRIBE with outgoing and incoming construct triples.
            constructTemplate.add(describePattern.outgoing().getEdge());
            constructTemplate.add(describePattern.incoming().getEdge());
            query.getBody().add(Exp.create(Type.OPTIONAL, Exp.create(Type.AND), describePattern.optionalBody()));
        }
        query.setConstruct(constructTemplate);
        query.setConstruct(true);
        query.setConstructNodes(constructTemplate.getNodes());
    }

    private DescribePattern describePattern(Node describedNode, int index) {
        Node outgoingPredicate = createSyntheticDescribeNode("p", index, 0);
        Node outgoingValue = createSyntheticDescribeNode("v", index, 0);
        Node incomingPredicate = createSyntheticDescribeNode("p", index, 1);
        Node incomingValue = createSyntheticDescribeNode("v", index, 1);

        Exp outgoing = Exp.create(Type.EDGE, new AstBackedEdge(describedNode, outgoingPredicate, outgoingValue));
        Exp incoming = Exp.create(Type.EDGE, new AstBackedEdge(incomingValue, incomingPredicate, describedNode));
        Exp outgoingBgp = Exp.create(Type.BGP);
        outgoingBgp.add(outgoing);
        Exp incomingBgp = Exp.create(Type.BGP);
        incomingBgp.add(incoming);
        return new DescribePattern(outgoing, incoming, Exp.create(Type.UNION, outgoingBgp, incomingBgp));
    }

    private Node createSyntheticDescribeNode(String role, int describedIndex, int directionIndex) {
        return NodeImpl.forVariable("__describe_" + role + "_" + describedIndex + "_" + directionIndex);
    }

    private record DescribePattern(Exp outgoing, Exp incoming, Exp optionalBody) {
    }

    /**
     * Maps {@code ORDER BY} conditions that are already expressible in runtime KGRAM terms.
     *
     * <p>Variables reuse already-visible query nodes. Other expressions are wrapped as runtime
     * filters and attached to synthetic internal nodes, just like the historical pipeline does.</p>
     */
    private void applyOrderBy(
            Query query, SolutionModifierAst solutionModifier, WhereCompiler compiler) {
        if (!solutionModifier.hasOrderBy()) {
            return;
        }
        List<Exp> orderByExpressions = new ArrayList<>();
        int syntheticIndex = 0;
        for (OrderConditionAst orderCondition : solutionModifier.orderBy()) {
            Exp orderExpression = toOrderByExpression(query, orderCondition, syntheticIndex++, compiler);
            orderExpression.status(orderCondition.orderDirection() == ASTConstants.OrderDirection.DESC);
            orderByExpressions.add(orderExpression);
        }
        query.setOrderBy(orderByExpressions);
    }

    private Exp toOrderByExpression(
            Query query,
            OrderConditionAst orderCondition,
            int syntheticIndex,
            WhereCompiler compiler) {
        TermAst expression = orderCondition.expression();
        if (expression instanceof VarAst(String name)) {
            Exp selectExpression = query.getSelectExp(name);
            Node node = selectExpression != null
                    ? selectExpression.getNode()
                    : visibleBodyNode(query, name);
            if (node == null) {
                throw new IllegalArgumentException(
                        "ORDER BY variable ?" + name + " is not visible in the compiled query");
            }
            return Exp.create(Type.NODE, node);
        }
        Filter filter = new AstBackedExpr(expression, compiler).getFilter();
        Exp exp = Exp.create(Type.NODE, createSyntheticOrderNode(syntheticIndex));
        exp.setFilter(filter);
        return exp;
    }

    private Node createSyntheticOrderNode(int syntheticIndex) {
        return NodeImpl.forVariable("__order_by_" + syntheticIndex);
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

    /**
     * Resolves only variables visible from the outer body scope. This deliberately
     * excludes nodes collected from MINUS/EXISTS bodies, which KGRAM stores as query
     * nodes for internal evaluation but which are not projectable SPARQL bindings.
     */
    private Node visibleBodyNode(Query query, String name) {
        for (Node node : query.selectNodesFromPattern()) {
            if (node.getLabel().equals(name)) {
                return node;
            }
        }
        return null;
    }

    private void applyLimitOffset(Query query, SolutionModifierAst solutionModifier) {
        if (solutionModifier.hasLimit()) {
            query.setLimit(Math.toIntExact(solutionModifier.limit()));
        }
        if (solutionModifier.hasOffset()) {
            query.setOffset(Math.toIntExact(solutionModifier.offset()));
        }
    }

    /**
     * Rejects unsupported clauses for {@code CONSTRUCT} queries.
     *
     * <p>Planned roadmap items:
     * <ul>
     *   <li>Issue #389: {@code GROUP BY} / {@code HAVING} requires aggregate-aware CONSTRUCT semantics.</li>
     *   <li>Issue #389: {@code DISTINCT} / {@code REDUCED} defensive guards.</li>
     * </ul>
     * </p>
     */
    private static void rejectUnsupportedConstructClauses(ConstructQueryAst constructQueryAst) {
        SolutionModifierAst mod = constructQueryAst.solutionModifier();
        if (mod.hasGroupBy() || mod.hasHaving() || mod.distinct() || mod.reduced()) {
            throw new UnsupportedQueryFeatureException(
                    "GROUP BY, HAVING, DISTINCT and REDUCED are not supported yet by the next pipeline for CONSTRUCT");
        }
    }

    /**
     * Compiles a {@code CONSTRUCT} template into a KGRAM {@link Exp} (a BGP of edges), kept separate
     * from the {@code WHERE} body and carried by {@link Query#setConstruct(Exp)}.
     */
    private Exp compileConstructTemplate(
            Query query, ConstructTemplateAst template, WhereCompiler compiler) {
        Exp bgp = Exp.create(Type.BGP);
        for (TriplePatternAst triple : template.triplePatternAsts()) {
            Node subject = constructNode(query, triple.subject(), compiler);
            Node predicate = constructNode(
                    query, WhereCompiler.simplePredicate(triple.predicate()), compiler);
            Node object = constructNode(query, triple.object(), compiler);
            bgp.add(new AstBackedEdge(subject, predicate, object));
        }
        return bgp;
    }

    /**
     * Resolves a template term to a runtime {@link Node}. A variable reuses the body node when it is
     * bound by the {@code WHERE}; otherwise it stays a fresh node (an unbound template variable is
     * valid SPARQL and simply skips its triple at instantiation, so this does not throw). IRIs, blank
     * nodes and literals become fresh constant nodes.
     */
    private Node constructNode(Query query, TermAst term, WhereCompiler compiler) {
        if (term instanceof VarAst(String name)) {
            Node bound = visibleBodyNode(query, name);
            return bound != null ? bound : NodeImpl.forVariable(name);
        }
        return compiler.termResolver().toNode(term);
    }
}
