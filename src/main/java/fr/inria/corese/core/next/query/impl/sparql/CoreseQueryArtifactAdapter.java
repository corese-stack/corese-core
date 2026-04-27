package fr.inria.corese.core.next.query.impl.sparql;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.bridge.CoreseAstQueryBuilder;
import fr.inria.corese.core.next.query.kgram.api.core.Filter;

import java.util.Objects;

/**
 * Adapts SPARQL query AST to KGRAM Exp/Query. Delegates to {@link CoreseAstQueryBuilder}.
 */
public final class CoreseQueryArtifactAdapter {

    private final CoreseAstQueryBuilder queryBuilder;

    public CoreseQueryArtifactAdapter() {
        this(new CoreseAstQueryBuilder());
    }

    public CoreseQueryArtifactAdapter(CoreseAstQueryBuilder queryBuilder) {
        this.queryBuilder = Objects.requireNonNull(queryBuilder);
    }

    /**
     * Converts a WHERE filter term from the next AST into a KGRAM {@link Filter} (backed by
     * {@link fr.inria.corese.core.sparql.triple.parser.Expression} under {@link fr.inria.corese.core.next.query.impl.sparql.bridge.AstBackedExpr}).
     */
    public Filter adaptFilter(TermAst filterExpression) {
        return queryBuilder.toNextFilter(filterExpression);
    }

    // Full QueryAst → Query / Exp translation is future work (see CoreseAstQueryBuilder javadoc).

}
