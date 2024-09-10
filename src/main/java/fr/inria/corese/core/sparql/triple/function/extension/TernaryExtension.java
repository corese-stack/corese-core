package fr.inria.corese.core.sparql.triple.function.extension;

import static fr.inria.corese.core.kgram.api.core.ExprType.XT_REPLACE;
import fr.inria.corese.core.kgram.api.query.Environment;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.kgram.api.query.Producer;
import fr.inria.corese.core.sparql.api.Computer;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.triple.function.term.Binding;
import fr.inria.corese.core.sparql.triple.function.term.TermEval;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2018
 *
 */
public class TernaryExtension extends TermEval {
    

    public TernaryExtension(String name) {
        super(name);
        setArity(3);
    }

    @Override
    public IDatatype eval(Computer eval, Binding b, Environment env, Producer p) throws EngineException {
        IDatatype dt1 = getBasicArg(0).eval(eval, b, env, p);
        IDatatype dt2 = getBasicArg(1).eval(eval, b, env, p);
        IDatatype dt3 = getBasicArg(2).eval(eval, b, env, p);
        if (dt1 == null || dt2 == null || dt3 == null) {
            return null;
        }
        switch (oper()) {
             case XT_REPLACE:
                return replace(dt1, dt2, dt3);
        }
        return null;
    }
    
    IDatatype replace(IDatatype dt1, IDatatype dt2, IDatatype dt3) {
        return DatatypeMap.newInstance(dt1.stringValue().replace(dt2.stringValue(), dt3.stringValue()));
    }
    
   

  

}
