package fr.inria.acacia.corese.triple.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import fr.inria.acacia.corese.triple.cst.Keyword;
import fr.inria.edelweiss.kgram.api.core.Expr;
import fr.inria.edelweiss.kgram.api.core.Filter;
import fr.inria.edelweiss.kgram.api.core.Regex;

/**
 * <p>Title: Corese</p>
 * <p>Description: A Semantic Search Engine</p>
 * <p>Copyright: Copyright INRIA (c) 2007</p>
 * <p>Company: INRIA</p>
 * <p>Project: Acacia</p>
 * <br>
 * The root class of the expressions of the query language: Atom, Variable, Constant, Term
 * <br>
 * @author Olivier Corby
 */

public class Expression extends Statement 
implements Regex, Filter, Expr {
	String name, longName;
	public static final int STDFILTER = 0;
	public static final int ENDFILTER = 1;
	public static final int POSFILTER = 2;
	public static final int BOUND = 4;
	int type = -1, min = -1, max = -1;
	
	boolean isQName = false;
	boolean isEget = false;
	boolean isSystem = false;
	boolean isInverse = false, isReverse = false;

	public Expression(){}
	
	public Expression(String str) {
		name=str;
	}
	
	public  int getArity(){
		return -1;
	}
	
	public ArrayList<Expression> getArgs(){
		return null;
	}
	
	public Expression getArg(int i){
		return null;
	}
	
	public void compile(ASTQuery ast){
	}
	
	public Expression and(Expression e2){
		if (e2 == null){
			return this;
		}
		else {
			return  Term.create(Keyword.SEAND, this, e2);
		}
	}
	
	public Expression star(){
		return Term.function(Term.STAR, this);
	}
	
	public String getName(){
		return name;
	}
	
	public String getLongName(){
		return longName;
	}
	
	public void setLongName(String name){
		longName = name;
	}
	
	public String getKey(){
		return toString();
	}
	
	public void setName(String str){
		name=str;
	}
	
	public boolean isSystem(){
		return isSystem;
	}
	
	public void setSystem(boolean b){
		isSystem = b;
	}
	
	public boolean isArray(){
		return false;
	}
	
	boolean isAtom(){
		return false;
	}
	
	public boolean isConstant(){
		return false;
	}

	public boolean isVariable(){
		return false;
	}
	
	public boolean isSimpleVariable(){
		return false;
	}
	
	// blank as variable in sparql query
	public boolean isBlankNode(){
		return false;
	}
	
	public boolean isTerm(){
		return false;
	}
	
	public boolean isTerm(String oper){
		return false;
	}
	
	public boolean isFunction(){
		return false;
	}
	
	public boolean isFunction(String str){
		return false;
	}
	
	void validate(Parser parser){
		
	}
	
	Bind validate(Bind env){
		return env;
	}
	
	public String getLang(){
		return null;
	}
	
	public String getDatatype(){
		return null;
	}
	
	public String getSrcDatatype() {
		return null;
	}
	
	
	public boolean isOptionVar(Vector<String> stdVar){
		Variable var = getOptionVar(stdVar);
		return var != null;
	}
	
	public Variable getOptionVar(Vector<String> stdVar){
		return null;
	}
	
	public boolean isAnd(){
		return false;
	}
	
	public boolean isSeq(){
		return false;
	}
	
	public boolean isOr(){
		return false;
	}
	
	public boolean isNot(){
		return false;
	}
	
	public boolean isInverse(){
		return isInverse;
	}
	
	public void setInverse(boolean b){
		isInverse = b;
	}
	
	public boolean isReverse(){
		return isReverse;
	}
	
	public void setReverse(boolean b){
		isReverse = b;
	}
	
	public Expression translate(){
		return this;
	}
	
	void setMin(int n){
		min = n;
	}
	
	public int getMin(){
		return min;
	}
	
	void setMax(int n){
		max = n;
	}
	
	public int getMax(){
		return max;
	}
	
	boolean isOrVarEqCst(Variable var){
		return false;
	}
	
	void getCst(Vector<Constant> vec){}
	
	public boolean isStar(){
		return false;
	}
	
	public boolean isOpt(){
		return false;
	}
	
	public boolean isFinal(){
		return false;
	}
	
	public Expression reverse(){
		return this;
	}
	
	public int regLength(){
		return 0;
	}
	
	public int length(){
		return 0;
	}
	
	public boolean isPlus(){
		return false;
	}
	
	public boolean  isType (ASTQuery ast, int type){
		return false;
	}
	
	public boolean  isType (ASTQuery ast, Variable var, int type){
		return false;
	}
	
	public boolean isVisited(){
		return false;
	}
	
	public void setVisited(boolean b){
	}
	
	public boolean isPath(){
		return false;
	}
	
	public boolean isBound(){
		return false;
	}
	
	public Variable getVariable(){
		return null;
	}
	
	// get:gui::?name
	public Variable getIntVariable() {
        return null;
    }
	
	public String toSparql() {
		return null;
	}
	
	/**
	 * Translate some terms like :
	 * different(?x ?y ?z) -> (?x != ?y && ?y != ?z && ?x != ?z)
	 */
	public Expression process(){
		return  this;
	}
	
	/**
	 * use case: select fun(?x) as ?y
	 * rewrite occurrences of ?y as fun(?x)
	 */
	public Expression process(ASTQuery ast){
		return  this;
	}
	
	public Expression rewrite(){
		return this;
	}
	
	/**
	 * Process get:gui
	 * filter ?x >= get:gui --> ?x >= 12
	 */
	public Expression parseGet(Parser parser){
		return this;
	}
	
	public boolean isQName() {
		return isQName;
	}
	
	public void setQName(boolean isQName) {
		this.isQName = isQName;
	}
	
	public boolean isEget() {
		return isEget;
	}
	
	public void setEget(boolean isEget) {
		this.isEget = isEget;
	}
	
	/*************************************************************
	 * 
	 * KGRAM Filter & Exp
	 * 
	 */
	
	public Filter getFilter(){
		return this;
	}

	
	public Expr getExp() {
		// TODO Auto-generated method stub
		return this;
	}

	
	public List<String> getVariables() {
		// TODO Auto-generated method stub
		List<String> list = new ArrayList<String>();
		getVariables(list);
		return list;
	}
	
	public void getVariables(List<String> list) {
	}

	
	public int arity() {
		// TODO Auto-generated method stub
		return 0;
	}

	
	public String getLabel() {
		// TODO Auto-generated method stub
		if (longName!=null) return longName;
		return name;
	}

	
	public Object getValue() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public boolean isAggregate() {
		// TODO Auto-generated method stub
		return false;
	}
	
	public boolean isRecAggregate() {
		// TODO Auto-generated method stub
		return false;
	}
	
	public boolean isFunctional() {
		// TODO Auto-generated method stub
		return false;
	}

	
	public int oper() {
		// TODO Auto-generated method stub
		return -1;
	}

	
	public int type() {
		// TODO Auto-generated method stub
		return type;
	}

	
	public List<Expr> getExpList() {
		// TODO Auto-generated method stub
		return new ArrayList<Expr>();
	}
	
	public Expr getExp(int i){
		return null;
	}

	
	public int getIndex() {
		// TODO Auto-generated method stub
		return -1;
	}

	
	public void setIndex(int index) {
		// TODO Auto-generated method stub
		
	}

	
	public void setArg(Expr exp) {
		// TODO Auto-generated method stub
	}
	
	public Expr getArg(){
		return null;
	}
	
	public Object getPattern(){
		return null;
	}

	
	public boolean isDistinct() {
		// TODO Auto-generated method stub
		return false;
	}

	
	public String getModality() {
		// TODO Auto-generated method stub
		return null;
	}
	
}