package fr.inria.corese.core.kgram.api.query;

import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.core.Eval;
import fr.inria.corese.core.kgram.core.Exp;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.kgram.core.SparqlException;

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

    void set(String uri, double version);

    boolean isSparql0(Node serv);

}   
