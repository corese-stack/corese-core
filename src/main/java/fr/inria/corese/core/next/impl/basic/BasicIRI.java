<<<<<<<< HEAD:src/main/java/fr/inria/corese/core/next/impl/common/BasicIRI.java
package fr.inria.corese.core.next.impl.common;

import fr.inria.corese.core.next.api.model.base.AbstractIRI;
========
package fr.inria.corese.core.next.impl.basic;

import fr.inria.corese.core.next.api.base.model.AbstractIRI;
>>>>>>>> feature/temporal_literal:src/main/java/fr/inria/corese/core/next/impl/basic/BasicIRI.java

/**
 * Basic implementation of IRI
 */
public class BasicIRI extends AbstractIRI {

    public BasicIRI(String fullIRI) {
        super(fullIRI);
    }

    public BasicIRI(String namespace, String localName) {
        super(namespace, localName);
    }

}
