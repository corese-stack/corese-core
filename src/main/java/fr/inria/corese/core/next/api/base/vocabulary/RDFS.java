<<<<<<<< HEAD:src/main/java/fr/inria/corese/core/next/impl/common/vocabulary/RDFS.java
package fr.inria.corese.core.next.impl.common.vocabulary;

import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.impl.common.BasicIRI;
========
package fr.inria.corese.core.next.api.base.vocabulary;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.impl.basic.BasicIRI;
>>>>>>>> feature/temporal_literal:src/main/java/fr/inria/corese/core/next/api/base/vocabulary/RDFS.java

/**
 * Vocabulary for RDF Schema (RDFS).
 */
public enum RDFS implements Vocabulary {
    Resource("Resource"),
    Class("Class"),
    Literal("Literal"),
    Property("Property"),
    Datatype("Datatype"),
    Container("Container"),
    ContainerMembershipProperty("ContainerMembershipProperty"),
    subClassOf("subClassOf"),
    subPropertyOf("subPropertyOf"),
    domain("domain"),
    range("range"),
    label("label"),
    comment("comment"),
    member("member"),
    seeAlso("seeAlso"),
    isDefinedBy("isDefinedBy")
    ;

    private final IRI iri;
    RDFS(String localName) {
        this.iri = new BasicIRI(getNamespace(), localName);
    }

    @Override
    public String getNamespace() {
        return "http://www.w3.org/2000/01/rdf-schema#";
    }

    @Override
    public IRI getIRI() {
        return this.iri;
    }

    @Override
    public String getPreferredPrefix() {
        return "rdfs";
    }
}
