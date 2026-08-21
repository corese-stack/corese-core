package fr.inria.corese.core.next.query.impl.sparql.parser.listener;

import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlQueryAstBuilder;

public interface QueryAstListener {
    SparqlQueryAstBuilder queryBuilder();
}
