package fr.inria.corese.core.sparql.triple.function.template;

import static fr.inria.corese.core.kgram.api.core.ExprType.NUMBER;
import static fr.inria.corese.core.kgram.api.core.ExprType.STL_INDEX;
import static fr.inria.corese.core.kgram.api.core.ExprType.STL_NUMBER;
import fr.inria.corese.core.sparql.api.Computer;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.triple.function.term.Binding;
import fr.inria.corese.core.kgram.api.query.Environment;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.kgram.api.query.Producer;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2018
 *
 */
public class TemplateNumber extends TemplateFunction {  
    static int index = 0;
        
    public TemplateNumber(String name){
        super(name);
    }
    
    @Override
    public IDatatype eval(Computer eval, Binding b, Environment env, Producer p) throws EngineException {
        switch (oper()) {
            case STL_INDEX:
                return DatatypeMap.newInstance(index++);
                
            case STL_NUMBER: return DatatypeMap.newInstance(1 + env.count());
            
            case NUMBER: return DatatypeMap.newInstance(env.count());
            
            default: return null;
        }
    }
   
}

