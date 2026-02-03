package fr.inria.corese.core.next.kgram.filter;

import fr.inria.corese.core.next.kgram.api.core.Expr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 * Manage bindings of Matcher
 * Can bind and unbind
 * 
 * @author Olivier Corby, Edelweiss, INRIA 2010
 *
 */
public class MatchBind {
	private static final Logger logger = LoggerFactory.getLogger(MatchBind.class);

	Hashtable<Expr, Expr> table;
	List<Expr> stack;

	MatchBind (){
		table = new Hashtable<>();
		stack = new ArrayList<>();
	}
	
	static MatchBind create(){
		return new MatchBind();
	}
	
	boolean hasValue(Expr exp){
		return table.containsKey(exp);
	}
	
	Expr getValue(Expr exp){
		return table.get(exp);
	}
        	
	void setValue(Expr qe, Expr te){
		if (qe instanceof Pattern){
			table.put(qe, te);
			stack.add(qe);
		}
	}
	
	void reset(Expr exp){
		table.remove(exp);
		stack.remove(exp);
	}
	
	int size(){
		return stack.size();
	}
	
	/**
	 * Remove last bindings until stack.size() = size 
	 */
	void clean(int size){
		while (stack.size()>size){
			reset(stack.getLast());
		}
		if (table.size() != stack.size()){
			logger.error("Match Bind: Stack and Table have different sizes");
		}
	}

}
