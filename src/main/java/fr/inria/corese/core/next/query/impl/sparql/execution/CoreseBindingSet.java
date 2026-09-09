package fr.inria.corese.core.next.query.impl.sparql.execution;


import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.query.api.result.Binding;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.result.CoreseBinding;

import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adapts a KGRAM {@link Mapping} to the {@link BindingSet} API.
 */
public final class CoreseBindingSet implements BindingSet {

    private final Mapping mapping;
    private final Set<String> visibleNames;

    public CoreseBindingSet(Mapping mapping) {
        this(mapping, mapping.getVariableNames());
    }

    CoreseBindingSet(Mapping mapping, Set<String> visibleNames) {
        this.mapping = Objects.requireNonNull(mapping, "mapping");
        this.visibleNames = visibleNames.stream()
                .filter(name -> mapping.getValue(name) != null)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<String> getBindingNames() {
        return visibleNames;
    }

    @Override
    public boolean hasBinding(String name) {
        return visibleNames.contains(name) && this.mapping.getValue(name) != null;
    }

    @Override
    public Value getValue(String name) {
        return visibleNames.contains(name) && this.mapping.getValue(name) instanceof Value value ? value : null;
    }

    @Override
    public Iterator<Binding> iterator() {
        return visibleNames.stream()
                .map(this::toBinding)
                .iterator();
    }

    private Binding toBinding(String name) {
        if (mapping.getValue(name) instanceof Value value) {
            return new CoreseBinding(name, value);
        }
        throw new IllegalStateException(
                "Query binding is not backed by an RDF value: " + name);
    }
}
