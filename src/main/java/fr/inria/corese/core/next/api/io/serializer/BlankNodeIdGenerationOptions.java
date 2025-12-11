package fr.inria.corese.core.next.api.io.serializer;

public interface BlankNodeIdGenerationOptions {

    /**
     * Checks if deterministic blank node IDs should be generated.
     *
     * @return {@code true} if stable blank node IDs are enabled, {@code false} otherwise.
     */
    boolean stableBlankNodeIds();
}
