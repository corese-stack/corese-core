package fr.inria.corese.core.sparql.triple.function.term;

import fr.inria.corese.core.sparql.api.Computer;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.triple.parser.Expression;
import fr.inria.corese.core.sparql.triple.function.core.BinaryFunction;
import fr.inria.corese.core.kgram.api.query.Environment;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.kgram.api.query.Producer;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2017
 *
 */
public class GE extends BinaryFunction {
        
    public GE(){
    }
   
    public GE(String name){
        super(name);
    }
    
    public GE(String name, Expression e1, Expression e2){
        super(name, e1, e2);
    }
         
    @Override
    public IDatatype eval(Computer eval, Binding b, Environment env, Producer p) throws EngineException {
        IDatatype dt1 = getExp1().eval(eval, b, env, p);
        IDatatype dt2 = getExp2().eval(eval, b, env, p);
        if (dt1 == null || dt2 == null) {
            return null;
        }
        return overload(eval, b, env, p, dt1, dt2, dt1.ge(dt2));
    }

}
