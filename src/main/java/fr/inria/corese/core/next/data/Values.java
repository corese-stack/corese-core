package fr.inria.corese.core.next.data;

import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.impl.adapter.CoreseValueFactory;

/**
 * Public entry point for creating RDF terms and statements independently of a
 * repository.
 *
 * <p>The returned factory is thread-safe and may be shared. When working with a
 * repository, prefer its own value factory so backend-specific implementations
 * remain possible.</p>
 */
public final class Values {

    private static final ValueFactory DEFAULT_FACTORY = new CoreseValueFactory();

    private Values() {
    }

    /**
     * Returns Corese's shared default RDF value factory.
     *
     * @return the default value factory
     */
    public static ValueFactory factory() {
        return DEFAULT_FACTORY;
    }
}
