package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.query.impl.parser.SparqlUpdateAstBuilder;

public interface UpdateAstListener {
    SparqlUpdateAstBuilder updateBuilder();
}
