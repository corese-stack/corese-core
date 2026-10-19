package fr.inria.corese.core.next.query.impl.engine.model;

import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;

import fr.inria.corese.core.next.query.impl.engine.spi.Environment;
import fr.inria.corese.core.next.query.impl.engine.spi.Evaluator;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;
import fr.inria.corese.core.next.data.api.model.DatatypeValue;

import java.util.List;

/**
 * Compiled expression contract used by runtime evaluation and filter planning.
 * Constants use the native datatype model; nested EXISTS patterns expose compiled
 * operators rather than parser or syntax-tree objects.
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 */
public interface Expr {

    Filter getFilter();

    /** Returns a compiled EXISTS pattern, or null when this expression has none. */
    Exp getPattern();

    String getLabel();

    List<Expr> getExpList();

    Expr getExp(int i);

    /**
     * Returns the constant value of this expression, if any.
     *
     * @return the datatype value, or null if not a constant
     */
    DatatypeValue getValue();

    int type();

    int subtype();

    int oper();

    boolean match(int oper);

    boolean isAggregate();

    boolean isRecAggregate();

    boolean isRecExist();

    boolean isVariable();

    boolean isConstant();

    int arity();

    int getIndex();

    void setIndex(int index);

    /**
     * Evaluates this expression with the given context.
     *
     * @param eval the evaluator to use
     * @param b the binding context
     * @param env the environment
     * @param p the producer
     * @return the result of the evaluation
     */
    DatatypeValue evalWE(Evaluator eval, BindingContext b, Environment env, Producer p);

    /**
     * Tests if this expression evaluates to true.
     *
     * @param eval the evaluator to use
     * @param b the binding context
     * @param env the environment
     * @param p the producer
     * @return true if the expression is truthy
     */
    default boolean test(Evaluator eval, BindingContext b, Environment env, Producer p) {
        DatatypeValue dt = evalWE(eval, b, env, p);
        if (dt == null) {
            return false;
        }
        return dt.isTrue();
    }

}
