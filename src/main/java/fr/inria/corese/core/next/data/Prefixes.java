package fr.inria.corese.core.next.data;

import fr.inria.corese.core.next.data.api.namespace.PrefixMapping;
import fr.inria.corese.core.next.data.impl.namespace.PrefixHandler;

/** Public entry point for creating mutable RDF prefix mappings. */
public final class Prefixes {

    private Prefixes() {
    }

    /**
     * Creates an empty prefix mapping.
     *
     * @return a new mutable mapping
     */
    public static PrefixMapping create() {
        return new PrefixHandler(false);
    }

    /**
     * Creates a prefix mapping initialized with Corese's common vocabulary
     * prefixes: RDF, RDFS, XSD, OWL, and FOAF.
     *
     * @return a new mutable mapping with common prefixes
     */
    public static PrefixMapping createWithDefaults() {
        return new PrefixHandler(true);
    }
}
