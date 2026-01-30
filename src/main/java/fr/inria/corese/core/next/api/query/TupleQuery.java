package fr.inria.corese.core.next.api.query;

import fr.inria.corese.core.next.api.Value;
import fr.inria.corese.core.next.api.query.result.TupleQueryResult;

public interface TupleQuery extends Query {
    TupleQueryResult evaluate();

    // Bindings
    TupleQuery setBinding(String name, Value value);
    TupleQuery clearBindings();
}