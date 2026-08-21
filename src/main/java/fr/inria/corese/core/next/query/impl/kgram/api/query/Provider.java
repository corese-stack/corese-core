package fr.inria.corese.core.next.query.impl.kgram.api.query;

import fr.inria.corese.core.next.query.impl.kgram.api.core.Node;
import fr.inria.corese.core.next.query.impl.kgram.core.Eval;
import fr.inria.corese.core.next.query.impl.kgram.core.Exp;
import fr.inria.corese.core.next.query.impl.kgram.core.Mappings;
import fr.inria.corese.core.next.query.impl.kgram.core.SparqlException;

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
