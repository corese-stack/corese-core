package fr.inria.corese.core.next.query.impl.kgram.api.core;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;
import java.util.Optional;


/**
 * Native filter contract backed by the Corese-next SPARQL AST.
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

    /** Evaluable expression processed by the native KGRAM evaluator. */
    Expr getExp();

    TermAst getFilterExpression();

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
