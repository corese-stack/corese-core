package fr.inria.corese.core.sparql.triple.function.core;

import fr.inria.corese.core.sparql.api.Computer;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.triple.function.term.Binding;
import fr.inria.corese.core.sparql.triple.function.term.TermEval;
import fr.inria.corese.core.kgram.api.core.ExprType;
import fr.inria.corese.core.kgram.api.query.Environment;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.kgram.api.query.Producer;
import java.util.Map;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2017
 *
 */
public class BlankNode extends TermEval {  
    static final String name = "_:_bnode_";
    long count = 0;
    
    public BlankNode(){}
    
    public BlankNode(String name){
        super(name);
    }
    
    @Override
    public IDatatype eval(Computer eval, Binding b, Environment env, Producer p) throws EngineException {
        if (arity() == 1){
            return bnode(eval, b, env, p);
        }
        switch (oper()){
            case ExprType.PATHNODE: 
                return DatatypeMap.createPointer(name + count());
                //return DatatypeMap.createBlank(name + count());
            default: return DatatypeMap.createBlank();                   
        }
    }
    
    synchronized long count(){
        return count++;
    }
    
    IDatatype bnode(Computer eval, Binding b, Environment env, Producer p) throws EngineException {
        IDatatype dt = getBasicArg(0).eval(eval, b, env, p);
        if (dt == null) return null;
        Map<String, IDatatype> map = env.getMap();
        IDatatype bn =  map.get(dt.getLabel());
        if (bn == null) {
            //bn = DatatypeMap.createBlank();
            bn = DatatypeMap.createBlank(p.blankNode());
            map.put(dt.getLabel(), bn);
        }
        return bn;
    }
   
}
