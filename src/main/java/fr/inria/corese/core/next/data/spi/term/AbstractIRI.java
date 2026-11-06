package fr.inria.corese.core.next.data.spi.term;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.exception.IncorrectFormatException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;

/**
 * Base class for IRI implementations. Includes base functionality for IRI
 * handling.
 */
public abstract class AbstractIRI implements IRI, Comparable<IRI> {

    @Serial
    private static final long serialVersionUID = -1005683238501772511L;

    private final String value;
    private final String namespace;
    private final String localName;
    private transient int cachedHash;

    /**
     * Constructor for AbstractIRI.
     *
     * @param fullIRI the full IRI string
     * @throws IncorrectFormatException if the IRI format is incorrect
     */
    protected AbstractIRI(String fullIRI) {
        if (!IRIUtils.isStandardIRI(fullIRI)) {
            throw new IncorrectFormatException("IRI '" + fullIRI + "' must be a valid IRI");
        }
        this.value = fullIRI;
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
        String full = namespace + localName;
        if (!IRIUtils.isStandardIRI(full)) {
            throw new IncorrectFormatException("IRI '" + full + "' must be a valid IRI");
        }
        this.value = full;
        this.namespace = namespace;
        this.localName = localName;
    }

    private static int computeCanonicalHash(String ns, String ln) {
        int hash = 7;
        hash = 31 * hash + ns.hashCode();
        hash = 31 * hash + ln.hashCode();
        return hash;
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
        // Fallback to reconstructed string for legacy serialized streams where value was null
        return this.value != null ? this.value : (this.namespace + this.localName);
    }

    @Override
    public int compareTo(IRI o) {
        return this.stringValue().compareTo(o.stringValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IRI that)) {
            return false;
        }
        return this.stringValue().equals(that.stringValue());
    }

    @Override
    public int hashCode() {
        int h = this.cachedHash;
        if (h == 0) {
            String val = stringValue();
            String canonicalNs = IRIUtils.guessNamespace(val);
            String canonicalLn = IRIUtils.guessLocalName(val);
            h = computeCanonicalHash(canonicalNs, canonicalLn);
            if (h == 0) {
                h = 1;
            }
            this.cachedHash = h;
        }
        return h;
    }

    @Override
    public String toString() {
        return this.stringValue();
    }

    @Serial
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        // cachedHash is transient and will be lazily recomputed on first hashCode() call
    }
}
