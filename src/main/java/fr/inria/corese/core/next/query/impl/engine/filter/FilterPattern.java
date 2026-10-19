package fr.inria.corese.core.next.query.impl.engine.filter;

import fr.inria.corese.core.next.query.impl.engine.pattern.PlanCopy;
import fr.inria.corese.core.next.query.impl.engine.pattern.PlanCopyable;

import fr.inria.corese.core.next.query.impl.engine.model.BindingContext;
import fr.inria.corese.core.next.query.impl.engine.model.Expr;
import fr.inria.corese.core.next.query.impl.engine.model.ExprType;
import fr.inria.corese.core.next.query.impl.engine.model.Filter;

import fr.inria.corese.core.next.query.impl.engine.spi.Environment;
import fr.inria.corese.core.next.query.impl.engine.spi.Evaluator;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.data.api.model.DatatypeValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Filter Expression Pattern ExpressionPatternMatcher.
 *
 * This class provides pattern matching capabilities for filter expressions.
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 */
public final class FilterPattern implements ExprType, Expr, PlanCopyable<FilterPattern> {
    private static final String NOT_SUPPORTED_YET = "Not supported yet.";
    int type;
    int oper;
    String label;
    // recursive pattern, ako *
    boolean rec = false;
    boolean matchConstant = true;
    List<Expr> args;

    /** Copies optimizer metadata into the execution graph. */
    @Override
    public FilterPattern copyPlan(PlanCopy copies) {
        FilterPattern result = new FilterPattern(type);
        copies.remember(this, result);
        result.oper = oper;
        result.label = label;
        result.rec = rec;
        result.matchConstant = matchConstant;
        result.args = copies.copy(args);
        return result;
    }

    FilterPattern(int t) {
        type = t;
        oper = JOKER;
        args = new ArrayList<>();
    }

    FilterPattern(int t, int o) {
        this(t);
        oper = o;
    }

    FilterPattern(int t, int o, Expr e1) {
        this(t, o);
        add(e1);
    }

    FilterPattern(int t, int o, Expr e1, Expr e2) {
        this(t, o, e1);
        add(e2);
    }

    public FilterPattern(int t, int o, int e1, int e2){
        this(t, o);
        add(new FilterPattern(e1));
        add(new FilterPattern(e2));
    }

    static FilterPattern variable(String label) {
        FilterPattern p = new FilterPattern(VARIABLE);
        p.setLabel(label);
        return p;
    }

    static FilterPattern constant() {
        return new FilterPattern(CONSTANT);
    }

    void add(Expr exp) {
        args.add(exp);
    }

    public int arity() {
        return args.size();
    }

    void setRec() {
        rec = true;
    }

    boolean isRec() {
        return rec;
    }

    void setMatchConstant() {
        matchConstant = false;
    }

    boolean isMatchConstant() {
        return matchConstant;
    }

    public Expr getExp(int i) {
        return args.get(i);
    }

    public List<Expr> getExpList() {
        return args;
    }

    public Filter getFilter() {
        return null;
    }

    public int getIndex() {
        return 0;
    }

    public String getLabel() {
        return label;
    }

    void setLabel(String l) {
        label = l;
    }

    @Override
    public DatatypeValue getValue() {
        return null;
    }

    public boolean isAggregate() {
        return false;
    }

    public int oper() {
        return oper;
    }

    public void setIndex(int index) {
        // FilterPattern indices are immutable
    }

    public int type() {
        return type;
    }

    public String toString() {
        return "pat(" + type + ", " + oper + ")";
    }

    @Override
    public Exp getPattern() {
        return null;
    }

    @Override
    public boolean isVariable() {
        return false;
    }

    @Override
    public boolean isRecAggregate() {
        return false;
    }

    @Override
    public boolean isRecExist() {
        return false;
    }

    @Override
    public int subtype() {
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
    }

    @Override
    public boolean match(int oper) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
    }

    @Override
    public boolean isConstant() {
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
    }

    @Override
    public DatatypeValue evalWE(Evaluator eval, BindingContext b, Environment env, Producer p) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
    }
}
