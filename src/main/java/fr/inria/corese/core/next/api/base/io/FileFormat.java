package fr.inria.corese.core.next.api.base.io;

import java.util.List;
import java.util.Objects;

/**
 * Represents a general file format, including its name, associated file
 * extensions, and MIME types.
 */
public class FileFormat {

    private final String name;
    private final List<String> extensions;
    private final List<String> mimeTypes;

    /**
     * Constructs a new FileFormat instance.
     *
     * @param name       The human-readable name of the format.
     * @param extensions The list of file extensions.
     * @param mimeTypes  The list of MIME types.
     * @throws NullPointerException if name, extensions or mimeTypes is null or
     *                              empty.
     */
    public FileFormat(String name, List<String> extensions, List<String> mimeTypes) {
        this.name = Objects.requireNonNull(name, "Format name cannot be null");
        this.extensions = List.copyOf(Objects.requireNonNull(extensions, "Extensions list cannot be null"));
        this.mimeTypes = List.copyOf(Objects.requireNonNull(mimeTypes, "MIME types list cannot be null"));

        if (extensions.isEmpty()) {
            throw new IllegalArgumentException("At least one file extension must be provided");
        }
        if (mimeTypes.isEmpty()) {
            throw new IllegalArgumentException("At least one MIME type must be provided");
        }
    }

    /**
     * Returns the name of the format.
     *
     * @return The format name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the list of known file extensions.
     *
     * @return A list of extensions.
     */
    public List<String> getExtensions() {
        return extensions;
    }

    /**
     * Returns the list of associated MIME types.
     *
     * @return A list of MIME types.
     */
    public List<String> getMimeTypes() {
        return mimeTypes;
    }

    /**
     * Returns the default (primary) file extension.
     *
     * @return The first extension in the list.
     */
    public String getDefaultExtension() {
        return extensions.get(0);
    }

    /**
     * Returns the default (primary) MIME type.
     *
     * @return The first MIME type in the list.
     */
    public String getDefaultMimeType() {
        return mimeTypes.get(0);
    }

    @Override
    public String toString() {
        return "FileFormat{name='%s', extensions=%s, mimeTypes=%s}".formatted(name, extensions, mimeTypes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof FileFormat other))
            return false;
        return name.equalsIgnoreCase(other.name)
                && extensions.equals(other.extensions)
                && mimeTypes.equals(other.mimeTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.toLowerCase(), extensions, mimeTypes);
    }
}
