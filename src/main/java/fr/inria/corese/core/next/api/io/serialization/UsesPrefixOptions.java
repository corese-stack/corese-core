package fr.inria.corese.core.next.api.io.serialization;

import fr.inria.corese.core.next.impl.common.prefix.PrefixHandler;
import fr.inria.corese.core.next.impl.io.serialization.option.PrefixOrderingEnum;

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
     * @return The {@link PrefixOrderingEnum} for prefix ordering.
     */
    public PrefixOrderingEnum getPrefixOrdering();
    /**
     * Returns an unmodifiable map of custom URI prefixes.
     *
     * @return The {@link PrefixHandler} managing all prefix mappings.
     */
    public PrefixHandler getPrefixHandler();
}
