package fr.inria.corese.core.next.data.io.serializer;

/**
 * Interface to specify which line ending a serializer must use.
 */
public interface LineEndingOptions {

    /**
     *
     * @return the end line characters
     */
    String getLineEnding();
}
