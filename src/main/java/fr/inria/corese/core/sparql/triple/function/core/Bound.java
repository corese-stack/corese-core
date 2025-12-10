package fr.inria.corese.core.sparql.triple.function.core;

import fr.inria.corese.core.sparql.api.Computer;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.triple.function.term.Binding;
import fr.inria.corese.core.sparql.triple.function.term.TermEval;
import fr.inria.corese.core.kgram.api.query.Environment;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.kgram.api.query.Producer;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2017
 *
 */
public class Bound extends TermEval {  
    
    public Bound(){}

    public Bound(String name){
        super(name);
        setArity(1);
    }
    
    @Override
    public IDatatype eval(Computer eval, Binding b, Environment env, Producer p) throws EngineException {
        boolean save = b.isCoalesce();
        b.setCoalesce(true);
        IDatatype dt = getBasicArg(0).eval(eval, b, env, p);
        b.setCoalesce(save);
        if (dt == null) {
            return FALSE;
        }
        return TRUE;
    }

}
