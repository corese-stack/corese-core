package fr.inria.corese.core.next.impl.temp;

import java.io.Serial;

import fr.inria.corese.core.next.api.base.model.AbstractNamespace;

/*
 * A simple implementation of the {@link Namespace} interface.
 * <p>
 * This class represents a namespace with a prefix and a URI.
 * </p>
 */
public class ModelNamespace extends AbstractNamespace {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String prefix;
    private final String namespaceURI;

    public ModelNamespace(String prefix, String namespaceURI) {
        this.prefix = prefix;
        this.namespaceURI = namespaceURI;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public String getName() {
        return namespaceURI;
    }
}