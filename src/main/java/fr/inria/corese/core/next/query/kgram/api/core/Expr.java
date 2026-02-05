package fr.inria.corese.core.next.query.kgram.api.core;

import fr.inria.corese.core.next.query.kgram.api.query.Environment;
import fr.inria.corese.core.next.query.kgram.api.query.Evaluator;
import fr.inria.corese.core.next.query.kgram.api.query.Producer;
import fr.inria.corese.core.sparql.api.IDatatype;

import java.util.List;

/**
 * Expr api refer to sparql.triple.parser.Expression
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 */
public interface Expr {

    Filter getFilter();

    // Exp as Object for modularity
    Object getPattern();

    boolean isSystem();

    boolean isPublic();

    void setPublic(boolean b);

    boolean isDynamic();

    boolean isTrace();

    boolean isDebug();

    String getLabel();

    String getModality();

    List<Expr> getExpList();

    Expr getExp(int i);

    void setExp(int i, Expr e);

    Expr getArg();

    void setArg(Expr exp);

    /**
     * Returns the constant value of this expression, if any.
     *
     * @return the datatype value, or null if not a constant
     */
    DatatypeValue getValue();

    /**
     * Returns the datatype value of this expression.
     *
     * @return the datatype value
     */
    DatatypeValue getDatatypeValue();

    int type();

    int subtype();

    void setSubtype(int n);

    int oper();

    boolean match(int oper);

    void setOper(int n);

    boolean isAggregate();

    boolean isRecAggregate();

    boolean isExist();

    boolean isRecExist();

    boolean isVariable();

    boolean isConstant();

    boolean isFuncall();

    boolean isBound();

    boolean isDistinct();

    int arity();

    int getIndex();

    void setIndex(int index);

    Expr getDefine();

    void setDefine(Expr exp);

    Expr getFunction();

    Expr getBody();

    Expr getVariable();

    Expr getDefinition();

    boolean hasMetadata(String name);

    /**
     * Evaluates this expression with the given context.
     *
     * @param eval the evaluator to use
     * @param b the binding context
     * @param env the environment
     * @param p the producer
     * @return the result of the evaluation
     */
    IDatatype evalWE(Evaluator eval, BindingContext b, Environment env, Producer p);

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
        IDatatype dt = evalWE(eval, b, env, p);
        if (dt == null) {
            return false;
        }
        return dt.isTrueTest();
    }

}