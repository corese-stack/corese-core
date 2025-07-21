package fr.inria.corese.core.sparql.api;

import fr.inria.corese.core.kgram.api.core.Expr;
import fr.inria.corese.core.kgram.api.query.Environment;
import fr.inria.corese.core.kgram.api.query.Producer;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.function.term.Binding;
import fr.inria.corese.core.sparql.triple.parser.Context;
import fr.inria.corese.core.sparql.triple.parser.NSManager;

/**
 * implemented by fr.inria.corese.core.query.PluginTransform
 * @author corby
 */
public interface ComputerProxy {
        
    TransformProcessor getTransformer(Binding b, Environment env, Producer p) 
            throws EngineException ;
    
    TransformProcessor getTransformer(Binding b, Environment env, Producer p, Expr exp, IDatatype uri, IDatatype gname) 
            throws EngineException;
    
    // implemented by fr.inria.corese.core.query.PluginImpl
    // plugin of Interpreter
    GraphProcessor getGraphProcessor();

    TransformVisitor getVisitor(Binding b, Environment env, Producer p);
    
    Context getContext(Binding b, Environment env, Producer p);
    
    NSManager getNSM(Binding b, Environment env, Producer p);

}
