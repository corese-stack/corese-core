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

	static List<Pattern> alwaysFalse;
	

	Matcher matcher;
	Query query;

	public Checker(Query q){
		matcher = new Matcher();
		query = q;
	}
	
	/**
	 * var1=cst || var2=cst
	 */
	public boolean check(String v1, String v2, Expr ee){
		Pattern p = path(v1, v2);
		return matcher.match(p, ee);
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
	 * Check two filters for contradiction:
	 * ?x = ?y vs ?x != ?y
	 */
	
	public boolean check(Expr e1, Expr e2){
		// fake a AND using a pattern
		Pattern and = new Pattern(BOOL, AND, e1, e2);
		return check(and);
	}
	
	
	/**
	 * Return true if a false pattern matches
	 * return false otherwise
	 */
	boolean match(Expr ee){


		return match(ee, alwaysFalse());
	}


	boolean match(Expr ee, List<Pattern> pat) {
		boolean suc = false, b;
		
		for (Pattern p : pat){
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
	
	List<Pattern> alwaysFalse(){
		if (alwaysFalse == null){
			List<Pattern> pat = new ArrayList<>();
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
	Pattern path(String v1, String v2){
		Pattern e1 = term(EQ, Pattern.variable(v1), constant());
		Pattern e2 = term(EQ, Pattern.variable(v2), constant());
		return or(e1, e2);
	}
	
	// always false
	Pattern neqSelf(){
		// EXP != EXP
		Pattern exp = pat();
		return term(NE, exp, exp);
		
	}
	
	Pattern ltSelf(){
		// EXP < EXP
		Pattern exp = pat();
		return term(LT, exp, exp);
		
	}
	
	Pattern notEqSelf(){
		// !(EXP = EXP)
		Pattern exp = pat();
		return not(term(EQ, exp, exp));
		
	}
	
	Pattern notGeSelf(){
		// !(EXP >= EXP)
		Pattern exp = pat();
		return not(term(GE, exp, exp));
		
	}
	
	
	
	Pattern patNotPat(){
		// EXP && ! EXP
		Pattern exp = pat();
		return and(exp, not(exp));
	}
	
	
	Pattern notOr(){
		// ! (EXP || ! EXP)
		Pattern exp = pat();
		return not(or(exp, not(exp)));
	}

	
	// TODO:
	// we can have both with list of values 
	//  ?x = xpath() && ?x != xpath()
	Pattern eqNeq(){
		// EXP1 = EXP2 && EXP1 != EXP2
		Pattern e1 = pat();
		Pattern e2 = pat();
        return and(term(EQ, e1, e2), term(NE, e1, e2));
	}
	
	
	Pattern eqGt(){
		// EXP1 = EXP2 && EXP1 > EXP2
		Pattern e1 = pat();
		Pattern e2 = pat();
		return and(term(EQ, e1, e2), term(GT, e1, e2));
	}
	
	Pattern ltGt(){
		// EXP1 > EXP2 && EXP1 < EXP2
		Pattern e1 = pat();
		Pattern e2 = pat();
		return and(term(GT, e1, e2), term(LT, e1, e2));
	}
	

	Pattern gtNotGe(){
		// EXP1 > EXP2 && ! (EXP1 >= EXP2)
		Pattern e1 = pat();
		Pattern e2 = pat();
		return and(term(GT, e1, e2), not(term(GE, e1, e2)));
	}

	Pattern eqNotGe(){
		// EXP1 = EXP2 && ! (EXP1 >= EXP2)
		Pattern e1 = pat();
		Pattern e2 = pat();
		return and(term(EQ, e1, e2), not(term(GE, e1, e2)));
	}


	Pattern constant(){
		return Pattern.constant();
	}


	Pattern pat() {
		return new Pattern(ExprType.JOKER);
	}

	Pattern pat(Pattern e1) {
		return new Pattern(ExprType.BOOLEAN, ExprType.NOT, e1);
	}
	
	Pattern pat(int type, int ope, Pattern e1, Pattern e2){
		return new Pattern(type, ope, e1, e2);
	}
	
	Pattern not(Pattern e){
		return pat(e);
	}
	
	Pattern and(Pattern e1, Pattern e2){
		return pat(BOOLEAN, AND, e1, e2);
	}
	
	Pattern or(Pattern e1, Pattern e2){
		return pat(BOOLEAN, OR, e1, e2);
	}
	
	Pattern term(int ope, Pattern e1, Pattern e2){
		return pat(TERM, ope, e1, e2);
	}
	

	

}
