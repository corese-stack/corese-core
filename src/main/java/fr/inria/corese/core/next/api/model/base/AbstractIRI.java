package fr.inria.corese.core.next.api.model.base;

import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.model.Statement;
import fr.inria.corese.core.next.impl.common.util.IRIUtils;

import java.util.Objects;

public abstract class AbstractIRI implements IRI {
    private static final long serialVersionUID = 121677524244147770L;
    private String namespace;
    private String localName;

    protected AbstractIRI(String fullIRI) {
        this.namespace = IRIUtils.guessNamespace(fullIRI);
        this.localName = IRIUtils.guessLocalName(fullIRI);
    }

    protected AbstractIRI(String namespace, String localName) {
        this.namespace = namespace;
        this.localName = localName;
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
        return this.toString();
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof IRI
                && getNamespace().equals(((IRI) o).getNamespace())
                && getLocalName().equals(((IRI) o).getLocalName());
    }

    @Override
    public String toString() {
        return namespace + localName;
    }
}
