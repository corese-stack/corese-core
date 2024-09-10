package fr.inria.corese.core.sparql.exceptions;

/**
 *
 */
public class StopException extends EngineException {
    
    public StopException() {
        setStop(true);
    }
    
    
}
