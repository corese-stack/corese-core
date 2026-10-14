package fr.inria.corese.core.next.data.api.term;

import fr.inria.corese.core.next.data.spi.term.AbstractIRI;
import fr.inria.corese.core.next.data.api.exception.IncorrectFormatException;

import java.io.Serial;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Immutable default implementation of an RDF IRI.
 */
public final class SimpleIRI extends AbstractIRI {

    @Serial
    private static final long serialVersionUID = -2444556019914696994L;

    // SimpleIRI is immutable. Preserve AbstractIRI's exact hash, but parse it once.
    // Transient so older serialized instances rebuild the cache during deserialization.
    private transient int cachedHash;

    /**
     * Creates an IRI from its complete string representation.
     *
     * @param fullIRI the full IRI string
     * @throws IncorrectFormatException if fullIRI does not form a correct IRI
     */
    public SimpleIRI(String fullIRI) {
        super(fullIRI);
        // This constructor has already inferred the canonical split in AbstractIRI.
        cachedHash = 31 * (31 * 7 + getNamespace().hashCode()) + getLocalName().hashCode();
    }

    /**
     * Creates an IRI from a namespace and a local name.
     *
     * @param namespace the namespace of the IRI
     * @param localName the local name of the IRI
     * @throws IncorrectFormatException if namespace and local name do not form a correct IRI
     */
    public SimpleIRI(String namespace, String localName) {
        super(namespace, localName);
        cachedHash = super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IRI that)) {
            return false;
        }
        return stringValue().equals(that.stringValue());
    }

    @Override
    public int hashCode() {
        return cachedHash;
    }

    @Serial
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        cachedHash = super.hashCode();
    }
}
