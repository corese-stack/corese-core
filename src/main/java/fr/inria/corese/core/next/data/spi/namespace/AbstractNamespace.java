package fr.inria.corese.core.next.data.spi.namespace;

import java.io.Serial;
import java.util.Objects;

import fr.inria.corese.core.next.data.api.namespace.Namespace;

/**
 * A base implementation of the {@link Namespace} interface.
 * <p>
 * Provides standard implementations for {@code equals}, {@code hashCode},
 * {@code compareTo}, and {@code toString},
 * based on the prefix and URI of the namespace.
 * </p>
 */
public abstract class AbstractNamespace implements Namespace {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Checks equality based on prefix and name.
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Namespace ns)) {
            return false;
        }
        return Objects.equals(getPrefix(), ns.getPrefix())
                && Objects.equals(getNamespace(), ns.getNamespace());
    }

    /**
     * Computes hash code from prefix and name.
     */
    @Override
    public int hashCode() {
        return Objects.hash(getPrefix(), getNamespace());
    }

    /**
     * Returns a readable string representation of the namespace.
     */
    @Override
    public String toString() {
        return getPrefix() + " :: " + getNamespace();
    }
}
