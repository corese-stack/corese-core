package fr.inria.corese.core.next.kgram.api.query;

import fr.inria.corese.core.next.kgram.core.Eval;

/**
 * Interface for the connector that evaluates filters
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 */
public interface Evaluator {

    Mode getMode();

    void setMode(Mode mode);

    void setProducer(Producer p);

    void setKGRAM(Eval o);

    void start(Environment env);

    void finish(Environment env);

    void init(Environment env);

    enum Mode {
        KGRAM_MODE, SPARQL_MODE, CACHE_MODE, NO_CACHE_MODE
    }

}
