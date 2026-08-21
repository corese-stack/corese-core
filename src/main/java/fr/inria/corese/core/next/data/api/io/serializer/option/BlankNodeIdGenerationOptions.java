package fr.inria.corese.core.next.data.api.io.serializer.option;

/**
 * Interface for options that determine the generation of blank node Ids for serializers.
 */
public interface BlankNodeIdGenerationOptions {

    /**
     * Checks if deterministic blank node IDs should be generated.
     *
     * @return {@code true} if stable blank node IDs are enabled, {@code false} otherwise.
     */
    boolean stableBlankNodeIds();
}
