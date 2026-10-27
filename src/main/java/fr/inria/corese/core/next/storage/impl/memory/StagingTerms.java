package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.api.term.Value;

import java.util.HashMap;

/**
 * Batch-private canonical dictionary, discarded before a partition is published. Each instance owns its mutable lookup
 * probe and is confined to one batch writer. Serialization is outside this transient dictionary's contract; HashMap is
 * used solely for canonical lookup and insertion.
 */
@SuppressWarnings({ "serial", "java:S1948", "java:S2160" })
final class StagingTerms extends HashMap<TermKey, TermKey> {
    /**
     * Mutable non-stored search probe owned exclusively by this staging dictionary.
     */
    private final Lookup lookup = new Lookup();

    /**
     * Creates a batch-private canonicalization dictionary with an independent search probe.
     */
    StagingTerms() {
    }

    /**
     * Creates a batch-private canonicalization dictionary with an independent search probe.
     *
     * @param expectedTerms bounded expected number of distinct staged terms
     */
    StagingTerms(final int expectedTerms) {
        super((expectedTerms * 4 + 2) / 3);
    }

    /**
     * Creates a batch-private canonicalization dictionary with an independent search probe.
     *
     * @param source source index or container; it must remain unchanged during copying or traversal
     */
    StagingTerms(final StagingTerms source) {
        super(source);
    }

    /**
     * Looks up an existing canonical term key matching the given value and structural hash.
     *
     * <p>
     * The private, non-stored probe is passed to HashMap.get(Object), which compares the search object's
     * equals(storedKey). Stored keys always remain immutable TermKey instances.
     * </p>
     *
     * @param value non-null RDF value to look up
     * @param hash  cached structural hash of the RDF value
     * @return the matching canonical TermKey, or null if not found
     */
    @SuppressWarnings("java:S2175")
    TermKey find(final Value value, final int hash) {
        lookup.value = value;
        lookup.hash = hash;
        try {
            return get(lookup);
        } finally {
            lookup.value = null;
        }
    }

    /**
     * Reuses the canonical key for a captured term or records a new immutable key.
     *
     * @param value non-null RDF value or entry value
     * @return the dictionary-owned canonical key
     */
    TermKey intern(final Value value) {
        final int hash = TermKey.structuralHash(value);
        TermKey key = find(value, hash);
        if (key == null) {
            key = new TermKey(value, hash);
            put(key, key);
        }
        return key;
    }

    // HashMap.get invokes the search object's equals(storedKey). This probe is
    // never inserted: every stored key is an immutable TermKey. Each private
    // staging dictionary, including copies, owns its own probe.
    /**
     * Dictionary-private mutable search probe; it is never stored as a key and is not shared between writers.
     */
    private static final class Lookup {

        /** Creates the writer-confined probe; its value is assigned only during lookup. */
        private Lookup() {
        }

        /**
         * Captured RDF value or transient lookup value, according to the enclosing class.
         */
        private Value value;
        /**
         * Cached structural hash of the captured or transient lookup value.
         */
        private int hash;

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
         * {@inheritDoc}
         *
         * @param other value or search object to compare with the stored key
         * @return true when the other object satisfies this structural lookup contract
         */
        @Override
        public boolean equals(final Object other) {
            return other == this || other instanceof TermKey key && key.matchesLookup(value, hash);
        }
    }
}
