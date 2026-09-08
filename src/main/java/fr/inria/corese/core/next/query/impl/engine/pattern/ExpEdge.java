package fr.inria.corese.core.next.query.impl.engine.pattern;

import fr.inria.corese.core.next.query.impl.engine.model.Expr;
import fr.inria.corese.core.next.query.impl.engine.model.ExprType;
import fr.inria.corese.core.next.query.impl.engine.model.Filter;
import fr.inria.corese.core.next.query.impl.engine.model.Node;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2017
 *
 */
public class ExpEdge extends Exp {

    ExpEdge(Type t){
        super(t);
    }

    /**
     * node: subject|predicate|object
     * type: operator or function or any
     * return filter exp list where
     * - first arg is variable = node variable
     * - second arg is constant
     * - oper = type or oper = boolean connector on such subexp
     */
    @Override
    public List<Filter> getFilters(int node, int type){
        Node n = getNode(node);
        if (n == null || ! n.isVariable()){
            return new ArrayList<>(0);
        }
        ArrayList<Filter> list = new ArrayList<>();
        for (Filter f : getFilters()){
            if (match(f.getExp(), n, node, type)){
                list.add(f);
            }
        }
        return list;
    }

    /**
     * If e is boolean connector, check subexp recursively
     * Otherwise check:
     * e match type
     * first arg is variable n
     * second arg (if any) is constant
     * type may be a query type such as TINKERPOP that match a set of oper
     */
    boolean match(Expr e, Node n, int node, int type) {
        if (e.type() == ExprType.BOOLEAN) {
            for (Expr ee : e.getExpList()) {
                if (!match(ee, n, node, type)) {
                    return false;
                }
            }
            return true;
        }
        if (!e.match(type) || !match(e, n, node)) {
            return false;
        }
        if (e.arity() == 1) {
            return true;
        }
        Expr constant = e.getExp(1);
        if (constant.isConstant()) {
            return e.arity() != 3 || e.getExp(2).isConstant();
        }
        return e.oper() == ExprType.IN && allConstant(constant.getExpList());
    }

    private boolean allConstant(List<Expr> expressions) {
        for (Expr expression : expressions) {
            if (!expression.isConstant()) {
                return false;
            }
        }
        return true;
    }

    /**
     * lang, datatype not with predicate
     */
    boolean compatible(Expr e, int node){
        if (node == PREDICATE){
            return !e.match(ExprType.LANG) && !e.match(ExprType.DATATYPE);
        }
        return true;
    }

    /**
     * exp is
     * var = cst
     * datatype(var) = cst
     * lang(var) = cst
     */
    boolean match(Expr exp, Node n, int node) {
        if (exp.arity() > 0) {
            Expr fst = exp.getExp(0);
            if (fst.isVariable()){
                return fst.getLabel().equals(n.getLabel());
            }
            else if (compatible(fst, node) && (fst.match(ExprType.DATATYPE) || fst.match(ExprType.LANG)) && fst.arity() == 1) {
                // datatype(var) == cst
                Expr varEpr = fst.getExp(0);
                return varEpr.isVariable() && varEpr.getLabel().equals(n.getLabel());
            }
        }
        return false;
    }

}
