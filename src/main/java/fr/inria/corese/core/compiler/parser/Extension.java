package fr.inria.corese.core.compiler.parser;

import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import fr.inria.corese.core.kgram.core.Query;

/**
 * SPARQL Extension in Pragma
 * 
 * @author Olivier Corby, Edelweiss, INRIA 2011
 *
 */
public class Extension extends Pragma {
	
	Extension(Query q, ASTQuery a){
		super(q, a);
	}
	
	public static Extension create(Query q){
		return new Extension(q,  q.getAST());
	}
	
	public void parse(fr.inria.corese.core.sparql.triple.parser.Exp exp){
		if (exp.isQuery()){
			
		}
		else if (exp.isBGP()){
			for (fr.inria.corese.core.sparql.triple.parser.Exp ee : exp.getBody()){
				parse(ee);
			}
		}
	}

}
