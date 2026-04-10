package fr.inria.corese.core.next.query.kgram.api.core;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.sparql.triple.parser.Expression;

import java.util.List;
import java.util.Optional;


/**
 * Interface of Filter that contains an evaluable expression
 * Filter (and Expr) api refer to sparql.triple.parser.Expression
 *
 * <p>Migration: filters built from the Corese-next SPARQL AST should use
 * {@link fr.inria.corese.core.next.query.impl.sparql.bridge.SparqlAstToExpression}
 * and {@link fr.inria.corese.core.next.query.impl.sparql.bridge.AstBackedExpr} /
 * {@link fr.inria.corese.core.next.query.impl.sparql.bridge.NextFilterFromAst}.
 * {@link #getFilterExpression()} remains the bridge to the legacy interpreter until KGRAM is fully unified.
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 */
public interface Filter {

    /**
     * When non-empty, the Corese-next AST node this filter was produced from
     * (filter expression: {@link TermAst} / constraint subtypes).
     */
    default Optional<TermAst> coreseNextSource() {
        return Optional.empty();
    }

    /**
     * List of variable names contained in the filter
     *
     */
    List<String> getVariables();

    List<String> getVariables(boolean excludeLocal);

    /**
     * Evaluable expression processed by KGRAM generic Interpreter
     * Expr api refer also to sparql.triple.parser.Expression
     *
     */
    Expr getExp();

    Expression getFilterExpression();

    /**
     * Does filter contain a bound() function
     *
     */
    boolean isBound();

    /**
     * Is it an aggregate function such as count() min() sum()
     *
     */
    boolean isAggregate();

    boolean isRecAggregate();

    boolean isFunctional();

    boolean isRecExist();


}
