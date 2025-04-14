package fr.inria.corese.core.next.api.base.model;

import fr.inria.corese.core.next.impl.exception.IncorrectFormatException;
import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.impl.common.util.IRIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * Base class for IRI implementations. Includes base functionality for IRI handling.
 */
public abstract class AbstractIRI implements IRI, Comparable<IRI>, Serializable {
    private static final Logger logger = LoggerFactory.getLogger(AbstractIRI.class);

    private static final long serialVersionUID = -1005683238501772511L;

    private final String namespace;
    private final String localName;

    /**
     * Constructor for AbstractIRI.
     *
     * @param fullIRI the full IRI string
     * @throws IncorrectFormatException if the IRI format is incorrect
     */
    protected AbstractIRI(String fullIRI) {
        if(! IRIUtils.isStandardIRI(fullIRI)) {
            throw new IncorrectFormatException("IRI '"+ fullIRI +"' must be a valid IRI");
        }
        this.namespace = IRIUtils.guessNamespace(fullIRI);
        this.localName = IRIUtils.guessLocalName(fullIRI);
    }

    /**
     * Constructor for AbstractIRI with namespace and local name.
     *
     * @param namespace the namespace of the IRI
     * @param localName the local name of the IRI
     * @throws IncorrectFormatException if the IRI format is incorrect
     */
    protected AbstractIRI(String namespace, String localName) {
        if(! IRIUtils.isStandardIRI(namespace + localName)) {
            throw new IncorrectFormatException("IRI '"+ namespace + localName +"' must be a valid IRI");
        }
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
        return this.namespace + this.localName;
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

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (this.namespace == null ? 0 : this.namespace.hashCode());
        hash = 31 * hash + (this.localName == null ? 0 : this.localName.hashCode());
        return hash;
    }
}
