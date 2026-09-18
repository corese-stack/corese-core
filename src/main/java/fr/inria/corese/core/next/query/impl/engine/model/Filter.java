package fr.inria.corese.core.next.query.impl.engine.model;

import java.util.List;

/**
 * Native filter contract for the Corese-next execution engine.
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 */
public interface Filter {

    /**
     * List of variable names contained in the filter
     *
     */
    List<String> getVariables();

    List<String> getVariables(boolean excludeLocal);

    /** Evaluable expression processed by the native KGRAM evaluator. */
    Expr getExp();

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
