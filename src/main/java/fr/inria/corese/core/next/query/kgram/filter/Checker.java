package fr.inria.corese.core.next.query.kgram.filter;

import fr.inria.corese.core.next.query.kgram.api.core.Expr;
import fr.inria.corese.core.next.query.kgram.api.core.ExprType;
import fr.inria.corese.core.next.query.kgram.core.Query;

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
public class Checker implements ExprType {
	public static boolean verbose = true;
	
	static final int BOOL = BOOLEAN;

	static List<FilterPattern> alwaysFalse;
	

	Matcher matcher;
	Query query;

	public Checker(Query q){
		matcher = new Matcher();
		query = q;
	}
	
	/**
	 * Check one filter 
	 */
	public boolean check(Expr ee){
		boolean b = match(ee);
		// match = true means that a false pattern matches
		// hence return false (check correctness is false)
		return ! b;
	}

	
	/**
	 * Return true if a false pattern matches
	 * return false otherwise
	 */
	boolean match(Expr ee){


		return match(ee, alwaysFalse());
	}


	boolean match(Expr ee, List<FilterPattern> pat) {
		boolean suc = false, b;
		
		for (FilterPattern p : pat){
			b = matcher.match(p, ee);
			if (b){
				suc = true;
			}
		}
		
		return suc;
	}
	
	

	/*************************************************
	 * Always false patterns
	 * 
	 */
	
	List<FilterPattern> alwaysFalse(){
		if (alwaysFalse == null){
			List<FilterPattern> pat = new ArrayList<>();
			pat.add(neqSelf());
			pat.add(ltSelf());
			pat.add(notEqSelf());
			pat.add(notGeSelf());
			
			pat.add(patNotPat());
			pat.add(notOr());
			pat.add(eqNeq());
			pat.add(eqGt());
			pat.add(ltGt());
			pat.add(gtNotGe());
			pat.add(eqNotGe());

			alwaysFalse = pat;
		}
		return alwaysFalse;
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

	
	// TODO:
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
