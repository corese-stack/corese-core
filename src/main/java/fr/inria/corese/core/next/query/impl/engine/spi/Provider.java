package fr.inria.corese.core.next.query.impl.engine.spi;

import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.eval.Eval;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;
import fr.inria.corese.core.next.query.impl.engine.eval.SparqlException;

/**
 * Service Provider
 *
 * @author Olivier Corby, Edelweiss, INRIA 2011
 */
public interface Provider {
    @SuppressWarnings("unused")
    default Mappings service(Node serv, Exp exp, Mappings map, Eval eval)
            throws SparqlException {
        return null;
    }

}
