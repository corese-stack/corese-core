package fr.inria.corese.core.next.query.kgram.filter;

import fr.inria.corese.core.next.query.kgram.api.core.*;
import fr.inria.corese.core.next.query.kgram.core.Exp;
import fr.inria.corese.core.next.query.kgram.core.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Filter Exp Compiler for Optimizations
 * If Filter leads to optimization, we tag the Filter Exp 
 * with e.g. a BIND(?x = ?y)
 * Query.bind() will handle later this tag BIND
 * (1)
 * x p t . y p z . filter(y = x)
 * ->
 * x p t . bind(y, x) . y p z
 * (2)
 * x p t filter(?x = <uri>)
 * ->
 * bind(x, <uri>) . x p t
 * (3)
 * x p t filter(?x = <uri> || ?x = <uri2>)
 * ->
 * bind(x, (<uri>, <uri2>)) . x p t
 * (4)
 * optional{} !bound(?x)
 * !bound() backjump before optional{}
 * if there is no optional, !bound() backjump to edge before where ?x is bound
 * 
 * @author Olivier Corby, Edelweiss, INRIA 2010
 *
 */
public class Compile implements ExprType {
	
	Query query;
	Matcher matcher;
	Checker checker;
	
	public Compile(Query q){
		query = q;
		matcher = new Matcher();
		checker = new Checker(query);
	}
	
	/**
	 * exp is a FILTER Exp
	 * when !bound(?x) : get Node ?x for optimizing backjump with optional
	 * when ?x = ?y : get list Node, for optimizing edge 
	 * TODO:
	 * assign filter to edge and put edge first in its component:
	 * x p t . y p z filter(y = x)
	 * we can bind y = x before enumerate y p z
	 */
	@SuppressWarnings("unused")
	public void process(Query q, Exp exp) {
		Filter ff = exp.getFilter();
		Expr ee = ff.getExp();
				
		switch (ee.oper()){
                    						
		case OR:
			or(exp);
			break;
			
		case EQ:
			eq(exp);
			break;
		//todo: IN, VALUES
                case IN:
                        in(exp);
                        break;
		default:
			if (matcher.isGL(ee.oper())){
				gl(exp);
			}
			
		}
                
	}
	

	
	/**
	 *  ?x = ?y
	 *  ?x = cst
	 */
	void eq(Exp exp){
		Filter ff = exp.getFilter();
		Expr ee = ff.getExp();

		// ?x = ?y
		FilterPattern pat = new FilterPattern(TERM, EQ, VARIABLE, VARIABLE);
		if (matcher.match(pat, ee)){
			// compute Node list corresponding to variables
			List<Node> lNode = query.getNodes(exp);
			if (lNode.size()==2){
				Exp bind = Exp.create(ExpType.Type.OPT_BIND);
				for (Node qNode : lNode){
					Exp var = Exp.create(ExpType.Type.NODE, qNode);
					bind.add(var);
				}
				exp.add(bind);
			}
		}
		// ?x = 'constant'
		else if (matcher.match(new FilterPattern(TERM, EQ, VARIABLE, CONSTANT), ee)){
			Exp bind = buildCst(exp, ExpType.Type.OPT_BIND);
			if (bind != null){
				exp.add(bind);		
			}
		}
	}
	

	
	Exp buildCst(Exp exp, ExpType.Type type){
		Filter ff = exp.getFilter();
		Expr ee = ff.getExp();
		Node node = query.getProperAndSubSelectNode(ff.getVariables().getFirst());
		if (node != null){
			// variable ?x
			Exp bind = Exp.create(type, Exp.create(ExpType.Type.NODE, node));
			List<Expr> list = new ArrayList<>();
			list.add(getConstants(ee).getFirst());
			bind.setObject(list);
			return bind;
		}
		return null;
	}
	
	Exp buildVar(Exp exp){
		List<Node> lNode = query.getNodes(exp);
		if (lNode.size()==2){
			Exp bind = Exp.create(ExpType.Type.TEST);
			for (Node node : lNode){
				bind.add(Exp.create(ExpType.Type.NODE, node));
			}
			return bind;
		}
		return null;
	}
	
	
	
	/**
	 * VAR < CST 
	 */
	void gl(Exp exp){
		Filter ff = exp.getFilter();
		Expr ee = ff.getExp();
		FilterPattern pat = new FilterPattern(TERM, GL, VARIABLE, CONSTANT);
		if (matcher.match(pat, ee)){
			Exp test = buildCst(exp, ExpType.Type.TEST);
			if (test!=null)
				exp.add(test);
		}
		else {
			pat = new FilterPattern(TERM, GL, VARIABLE, VARIABLE);
			if (matcher.match(pat, ee)){
				Exp test = buildVar(exp);
				if (test!=null)
					exp.add(test);
			}
		}
		
	}
	
	 /**  
	  * ?x = cst1 || ?x = cst2 
	  */
	void or(Exp exp){
		Filter ff = exp.getFilter();
		Expr ee = ff.getExp();
		FilterPattern pat = new FilterPattern(BOOLEAN, OR, new FilterPattern(TERM, EQ, VARIABLE, CONSTANT));
		pat.setRec();
		pat.setMatchConstant();
		if (matcher.match(pat, ee)) {
			Node node = query.getProperAndSubSelectNode(ff.getVariables().getFirst());
			if (node != null){
				List<Expr> list = getConstants(ee);
				Exp bind = Exp.create(ExpType.Type.OPT_BIND, Exp.create(ExpType.Type.NODE, node));
				bind.setObject(list);
				exp.add(bind);
			}
		}
		
	}

     /*
     * ?x IN (2 4)
     * only consider the case with all constants (for the moment)
     */
    void in(Exp exp) {
        Filter ff = exp.getFilter();
        Expr expr = ff.getExp();
        List<String> lvar = ff.getVariables();       
        Expr var = expr.getExp(0);
        if (! var.isVariable()){
            return;
        }
        List<Expr> values = expr.getExp(1).getExpList();
		List<Expr> list = new ArrayList<>();
        //all has to be constants
        for (Expr e : values) {
            if (e.type() != CONSTANT) {
                return;
            }
            list.add(e);
        }

        Node node = query.getProperAndSubSelectNode(lvar.getFirst());
        if (node != null) {
            Exp bind = Exp.create(ExpType.Type.OPT_BIND, Exp.create(ExpType.Type.NODE, node));
            bind.setObject(list);
            exp.add(bind);
        }
    }

	List<Expr> getConstants(Expr exp){
		List<Expr> list = new ArrayList<>();
		return getConstants(exp, list);
	}
	
	
	List<Expr> getConstants(Expr exp, List<Expr> list){
		switch (exp.type()){
		
		case CONSTANT:
			list.add(exp); break;
			
		case VARIABLE: break;
		
		default:
			for (Expr ee : exp.getExpList()){
				getConstants(ee, list);
			}
		}
		return list;
	}
	
	
	
	public boolean isLocal(Filter f){
		return f.isBound() || 
			f.getExp().oper()  == DEBUG;
	}
}
