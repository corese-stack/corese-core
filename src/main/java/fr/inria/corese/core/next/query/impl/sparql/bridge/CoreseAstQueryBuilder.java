package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.api.exception.UnsupportedQueryFeatureException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.PathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.PredicatePathAst;
import fr.inria.corese.core.next.query.impl.kgram.api.core.ExpType.Type;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Filter;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Node;
import fr.inria.corese.core.next.query.impl.kgram.core.Exp;
import fr.inria.corese.core.next.query.impl.kgram.core.Query;
import fr.inria.corese.core.next.query.impl.kgram.tool.NodeImpl;
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

        QueryPrologueAst prev = SparqlAstToExpression.getCurrentPrologue();
        try {
            SparqlAstToExpression.setCurrentPrologue(askQueryAst.prologue());
            Query query = createQuery(
                    askQueryAst.whereClause(),
                    askQueryAst.datasetClause(),
                    askQueryAst.solutionModifier());
            applyOrderBy(query, askQueryAst.solutionModifier());
            query.setAsk(true);
            return query;
        } finally {
            SparqlAstToExpression.setCurrentPrologue(prev);
        }
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

        QueryPrologueAst prev = SparqlAstToExpression.getCurrentPrologue();
        try {
            SparqlAstToExpression.setCurrentPrologue(selectQueryAst.prologue());
            Query query = createQuery(
                    selectQueryAst.whereClause(),
                    selectQueryAst.datasetClause(),
                    selectQueryAst.solutionModifier());
            applyProjection(query, selectQueryAst.projection());
            query.setDistinct(selectQueryAst.solutionModifier().distinct());
            applyOrderBy(query, selectQueryAst.solutionModifier());
            return query;
        } finally {
            SparqlAstToExpression.setCurrentPrologue(prev);
        }
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

        QueryPrologueAst prev = SparqlAstToExpression.getCurrentPrologue();
        try {
            SparqlAstToExpression.setCurrentPrologue(describeQueryAst.prologue());
            Query query = createQuery(
                    describeQueryAst.whereClause(),
                    describeQueryAst.datasetClause(),
                    describeQueryAst.solutionModifier());
            applyOrderBy(query, describeQueryAst.solutionModifier());
            List<Node> describedNodes = describeNodes(query, describeQueryAst);
            lowerDescribeToConstructQuery(query, describedNodes);
            return query;
        } finally {
            SparqlAstToExpression.setCurrentPrologue(prev);
        }
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

        QueryPrologueAst prev = SparqlAstToExpression.getCurrentPrologue();
        try {
            SparqlAstToExpression.setCurrentPrologue(constructQueryAst.prologue());
            Query query = createQuery(
                    constructQueryAst.whereClause(),
                    constructQueryAst.datasetClause(),
                    constructQueryAst.solutionModifier());
            applyOrderBy(query, constructQueryAst.solutionModifier());
            Exp template = compileConstructTemplate(query, constructQueryAst.constructTemplate());
            query.setConstruct(true);
            query.setConstruct(template);
            query.setConstructNodes(template.getNodes());
            return query;
        } finally {
            SparqlAstToExpression.setCurrentPrologue(prev);
        }
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
        return SparqlAstToExpression.toNextFilter(filterExpression, whereCompiler);
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

    static TermAst simplePredicate(PathAst path) {
        if (path instanceof PredicatePathAst(TermAst predicate)) {
            return predicate;
        }
        throw new UnsupportedQueryFeatureException(
                "Property path bridge compilation is not supported yet by the next pipeline for: "
                        + path.getClass().getSimpleName());
    }

    /**
     * Rejects unsupported clauses for {@code ASK} queries.
     *
     * <p>Planned roadmap items:
     * <ul>
     *   <li>Issue #388: inline {@code VALUES} requires a dedicated runtime mapping.</li>
     *   <li>Issue #388: {@code GROUP BY} / {@code HAVING} requires aggregate-aware ASK semantics.</li>
     *   <li>Issue #388: {@code REDUCED} support aligned with next-pipeline query-form policy.</li>
     * </ul>
     * </p>
     */
    private static void rejectUnsupportedAskClauses(AskQueryAst askQueryAst) {
        if (!askQueryAst.valuesClause().mappings().isEmpty()) {
            throw new UnsupportedQueryFeatureException(
                    "Inline VALUES is not supported yet by the next pipeline for ASK");
        }
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
     *   <li>Issue #387: inline {@code VALUES} requires a dedicated runtime mapping.</li>
     *   <li>Issue #387: {@code SELECT} expressions and aliases need a runtime story and reuse in later clauses.</li>
     *   <li>Issue #387: {@code GROUP BY} / {@code HAVING} require aggregate semantics, not only AST field propagation.</li>
     *   <li>Issue #387: {@code REDUCED} support aligned with next-pipeline query-form policy.</li>
     * </ul>
     * </p>
     */
    private static void rejectUnsupportedSelectClauses(SelectQueryAst selectQueryAst) {
        if (!selectQueryAst.valuesClause().mappings().isEmpty()) {
            throw new UnsupportedQueryFeatureException(
                    "Inline VALUES is not supported yet by the next pipeline for SELECT");
        }
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
     *   <li>Issue #390: inline {@code VALUES} requires a dedicated runtime mapping.</li>
     *   <li>Issue #390: {@code GROUP BY} / {@code HAVING} requires aggregate-aware DESCRIBE semantics.</li>
     *   <li>Issue #390: {@code REDUCED} support aligned with next-pipeline query-form policy.</li>
     * </ul>
     * </p>
     */
    private static void rejectUnsupportedDescribeClauses(DescribeQueryAst describeQueryAst) {
        if (!describeQueryAst.valuesClause().mappings().isEmpty()) {
            throw new UnsupportedQueryFeatureException(
                    "Inline VALUES is not supported yet by the next pipeline for DESCRIBE");
        }
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
            SolutionModifierAst solutionModifier) {
        Query query = Query.create(whereCompiler.compile(whereClause));
        // Collect visible nodes once so later clauses (projection, ORDER BY, DESCRIBE)
        // can resolve variables against the compiled runtime body.
        query.collect();
        applyDataset(query, datasetClause);
        applyLimitOffset(query, solutionModifier);
        return query;
    }

    private void applyDataset(Query query, DatasetClauseAst datasetClause) {
        query.setFrom(toNodeList(datasetClause.graphs()));
        query.setNamed(toNodeList(datasetClause.namedGraphs()));
        query.setDatasetSpecified(
                !datasetClause.graphs().isEmpty() || !datasetClause.namedGraphs().isEmpty());
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
    private List<Node> describeNodes(Query query, DescribeQueryAst describeQueryAst) {
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
                nodes.add(toNode(term));
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
        return new NodeImpl(Variable.create("__describe_" + role + "_" + describedIndex + "_" + directionIndex));
    }

    private record DescribePattern(Exp outgoing, Exp incoming, Exp optionalBody) {
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
                    : visibleBodyNode(query, name);
            if (node == null) {
                throw new IllegalArgumentException(
                        "ORDER BY variable ?" + name + " is not visible in the compiled query");
            }
            return Exp.create(Type.NODE, node);
        }
        Filter filter = SparqlAstToExpression.toNextFilter(expression, whereCompiler);
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
     *   <li>Issue #389: inline {@code VALUES} requires a dedicated runtime mapping.</li>
     *   <li>Issue #389: {@code GROUP BY} / {@code HAVING} requires aggregate-aware CONSTRUCT semantics.</li>
     *   <li>Issue #389: {@code DISTINCT} / {@code REDUCED} defensive guards.</li>
     * </ul>
     * </p>
     */
    private static void rejectUnsupportedConstructClauses(ConstructQueryAst constructQueryAst) {
        if (!constructQueryAst.valuesClause().mappings().isEmpty()) {
            throw new UnsupportedQueryFeatureException(
                    "Inline VALUES is not supported yet by the next pipeline for CONSTRUCT");
        }
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
    private Exp compileConstructTemplate(Query query, ConstructTemplateAst template) {
        Exp bgp = Exp.create(Type.BGP);
        for (TriplePatternAst triple : template.triplePatternAsts()) {
            Node subject = constructNode(query, triple.subject());
            Node predicate = constructNode(query, simplePredicate(triple.predicate()));
            Node object = constructNode(query, triple.object());
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
    private Node constructNode(Query query, TermAst term) {
        if (term instanceof VarAst(String name)) {
            Node bound = visibleBodyNode(query, name);
            return bound != null ? bound : toNode(term);
        }
        return toNode(term);
    }
}
