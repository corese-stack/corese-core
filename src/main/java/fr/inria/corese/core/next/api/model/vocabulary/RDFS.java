package fr.inria.corese.core.next.api.model.vocabulary;

import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.model.impl.basic.BasicIRI;

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
}
