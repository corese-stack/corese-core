package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.kgram.api.core.ExpType;
import fr.inria.corese.core.next.query.kgram.core.Exp;
import fr.inria.corese.core.next.query.kgram.core.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds KGRAM Exp (body) and Query from AST query nodes.
 *
 * <p>
 * This class is part of the SPARQL → KGRAM bridge layer. It converts
 * high-level SPARQL AST structures (e.g. {@link QueryAst},
 * {@link GroupGraphPatternAst}) into executable KGRAM objects
 * ({@link Exp}, {@link Query}) used by the Corese next engine.
 * </p>
 *
 * <h3>Current Scope (MVP)</h3>
 * <p>
 * At this stage, only <b>Basic Graph Patterns (BGP)</b> are supported.
 * </p>
 * <ul>
 *   <li>Each {@link BgpAst} is flattened into a single {@link Exp}
 *       of type {@link ExpType.Type#BGP}.</li>
 *   <li>Each {@link TriplePatternAst} is converted into a KGRAM {@link fr.inria.corese.core.next.query.kgram.api.core.Edge}
 *       using {@link CoresePatternBuilder}.</li>
 *   <li>Other SPARQL constructs (OPTIONAL, UNION, FILTER, GRAPH, etc.)
 *       are currently ignored and not translated.</li>
 * </ul>
 *
 * <p>
 * The resulting {@link Query} contains only a WHERE body (BGP).
 * Projection (SELECT variables), modifiers, and other SPARQL features
 * are not yet mapped to KGRAM.
 * </p>
 *
 */
public final class CoreseAstQueryBuilder {
    private final CoresePatternBuilder patternBuilder;

    public CoreseAstQueryBuilder() {
        this.patternBuilder = new CoresePatternBuilder();
    }

    public CoreseAstQueryBuilder(CoresePatternBuilder patternBuilder) {
        this.patternBuilder = patternBuilder;
    }

    /**
     * Builds the KGRAM body expression from a {@link QueryAst}.
     *
     * <p>If the AST is null, an empty BGP expression is returned.</p>
     *
     * @param ast SPARQL query AST (may be null)
     * @return KGRAM body expression (never null)
     */
    public Exp buildBody(QueryAst ast) {
        if (ast == null) {
            return Exp.create(ExpType.Type.BGP);
        }
        return buildBody(ast.whereClause());
    }

    /**
     * Builds the KGRAM body expression from a {@link GroupGraphPatternAst}.
     *
     * <p>
     * Only {@link BgpAst} patterns are processed. All triple patterns
     * are flattened into a single BGP expression.
     * </p>
     *
     * @param group group graph pattern (may be null or empty)
     * @return a single BGP {@link Exp} containing all triples
     */
    public Exp buildBody(GroupGraphPatternAst group) {
        if (group == null || group.patterns().isEmpty()) {
            return Exp.create(ExpType.Type.BGP);
        }
        List<TriplePatternAst> allTriples = new ArrayList<>();
        for (PatternAst p : group.patterns()) {
            if (p instanceof BgpAst bgp) {
                allTriples.addAll(bgp.triples());
            }
        }
        Exp bgpExp = Exp.create(ExpType.Type.BGP);
        for (TriplePatternAst t : allTriples) {
            bgpExp.add(patternBuilder.toEdge(t));
        }
        return bgpExp;
    }

    /**
     * Builds a minimal KGRAM {@link Query} from a SPARQL {@link QueryAst}.
     *
     * <p>
     * The returned query contains only the WHERE body (BGP).
     * Projection, ORDER BY, LIMIT, etc. are not yet supported.
     * </p>
     *
     * @param ast SPARQL query AST (may be null)
     * @return KGRAM query ready to be executed by the engine
     */
    public Query buildQuery(QueryAst ast) {
        Exp body = buildBody(ast);
        Query q = new Query();
        q.setBody(body);
        return q;
    }
}
