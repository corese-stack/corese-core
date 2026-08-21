package fr.inria.corese.core.next.data.api.namespace;

import java.io.Serial;
import java.util.Objects;

import fr.inria.corese.core.next.data.api.support.namespace.AbstractNamespace;

/**
 * Immutable default implementation of a {@link Namespace} binding.
 */
@SuppressWarnings("java:S2160")
public final class SimpleNamespace extends AbstractNamespace {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String prefix;
    private final String namespaceURI;

    public SimpleNamespace(String prefix, String namespaceURI) {
        this.prefix = Objects.requireNonNull(prefix, "Prefix cannot be null");
        this.namespaceURI = Objects.requireNonNull(namespaceURI, "Namespace IRI cannot be null");
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public String getNamespace() {
        return namespaceURI;
    }
}
