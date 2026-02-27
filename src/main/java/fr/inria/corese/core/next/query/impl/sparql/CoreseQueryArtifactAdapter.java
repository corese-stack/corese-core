package fr.inria.corese.core.next.query.impl.sparql;

import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.bridge.CoreseAstQueryBuilder;
import fr.inria.corese.core.next.query.kgram.core.Exp;
import fr.inria.corese.core.next.query.kgram.core.Query;

/**
 * Adapts SPARQL query AST to KGRAM Exp/Query. Delegates to {@link CoreseAstQueryBuilder}.
 */
public final class CoreseQueryArtifactAdapter {

    private final CoreseAstQueryBuilder queryBuilder;

    public CoreseQueryArtifactAdapter() {
        this.queryBuilder = new CoreseAstQueryBuilder();
    }

    public CoreseQueryArtifactAdapter(CoreseAstQueryBuilder queryBuilder) {
        this.queryBuilder = queryBuilder;
    }

    /**
     * Converts the WHERE clause of the given query AST into a KGRAM body expression.
     *
     * @param ast query AST (e.g. from {@link fr.inria.corese.core.next.query.impl.parser.SparqlParser}), may be null
     * @return body Exp for use with {@link Query#setBody(Exp)} (empty BGP if ast is null)
     */
    public Exp toBody(QueryAst ast) {
        return queryBuilder.buildBody(ast);
    }

    /**
     * Converts the given query AST into a minimal KGRAM Query with the WHERE
     * clause as body. Select, FROM, etc. are not set.
     *
     * @param ast query AST, may be null
     * @return Query with body set from ast (empty BGP if ast is null)
     */
    public Query toQuery(QueryAst ast) {
        return queryBuilder.buildQuery(ast);
    }
}
