package fr.inria.corese.core.next.data.api.io.serializer.option;

import fr.inria.corese.core.next.data.api.namespace.PrefixMapping;

/**
 * Interface for the options of serializer that can declare prefixes
 */
public interface UsesPrefixOptions {

    /**
     * Checks if prefix declarations should be used for compact IRIs.
     *
     * @return {@code true} if prefixes are used, {@code false} otherwise.
     */
    boolean usePrefixes();
    /**
     * Checks if the serializer should automatically discover and declare prefixes.
     *
     * @return {@code true} if auto-declaration is enabled, {@code false} otherwise.
     */
    boolean autoDeclarePrefixes();
    /**
     * Returns the policy for ordering prefix declarations.
     *
     * @return The {@link PrefixOrdering} for prefix ordering.
     */
    PrefixOrdering getPrefixOrdering();
    /**
     * Returns an unmodifiable map of custom URI prefixes.
     *
     * @return the prefix mappings used by this configuration.
     */
    PrefixMapping getPrefixMapping();
}
