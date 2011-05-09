package fr.inria.acacia.corese.triple.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;

import fr.inria.acacia.corese.cg.datatype.DatatypeMap;
import fr.inria.acacia.corese.triple.cst.Keyword;
import fr.inria.acacia.corese.triple.cst.KeywordPP;
import fr.inria.acacia.corese.triple.cst.RDFS;
import fr.inria.acacia.corese.triple.update.ASTUpdate;

/**
 * <p>Title: Corese</p>
 * <p>Description: A Semantic Search Engine</p>
 * <p>Copyright: Copyright INRIA (c) 2007</p>
 * <p>Company: INRIA</p>
 * <p>Project: Acacia</p>
 * <br>
 * This class is the abstract syntax tree, it represents the initial query (except for get:gui).<br>
 * When complete, it will be transformed into a Query Graph.java.
 * <br>
 * @author Virginie Bottollier
 */

public class ASTQuery  implements Keyword {

	/** Use to keep the class version, to be consistent with the interface Serializable.java */
	private static final long serialVersionUID = 1L;

	/** logger from log4j */
	private static Logger logger = Logger.getLogger(ASTQuery.class);	
	
	static final String SQ1 = "\"";
	static final String SQ3 = "\"\"\"";
	static final String SSQ = "'";
	static final String SSQ3 = "'''";
	static final String BS = "\\\\";
	static final String ESQ = "\\\""; // occurrence of \" in a string
	static final String ESSQ = "\\'"; // occurrence of \'
	static String RootPropertyQN =  RDFS.RootPropertyQN; // cos:Property
	static String RootPropertyURI = RDFS.RootPropertyURI; //"http://www.inria.fr/acacia/corese#Property";
	static final String LIST = "list";
	
	static int nbt=0; // to generate an unique id for a triple if needed
	
	public final static int QT_SELECT 	= 0;
	public final static int QT_ASK 		= 1;
	public final static int QT_CONSTRUCT = 2;
	public final static int QT_DESCRIBE = 3;
	public final static int QT_DELETE 	= 4;
	public final static int QT_UPDATE 	= 5;
	

	/** if graph rule */
	boolean rule = false;
	boolean isConclusion = false;
	/** approximate projection */
	boolean more = false;
    boolean isDelete = false;
	/** default process join */
	boolean join = false;
	/** join result into one graph */
	boolean one = false;
	/** sparql bind */
	boolean XMLBind = true;
	boolean fake = false;
	/** select distinct where : all are distinct */
	boolean distinct = false;
	/** true : sparql, false : corese */
	boolean strictDistinct = true;
	/** relation on which join connex */
	boolean connex = false;
	boolean union = false;
    boolean hasScore = false;
	/** display blank node id */
	boolean displayBNID = false;
	/** display with rdf:resource */
	boolean flat = false;
	/** display in RDF */
	boolean rdf = false, isJSON = false;
	/** display types of query */
	boolean pQuery = false;
	/** select * : select all variables from query */
	boolean selectAll = false;
    boolean hasGet=false;   // is there a get:gui ?
    boolean hasGetSuccess=false; // if get:gui, does the first one has a value ?
    // validation mode (check errors)
    private boolean validate = false; 
	boolean sorted = true; // if the relations must be sorted (default true)
	boolean debug = false;
    boolean nosort = false, 
    // load from and from named documents before processing
    isLoad = false, 
    isCorrect = true;
    /** booleans useful for the sparql pretty printer */
    boolean where = false;
    boolean merge = false;
    /** used in QueryGraph.java to compile the construct */
    boolean constructCompiled = false;
    // construct in the std graph:
    boolean isAdd = false;
    boolean describeAll = false;
    // generate a relation for rdf:type
    boolean isRelationForType = false;
    boolean isKgram = false, 
    // true means with SPARQL 1.1 syntax for select (fun(?x) as ?y) where
    isSPARQL1 = false,
    isBind = false;
    
	/** max cg result */
	int MaxResult = Integer.MAX_VALUE;
	int DefaultMaxResult = Integer.MAX_VALUE;
	/** max projection */
	int MaxProjection = Integer.MAX_VALUE;
	int DefaultMaxProjection = Integer.MAX_VALUE;
	// path length max
	int DefaultMaxLength = 5;
	int MaxDisplay = 10000;
	/** Offset */
	int Offset = 0;
	static long nbBNode = 0;
    int nbd = 0; // to generate an unique id for a variable if needed
	int resultForm = QT_SELECT;
	
	
	/** if more, reject 2 times worse projection than best one */
	float Threshold = 1;
	float DefaultThreshold = 1;
	//byte access = Cst.ADMIN;
	
	// predefined ns from server
	String namespaces, base;
	// relax by dd:distance
	String distance; 
	/** the source text of the query */
	String text = null;
	/** Represents the ASTQuery before compilation */
    String queryPrettyPrint = "";
 
	
    /** Source body of the query returned by javacc parser */
    Exp bodyExp;
	/** the source triple query as an Exp */
	//Exp tripleQuery;
	/** Compiled triple query expression */
	Exp query ;
	// compiled construct (graph ?g removed)
	Exp constructExp, 
	// genuine construct
	construct,
	delete;

    // triples that define prefix/namespace
    Exp prefixExp = new And();
    
	Expression having;
    
	Vector<Variable> selectVar = new Vector<Variable>();
	Vector<Expression> sort = new Vector<Expression>();
	Vector<Expression> lGroup = new Vector<Expression>();

	Vector<String> select = new Vector<String>();
	Vector<String> group = new Vector<String>();
	Vector<String> from  = new Vector<String>();
	Vector<String> named = new Vector<String>();
	// values given by query
	Vector<String> defFrom, defNamed; 
    Vector<String> describe = new Vector<String>();  
	Vector<String> source = new Vector<String>();
	Vector<String> stack = new Vector<String>(); // bound variables
	Vector<String> vinfo;
	Vector<String> errors;
	
	ArrayList<Variable> varBindings;
	ArrayList<ArrayList<Constant>> valueBindings;
	
	Vector<Boolean> reverseTable = new Vector<Boolean>();
//	Vector<Boolean> sortDistanceTable = new Vector<Boolean>();
	
	Hashtable<String, Expression> selectFunctions = new Hashtable<String, Expression>();
	ExprTable selectExp   = new ExprTable();
	ExprTable regexExpr   = new ExprTable();

    // pragma display {}
    Hashtable<String, Exp> pragma;
    Hashtable<String, Exp> blank;

	NSTable tfrom, tdeny;
	TTable ttable;
	NSManager nsm;
	ASTUpdate astu;
	
	class ExprTable extends Hashtable<Expression,Expression> {};

	/**
	 * The constructor of the class It looks like the one for QueryGraph
	 */
	private ASTQuery() {
    }
	
	ASTQuery(String query) {
		setText(query);
    }
	
	public static ASTQuery create(String query){
		return new ASTQuery(query);
	}
	
	public static ASTQuery create(){
		return new ASTQuery();
	}
	
	public static ASTQuery create(Exp exp){
		ASTQuery ast = new ASTQuery();
		ast.setBody(exp);
		return ast;
	}
	
    public static ASTQuery create(String query, boolean isRule, boolean isConclusion) {
    	ASTQuery aq = new ASTQuery(query);
	    aq.setConclusion(isConclusion);
	    aq.setRule(isRule);
	    return aq;
    }
    
    /**
	 * AST for a subquery
	 * share prefix declaration
	 */
	public ASTQuery subCreate(){
		ASTQuery ast = create();
		ast.setNSM(getNSM());
		ast.setEdgeForType(isEdgeForType());
		ast.setKgram(isKgram());
		return ast;
	}
	
	public void setDefaultFrom(String[] from){
		if (from != null && from.length>0){
			defFrom = new Vector<String>(from.length,0);
			for (String name : from){
				defFrom.add(name);
			}
		}
	}
	
	public void setDefaultFrom(List<String> from){
		if (from != null && from.size()>0){
			defFrom = new Vector<String>(from.size());
			for (String name : from){
				defFrom.add(name);
			}
		}
	}
	
	public void setDefaultNamed(String[] from){
		if (from != null && from.length>0){
			defNamed = new Vector<String>(from.length,0);
			for (String name : from){
				defNamed.add(name);
			}
		}
	}
	
	public void setDefaultNamed(List<String> from){
		if (from != null && from.size()>0){
			defNamed = new Vector<String>(from.size());
			for (String name : from){
				defNamed.add(name);
			}
		}
	}
	
	public void setValidate(boolean b){
		validate = b;
	}
	
	public boolean isValidate(){
		return validate;
	}
	
	Vector<String> getDefaultFrom(){
		return defFrom;
	}
	
	Vector<String> getDefaultNamed(){
		return defNamed;
	}
	
	public Vector<String> getActualFrom(){
		if (from.size() > 0) return from;
		if (defFrom != null) return defFrom;
		return from;
	}
	
	public Vector<String> getActualNamed(){
		if (named.size() > 0) return named;
		if (defNamed != null) return defNamed;
		return named;
	}
	
	

    
 
	/**
	 *
	 * @param info
	 */
	public void addInfo(String info) {
		if (vinfo == null)
			vinfo = new Vector<String>(1, 1);
		vinfo.add(info);
	}

	/**
	 *
	 * @param error
	 */
	public void addError(String error) {
		if (errors == null)
			errors = new Vector<String>(1, 1);
		if (!errors.contains(error)) errors.add(error);
	}
	
	public Vector<String> getErrors(){
		return errors;
	}

//	public byte getAccess() {
//		return access;
//	}
//
//	public void setAccess(byte access) {
//		this.access = access;
//	}

	public void setConnex(boolean connex) {
		this.connex = connex;
	}

	public void setDisplayBNID(boolean displayBNID) {
        this.displayBNID = displayBNID;
	}

	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}
	
	public void setReduced(boolean b) {
		//this.distinct = distinct;
	}
	
	public void setStrictDistinct(boolean strictDistinct) {
		this.strictDistinct = strictDistinct;
	}

	public boolean isStrictDistinct() {
		return strictDistinct;
	}
	
	public void setFake(boolean fake) {
		this.fake = fake;
	}

	public void setRDF(boolean rdf) {
		this.rdf = rdf;
	}
	
	public void setJSON(boolean b) {
		isJSON = b;
	}
	
	public boolean isJSON(){
		return isJSON;
	}
	
	public void setFlat(boolean flat) {
		this.flat = flat;
	}

//	public void setGroup(Vector<String> group) {
//		this.group = group;
//	}

	public void setConclusion(boolean isConclusion) {
		this.isConclusion = isConclusion;
	}

	public void setJoin(boolean join) {
		this.join = join;
	}

	public void setMaxDisplay(int maxDisplay) {
		MaxDisplay = maxDisplay;
	}

	public void setMaxProjection(int maxProjection) {
		MaxProjection = maxProjection;
	}

	public void setMaxResult(int maxResult) {
		MaxResult = maxResult;
	}

	public void setMore(boolean more) {
		this.more = more;
	}

	public void setNamed(Vector<String> named) {
		this.named = named;
	}
	
	public void setOne(boolean one) {
      this.one = one;
	}

    public boolean isUnion() {
        return union;
    }

    public void setUnion(boolean union) {
        this.union = union;
    }

	public void setPQuery(boolean query) {
		pQuery = query;
	}

	public void setQuery(Exp query) {
		this.query = query;
	}

    public void setScore(boolean score) {
        this.hasScore = score;
    }

    public boolean getScore() {
        return hasScore;
    }

    void setSource(String src){
        if (! source.contains(src))
          source.add(src);
    }

    public Vector<String> getSource() {
        return source;
    }
    
    public void setDistance(String dist){
    	distance = dist;
    }
    
    public String getDistance(){
    	return distance;
    }

	public void setRule(boolean rule) {
		this.rule = rule;
	}

	public void setSelectAll(boolean selectAll) {
		// We print relations between concepts if SELECT DISPLAY RDF *
		// SELECT DISPLAY RDF * <=> SELECT DISPLAY RDF
		if (selectAll && isRDF()) this.selectAll = false;
		else this.selectAll = selectAll;
	}
	
	public void setBasicSelectAll(boolean b) {
		selectAll = b;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setThreshold(float threshold) {
		Threshold = threshold;
	}

	public void setXMLBind(boolean bind) {
		XMLBind = bind;
	}

	public boolean isConnex() {
		return connex;
	}

	public boolean isDisplayBNID() {
		return displayBNID;
	}

	public boolean isDistinct() {
		return distinct;
	}

	public boolean isFake() {
		return fake;
	}

	public boolean isRDF() {
		return rdf;
	}
	
	public boolean isFlat() {
		return flat;
	}

	public Vector<String> getFrom() {
		return from;
	}

	public Vector<String> getGroup() {
		return group;
	}
	
	public boolean isGroup(){
		return group.size() > 0;
	}

	public boolean isConclusion() {
		return isConclusion;
	}

	public boolean isJoin() {
		return join;
	}

	public int getMaxDisplay() {
		return MaxDisplay;
	}

	public int getMaxProjection() {
		return MaxProjection;
	}

	public int getMaxResult() {
		return MaxResult;
	}

	public boolean isMore() {
		return more;
	}

	public Vector<String> getNamed() {
		return named;
	}
	
	public void setLoad(boolean b){
		isLoad = b;
	}
	
	public boolean isLoad(){
		return isLoad;
	}

	/**
	 * NS Manager
	 */
	public NSManager getNSM() {
		if (nsm == null){ 
			nsm = NSManager.create(getDefaultNamespaces());
			nsm.setBase(getDefaultBase());
		}
		return nsm;
	}
	
	public void setNSM(NSManager nsm){
		this.nsm = nsm;
	}
	
	public String getDefaultNamespaces() {
		return namespaces;
	}
	
	public void setDefaultNamespaces(String ns){
		namespaces = ns;
	}
	
	public String getDefaultBase() {
		return base;
	}
	
	public void setDefaultBase(String ns){
		base = ns;
	}

    public boolean isOne() {
        return one;
	}

    /** here we want to know if there is the keyword one and one or several union in the query */
    public boolean isAll() {
        return (isUnion() && !isOne());
    }

	public boolean isPQuery() {
		return pQuery;
	}

	public Exp getQuery() {
		return query;
	}
	
	public Exp getExtBody(){
		if (query != null) return query;
		return getBody();
	}

	public boolean[] getReverse() {
		//return reverse;
        return Parser.getArray(reverseTable);
	}

	public boolean isRule() {
		return rule;
	}

	public Vector<String> getSelect() {
		return select;
	}
	
	public List<Variable> getSelectVar() {
		return selectVar;
	}

	public boolean isSelectAll() {
		return selectAll;
	}

	public Vector<Expression> getSort() {
		return sort;
	}
	
	public List<Expression> getGroupBy2() {
		ArrayList<Expression> list = new ArrayList<Expression>();
		for (String var : group){
			list.add(Variable.create(var));
		}
		return list;
	}



	public String getText() {
		return text;
	}

	public float getThreshold() {
		return Threshold;
	}

	public boolean isXMLBind() {
		return XMLBind;
	}

	/** created for the new parser */
	
	
	public static Term createRegExp(Expression exp) {
		Term term =  Term.function(REGEX, exp);
		return term;
	}
	
	boolean checkBlank(Expression exp){
		if (exp.isBlankNode()){
			setCorrect(false);
			return false;
		}
		return true;
	}
	
	/**
	 * BIND( f(?x) as ?y )
	 */
	public Exp createBind(Expression exp, Variable var){
		ASTQuery ast = subCreate();
		ast.setBody(BasicGraphPattern.create());
		ast.setSelect(var, exp); 
		ast.setBind(true);
		Query q = Query.create(ast); 
		return q;
	}
	
	Term createTerm(String oper, Expression exp1, Expression exp2){
		checkBlank(exp1);
		checkBlank(exp2);
		Term term =   Term.create(oper, exp1, exp2);
		return term;
	}
	
	public  Term createConditionalAndExpression(String oper, Expression exp1, Expression exp2) {
		return createTerm(oper, exp1, exp2);
	}

	public  Term createConditionalOrExpression(String oper, Expression exp1, Expression exp2) {
		if (oper.equals(SOR)){
			oper = SEOR;
		}
		return createTerm(oper, exp1, exp2);

	}

	public  Term createRelationalExpression(String oper, Expression exp1, Expression exp2) {
		return createTerm(oper, exp1, exp2);

	}

	public  Term createMultiplicativeExpression(String oper, Expression exp1, Expression exp2) {
		return createTerm(oper, exp1, exp2);

	}

	public  Term createAdditiveExpression(String oper, Expression exp1, Expression exp2) {
		return createTerm(oper, exp1, exp2);

	}

    public  Expression createUnaryExpression(String oper, Expression expression) {
    	checkBlank(expression);
        if (oper.equals(SENOT)) {
            expression =  new Term(oper, expression);
        } 
        else if (oper.equals("-")) {
            expression = new Term(oper, 
            		 Constant.create("0", RDFS.qxsdInteger), expression);
        } // else : oper.equals("+") => don't do anything
        return expression;
    }

    public  Term createFunction(String name) {
    	Term term = Term.function(name);
    	if (nsm!=null) term.setLongName(nsm.toNamespace(name));
    	return term;
    }
    
    public  Term createFunction(String name, ExpressionList el) {
    	Term term = createFunction(name);
    	term.setDistinct(el.isDistinct());
    	if (el.getSeparator()!=null){
    		term.setModality(clean(el.getSeparator()));
    	}
    	for (Expression exp : el){
    		term.add(exp);
    	}
    	return term;
    }
    
    public Triple createTriple(Expression exp){
    	checkBlank(exp);
    	return Triple.create(exp);
    }
    
    public  Term createList(ExpressionList el) {
    	return createFunction(LIST, el);
    }

    public Term negation(Expression e){
    	return Term.negation(e);
    }
    
    public  Term createExist(Exp exp, boolean negation) {
    	Term term = Term.function(Term.EXIST);
    	term.setExist(Exist.create(exp));
    	if (negation){
    		term = negation(term);
    	}
    	return term;
    }
    

	public  Term createFunction(String name, Expression expression1) {
        Term term = Term.function(name);
        term.add(expression1);
		return term;
	}

	static Term createTerm(String s) {
		Term term = new Term(s);
		return term;
	}

	public Term createGet(Expression exp, int n){
		return Term.function("get", exp, Constant.create(n));
	}
	
	public Atom createAtom(String value, Parser parser) {
		String datatype = null;
		String lang = null;
		int index = value.indexOf(KeywordPP.SDT); // ^^
		boolean literal = false;
		if (index != -1) {
			datatype = value.substring(index + KeywordPP.SDT.length());
			value = value.substring(0, index);
			literal = true;
		}
		index = value.indexOf(KeywordPP.LANG); // @fr
		if (index != -1) {
			lang = value.substring(index + 1);
            lang = computeGuiString(parser,lang);
			value = value.substring(0, index);
			literal = true;
		}
		return createAtom(value, datatype, lang, literal);
	}

	public static Atom createAtom(String value, String datatype, String lang, boolean literal) {
		value = Triple.process(value); // remove count::
		if (Triple.isVariable(value)){ 
			return createVariable(value); //, datatype, lang);
		} else {
			return createConstant2(value, datatype, lang, literal);
		}
	}


	public static Variable createVariable(String s){ //, String datatype, String lang) {
        return Variable.create(s);
	}
	
	public static Variable createVariable(String s, ASTQuery aq) {
		Variable var = createVariable(s); //, datatype, lang);
		// if we are in "describe *", add this variable to the list of variable to describe
        // notice: if the variable is already in the list, it won't add it again
        if (aq.isDescribeAll()) {
            aq.setDescribe(s);
        }
        return var;
	}

	// URI
	public  Constant createConstant(String s) {
		Constant cst = Constant.create(s);
		cst.setLongName(getNSM().toNamespaceB(s));
		return cst;
	}
	
	/*
	 * Draft property regexp
	 */
	public  Constant createProperty(Expression exp) {
		if (exp.isConstant()){
			// no regexp, std property
			return (Constant) exp;
		}
		Constant cst =  createConstant(RootPropertyQN);
		cst.setExpression(exp);
		return cst;
	}
	
	/**
	 * Create a triple for SPARQL JJ Parser
	 * Process the case where the property is a regex
	 * generate a filter(match($path, regex))
	 * return {triple . filter}
	 */
	public  Exp createTriple(Expression subject, Atom predicate, Expression object){
		Triple t = Triple.create(subject, predicate, object);
		
		Expression exp = t.getProperty().getExpression();
		
		if (exp != null){
			// property path or xpath
			Variable var = t.getVariable();
			if (var == null){
				var = new Variable(ExpParser.SYSVAR + nbd++);
				var.setBlankNode(true);
				t.setVariable(var);
			}
			if (exp.getName().equals(Term.XPATH)){
				t.setRegex(exp);
				return t;
			}
			
			var.setPath(true);
			String mode = "";
			boolean isInverse = false;
			while (true){
				if (exp.isFunction()){
					if (exp.getName().equals(SINV)){
						exp = exp.getArg(0);
						isInverse = true;
						mode += "i";
					}
					else if (exp.getName().equals(SSHORT)){
						exp = exp.getArg(0);
						mode += "s";
					}
					else if (exp.getName().equals(SSHORTALL)){
						exp = exp.getArg(0);
						mode += "sa";
					}
					else if (exp.getName().equals(SDEPTH)){
						exp = exp.getArg(0);
						mode += "d";
					}
					else if (exp.getName().equals(SBREADTH)){
						exp = exp.getArg(0);
						mode += "b";
					}
					else break;
				}
				else break;
			}
			t.setRegex(exp);
			t.setMode(mode);
			
			if (isKgram()){
				if (isInverse){
					exp = Term.function(Term.SEINV, exp);
					t.setRegex(exp);
				}
				return t;
			}
			else 
			{
				Term fun = Term.function(MATCH, var, exp);
				if (mode != ""){
					fun.add(new Constant(mode, RDFS.xsdstring));
				}

				Triple ft = Triple.create(fun);
				BasicGraphPattern bg = BasicGraphPattern.create(t);
				bg.add(ft);
				return bg;
			}
		}
		else return t;
	}
	
	// regex only
	public  Expression createOperator(String ope, Expression exp) {
		Term fun = null;
		if (ope.equals(SINV)) {
			fun = Term.function(SINV, exp);
		} 
		else if (ope.equals(SBE)) {
			fun = Term.function(SBE, exp);
		} 
		else if (ope.equals(SMULT)){
			fun = star(exp);
		}
		else if (ope.equals(SPLUS)){
			if (isKgram()){
				// first exp is member of visited (SPARQL 1.1)
				// for checking loop
				fun = createOperator(1, Integer.MAX_VALUE, exp);
				fun.setPlus(true);
			}
			else {
				fun = sequence(exp, Term.function(Term.STAR, exp));
			}
		}
		else if (ope.equals(Keyword.SQ)){
			fun = Term.function(Term.OPT, exp);
		}
		else {
			fun = Term.function(ope, exp);
		}
		return fun;
	}
	
	/**
	 * Filter associated to a path regex
	 */
	public void setRegexTest(Expression exp, Expression test){
		regexExpr.put(exp, test);
		exp.setExpr(test);
	}
	
	public Collection<Expression> getRegexTest(){
		return regexExpr.values();
	}
	
	Term star(Expression exp){
		return Term.function(Term.STAR, exp);
	}
	
	Term sequence(Expression e1, Expression e2){
		return Term.create(SDIV, e1, e2);
	}
	
	Expression alter(Expression e1, Expression e2){
		return Term.create(SOR, e1, e2);

	}
	
	public  Expression createOperator(String s1, String s2, Expression exp) {
		int n1 = 0, n2 = Integer.MAX_VALUE;
		if (s1 != null) n1 = Integer.parseInt(s1);
		if (s2 != null) n2 = Integer.parseInt(s2);
		exp = star(exp);
		exp.setMin(n1);
		exp.setMax(n2);
		return createOperator(n1, n2, exp);
	}
	
	Term createOperator(int n1, int n2, Expression exp) {
		Term t = star(exp);
		t.setMin(n1);
		t.setMax(n2);
		return t;
	}

	// Literal
	public Constant createConstant(String s, String datatype, String lang) {
		if (datatype == null){ // || datatype.equals("")){
			datatype = datatype(lang);
		}
		else if (! knownDatatype(datatype)){
			datatype = getNSM().toNamespaceB(datatype);
		}
		s = clean(s);
		return  Constant.create(s, datatype, lang);
	}
	
	String datatype(String lang){
		return DatatypeMap.datatype(lang);
	}
	
	private  boolean knownDatatype(String datatype) {
		if (datatype.startsWith(RDFS.XSD) ||
				datatype.startsWith(RDFS.XSDPrefix) ||
				datatype.startsWith(RDFS.RDF) ||
				datatype.startsWith(RDFS.RDFPrefix) )
		return true;
		else return false;
	}

	/**
	 * old parser, 
	 * @deprecated
	 */
	public static Constant createConstant2(String s, 
			String datatype, String lang, boolean literal) {
		Constant cst;

		// old parser (deprecated)
		if (isString(s)) { literal = true; }
		if (datatype == null || datatype.equals("")){
			if (isNumber(s)){
				if (isInteger(s))
					datatype = RDFS.qxsdInteger;
				else datatype = RDFS.qxsdDouble;
				literal=true;
			}
			else if (isString(s)){
				literal = true;
				if (lang == null || lang.equals("")) 
					datatype = RDFS.qxsdString;
				else  datatype = RDFS.qrdfsLiteral;
			}
			else  if (! literal) { // no datatype and not a string : a resource
				datatype = RDFS.qrdfsResource;
			}
			// does not happen with std syntax:

			else {
				datatype = RDFS.qrdfsLiteral;
			}
		}

		if (literal){
			s = clean(s);
			cst = Constant.create(s, datatype, lang);
		}
		else cst = Constant.create(s);
		return cst;
	}
	
	

	public static Triple createTriple () {
		return new Triple(getTripleId());
	}

    public String computeGuiString(Parser parser, String s) {
    	if (parser != null && parser.ispGet(s)){ // it is @get:lang computed from GUI
            s=parser.getValue(parser.pget(s));
            if (parser.isEmpty(s)) {
              s="";
            }
        }
        return s;
    }

    /** used for collections */
    public  Exp generateFirst(Expression expression1, Expression expression2) {
        Atom atom = createConstant(RDFS.qrdfFirst);
        Triple triple = Triple.create(expression1, atom, expression2);
        return triple; 
    }

    public  Exp generateRest(Expression expression1, Expression expression2) {
        Atom atom = createConstant(RDFS.qrdfRest);
        Triple triple = Triple.create(expression1, atom, expression2);
        return triple; 
    }

	static boolean isString(String value) {
		return value.startsWith("\"") || value.startsWith("'");
	}

	public static boolean isNumber(String str) {
		try {
			new Float(str);
			return true;
		}
		catch (NumberFormatException e) {
			return false;
		}
		catch (Exception e) {
			logger.fatal(e.getMessage());
			return false;
		}
	}

	public static boolean isInteger(String str) {
		try {
			new Integer(str);
			return true;
		}
		catch (NumberFormatException e) {
			return false;
		}
		catch (Exception e) {
			logger.fatal(e.getMessage());
			return false;
		}
	}

	public void setMerge(boolean b){
        merge = true;
	}

    public boolean isMerge() {
        return merge;
    }

	public void setList(boolean b){
		this.setJoin(!b);
	}
	
	public void setCorrect(boolean b){
		isCorrect = b;
	}
	
	public boolean isCorrect(){
		return isCorrect;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean b) {
		debug = b;
	}

    public boolean isNosort() {
        return nosort;
    }

    public void setNosort(boolean b) {
        nosort = b;
    }

	public boolean isSorted() {
		return sorted;
	}

	public void setSorted(boolean b) {
		sorted = b;
	}

	public int getOffset() {
		return Offset;
	}

	public void setOffset(int offset) {
		Offset = offset;
	}

	public int getResultForm() {
		return resultForm;
	}

	public void setResultForm(int resultForm) {
		this.resultForm = resultForm;
	}
	
	public boolean isAdd(){
		return isAdd;
	}
	
	public void setAdd(boolean b){
		isAdd = b;
	}

    public void setWhere(boolean b) {
        where = b;
    }

    public boolean isWhere() {
        return where;
    }

    public long getNbBNode() {
		nbBNode++;
		return nbBNode;
	}

    public Variable newBlankNode() {
		Variable var = createVariable( ExpParser.BNVAR + getNbBNode());
		var.setBlankNode(true);
		return var;
	}
    
    public Variable newBlankNode(Exp exp, String label) {
    	if (blank == null) blank = new Hashtable<String, Exp>();
    	Exp ee = blank.get(label);
    	if (ee == null){
    		blank.put(label, exp);
    	}
    	else if (ee != exp){
    		setCorrect(false);
    		logger.error("Blank Node used in different patterns: " + label);
    	}

    	Variable var;
    	if (exp instanceof BasicGraphPattern) {
    		BasicGraphPattern pat = (BasicGraphPattern)exp;
    		var = pat.getBNode(label);
    		if (var==null) {
    			// create a new blank node and put it in the table
    			var = newBlankNode();
    			pat.addBNodes(label, var);
    		}
    	} 
    	else {
    		logger.error("ERROR - stack not instance of BasicGraphPattern: "+exp.getClass());
    		var = newBlankNode();
    	}
    	return var;
    }

    /**
     * use case:
     * select sql() as (?x, ?y)
     * @param var1
     * @param var2
     */
    public void addVariable(Variable var1, Variable var2){
    	var1.addVariable(var2);
    }
	
	public Array newArray(ExpressionList list){
		Array array =  new Array(list);
		return array;
	}

    public void setDescribe(String var) {
        if (!describe.contains(var))
            describe.add(var);
    }
    
    public void setDescribe(Atom var){
    	setResultForm(QT_DESCRIBE);   
    	setDescribe(var.getName());
    }
    
    public Vector<String> getDescribe() {
        return describe;
    }

    public void setDescribeAll(boolean b) {
        describeAll = b;
    }

    boolean isDescribeAll() {
        return describeAll;
    }
    
    public boolean isEdgeForType(){
    	return isRelationForType;
    }
    
    public void setEdgeForType(boolean b){
    	isRelationForType = b;
    }
    
    public boolean isKgram(){
    	return isKgram;
    }
    
    public void setKgram(boolean b){
    	isKgram = b;
    }
    
    public boolean isBind(){
    	return isBind;
    }
    
    public void setBind(boolean b){
    	isBind = b;
    }
    
    public boolean isSPARQL1(){
    	return isSPARQL1;
    }
    
    public void setSPARQL1(boolean b){
    	isSPARQL1 = b;
    }   

    public int getVariableId() {
        return nbd++;
    }

    public void complete(Parser parser) {
        setQueryPrettyPrint(getSparqlPrettyPrint());
     
    	// if we are in the first parser, getBodyExp = null
        if (isConstruct()  && getBody()!=null) {
        	// construct {} group by ?x => no merge
        	if (! isGroup() && ! isConnex()) setMerge(true);
        	setDisplayBNID(true); // pp rdf need bnid (cf sparql w3c test base)
            compileConstruct(parser);
        } 
        else if (isAsk()) {
        	compileAsk();
        } 
        else if (isDescribe()) {
        	compileDescribe(parser);
        } 
    }
    
    
    private void compileConstruct(Parser parser) {
        if (getConstruct() != null){
        	setConst(getConstruct());
            Exp exp = getConstruct().complete(parser);
            Env env = new Env(false);
            // assign graph ?src variable to inner triples
    		exp.setSource(parser,  env, null, false);
            exp = exp.distrib();
            // set the compiled exp as construct:
            setConstruct(exp);
        }
    }

    private void compileDescribe(Parser parser) {
    	String root = ExpParser.SYSVAR;
    	if (isKgram()){
    		// it must not be a systemvariable()
    		// for CoreseGraph.construct()
    		root = ExpParser.KGRAMVAR;
    	}
		Vector<String> describe = getDescribe();
		Exp bodyExpLocal = getBody();
		
		boolean describeAllTemp = isDescribeAll();
        setDescribeAll(false);
		
        BasicGraphPattern body = BasicGraphPattern.create();
        
		for (String sd : describe) {
			
			Expression expression;
			if (Triple.isVariable(sd)){ 
				// Var
				expression = createVariable(sd);				
			} else {
				// IRIref
				expression = createConstant(sd);
			}
			
			setMerge(true);

			//// create variables
			int nbd = getVariableId();
			Variable prop1 = createVariable(root + "p_" + nbd);
			Variable val1  = createVariable(root + "v_" + nbd);
			
			nbd = getVariableId();
			Variable prop2 = createVariable(root + "p_" + nbd);
			Variable val2  = createVariable(root + "v_" + nbd);
		
			//// create triple sd ?p0 ?v0
			Triple triple = Triple.create(expression, prop1, val1);
			Exp e1 = triple; //.parse(parser, expression, v2, v3);
			BasicGraphPattern bgp1 = BasicGraphPattern.create();
			bgp1.add(e1);
			body.add(e1);
			
			//// create triple ?v0 ?p0 sd
			Triple triple2 = Triple.create(val2, prop2, expression);
			Exp e2 = triple2; //.parse(parser, v3, v2, expression);
			BasicGraphPattern bgp2 = BasicGraphPattern.create();
			bgp2.add(e2);
			body.add(e2);

			//// create the union of both
			setUnion(true);
			Or union = new Or();
			union.add(bgp1);
			union.add(bgp2);
			
			// make the union optional
			Option opt =  Option.create(union);
			
			bodyExpLocal.add(opt);
			
			if (expression.isVariable()){ 
				setSelect((Variable)expression);
			}
		}
		setDescribeAll(describeAllTemp);
		setBody(bodyExpLocal);
		setRDF(true);
		
		if (isKgram()){
			setConst(body);
			setConstruct(body);
		}
		//System.out.println("ASTQuery.java - "+getSelect()+"\n"+getBodyExp());
    }
    
    private void compileAsk() {
    	setMaxResult(1);
    }

    /**
     * Return a pretty printed version of the source of the SPARQL query (2nd parser)
     * @return
     */
    public String getSparqlPrettyPrint() {
    	// we calculate the pretty print only one time
    	if (this.queryPrettyPrint.equals("") || this.queryPrettyPrint.length() == 0) {
        	String str = "";
	        // Prefix
	        str += getSparqlPrefix();
	        // Header
	        str += getSparqlHeader();
	        // Body
	        str += getSparqlBody();
	        // SolutionModifier
	        if (! isAsk()) 
	        	str += getSparqlSolutionModifier();
	        //else str += PrettyPrintCst.CLOSE_BRACKET;
	        return str;
        } else {
        	return this.queryPrettyPrint;
        }
    }

    public String getSparqlPrefix() {
        String pre = "";
        for (Exp e : getPrefixExp().getBody()) {
            Triple t = (Triple) e;
            String r = t.getSubject().getName();
            String p = t.getPredicate().getName();
            String v = t.getObject().getName();
            // if v starts with "<function://", we have add a ".", so we have to remove it now
            if (v.startsWith(KeywordPP.CORESE_PREFIX)) {
            	v = v.substring(0, v.length()-1) ;
            }
            //logger.debug("ASTQuery.java - element: - "+r+" "+p+" "+v);
//            if (p.equalsIgnoreCase(PrettyPrintCst.AS)) { // if we are in the "AS" case, we change it to "PREFIX" because it's more SPARQL compliant
//                pre += PrettyPrintCst.PREFIX + PrettyPrintCst.SPACE + r + ":" + 
//                PrettyPrintCst.SPACE + PrettyPrintCst.OPEN + v + PrettyPrintCst.CLOSE + 
//                PrettyPrintCst.SPACE_LN;
//            } else 
            if (r.equalsIgnoreCase(KeywordPP.PREFIX)) {
                pre += KeywordPP.PREFIX + KeywordPP.SPACE + p + ": " + 
                KeywordPP.OPEN + v + KeywordPP.CLOSE + KeywordPP.SPACE_LN;
            } else if (r.equalsIgnoreCase(KeywordPP.BASE)) {
                pre += KeywordPP.BASE + KeywordPP.SPACE + 
                KeywordPP.OPEN +  v + KeywordPP.CLOSE +  KeywordPP.SPACE_LN;
            }
        }
        return pre;
    }

    /**
     * Return the header part of the SPARQL-like Query (2nd parser)
     * @return
     */
    public String getSparqlHeader() {

        String head = "";
        Vector<String> from = getFrom();
        Vector<String> named = getNamed();
        Vector<String> describe = getDescribe();
        Vector<String> select = getSelect();

        // Select
        if (isSelect()) {
            head += KeywordPP.SELECT + KeywordPP.SPACE;
            // Debug
            if (isDebug())
                head += KeywordPP.DEBUG + KeywordPP.SPACE;
            // Nosort
            if (isNosort())
                head += KeywordPP.NOSORT + KeywordPP.SPACE;
            // One
            if (isOne())
                head += KeywordPP.ONE + KeywordPP.SPACE;
            // More
            if (isMore())
                head += KeywordPP.MORE + KeywordPP.SPACE;
            if (isMerge())
                head += KeywordPP.MERGE + KeywordPP.SPACE;
            // sort : if we have sort count or sort distance, we write it there for the moment, 
            // because we don't know on which variable is the count or the distance
//            if (sort != null) {
//                for (Expression o : sort){
//                	String s = "";
//                	if (o instanceof Term) s = ((Term)o).toSparql();
//                	else s = o.toString();
//                	if (s.equalsIgnoreCase(PrettyPrintCst.COUNT) || s.equalsIgnoreCase(PrettyPrintCst.DISTANCE)) {
//                		head += PrettyPrintCst.SORT + " " + s + " ";                	  
//                	}
//                }
//            }
                // Distinct
            if (isDistinct())
                head += KeywordPP.DISTINCT + KeywordPP.SPACE;
            // Sorted
            if (isDistinct() && !isStrictDistinct())
                head += KeywordPP.SORTED + KeywordPP.SPACE;
            // Variables
            if (select != null && select.size()>0){
              Enumeration<String> en=select.elements();
              while (en.hasMoreElements()){
				  String s = en.nextElement();
            	  if (getSelectFunctions().get(s) != null) {
            		  s = getSelectFunctions().get(s).toSparql() + KeywordPP.SPACE + "as" + KeywordPP.SPACE + s;
            	  }
            	  head += s + KeywordPP.SPACE;
            	  //head += en.nextElement() + PrettyPrintCst.SPACE;     
              }
            } else if (isSelectAll()) {
                head += KeywordPP.STAR + KeywordPP.SPACE;
            }
              // Threshold
            if (getThreshold() != getDefaultThreshold())
                head += KeywordPP.THRESHOLD + KeywordPP.SPACE + (int)getThreshold() + KeywordPP.SPACE;
        } else if (isAsk()) {
            // Ask
            head += KeywordPP.ASK + KeywordPP.SPACE;
        } else if (isConstruct()) {
            // Construct
            head += KeywordPP.CONSTRUCT + 
            KeywordPP.SPACE + //PrettyPrintCst.OPEN_BRACKET + PrettyPrintCst.SPACE + 
            getConstruct().toSparql(); // + PrettyPrintCst.CLOSE_BRACKET;
        } else if (isDescribe()) {
            // Describe
            head += KeywordPP.DESCRIBE + KeywordPP.SPACE;
            if (isDescribeAll()) {
                head += KeywordPP.STAR + KeywordPP.SPACE;
            } else if (describe != null && describe.size()>0) {
                for (int i=0;i<describe.size();i++) {
                    String v = describe.get(i);
                    // if we have an IRI, we have to put "<" and ">" back
                    if (!Triple.isVariable(v))
                        v = KeywordPP.OPEN + v + KeywordPP.CLOSE;
                    head += v + KeywordPP.SPACE;
                }
            }
        } else if (isDelete()) {
        	head += KeywordPP.DELETE + KeywordPP.SPACE + KeywordPP.STAR + KeywordPP.SPACE;
        }
        // DataSet
        if (! isConstruct())    // because it's already done in the construct case
            head += KeywordPP.SPACE_LN;
        // From
        for (int i=0;i<from.size();i++) {
            head += KeywordPP.FROM + KeywordPP.SPACE + KeywordPP.OPEN + from.get(i) + KeywordPP.CLOSE + KeywordPP.SPACE_LN;
        }
        // From Named
        for (int i=0;i<named.size();i++) {
            head += KeywordPP.FROM + KeywordPP.SPACE + KeywordPP.NAMED + KeywordPP.SPACE + KeywordPP.OPEN + named.get(i) + KeywordPP.CLOSE + KeywordPP.SPACE_LN;
        }
        // Where
        if (! (isDescribe() && ! isWhere()))
            head += KeywordPP.WHERE + KeywordPP.SPACE_LN ; //+ 
            //PrettyPrintCst.OPEN_BRACKET + PrettyPrintCst.SPACE;  // warning: for describe, not always WHERE
        
        return head;
    }

    public String getSparqlBody() {
        String body = "";
        //if ((getResultForm() != QT_DESCRIBE || isWhere()) && getBodyExp()!=null)
        if (! isDescribe() || getBody()!=null)
            body += getBody().toSparql();
        return body;
    }

    /**
     * return the solution modifiers : order by, limit, offset
     * @param parser
     * @return
     */
    public String getSparqlSolutionModifier() {
        String sm = "";
        Vector<Expression> sort = getSort();
        Vector<Boolean> reverse = getReverseTable();
 
        if (group != null) {
            Enumeration<String> en=group.elements();
            while (en.hasMoreElements()){
              sm += KeywordPP.GROUPBY + KeywordPP.SPACE + en.nextElement() + KeywordPP.SPACE;
            }
        }
        
        if (sort.size() > 0 ) {
            sm += KeywordPP.ORDERBY + KeywordPP.SPACE;
            for (int i=0;i<sort.size();i++) {
                Object o = sort.get(i);
                String s = "";
                if (o instanceof Term) s = ((Term)o).toSparql();
                else s = o.toString();
                if (!s.equalsIgnoreCase(KeywordPP.COUNT) && 
                		!s.equalsIgnoreCase(KeywordPP.DISTANCE)) {
	                boolean breverse = ((Boolean)reverse.get(i)).booleanValue();
	                if (breverse) {
	                    sm += KeywordPP.DESC;
	                        sm += "(";
	                }
	                sm += s + KeywordPP.SPACE;
	                if (breverse) 
	                    sm += ")" + KeywordPP.SPACE;
                }
            }
        }
        if (getOffset() > 0)
            sm += KeywordPP.OFFSET + KeywordPP.SPACE + (int)getOffset() + KeywordPP.SPACE;
        if (getMaxProjection() != getDefaultMaxProjection())
            sm += KeywordPP.PROJECTION + KeywordPP.SPACE + (int)getMaxProjection() + KeywordPP.SPACE;
        if (getMaxResult() != getDefaultMaxResult())
            sm += KeywordPP.LIMIT + KeywordPP.SPACE + (int)getMaxResult() + KeywordPP.SPACE;
 
        // DISPLAY rdf/table/flat/asquery/integer/xml/blank
        if (isRDF())
            sm += KeywordPP.DISPLAY + KeywordPP.SPACE + KeywordPP.DRDF + KeywordPP.SPACE;
        else if (isFlat())
            sm += KeywordPP.DISPLAY + KeywordPP.SPACE + KeywordPP.FLAT + KeywordPP.SPACE;
        else if (isPQuery())
            sm += KeywordPP.DISPLAY + KeywordPP.SPACE + KeywordPP.ASQUERY + KeywordPP.SPACE;
        else if (getMaxDisplay() != 1000)   // attention chiffre en dur
            sm += KeywordPP.DISPLAY + KeywordPP.SPACE + (int)getMaxDisplay() + KeywordPP.SPACE;
        else if (isDisplayBNID())
            sm += KeywordPP.DISPLAY + KeywordPP.SPACE + KeywordPP.BLANK + KeywordPP.SPACE;
       
        if (!sm.equals(""))
            sm += KeywordPP.SPACE_LN;
        return sm;
    }

	public void setConstruct(Exp constructExp) {
		this.setResultForm(QT_CONSTRUCT);
        this.constructExp = constructExp;
    }
	
	public void duplicateConstruct(Exp exp){
		boolean check = checkConstruct(exp);
		if (check){
			setConstruct(exp);
		}
		else {
			setConstruct(null);
		}
	}
	
	/**
	 * construct where {exp}
	 * construct = duplicate(exp)
	 * and exp should have no filter and no graph pattern
	 */
	boolean checkConstruct(Exp body){
		for (Exp exp : body.getBody()){
			if (! exp.isTriple() || exp.isExp()) return false;
		}
		return true;
	}

    public Exp getConstruct() {
        return constructExp;
    }
    
    public void setConst(Exp exp) {
        this.construct = exp;
    }

    public Exp getConst() {
        return construct;
    }
    
    public void setDelete(Exp exp) {
        this.delete = exp;
    }

    public Exp getDelete() {
        return delete;
    }

    public static int getTripleId(){
      return nbt++;
    }

    public Exp getBody() {
        return bodyExp;
    }
    
    public Exp getHead() {
        return constructExp;
    }

    public void setBody(Exp bodyExp) {
        this.bodyExp = bodyExp;
    }
    
    public void setPragma(String name, Exp exp){
    	if (pragma == null){
    		pragma = new Hashtable<String, Exp>();
    	}
    	if (name == null) name = RDFS.COSPRAGMA;
    	else name = getNSM().toNamespace(name);
    	if (exp == null) pragma.remove(name);
    	else pragma.put(name, exp);
    	//pragma(exp);
    }
    
    public void pragma(){
    	if (pragma!=null){
    		for (String name : pragma.keySet()){
    			pragma(pragma.get(name));
    		}
    	}
    }
    
    /**
     * Store access pragma tables
     */
    void pragma(Exp pragma){
    	if (pragma == null) return;

    	NSManager nsm = getNSM();
    	for (Exp exp : pragma.getBody()){
    		if (exp.isRelation()){
    			Triple t = (Triple) exp;
    			//sem:root sem:type wiki:Page ;
    			String subject =  nsm.toNamespace(t.getSubject().getName());
    			String property = nsm.toNamespace(t.getProperty().getName());
    			String object =   nsm.toNamespace(t.getObject().getName());
    			//logger.debug("** PRAGMA AST: " + t);
    			if (property.equals(RDFS.ACCEPT)){
    				property =  RDFS.FROM;
    			}
    			if (subject.equals(RDFS.RDFRESOURCE)){
    				def(RDFS.RDFSUBJECT, property, object);
    				def(RDFS.RDFOBJECT, property, object);
    			}
    			else {
    				def(subject, property, object);
    			}
    		}
    	}
    	if (ttable!=null)
    		cleanPragma();

    }

    
   	// replace xxx from _:b . _:b type yyy by xxx type yyy
	// xxx from [type yyy] by xxx type yyy
    void cleanPragma(){
    	if (getSubject(RDFS.FROM)!=null)
    	for (String subject : getSubject(RDFS.FROM)){
    		SVector from = getValue(RDFS.FROM, subject);
    		if (from!=null){
    			for (int i=0; i<from.size(); i++){
    				// from.get(i) = _:b
    				SVector types = getValue(RDFS.RDFTYPE, from.get(i));
    				if (types != null){
    					// there exist _:b type yyy
    					// remove : xxx cos:from _:b
    					from.remove(i);
    					for (String type : types){
    						// add xxx rdf:type yyy
    						def(subject, RDFS.RDFTYPE, type);
    					}
    				}
    			}
    		}
    	}
    }
    
    
    public boolean hasNamespace(){
    	return ttable != null;
    }
    
    public boolean hasProperty(String name){
    	return ttable.get(name) != null;
    }
    
    // return from/deny
    public Iterable<String> getProperty(){
    	return ttable.keySet();
    }
    
    // prop : from/deny
    // return subjects of prop
    public Iterable<String> getSubject(String prop){
    	NSTable table =  ttable.get(prop);
    	if (table == null) return null;
    	return table.keySet();
    }
    
    public Iterable<String> getValues(String prop, String name){
    	NSTable table =  ttable.get(prop);
    	if (table == null) return null;
    	return table.get(name);
    }
    
    public SVector getValue(String prop, String name){
    	NSTable table =  ttable.get(prop);
    	if (table == null) return null;
    	return table.get(name);
    }
    
    void def(String name, String prop, String ns){
    	if (ttable == null) ttable = new TTable();
    	NSTable table = ttable.get(prop);
    	if (table == null){
    		table = new NSTable();
    		ttable.put(prop, table);
    	}
    	access(table, name, ns);
    }

    void access(NSTable table, String name, String ns){
    	SVector vec = table.get(name);
    	if (vec == null){
    		vec = new SVector();
    		table.put(name, vec);
    	}
    	if (! vec.contains(ns))
    		vec.add(ns);
    }
    
    // from -> NSTable
    class TTable extends Hashtable <String, NSTable>{}
    
    // subject from {ns}
    class NSTable extends Hashtable<String, SVector>{}
    
    class SVector extends Vector<String> {}
    

    
    public void setPragma(Exp exp){
    	 setPragma(RDFS.COSPRAGMA, exp);
    }
    
    public Exp getPragma(String name){
    	if (pragma == null) return null;
    	return pragma.get(name);
    }
    
    public Exp getPragma(){
    	return getPragma(RDFS.COSPRAGMA);
    }
    
    public boolean hasPragma(String subject, String property, String object){
    	if (getPragma() == null) return false;
    	for (Exp exp : getPragma().getBody()){
			if (exp.isRelation()){
				Triple t = (Triple) exp;
				if (t.getSubject().getName().equals(subject) && 
					t.getProperty().getName().equals(property) &&
					t.getObject().getName().equals(object)){
					return true;
				}
			}
		}
    	return false;
    }
    
    public void addPragma(Triple t){
    	Exp pragma = getPragma();
    	if (pragma == null){
    		pragma = BasicGraphPattern.create();
    		setPragma(pragma);
    	}
    	pragma.add(t);
    }
	

    public Exp getPrefixExp() {
        return prefixExp;
    }

    /**
     * Note: only for pretty print, do not really add the prefix in NSManager 
     * @param t
     */
    public void addPrefixExp(Triple t) {
        prefixExp.add(t);
    }
    
   
    public void defNamespace(String prefix, String ns){
    	if (prefix.endsWith(":")){
    		prefix = prefix.substring(0, prefix.length() - 1); // remove :
    	}
    	getNSM().defNamespace(ns, prefix);
    	Triple triple = Triple.createNS(
    			 Constant.create(KeywordPP.PREFIX),  Constant.create(prefix), 
    			 Constant.create(ns));
    	addPrefixExp(triple);
    }
    
    public void defBase(String ns){
    	getNSM().setBase(ns);
       	Triple triple = Triple.createNS(
    			 Constant.create(KeywordPP.BASE),  Constant.create(""), 
    			 Constant.create(ns));
    	addPrefixExp(triple);
    }
    
    public String defURI(String s){
    	if (s.startsWith(KeywordPP.CORESE_PREFIX)) {
      		s = s + ".";
      	}
    	return s;
    }

    public void setNamed(String uri) {
    	if (!named.contains(uri))
            named.add(uri);
    }
    
    public void setNamed(Atom uri) {
    	setNamed(uri.getName());
    }

    public void setFrom(String uri) {
        if (!from.contains(uri))
            from.add(uri);
    }
    
    public void setFrom(Atom uri) {
       setFrom(uri.getName());
    }

    public void setCount(String var) {
    }

	public void setSort(String var, boolean breverse) {
    }

    public void setSort(Expression sortExpression) {
    	setSort(sortExpression, false);
    }
    
    public void setHaving(Exp exp){
    	if (exp.getBody().size() == 0) return;
    	Exp body = exp.getBody().get(0);
    	if (! body.isTriple()) return;
    	having = body.getTriple().getExp();
    }
    
    public Expression getHaving(){
    	return having;
    }
    
    public void setSort(Expression sortExpression, boolean breverse) {     
    	sort.add(sortExpression);
    	reverseTable.add(new Boolean(breverse));
    	//sortDistanceTable.add(new Boolean(false));   
    }

//    public void setGroup(Variable var) {
//    	 setGroup(var.getName());
//    }
    
	public List<Expression> getGroupBy() {
		return lGroup;
	}
	
	public boolean isGroupBy(String name){
		for (Expression exp : lGroup){
			if (exp.isVariable() && exp.getName().equals(name)){
				return true;
			}
		}
		return false;
	}
	
	public void setGroup(Expression exp) {
    	if (exp.isVariable()){
   	 		setGroup(exp.getName());
    	}
    	lGroup.add(exp);
   }
	
	
	public void setGroup(Expression exp, Variable var) {
		if (var != null){
			// use case: group by (exp as var)
			// generate:
			// select (exp as var)
			// group by var
			setSelect(var);
			setSelect(var, exp);
			setGroup(var);
		}
		else {
			setGroup(exp);
		}
	}

    
    public void setGroup(String var) {
        if (!group.contains(var))
            group.add(var);
    }
    
    public void setVariableBindings(ArrayList<Variable> list){
    	varBindings = list;
    }
    
    public List<Variable> getVariableBindings(){
    	return varBindings;
    }
    
    public void setValueBindings(ArrayList<Constant> list){
    	if (valueBindings == null){
    		valueBindings = new ArrayList<ArrayList<Constant>>();
    	}
    	valueBindings.add(list);
   }
    
    public List<ArrayList<Constant>> getValueBindings(){
    	return valueBindings;
    }
    
    
   public Expression bind(List<Variable> lVar, List<Constant> lVal){
		Expression exp = null;
		int i = 0;
		for (Variable var : lVar){
			if (i>=lVal.size()){
				
			}
			else if (lVal.get(i)==null){
				i++;
			}
			else {
				Term term = Term.create(Term.SEQ, var, lVal.get(i++));
				if (exp == null){
					exp = term;
				}
				else {
					exp = exp.and(term);
				}
			}
		}
		return exp;
	}
   
   // TODO: complete
   public Expression bind(){
	   if (valueBindings == null) return null;
	   Expression exp = null;
	   
	   for (List<Constant> lVal :  valueBindings){
		   if (varBindings.size()!=lVal.size()){
			   setCorrect(false);
		   }
		   else if (exp == null){
			   exp = bind(varBindings, lVal);
		   }
	   }
	   return exp;
   }
   
   public void defSelect(Variable var, Expression exp){
	   checkSelect(var);
	   if (exp == null){
		   setSelect(var);
	   }
	   else {
		   setSelect(var, exp);
	   }
   }
      
    public void setSelect(Variable var) {
    	if (! select.contains(var.getName())){
            select.add(var.getName());
            selectVar.add(var);
    	}
    }
    
    public boolean checkSelect(Variable var){
    	if (select.contains(var.getName())){
    		setCorrect(false);
    		return false;
    	}
    	return true;
    }
        
    public void setSelect(Variable var, Expression e) {
    	setSelect(var);
    	selectFunctions.put(var.getName(), e);
    	selectExp.put(e, e);
    	
    	if (var.getVariableList()!=null){
    		// use case:
    		// select sql() as (nn_0, nn_1)
    		// compiled as :
    		// select sql() as var   get(var, i) as nn_i
    		// now generate get() for sub variables
    		int n = 0;
    		for (Variable vv : var.getVariableList()){
    			if (isKgram()){
    				setSelect(vv);
    			}
    			else {
    				Term get = createGet(var, n++);
    				setSelect(vv, get);
    			}
    		}
    	}
    }
    

    public void setSelect() {
    }

    
    void setGetGui(boolean b){
        if (! hasGet){
          hasGet=true;
        }
        if (b) hasGetSuccess=true;
    }

    public boolean isSelected(Exp exp){
        if (hasGet){
          return hasGetSuccess;
        }
        else return true;
    }


    public Vector<Boolean> getReverseTable() {
        return reverseTable;
    }

    public String toString() {
        return getSparqlPrettyPrint();
    }

    private void setQueryPrettyPrint(String queryPrettyPrint) {
        this.queryPrettyPrint = queryPrettyPrint;
    }

    public void setDescribe(boolean describe) {
        //this.describe = describe;
    	if (describe) setResultForm(QT_DESCRIBE);
    }
    
    public boolean isDescribe() {
        //return describe;
    	return (getResultForm() == QT_DESCRIBE);
    }
    
    public boolean isAsk() {
    	return (getResultForm() == QT_ASK);
    }
    
    public boolean isConstruct() {
    	return (getResultForm() == QT_CONSTRUCT);
    }
    
    public boolean isSelect() {
    	return (getResultForm() == QT_SELECT);
    }
    
    public boolean isUpdate() {
    	return (getResultForm() == QT_UPDATE);
    }
    
	public void setDelete(boolean b) {
		if	(b){
			setResultForm(QT_DELETE);
			isDelete = b;
		}
	}
	
	public void setInsert(boolean b) {
		if	(b){	
			setResultForm(ASTQuery.QT_CONSTRUCT);
			setAdd(true);
		}
	}

    public boolean isDelete() {
    	return isDelete;
    }
    
    
    void bind(String var){
        if (! stack.contains(var)){
            stack.add(var);
        }
    }
    boolean isBound(String var){
        return stack.contains(var);
    }

    public Vector<String> getStack() {
    	return stack;
    }

    public void setStack(Vector<String> stack) {
        this.stack = stack;
    }
    void clear(){
        stack.clear();
    }
    boolean hasOptionVar(Expression exp){
        return  exp.isOptionVar(stack);
    }
    int getStackSize(){
        return stack.size();
    }

    public boolean isHasGet() {
        return hasGet;
    }

    public void setHasGet(boolean hasGet) {
        this.hasGet = hasGet;
    }

    public boolean isHasGetSuccess() {
        return hasGetSuccess;
    }

    public void setHasGetSuccess(boolean hasGetSuccess) {
        this.hasGetSuccess = hasGetSuccess;
    }

    public boolean isConstructCompiled() {
        return constructCompiled;
    }

    public void setConstructCompiled(boolean constructCompiled) {
        this.constructCompiled = constructCompiled;
    }

	public void setDefaultThreshold(float threshold) {
		DefaultThreshold = threshold;
		setThreshold(threshold);
	}

	public void setDefaultMaxProjection(int maxProjection) {
		DefaultMaxProjection = maxProjection;
		setMaxProjection(maxProjection);
	}
	
	public void setDefaultMaxLength(int maxLength) {
		DefaultMaxLength = maxLength;
	}
	
	public int getDefaultMaxLength() {
		return DefaultMaxLength;
	}

	public void setDefaultMaxResult(int maxResult) {
		DefaultMaxResult = maxResult;
		setMaxResult(maxResult);
	}

	public float getDefaultThreshold() {
		return DefaultThreshold;
	}

	public int getDefaultMaxProjection() {
		return DefaultMaxProjection;
	}

	public int getDefaultMaxResult() {
		return DefaultMaxResult;
	}

	public static String getRootPropertyQN() {
		return RootPropertyQN;
	}

	public static void setRootPropertyQN(String rootPropertyQN) {
		RootPropertyQN = rootPropertyQN;
	}

	public static String getRootPropertyURI() {
		return RootPropertyURI;
	}

	public static void setRootPropertyURI(String rootPropertyURI) {
		RootPropertyURI = rootPropertyURI;
	}

	public static String clean(String str) {
		if (str.length() <= 1) return str;
		if ((str.startsWith(SQ3)   && str.endsWith(SQ3)) ||
			(str.startsWith(SSQ3)  && str.endsWith(SSQ3))){
			str = str.substring(3,(str.length()-3));
			return recClean(str);
		}
		else
		if ((str.startsWith(SQ1)   && str.endsWith(SQ1)) ||
			(str.startsWith(SSQ)  && str.endsWith(SSQ))){
			str = str.substring(1,(str.length()-1));
			return recClean(str);
		}
		return str;
	}
	
	//	 replace \\ by \ inside the string
	// \" -> "
	// \' -> '
	static String recClean(String str){
		int index = str.indexOf(BS);
		if (index != -1){
			str = str.substring(0, index) + "\\" + 
				  str.substring(index+BS.length());
			return recClean(str);
		}
		index = str.indexOf(ESQ); //  \" -> "
		if (index != -1){
			str = str.substring(0, index) + "\"" + 
				  str.substring(index+ESQ.length());
			return recClean(str);
		}
		index = str.indexOf(ESSQ); //  \' -> "
		if (index != -1){
			str = str.substring(0, index) + "'" + 
				  str.substring(index+ESSQ.length());
			return recClean(str);
		}
		return str;
	} 
	
	public boolean isDefineExp(Expression exp){
		return selectExp.get(exp) != null;
	}
	
	public Expression getExpression(String name){
		return selectFunctions.get(name);
	}
	
	public Expression getExtExpression(String name){
		Expression sexp = getExpression(name);
		if (sexp == null) return null;
		// rewrite var as exp
		return sexp.process(this);
	}

	public Hashtable<String, Expression> getSelectFunctions() {
		return selectFunctions;
	}

	public void setSelectFunctions(Hashtable<String, Expression> selectFunctions) {
		this.selectFunctions = selectFunctions;
	}
	
	/**
	 * Expand qnames from triples
	 *
	 */
	public ASTQuery expand(){
		if (getBody() != null){
			getBody().expand(getNSM());
		}
		if (isConstruct()){
			getConstruct().expand(getNSM());
		}
		Parser.create().ncompile(this);
		return this;
	}
	
	
	/************************************************************************
	 * 
	 * 	Update
	 * 
	 * 
	 * **********************************************************************/
	 
	public void set(ASTUpdate u){
		astu = u;
		u.set(this);
	}
	
	public ASTUpdate getUpdate(){
		return astu;
	}
	
}
