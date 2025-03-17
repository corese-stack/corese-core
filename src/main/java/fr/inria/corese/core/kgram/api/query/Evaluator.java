package fr.inria.corese.core.kgram.api.query;

import fr.inria.corese.core.kgram.core.Eval;
import fr.inria.corese.core.kgram.event.ResultListener;
import fr.inria.corese.core.sparql.api.Computer;
import fr.inria.corese.core.sparql.triple.function.term.Binding;

/**
 * Interface for the connector that evaluates filters
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 */
public interface Evaluator extends Computer {

    Mode getMode();

    void setMode(Mode mode);

    Binding getBinder();

    void setProducer(Producer p);

    void setKGRAM(Eval o);

    void addResultListener(ResultListener rl);

    void start(Environment env);

    void finish(Environment env);

    void init(Environment env);

    public enum Mode {
        KGRAM_MODE, SPARQL_MODE, CACHE_MODE, NO_CACHE_MODE
    }

}
