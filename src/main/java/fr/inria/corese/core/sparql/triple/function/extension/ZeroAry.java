package fr.inria.corese.core.sparql.triple.function.extension;

import fr.inria.corese.core.sparql.api.Computer;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.triple.function.term.Binding;
import fr.inria.corese.core.sparql.triple.function.term.TermEval;
import fr.inria.corese.core.kgram.api.core.ExprType;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.api.query.Environment;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.kgram.api.query.Producer;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2017
 *
 */
public class ZeroAry extends TermEval {

    public ZeroAry() {}
    
    public ZeroAry(String name) {
        super(name);
    }

    @Override
    public IDatatype eval(Computer eval, Binding b, Environment env, Producer p) throws EngineException {
        switch (oper()) {
            case ExprType.LENGTH: return pathLength(env);
        }
        return null;
    }

    IDatatype pathLength(Environment env) {
        Node qNode = env.getQueryNode(getExp(0).getLabel());
        if (qNode == null) {
            return null;
        }
        return value(env.pathLength(qNode));
    }

}
