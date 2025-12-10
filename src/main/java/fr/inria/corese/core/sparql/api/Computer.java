package fr.inria.corese.core.sparql.api;

import fr.inria.corese.core.next.kgram.api.query.Evaluator;

/**
 * Interface for Interpreter
 *
 * @author corby
 */
public interface Computer extends ComputerProxy {

    Evaluator getEvaluator();

    boolean isCompliant();
}
