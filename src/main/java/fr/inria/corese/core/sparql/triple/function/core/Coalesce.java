package fr.inria.corese.core.sparql.triple.function.core;

import fr.inria.corese.core.sparql.api.Computer;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.triple.parser.Expression;
import fr.inria.corese.core.sparql.triple.function.term.Binding;
import fr.inria.corese.core.sparql.triple.function.term.TermEval;
import fr.inria.corese.core.kgram.api.query.Environment;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.kgram.api.query.Producer;
import fr.inria.corese.core.sparql.triple.parser.Access;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2017
 *
 */
public class Coalesce extends TermEval {
    
    boolean exception = Access.COALESCE_EXCEPTION;

    public Coalesce(){}

    public Coalesce(String name) {
        super(name);
    }

    @Override
    public IDatatype eval(Computer eval, Binding b, Environment env, Producer p) throws EngineException {
        for (Expression arg : getArgs()) {
            boolean save = b.isCoalesce();
            b.setCoalesce(true);
            IDatatype dt = null;
            try {
                dt = arg.evalWE(eval, b, env, p);
            }
            catch (EngineException e) {
                if (exception) {
                    b.setCoalesce(save);
                    throw e;
                }
            }
            b.setCoalesce(save);
            if (dt != null) {
                return dt;
            }
        }
        return null;
    }
}
