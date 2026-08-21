package fr.inria.corese.core.next.query.impl.kgram.core;

/**
 *
 * @author corby
 */
public class SparqlException extends Exception {
    private boolean stop = false;

    // isStop true means stop query processing, perform aggregate etc. and return partial result
    // isStop false means this is an exception
    // see LDScriptException in sparql

    public boolean isStop() {
        return stop;
    }

}
