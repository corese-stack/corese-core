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


}
