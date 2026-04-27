package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.kgram.api.core.Filter;

/**
 * Builds KGRAM {@code Exp} / {@code Query} structures from Corese-next query AST nodes.
 *
 */
public final class CoreseAstQueryBuilder {

    public CoreseAstQueryBuilder() {
    }

    /**
     * Converts a filter term (boolean expression) from the next parser into a
     * {@link fr.inria.corese.core.sparql.triple.parser.Expression}, then exposes it as a
     * {@link Filter} with {@link Filter#coreseNextSource()} set.
     *
     * <p>For static-style calls without an instance, use {@link SparqlAstToExpression#toNextFilter(TermAst)}.
     */
    public Filter toNextFilter(TermAst filterExpression) {
        return SparqlAstToExpression.toNextFilter(filterExpression);
    }
}
