package fr.inria.corese.core.next.api.model.impl;

import fr.inria.corese.core.next.api.exception.IncorrectFormatException;
import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.model.util.IRIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractIRI implements IRI, Comparable<IRI> {
    private static final Logger logger = LoggerFactory.getLogger(AbstractIRI.class);

    private final String namespace;
    private final String localName;
    private final String fullIRI;

    protected AbstractIRI(String fullIRI) {
        if(! IRIUtils.isStandardIRI(fullIRI)) {
            throw new IncorrectFormatException("IRI '"+ fullIRI +"' must be a valid IRI");
        }
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

    @Override
    public int compareTo(IRI o) {
        return this.stringValue().compareTo(o.stringValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractIRI that = (AbstractIRI) o;
        return this.stringValue().equals(that.stringValue());
    }
}
