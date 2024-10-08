package fr.inria.corese.core.sparql.triple.function.script;

import static fr.inria.corese.core.kgram.api.core.ExprType.THROW;
import fr.inria.corese.core.kgram.api.query.Environment;
import fr.inria.corese.core.kgram.api.query.Producer;
import fr.inria.corese.core.sparql.api.Computer;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.exceptions.LDScriptException;
import fr.inria.corese.core.sparql.exceptions.StopException;
import fr.inria.corese.core.sparql.triple.function.term.Binding;

/**
 *
 */
public class Throw extends LDScript {
    
    public Throw(String name) {
        super(name);
    }
    
    @Override
    public IDatatype eval(Computer eval, Binding b, Environment env, Producer p) throws EngineException {
          switch (oper()) {
              case THROW:
                  // throw(exp)
                  IDatatype dt = getArg(0).eval(eval, b, env, p);
                  throw new LDScriptException(dt);
                  
                  // stop()
              default:throw new StopException();
          }
    }
    
}
