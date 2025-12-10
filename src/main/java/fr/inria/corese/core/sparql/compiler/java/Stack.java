package fr.inria.corese.core.sparql.compiler.java;

import fr.inria.corese.core.kgram.api.core.ExprType;
import fr.inria.corese.core.sparql.triple.function.script.ForLoop;
import fr.inria.corese.core.sparql.triple.function.script.Function;
import fr.inria.corese.core.sparql.triple.function.script.Let;
import fr.inria.corese.core.sparql.triple.parser.Expression;
import fr.inria.corese.core.sparql.triple.parser.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * Stack record bound variables for parameter passing to exists and subquery
 * Bound variables are function parameters and let local variables
 * TODO: for (?x in exp)
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2017
 */
public class Stack {

    ArrayList<Variable> varList;
    ArrayList<Expression> expList;

    Stack() {
        varList = new ArrayList<>();
        expList = new ArrayList<>();
    }

    void push(Function exp) {
        expList.add(exp);
        for (Expression arg : exp.getFunction().getArgs()) {
            push(arg.getVariable());
        }
    }

    void push(Variable var) {
        varList.add(var);
    }

    void push(Let exp) {
        expList.add(exp);
    }

    void push(ForLoop exp) {
        expList.add(exp);
        push(exp.getVariable());
    }

    void popFunction(Expression exp) {
        for (Expression arg : exp.getFunction().getArgs()) {
            popVar();
        }
    }

    void popLet(Let exp) {
        for (Expression decl : exp.getDeclaration()) {
            popVar();
        }
    }

    void popFor(Expression exp) {
        popVar();
    }

    void popVar() {
        varList.remove(varList.size() - 1);
    }

    void pop(Expression e) {
        Expression exp = expList.remove(expList.size() - 1);
        switch (exp.oper()) {
            case ExprType.FUNCTION:
                popFunction(exp);
                break;
            case ExprType.LET:
                popLet(exp.getLet());
                break;
            case ExprType.FOR:
                popFor(exp);
                break;
        }
    }

    List<Variable> getVariables() {
        return varList;
    }

    boolean isBound(Variable variable) {
        return getVariables().contains(variable);
    }

}
