package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.api.exception.UnsupportedQueryFeatureException;
import fr.inria.corese.core.next.query.impl.engine.model.ExpType.Type;
import fr.inria.corese.core.next.query.impl.engine.model.Filter;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.sparql.ast.AskQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ConstraintAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ConstructQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.DatasetClauseAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.DescribeQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.GroupGraphPatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SolutionModifierAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ValuesAst;

import java.util.ArrayList;
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
        SolutionModifierCompiler.applyOrderBy(query, askQueryAst.solutionModifier(), compiler);
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

        WhereCompiler compiler = whereCompiler.withPrologue(selectQueryAst.prologue());
        Query query = createQuery(
                selectQueryAst.whereClause(),
                selectQueryAst.datasetClause(),
                selectQueryAst.solutionModifier(),
                selectQueryAst.valuesClause(),
                compiler);
        SolutionModifierCompiler.applyGroupBy(query, selectQueryAst.solutionModifier(), compiler);
        SolutionModifierCompiler.applyProjection(query, selectQueryAst.projection(), compiler);
        // Full deduplication or preserving cardinality is permitted by SELECT REDUCED.
        query.setDistinct(selectQueryAst.solutionModifier().distinct());
        SolutionModifierCompiler.applyOrderBy(query, selectQueryAst.solutionModifier(), compiler);
        SolutionModifierCompiler.applyHaving(query, selectQueryAst.solutionModifier(), compiler);
        if (query.getHaving() != null && !query.hasGroupBy()) {
            query.setAggregate(true);
        }
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
        SolutionModifierCompiler.applyOrderBy(query, describeQueryAst.solutionModifier(), compiler);
        DescribeQueryCompiler.compile(query, describeQueryAst, compiler);
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

        WhereCompiler compiler = whereCompiler.withPrologue(constructQueryAst.prologue());
        Query query = createQuery(
                constructQueryAst.whereClause(),
                constructQueryAst.datasetClause(),
                constructQueryAst.solutionModifier(),
                constructQueryAst.valuesClause(),
                compiler);
        SolutionModifierCompiler.applyOrderBy(query, constructQueryAst.solutionModifier(), compiler);
        ConstructQueryCompiler.compile(query, constructQueryAst, compiler);
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
     * Defensively rejects grouping and duplicate modifiers unsupported for {@code ASK}.
     *
     * @param askQueryAst query whose modifiers are checked
     * @throws UnsupportedQueryFeatureException if an unsupported modifier is present
     */
    private static void rejectUnsupportedAskClauses(AskQueryAst askQueryAst) {
        SolutionModifierAst mod = askQueryAst.solutionModifier();
        if (mod.hasGroupBy() || mod.hasHaving() || mod.distinct() || mod.reduced()) {
            throw new UnsupportedQueryFeatureException(
                    "GROUP BY, HAVING, DISTINCT and REDUCED are not supported yet by the next pipeline for ASK");
        }
    }

    /**
     * Defensively rejects grouping and duplicate modifiers unsupported for {@code DESCRIBE}.
     *
     * @param describeQueryAst query whose modifiers are checked
     * @throws UnsupportedQueryFeatureException if an unsupported modifier is present
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
        SolutionModifierCompiler.applyLimitOffset(query, solutionModifier);
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
}
