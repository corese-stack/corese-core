package fr.inria.corese.core.next.api.query;

import fr.inria.corese.core.next.api.Value;
import fr.inria.corese.core.next.api.query.result.TupleQueryResult;

/**
 * Compiled SPARQL query corresponding to a SELECT query
 */
public interface TupleQuery extends Query<TupleQueryResult> {
    @Override
    TupleQueryResult evaluate();

    /**
     * Set bindings before execution
     * @param name variable name
     * @param value binding value for the variable
     * @return this
     */
    TupleQuery setBinding(String name, Value value);

    /**
     * Remove all the set bindings
     * @return this
     */
    TupleQuery clearBindings();


}