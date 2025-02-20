package fr.inria.corese.core.next.api.model.impl;

import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.model.util.IRIUtils;

public abstract class AbstractIRI implements IRI {
    private String namespace;
    private String localName;
    private final String fullIRI;

    protected AbstractIRI(String fullIRI) {
        this.fullIRI = fullIRI;
        this.namespace = IRIUtils.guessNamespace(fullIRI);
        this.localName = IRIUtils.guessLocalName(fullIRI);
    }

    protected AbstractIRI(String namespace, String localName) {
        this.namespace = namespace;
        this.localName = localName;
        this.fullIRI = namespace + localName;
    }

    @Override
    public boolean isIRI() {
        return true;
    }

    @Override
    public String getNamespace() {
        return this.namespace;
    }

    @Override
    public String getLocalName() {
        return this.localName;
    }

    @Override
    public String stringValue() {
        return this.fullIRI;
    }
}
