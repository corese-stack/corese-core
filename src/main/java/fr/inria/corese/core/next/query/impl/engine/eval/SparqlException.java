package fr.inria.corese.core.next.query.impl.engine.eval;

/**
 *
 * @author corby
 */
public class SparqlException extends Exception {
    private static final long serialVersionUID = 1L;

    private final boolean stop;

    public SparqlException() {
        this(false);
    }

    public SparqlException(boolean stop) {
        this.stop = stop;
    }

    // isStop true means stop query processing, perform aggregate etc. and return partial result
    // isStop false means this is an exception
    // see LDScriptException in sparql

    public boolean isStop() {
        return stop;
    }

}
