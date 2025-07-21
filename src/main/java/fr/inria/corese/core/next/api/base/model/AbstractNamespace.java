package fr.inria.corese.core.next.api.base.model;

import java.io.Serial;
import java.util.Comparator;
import java.util.Objects;

import fr.inria.corese.core.next.api.Namespace;

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
     * Comparator that orders namespaces by prefix, then by URI.
     * Null values are ordered first.
     */
    private static final Comparator<Namespace> ORDERING = Comparator.nullsFirst(
            Comparator.comparing(Namespace::getPrefix)
                    .thenComparing(Namespace::getName));

    /**
     * Compares this namespace to another based on prefix and name.
     */
    @Override
    public int compareTo(Namespace other) {
        return ORDERING.compare(this, other);
    }

    /**
     * Checks equality based on prefix and name.
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Namespace)) {
            return false;
        }
        Namespace ns = (Namespace) object;
        return Objects.equals(getPrefix(), ns.getPrefix())
                && Objects.equals(getName(), ns.getName());
    }

    /**
     * Computes hash code from prefix and name.
     */
    @Override
    public int hashCode() {
        return Objects.hash(getPrefix(), getName());
    }

    /**
     * Returns a readable string representation of the namespace.
     */
    @Override
    public String toString() {
        return getPrefix() + " :: " + getName();
    }
}
