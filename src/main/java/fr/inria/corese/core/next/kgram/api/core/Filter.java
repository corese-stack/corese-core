package fr.inria.corese.core.next.kgram.api.core;

import fr.inria.corese.core.sparql.triple.parser.Expression;

import java.util.List;


/**
 * Interface of Filter that contains an evaluable expression
 * Filter (and Expr) api refer to sparql.triple.parser.Expression
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
