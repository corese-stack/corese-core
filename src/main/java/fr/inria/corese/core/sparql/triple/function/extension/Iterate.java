package fr.inria.corese.core.sparql.triple.function.extension;

import fr.inria.corese.core.sparql.api.Computer;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
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
public class Iterate extends TermEval {  
    
    public Iterate(){}
    
    public Iterate(String name){
        super(name);
    }
    
    @Override
    public IDatatype eval(Computer eval, Binding b, Environment env, Producer p) throws EngineException {
        IDatatype[] args = evalArguments(eval, b, env, p, 0);
        
        if (args.length == 0) {
            return DatatypeMap.newIterate(0, Integer.MAX_VALUE-1);
        }
        
        int start = 0;
        int end = 1;
        
        if (args.length > 1){
            start = args[0].intValue();
            end =   args[1].intValue();
        }
        else {
            end =   args[0].intValue();
        }
        
        int step = 1;
        
        if (end < start){
            step = -1;
        }
        
        if (args.length == 3){
            step = args[2].intValue();
        }
                      
        IDatatype dt = DatatypeMap.newIterate(start, end, step);
        return dt;
    }
        
    
    
   
}
