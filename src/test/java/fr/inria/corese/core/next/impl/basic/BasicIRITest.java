<<<<<<<< HEAD:src/test/java/fr/inria/corese/core/next/impl/inmemory/BasicIRITest.java
package fr.inria.corese.core.next.impl.inmemory;

import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.IRITest;
import fr.inria.corese.core.next.impl.common.BasicIRI;
========
package fr.inria.corese.core.next.impl.basic;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.model.IRITest;
>>>>>>>> feature/temporal_literal:src/test/java/fr/inria/corese/core/next/impl/basic/BasicIRITest.java

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BasicIRITest extends IRITest {
    @Override
    public IRI createIRI(String iri) {
        return new BasicIRI(iri);
    }

    @Override
    public IRI createIRI(String namespace, String localName) {
        return new BasicIRI(namespace, localName);
    }
}
