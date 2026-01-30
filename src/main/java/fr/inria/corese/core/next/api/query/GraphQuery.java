package fr.inria.corese.core.next.api.query;

import fr.inria.corese.core.next.api.query.result.GraphQueryResult;

public interface GraphQuery extends Query<GraphQueryResult> {

    GraphQueryResult evaluate();

    GraphQuery setBinding(String name, Object value);
    GraphQuery clearBindings();
}
