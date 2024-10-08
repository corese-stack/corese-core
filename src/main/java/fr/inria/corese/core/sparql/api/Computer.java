
package fr.inria.corese.core.sparql.api;

import fr.inria.corese.core.kgram.api.query.Evaluator;

/**
 * Interface for Interpreter
 * @author corby
 */
public interface Computer extends ComputerProxy {
    
    Evaluator getEvaluator();
        
    //IDatatype exist(Expr exp, Environment env, Producer p) throws EngineException ;
        
    boolean isCompliant();
}
