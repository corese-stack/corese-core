package fr.inria.corese.core.sparql.triple.function.core;

import fr.inria.corese.core.next.kgram.api.query.Environment;
import fr.inria.corese.core.next.kgram.api.query.Producer;

/**
 *
 * @author corby
 */
public interface FunctionEvaluator {
    
    default void setProducer(Producer p) {}
    
    default void setEnvironment(Environment e) {}
    
}
