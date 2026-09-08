package fr.inria.corese.core.next.query.impl.engine.filter;

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
 * Filter Expression Pattern Matcher.
 *
 * This class provides pattern matching capabilities for filter expressions.
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 */
public final class FilterPattern implements ExprType, Expr {
    private static final String NOT_SUPPORTED_YET = "Not supported yet.";
	int type;
	int oper;
	String label;
	// recursive pattern, ako *
	boolean rec = false;
	boolean matchConstant = true;
	List<Expr> args;
	Expr exp;

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

	public boolean isBound() {
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
	public boolean isDistinct() {
		return false;
	}

	@Override
	public String getModality() {
		return null;
	}

	@Override
	public boolean isVariable() {
		return false;
	}

	@Override
	public Expr getArg() {
		return null;
	}

	@Override
	public void setArg(Expr exp) {
		// FilterPattern arguments are immutable
	}

	@Override
	public boolean isRecAggregate() {
		return false;
	}

	@Override
	public boolean isExist() {
		return false;
	}

	@Override
	public boolean isRecExist() {
		return false;
	}

	@Override
	public boolean isFuncall() {
		return false;
	}

	@Override
	public void setOper(int n) {
		// FilterPattern operator is immutable
	}

	@Override
	public void setExp(int i, Expr e) {
		// FilterPattern sub-expressions are immutable
	}

	@Override
	public Expr getDefine() {
		throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
	}

	@Override
	public void setDefine(Expr exp) {
		throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
	}

	@Override
	public int subtype() {
		throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
	}

	@Override
	public Expr getFunction() {
		throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
	}

	@Override
	public Expr getBody() {
		throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
	}

	@Override
	public Expr getVariable() {
		throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
	}

	@Override
	public Expr getDefinition() {
		throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
	}

	@Override
	public boolean isSystem() {
		throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
	}

	@Override
	public boolean isTrace() {
		throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
	}

	@Override
	public boolean isDebug() {
		throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
	}

	@Override
	public boolean isPublic() {
		throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
	}

	@Override
	public DatatypeValue getDatatypeValue() {
		throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
	}

	@Override
	public void setPublic(boolean b) {
		throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
	}

	@Override
	public void setSubtype(int n) {
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
	public boolean hasMetadata(String type) {
		throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
	}

	@Override
	public boolean isDynamic() {
		throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
	}

	@Override
	public DatatypeValue evalWE(Evaluator eval, BindingContext b, Environment env, Producer p) {
		throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
	}
}
