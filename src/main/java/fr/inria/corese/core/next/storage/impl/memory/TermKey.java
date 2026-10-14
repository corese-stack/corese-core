package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.api.term.BNode;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.term.SimpleIRI;
import fr.inria.corese.core.next.data.api.term.Triple;
import fr.inria.corese.core.next.data.api.term.Value;

/**
 * Captured RDF identity with a cached structural hash for graph-local index lookup.
 * Keys do not defensively copy values: the store captures supported RDF identities
 * before indexing, and those identities must remain stable while the key is retained.
 * Structural copies share key instances. The existing temporal-value accessors follow
 * the native data API contract and are outside the container copy-on-write protocol.
 */
final class TermKey {
    // String concatenation hashes without constructing an IRI string. Shared globally,
    // never retained per term; lengths count UTF-16 units just like String.hashCode().
    /**
     * Shared factors for UTF-16-compatible IRI string hashing.
     */
    private static final int[] POWERS_OF_31 = powersOf31();
    /**
     * Captured RDF value or transient lookup value, according to the enclosing class.
     */
    private final Value value;
    /**
     * Cached structural hash of the captured or transient lookup value.
     */
    private final int hash;

    /**
     * Captures an RDF value and its structural hash for immutable index lookup.
     *
     * @param value non-null captured RDF value with stable lexical identity
     */
    TermKey(final Value value) {
        this(value, structuralHash(value));
    }

    /**
     * Captures an RDF value and its structural hash for immutable index lookup.
     *
     * @param value non-null captured RDF value with stable lexical identity
     * @param hash  cached structural hash of the captured RDF value
     */
    TermKey(final Value value, final int hash) {
        this.value = value;
        this.hash = hash;
    }

    /**
     * A null search component remains a wildcard, never an indexed term.
     *
     * @param value non-null captured RDF value with stable lexical identity
     * @return a key for a non-null value, or null for a wildcard
     */
    static TermKey nullable(final Value value) {
        return value == null ? null : new TermKey(value);
    }

    /**
     * Reads an immutable RDF value or a value held in the container representation.
     *
     * @return the captured RDF value
     */
    Value value() {
        return value;
    }

    /**
     * Computes an RDF structural hash without allocating a temporary search key.
     *
     * @param value non-null captured RDF value with stable lexical identity
     * @return the RDF structural hash
     * @throws IllegalArgumentException if a capacity, term type or requested isolation level is unsupported
     */
    static int structuralHash(final Value value) {
        if (value instanceof IRI iri) {
            return iriHash(iri);
        }
        if (value instanceof BNode blank) {
            return blank.getID().hashCode();
        }
        if (value instanceof Literal literal) {
            final int result = 31 * literal.getLabel().hashCode() + iriHash(literal.getDatatype());
            return 31 * result + literal.getLanguage().hashCode();
        }
        if (value instanceof Triple triple) {
            final int result = 31 * structuralHash(triple.subject()) + structuralHash(triple.predicate());
            return 31 * result + structuralHash(triple.object());
        }
        throw new IllegalArgumentException("Unsupported RDF term: " + value.getClass().getName());
    }

    /**
     * Builds the shared UTF-16 length factors used for IRI string hashing.
     *
     * @return shared length factors for UTF-16 IRI hashing
     */
    private static int[] powersOf31() {
        final int[] powers = new int[32];
        powers[0] = 1;
        for (int i = 1; i < powers.length; i++) {
            powers[i] = 31 * powers[i - 1];
        }
        return powers;
    }

    /**
     * Computes an IRI hash compatible with its full lexical string without concatenating native components.
     *
     * @param iri captured IRI whose lexical hash is required
     * @return the hash of the IRI lexical string
     */
    private static int iriHash(final IRI iri) {
        if (!(iri instanceof SimpleIRI)) {
            return iri.stringValue().hashCode();
        }
        final String local = iri.getLocalName();
        final int length = local.length();
        if (length == 0) {
            return iri.getNamespace().hashCode();
        }
        int factor;
        if (length < POWERS_OF_31.length) {
            factor = POWERS_OF_31[length];
        } else {
            // Exponentiation by squaring also handles unusually long local names.
            factor = 1;
            int power = 31;
            for (int remaining = length; remaining != 0; remaining >>>= 1) {
                if ((remaining & 1) != 0) {
                    factor *= power;
                }
                power *= power;
            }
        }
        return factor * iri.getNamespace().hashCode() + local.hashCode();
    }

    /**
     * {@inheritDoc}
     *
     * @return the cached structural hash
     */
    @Override
    public int hashCode() {
        return hash;
    }

    /**
     * Compare a singleton's inline RDF value without allocating a second key.
     *
     * @param other value or search object to compare with the stored key
     * @return true when the inline RDF value matches this key
     */

    boolean matches(final Value other) {
        return value == other || hash == structuralHash(other) && sameValue(value, other);
    }

    /**
     * Lookup direction matches candidate.equals(storedKey), without a candidate object.
     *
     * @param candidate     search key
     * @param candidateHash structural hash of the search value
     * @return true when the supplied hash and search value match this stored key
     */

    boolean matchesLookup(final Value candidate, final int candidateHash) {
        return hash == candidateHash && sameValue(candidate, value);
    }

    /**
     * {@inheritDoc}
     *
     * @param other value or search object to compare with the stored key
     * @return true when the other object satisfies this structural lookup contract
     */
    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TermKey key)) {
            return false;
        }
        return sameValue(value, key.value);
    }

    /**
     * Compares RDF lexical identities, including native IRIs with different namespace splits.
     *
     * @param value non-null captured RDF value with stable lexical identity
     * @param other value or search object to compare with the stored key
     * @return true when both RDF lexical identities are equal
     */
    private static boolean sameValue(final Value value, final Value other) {
        if (value == other) {
            return true;
        }
        if (value instanceof SimpleIRI left && other instanceof SimpleIRI right
                && left.getNamespace().equals(right.getNamespace())) {
            return left.getLocalName().equals(right.getLocalName());
        }
        // Different namespace/localName splits may still denote the same RDF IRI.
        return value.equals(other);
    }
}
