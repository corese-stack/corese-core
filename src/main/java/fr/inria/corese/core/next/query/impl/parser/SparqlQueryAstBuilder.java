package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;

import java.util.*;
import java.util.stream.Collectors;

import static fr.inria.corese.core.next.util.StringUtils.trimVariableNames;

/**
 * Build a minimal SPARQL AST for:
 * - Triple patterns (?s ?p ?o)
 * - Basic Graph Patterns (BGP) via TriplesBlock
 * - GroupGraphPattern as a container (can contain multiple TriplesBlock and later OPTIONAL/UNION/etc.)
 * - ASK query
 * Compatible with the common SPARQL grammar shape:
 * GroupGraphPattern
 * : '{'
 * ( TriplesBlock?
 * ( GraphPatternNotTriples '.'? TriplesBlock? )*
 * )?
 * '}'
 * This builder expects the listener to call:
 * - enterGroup()/exitGroup() on enter/exitGroupGraphPattern
 * - enterBgp()/exitBgp() on enter/exitTriplesBlock
 * - addTriple(s,p,o) whenever a triple pattern is recognized (usually on exitTriplesSameSubject)
 * - enterAskQuery at the start of the declaration of an ASK query
 * - enterSelectQuery at the start of the declaration of a Select query
 * - enterConstructQuery / enterConstructTemplate / exitConstructTemplate / exitConstructQuery for CONSTRUCT
 * - addConstructTriple for triples inside the CONSTRUCT template (not WHERE)
 */
public class SparqlQueryAstBuilder extends SparqlAstBuilder {
    private ASTConstants.QUERY_TYPE queryType = ASTConstants.QUERY_TYPE.UNDEFINED;

    // --- Internal stacks (scopes) ---

    /**
     * Final query AST result, set when the top-level SELECT finishes.
     */
    private QueryAst selectQueryResult;

    /**
     * VALUES clause (agglomerate all VALUES declarations in the query)
     */
    protected final List<ValueMappingAst> values = new ArrayList<>();

    /**
     * SELECT projection (* or explicit variables). Set by SelectQueryFeature in enterSelectQuery.
     */
    private ProjectionAst projection = ProjectionAsts.selectAll();

    /** SELECT DISTINCT / REDUCED. Set by SelectQueryFeature when parsing SELECT (DISTINCT | REDUCED)? ... */
    private boolean distinct;
    private boolean reduced;

    /**
     * Variable to hold the value of LIMIT and OFFSET.
     */
    private Long limit;
    private Long offset;

    /**
     * Order conditions
     */
    private final List<OrderConditionAst> orderConditions = new ArrayList<>();

    /**
     * GROUP BY (top-level ASK / CONSTRUCT / DESCRIBE, or outside any SELECT frame).
     */
    private GroupByAst groupBy = new GroupByAst(List.of());

    /**
     * DESCRIBE resources (IRIs or variables). Set by DescribeQueryFeature.
     */
    private final List<TermAst> describeResources = new ArrayList<>();

    /**
     * Triples of the CONSTRUCT template (not the WHERE BGP). Filled between
     * {@link #enterConstructTemplate()} and {@link #exitConstructTemplate()}.
     */
    private final List<TriplePatternAst> constructTriples = new ArrayList<>();

    /**
     * Template AST after {@link #exitConstructTemplate()}; consumed in {@link #getResult()}.
     */
    private ConstructTemplateAst constructTemplate;

    public SparqlQueryAstBuilder(SparqlParserOptions options) {
        super(options);
    }

    public void enterAskQuery() {
        assertTopLevelQueryFormOnly("ASK");
        queryType = ASTConstants.QUERY_TYPE.ASK;
    }

    public void exitAskQuery() {
    }

    public void enterSelectQuery() {
        if (this.queryType == ASTConstants.QUERY_TYPE.UNDEFINED) {
            this.queryType = ASTConstants.QUERY_TYPE.SELECT;
        }
        selectStack.push(new SelectFrame());
    }

    public void exitSelectQuery() {
        SelectFrame frame = selectStack.pop();

        if (frame.whereClause == null) {
            throw new IllegalStateException("No WHERE clause for SELECT query");
        }

        // SELECT projection / ORDER BY scope: reported by SparqlQuerySemanticValidator (validate() collects all diagnostics).

        /*
         * Top-level SELECT keeps the dataset clause declared at query level.
         * Nested SELECT subqueries use an empty dataset clause for now.
         *
         * Prologue is inherited from the enclosing query source context.
         */
        DatasetClauseAst datasetClauseAst = hasCurrentGroup()
                ? DatasetClauseAst.none()
                : new DatasetClauseAst(datasetDefaultGraphs, datasetNamedGraphs);

        QueryPrologueAst prologueAst = new QueryPrologueAst(
                List.copyOf(getPrefixDeclaration()),
                new IriAst(getBaseUri())
        );

        ValuesAst valuesClause = new ValuesAst(this.values);

        SelectQueryAst selectQueryAst = new SelectQueryAst(
                frame.projection,
                datasetClauseAst,
                frame.whereClause,
                buildSolutionModifier(frame),
                prologueAst,
                valuesClause
        );

        /*
         * If we are still inside a parent group, this SELECT is a subquery.
         * Otherwise it is the top-level SELECT result.
         */
        if (hasCurrentGroup()) {
            currentGroup().add(new SubQueryAst(selectQueryAst));
        } else {
            selectQueryResult = selectQueryAst;
        }
    }

    public void enterConstructQuery() {
        assertTopLevelQueryFormOnly("CONSTRUCT");
        queryType = ASTConstants.QUERY_TYPE.CONSTRUCT;
        constructTemplate = null;
        constructTriples.clear();
    }

    public void exitConstructQuery() {
    }

    public void enterConstructTemplate() {
        constructTriples.clear();
    }

    public void exitConstructTemplate() {
        this.constructTemplate = new ConstructTemplateAst(List.copyOf(constructTriples));
    }

    /**
     * Adds a triple to the CONSTRUCT template (inside {@code ConstructTriples}, not WHERE).
     */
    public void addConstructTriple(TermAst s, TermAst p, TermAst o) {
        constructTriples.add(TriplePatternAst.of(s, p, o));
    }

    /**
     * Sets SELECT * (project all variables from the body).
     */
    public void setProjectionAll() {
        if (hasCurrentSelect()) {
            getCurrentSelectFrame().projection = ProjectionAsts.selectAll();
        }
        this.projection = ProjectionAsts.selectAll();
    }

    /**
     * Sets explicit SELECT variables (e.g. SELECT ?s ?p). Variable names may include ? or $ prefix.
     */
    public void setProjectionVariables(List<String> variableNames) {
        setProjectionVariables(variableNames, List.of());
    }

    /**
     * Sets explicit SELECT variables where some may be introduced by {@code (expr AS ?var)}.
     * Variable names may include ? or $ prefix.
     *
     * @param variableNames        all projected variable names (plain + expression-bound), in order
     * @param expressionBoundNames names of variables introduced by {@code (expr AS ?var)}
     */
    public void setProjectionVariables(List<String> variableNames, List<String> expressionBoundNames) {
        setProjectionVariables(variableNames, expressionBoundNames, Map.of(), Map.of());
    }

    /**
     * Sets explicit SELECT variables where some may be introduced by {@code (expr AS ?var)}.
     * Variable names may include ? or $ prefix.
     *
     * @param variableNames                 all projected variable names (plain + expression-bound), in order
     * @param expressionBoundNames          names of variables introduced by {@code (expr AS ?var)}
     * @param expressionTerms               expression AST for each expression-bound projection
     * @param expressionReferencedVariables referenced variables for each expression-bound projection
     */
    public void setProjectionVariables(
            List<String> variableNames,
            List<String> expressionBoundNames,
            Map<String, TermAst> expressionTerms,
            Map<String, Set<String>> expressionReferencedVariables
    ) {
        if (variableNames == null || variableNames.isEmpty()) {
            setProjectionAll();
            return;
        }
        List<VarAst> vars = variableNames.stream()
                .map(s -> s == null ? "" : trimVariableNames(s))
                .filter(s -> !s.isBlank())
                .map(VarAst::new)
                .toList();

        Set<String> expressionBound = expressionBoundNames == null ? Set.of() :
                expressionBoundNames.stream()
                        .map(s -> s == null ? "" : trimVariableNames(s))
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.toUnmodifiableSet());

        Map<String, TermAst> normalizedExpressionTerms =
                expressionTerms == null ? Map.of()
                        : expressionTerms.entrySet().stream()
                        .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                        .collect(Collectors.toUnmodifiableMap(
                                entry -> trimVariableNames(entry.getKey()),
                                Map.Entry::getValue));

        Map<String, Set<String>> normalizedReferencedVariables =
                expressionReferencedVariables == null ? Map.of()
                        : expressionReferencedVariables.entrySet().stream()
                        .filter(entry -> entry.getKey() != null)
                        .collect(Collectors.toUnmodifiableMap(
                                entry -> trimVariableNames(entry.getKey()),
                                entry -> entry.getValue() == null ? Set.of() : entry.getValue().stream()
                                        .filter(Objects::nonNull)
                                        .map(s -> trimVariableNames(s))
                                        .filter(s -> !s.isBlank())
                                        .collect(Collectors.toUnmodifiableSet())));

        ProjectionAst newProjection = vars.isEmpty()
                ? ProjectionAsts.selectAll()
                : ProjectionAsts.of(vars, expressionBound, normalizedExpressionTerms, normalizedReferencedVariables);

        if (hasCurrentSelect()) {
            getCurrentSelectFrame().projection = newProjection;
        }
        else this.projection = newProjection;
    }

    /**
     * Sets the projection from an existing AST.
     */
    public void setProjection(ProjectionAst projection) {
        ProjectionAst effectiveProjection = projection != null ? projection : ProjectionAsts.selectAll();
        if (hasCurrentSelect()) {
            getCurrentSelectFrame().projection = effectiveProjection;
        }
        else this.projection = effectiveProjection;
    }

    public void addValues(List<ValueMappingAst> mappings) {
        this.values.addAll(mappings);
    }

    /**
     * Sets SELECT DISTINCT. Called by SelectQueryFeature when {@code DISTINCT} is present.
     */
    public void setDistinct(boolean distinct) {
        if (hasCurrentSelect()) {
            getCurrentSelectFrame().distinct = distinct;
        }
        else this.distinct = distinct;
    }

    /**
     * Sets SELECT REDUCED. Called by SelectQueryFeature when {@code REDUCED} is present.
     */
    public void setReduced(boolean reduced) {
        if (hasCurrentSelect()) {
            getCurrentSelectFrame().reduced = reduced;
        }
        else this.reduced = reduced;
    }

    /**
     * Sets the LIMIT for pagination
     *
     */
    public void setLimit(long limit) {
        if (hasCurrentSelect()) {
            getCurrentSelectFrame().limit = limit;
        }
        else  this.limit = limit;
    }

    /**
     * Sets the OFFSET for pagination
     *
     */
    public void setOffset(long offset) {
        if (hasCurrentSelect()) {
            getCurrentSelectFrame().offset = offset;
        }
        else this.offset = offset;
    }

    public void addHavingCondition(TermAst condition) {
        if (condition == null) {
            throw new IllegalArgumentException("HAVING condition is null");
        }

        if (hasCurrentSelect()) {
            getCurrentSelectFrame().havingConditions.add(condition);
        } else {
            havingConditions.add(condition);
        }
    }

    /**
     * ASK, CONSTRUCT and DESCRIBE may only appear as the root {@code query} in {@code queryUnit}.
     * Nested graph patterns use {@code subSelect} (SELECT only) per SPARQL 1.1; the grammar enforces this,
     * but we guard against inconsistent listener wiring.
     */
    private void assertTopLevelQueryFormOnly(String queryForm) {
        if (!groupStack.isEmpty() || !selectStack.isEmpty()) {
            throw new QuerySyntaxException(
                    queryForm + " is only allowed as the top-level query form. "
                            + "Inside { ... }, SPArQL only allows SELECT subqueries.");
        }
    }

    // --- Result ---

    /**
     * Returns the final AST.
     * The top-level WHERE clause must have been set by exitGroup() (root group closed).
     * One of the enter*Query() methods must have been called for the corresponding query form.
     *
     * @return the root {@link QueryAst}
     * @throws QueryEvaluationException if query type could not be determined (no enter*Query() called)
     * @throws IllegalStateException    if no WHERE clause was set (exitGroup() not called for root) or unhandled query type
     */
    public QueryAst getResult() {
        if (selectQueryResult != null) return selectQueryResult;
        if (whereClause == null && this.queryType != ASTConstants.QUERY_TYPE.DESCRIBE) {
            throw new IllegalStateException("No WHERE clause: did you call exitGroup() for the top-level GroupGraphPattern?");
        }
        DatasetClauseAst datasetClauseAst = new DatasetClauseAst(datasetDefaultGraphs, datasetNamedGraphs);
        QueryPrologueAst prologueAst = new QueryPrologueAst(List.copyOf(getPrefixDeclaration()), new IriAst(getBaseUri()));
        ValuesAst valuesClause = new ValuesAst(this.values);
        return switch (this.queryType) {
            case ASK -> buildAskQueryAst(datasetClauseAst, prologueAst, valuesClause);
            case CONSTRUCT -> buildConstructQueryAst(datasetClauseAst, prologueAst,valuesClause);
            case DESCRIBE -> buildDescribeQueryAst(datasetClauseAst, prologueAst, valuesClause);
            case SELECT -> buildSelectQueryAst(datasetClauseAst, prologueAst, valuesClause);
            default -> throw new QueryEvaluationException("Could not determine the type of query during parsing");
        };
    }

    // --- Internal helpers ---

    /**
     * Builds the AST for ASK operations.
     */
    private AskQueryAst buildAskQueryAst(DatasetClauseAst datasetClauseAst, QueryPrologueAst prologue, ValuesAst valuesClause) {
        return new AskQueryAst(datasetClauseAst, whereClause, buildSolutionModifier(), prologue, valuesClause);
    }

    /**
     * Builds the AST for SELECT operations.
     */
    private SelectQueryAst buildSelectQueryAst(DatasetClauseAst datasetClauseAst, QueryPrologueAst prologue, ValuesAst valuesClause) {
        if (hasCurrentSelect()) {
            SelectFrame frame = getCurrentSelectFrame();
            return new SelectQueryAst(
                    frame.projection,
                    datasetClauseAst,
                    frame.whereClause,
                    buildSolutionModifier(frame),
                    prologue,
                    valuesClause
            );
        }
        return new SelectQueryAst(projection, datasetClauseAst, whereClause, buildSolutionModifier(), prologue, valuesClause);
    }

    /**
     * Builds the AST for DESCRIBE operations.
     */
    private DescribeQueryAst buildDescribeQueryAst(DatasetClauseAst datasetClauseAst, QueryPrologueAst prologue, ValuesAst valuesClause) {
        // TODO #306: validate variable scope for DESCRIBE modifiers when DescribeQueryAst carries them.
        return new DescribeQueryAst(
                datasetClauseAst,
                describeResources,
                whereClause,
                buildSolutionModifier(),
                prologue,
                valuesClause);
    }

    /**
     * Builds the AST for CONSTRUCT operations.
     */
    private ConstructQueryAst buildConstructQueryAst(DatasetClauseAst datasetClauseAst, QueryPrologueAst prologue, ValuesAst valuesClause) {
        // TODO #306: validate variable scope for CONSTRUCT modifiers when ConstructQueryAst carries them.
        return new ConstructQueryAst(
                constructTemplate != null ? constructTemplate : new ConstructTemplateAst(List.of()),
                datasetClauseAst,
                whereClause,
                buildSolutionModifier(),
                prologue,
                valuesClause);
    }

    /**
     * Sets {@code GROUP BY} for the current SELECT subquery frame, or for the top-level query
     * when no SELECT frame is active (e.g. {@code ASK} / {@code CONSTRUCT}).
     */
    public void setGroupBy(GroupByAst groupByAst) {
        GroupByAst use = groupByAst != null ? groupByAst : new GroupByAst(List.of());
        if (hasCurrentSelect()) {
            getCurrentSelectFrame().groupBy = use;
        } else {
            this.groupBy = use;
        }
    }

    /**
     * Builds the solution modifier (GROUP BY, DISTINCT, REDUCED, HAVING, ORDER BY, LIMIT, OFFSET).
     */
    private SolutionModifierAst buildSolutionModifier() {
        return new SolutionModifierAst(
                this.groupBy,
                distinct,
                reduced,
                this.orderConditions,
                new HavingAst(List.copyOf(havingConditions)),
                limit,
                offset);
    }

    /**
     * Builds solution modifiers for the given SELECT frame.
     */
    private SolutionModifierAst buildSolutionModifier(SelectFrame frame) {
        return new SolutionModifierAst(
                frame.groupBy,
                frame.distinct,
                frame.reduced,
                frame.orderConditions,
                new HavingAst(List.copyOf(frame.havingConditions)),
                frame.limit,
                frame.offset);
    }

    public boolean isOrdered() {
        if (hasCurrentSelect()) {
            return !getCurrentSelectFrame().orderConditions.isEmpty();
        }
        return !this.orderConditions.isEmpty();
    }

    /**
     * Set the order condition
     * @param expr either a variable or a contraint
     */
    public void addOrderExpression(ASTConstants.OrderDirection direction, TermAst expr) {
        if (hasCurrentSelect()) {
            getCurrentSelectFrame().orderConditions.add(new OrderConditionAst(direction, expr));
        }
        else this.orderConditions.add(new OrderConditionAst(direction, expr));
    }

    /**
     * Signals the start of a {@code DESCRIBE} query declaration.
     * Sets the internal query type to {@link ASTConstants.QUERY_TYPE#DESCRIBE}.
     */
    public void enterDescribeQuery() {
        assertTopLevelQueryFormOnly("DESCRIBE");
        queryType = ASTConstants.QUERY_TYPE.DESCRIBE;
    }

    /**
     * Called when the parser exits a {@code DESCRIBE} query. No-op.
     */
    public void exitDescribeQuery() {
    }

    /**
     * Adds a resource (IRI or variable) to the DESCRIBE target list.
     *
     * @param term the IRI or variable to describe; must not be {@code null}
     */
    public void addDescribeResource(TermAst term) {
        if (term == null) throw new IllegalArgumentException("DESCRIBE resource term is null");
        describeResources.add(term);
    }
}
