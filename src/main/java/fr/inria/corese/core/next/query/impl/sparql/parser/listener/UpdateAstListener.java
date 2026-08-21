package fr.inria.corese.core.next.query.impl.sparql.parser.listener;

import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlUpdateAstBuilder;

public interface UpdateAstListener {
    SparqlUpdateAstBuilder updateBuilder();
}
