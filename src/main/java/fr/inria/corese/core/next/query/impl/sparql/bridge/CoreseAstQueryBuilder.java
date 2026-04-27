package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.kgram.api.core.Filter;

/**
 * Builds KGRAM {@code Exp} / {@code Query} structures from Corese-next query AST nodes.
 *
 * <p>Filter expressions are converted today via {@link SparqlAstToExpression} and wrapped with
 * {@link AstBackedExpr} / {@link NextFilterFromAst}. Translating full {@code GroupGraphPatternAst}
 * or complete {@code QueryAst} into KGRAM “next” {@link fr.inria.corese.core.next.query.kgram.core.Query} /
 * {@link fr.inria.corese.core.next.query.kgram.core.Exp} is future work (required for
 * {@code EXISTS} / {@code NOT EXISTS} inside {@link SparqlAstToExpression}).
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
