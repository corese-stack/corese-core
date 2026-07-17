package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.query.impl.parser.SparqlQueryAstBuilder;

public interface QueryAstListener {
    SparqlQueryAstBuilder queryBuilder();
}
