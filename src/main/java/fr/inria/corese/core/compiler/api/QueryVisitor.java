package fr.inria.corese.core.compiler.api;

import fr.inria.corese.core.kgram.core.Mappings;

public interface QueryVisitor extends fr.inria.corese.core.sparql.api.QueryVisitor {

    default Mappings getMappings() {
        return null;
    }

}
