package fr.inria.corese.core.next.data.api.io.serializer.option;

/**
 * Interface for options that determine the generation of blank node IDs for serializers.
 */
public interface BlankNodeIdGenerationOptions {

    /**
     * Checks if sequential blank node IDs should be generated in first-appearance order
     * during serialization. This makes output repeatable for the same statement order,
     * but does not canonicalize blank nodes across isomorphic graphs with different IDs.
     *
     * @return {@code true} if stable blank node IDs are enabled, {@code false} otherwise.
     */
    boolean stableBlankNodeIds();
}
