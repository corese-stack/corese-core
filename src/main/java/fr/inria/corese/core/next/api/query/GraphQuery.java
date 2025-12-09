package fr.inria.corese.core.next.api.query;

import fr.inria.corese.core.next.api.result.GraphQueryResult;

public interface GraphQuery extends Query {

    GraphQueryResult evaluate();

    GraphQuery setBinding(String name, Object value);
    GraphQuery clearBindings();
}
