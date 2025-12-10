package fr.inria.corese.core.sparql.triple.function.extension;

import fr.inria.corese.core.sparql.api.Computer;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.triple.function.term.Binding;
import fr.inria.corese.core.sparql.triple.function.term.TermEval;
import fr.inria.corese.core.kgram.api.core.ExprType;
import fr.inria.corese.core.kgram.api.core.Node;
import static fr.inria.corese.core.kgram.api.core.PointerType.PATH;
import fr.inria.corese.core.kgram.api.core.Pointerable;
import fr.inria.corese.core.kgram.api.query.Environment;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.kgram.api.query.Producer;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2018
 *
 */
public class PathLength extends TermEval {

    public PathLength() {}
    
    public PathLength(String name) {
        super(name);
        setArity(1);
    }

    @Override
    public IDatatype eval(Computer eval, Binding b, Environment env, Producer p) throws EngineException {
        IDatatype dt = getArg(0).eval(eval, b, env, p);
        if (dt == null || dt.pointerType() != PATH) {
            return null;
        }
        return value(dt.getPointerObject().getPathObject().size());
    }

}
