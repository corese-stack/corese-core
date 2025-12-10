package fr.inria.corese.core.next.kgram.api.query;

import fr.inria.corese.core.next.kgram.api.core.Node;
import fr.inria.corese.core.next.kgram.core.Eval;
import fr.inria.corese.core.next.kgram.core.Exp;
import fr.inria.corese.core.next.kgram.core.Mappings;
import fr.inria.corese.core.next.kgram.core.SparqlException;

/**
 * Service Provider
 *
 * @author Olivier Corby, Edelweiss, INRIA 2011
 */
public interface Provider {

    default Mappings service(Node serv, Exp exp, Mappings map, Eval eval)
            throws SparqlException {
        return null;
    }

}   
