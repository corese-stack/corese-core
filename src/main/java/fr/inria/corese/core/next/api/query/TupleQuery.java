package fr.inria.corese.core.next.api.query;

import fr.inria.corese.core.next.api.result.TupleQueryResult;

public interface TupleQuery extends Query {
    TupleQueryResult evaluate();

    // Bindings
    TupleQuery setBinding(String name, Object value); // or Value/IDatatype
    TupleQuery clearBindings();
}