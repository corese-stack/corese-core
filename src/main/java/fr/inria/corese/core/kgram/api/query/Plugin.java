package fr.inria.corese.core.kgram.api.query;

import fr.inria.corese.core.kgram.core.Exp;

/**
 * Draft KGRAM Plugin for EXTERN expression.
 * KGRAM exec this function for EXTERN expression.
 * 
 * @author Olivier Corby, Edelweiss, INRIA 2010
 *
 */
public interface Plugin {

	void exec(Exp exp, Environment env, int n);
	
}
