package fr.inria.corese.core.next.query.impl.dataset;

import fr.inria.corese.core.next.query.api.dataset.Dataset;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Mutable implementation of {@link Dataset}.
 */
public final class CoreseDataset implements Dataset {

    private final Set<String> defaultGraphs = new LinkedHashSet<>();
    private final Set<String> namedGraphs = new LinkedHashSet<>();

    @Override
    public Set<String> getDefaultGraphs() {
        return Collections.unmodifiableSet(defaultGraphs);
    }

    @Override
    public Set<String> getNamedGraphs() {
        return Collections.unmodifiableSet(namedGraphs);
    }

    @Override
    public Dataset addDefaultGraph(String uri) {
        defaultGraphs.add(uri);
        return this;
    }

    @Override
    public Dataset addNamedGraph(String uri) {
        namedGraphs.add(uri);
        return this;
    }

    @Override
    public Dataset clear() {
        defaultGraphs.clear();
        namedGraphs.clear();
        return this;
    }
}
