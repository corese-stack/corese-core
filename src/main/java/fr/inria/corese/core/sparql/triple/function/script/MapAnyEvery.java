package fr.inria.corese.core.sparql.triple.function.script;

import fr.inria.corese.core.sparql.api.Computer;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.triple.function.term.Binding;
import static fr.inria.corese.core.kgram.api.core.ExprType.MAPANY;
import static fr.inria.corese.core.kgram.api.core.ExprType.MAPEVERY;
import fr.inria.corese.core.kgram.api.query.Environment;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.kgram.api.query.Producer;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2017
 *
 */
public class MapAnyEvery extends Funcall {

    public MapAnyEvery() {}
    
    public MapAnyEvery(String name) {
        super(name);
        setArity(2);
    }
    
//    @Override
//    public IDatatype eval(Computer eval, Binding b, Environment env, Producer p) throws EngineException {
//        return evalnew(eval, b, env, p);
//    }

    @Override
    public IDatatype eval(Computer eval, Binding b, Environment env, Producer p) throws EngineException {
        IDatatype name = getBasicArg(0).eval(eval, b, env, p);
        IDatatype[] param = evalArguments(eval, b, env, p, 1);
        if (name == null || param == null) {
            return null;
        }

        Function function = null;
        try {
            function = getDefineGenerate(this, env, name.stringValue(), param.length);
        } catch (EngineException ex) {
            log(ex.getMessage());
        }
        if (function == null) {
            return null;
        }

        /**
         * every (xt:fun, ?list) every (xt:fun, ?x, ?list) every (xt:fun, ?l1,
         * ?l2) TODO: when getLoop() it works with only one loop error follow
         * SPARQ semantics of OR (any) AND (every)
         *
         * @return
         */
        boolean every = oper() == MAPEVERY;
        boolean any = oper() == MAPANY;
        IDatatype iter = null;
        Iterator<IDatatype> loop = null;
        boolean isList = false, isLoop = false;


        int k = 0;
        for (IDatatype dt : param) {
            if (dt.isList() || dt.isLoop()) {
                iter = dt;
                break;
            }
            else {
                k++;
            }
        }
        if (iter == null) {
            return null;
        }
        IDatatype[] value = param; 
        boolean error = false, ok = true;
        
        for (IDatatype elem : iter) {

            value[k] = elem;

            if (elem != null) {
                // iterator may return null when it ends
                IDatatype res = call(eval, b, env, p, function, value);

                if (res == null) {
                    error = true;
                } else {
                    if (every) {
                        if (!res.booleanValue()) {
                            return FALSE;
                        }
                    } else if (any) {
                        // any
                        if (res.booleanValue()) {
                            return TRUE;
                        }
                    }
                }
            }
        }
        
        if (error) {
            return null;
        }
        return value(every);


    }
    
    
}
