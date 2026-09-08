package fr.inria.corese.core.next.query.impl.engine.filter;

import fr.inria.corese.core.next.query.impl.engine.model.Expr;
import fr.inria.corese.core.next.query.impl.engine.model.ExprType;

import java.util.ArrayList;
import java.util.List;


/**
 * Filter Exp Checker
 * Check presence of patterns that are always true or always false
 * Such as:
 * ?x != ?x
 * ?x &gt; ?y &amp;&amp; ?x &lt; ?y
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 *
 */
public final class Checker implements ExprType {
	static final int BOOL = BOOLEAN;

	private final List<FilterPattern> alwaysFalse;
	private final Matcher matcher;

	public Checker(){
		matcher = new Matcher();
		alwaysFalse = createAlwaysFalsePatterns();
	}

	/**
	 * Check one filter
	 */
	public boolean check(Expr ee){
		// match = true means that a false pattern matches
		// hence return false (check correctness is false)
		return !match(ee);
	}


	/**
	 * Return true if a false pattern matches
	 * return false otherwise
	 */
	boolean match(Expr ee){


		return match(ee, alwaysFalse);
	}


	boolean match(Expr ee, List<FilterPattern> pat) {
		for (FilterPattern p : pat){
			if (matcher.match(p, ee)){
				return true;
			}
		}
		return false;
	}



	/*************************************************
	 * Always false patterns
	 *
	 */

	List<FilterPattern> createAlwaysFalsePatterns(){
		List<FilterPattern> patterns = new ArrayList<>();
		patterns.add(neqSelf());
		patterns.add(ltSelf());
		patterns.add(notEqSelf());
		patterns.add(notGeSelf());
		patterns.add(patNotPat());
		patterns.add(notOr());
		patterns.add(eqNeq());
		patterns.add(eqGt());
		patterns.add(ltGt());
		patterns.add(gtNotGe());
		patterns.add(eqNotGe());
		return List.copyOf(patterns);
	}


	/**
	 * ?from=cst || ?to=cst
	 */
	FilterPattern path(String v1, String v2){
		FilterPattern e1 = term(EQ, FilterPattern.variable(v1), constant());
		FilterPattern e2 = term(EQ, FilterPattern.variable(v2), constant());
		return or(e1, e2);
	}

	// always false
	FilterPattern neqSelf(){
		// EXP != EXP
		FilterPattern exp = pat();
		return term(NE, exp, exp);

	}

	FilterPattern ltSelf(){
		// EXP < EXP
		FilterPattern exp = pat();
		return term(LT, exp, exp);

	}

	FilterPattern notEqSelf(){
		// !(EXP = EXP)
		FilterPattern exp = pat();
		return not(term(EQ, exp, exp));

	}

	FilterPattern notGeSelf(){
		// !(EXP >= EXP)
		FilterPattern exp = pat();
		return not(term(GE, exp, exp));

	}



	FilterPattern patNotPat(){
		// EXP && ! EXP
		FilterPattern exp = pat();
		return and(exp, not(exp));
	}


	FilterPattern notOr(){
		// ! (EXP || ! EXP)
		FilterPattern exp = pat();
		return not(or(exp, not(exp)));
	}


	// Note:
	// we can have both with list of values
	//  ?x = xpath() && ?x != xpath()
	FilterPattern eqNeq(){
		// EXP1 = EXP2 && EXP1 != EXP2
		FilterPattern e1 = pat();
		FilterPattern e2 = pat();
        return and(term(EQ, e1, e2), term(NE, e1, e2));
	}


	FilterPattern eqGt(){
		// EXP1 = EXP2 && EXP1 > EXP2
		FilterPattern e1 = pat();
		FilterPattern e2 = pat();
		return and(term(EQ, e1, e2), term(GT, e1, e2));
	}

	FilterPattern ltGt(){
		// EXP1 > EXP2 && EXP1 < EXP2
		FilterPattern e1 = pat();
		FilterPattern e2 = pat();
		return and(term(GT, e1, e2), term(LT, e1, e2));
	}


	FilterPattern gtNotGe(){
		// EXP1 > EXP2 && ! (EXP1 >= EXP2)
		FilterPattern e1 = pat();
		FilterPattern e2 = pat();
		return and(term(GT, e1, e2), not(term(GE, e1, e2)));
	}

	FilterPattern eqNotGe(){
		// EXP1 = EXP2 && ! (EXP1 >= EXP2)
		FilterPattern e1 = pat();
		FilterPattern e2 = pat();
		return and(term(EQ, e1, e2), not(term(GE, e1, e2)));
	}


	FilterPattern constant(){
		return FilterPattern.constant();
	}


	FilterPattern pat() {
		return new FilterPattern(ExprType.JOKER);
	}

	FilterPattern pat(FilterPattern e1) {
		return new FilterPattern(ExprType.BOOLEAN, ExprType.NOT, e1);
	}

	FilterPattern pat(int type, int ope, FilterPattern e1, FilterPattern e2){
		return new FilterPattern(type, ope, e1, e2);
	}

	FilterPattern not(FilterPattern e){
		return pat(e);
	}

	FilterPattern and(FilterPattern e1, FilterPattern e2){
		return pat(BOOLEAN, AND, e1, e2);
	}

	FilterPattern or(FilterPattern e1, FilterPattern e2){
		return pat(BOOLEAN, OR, e1, e2);
	}

	FilterPattern term(int ope, FilterPattern e1, FilterPattern e2){
		return pat(TERM, ope, e1, e2);
	}




}
