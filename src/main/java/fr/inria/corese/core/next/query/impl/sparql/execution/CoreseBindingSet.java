package fr.inria.corese.core.next.query.impl.sparql.execution;


import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.query.api.result.Binding;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.result.CoreseBinding;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Adapts a KGRAM {@link Mapping} to the {@link BindingSet} API.
 */
public final class CoreseBindingSet implements BindingSet {

    private final Mapping mapping;
    private final Set<String> visibleNames;

    /**
     * Adapts a completed solution without exposing its mutable variable-name index.
     *
     * @param mapping completed solution, no longer modified by its evaluator
     * @throws NullPointerException if the mapping is null
     */
    public CoreseBindingSet(Mapping mapping) {
        this(mapping, Set.copyOf(mapping.getVariableNames()));
    }

    /**
     * Shares a result-owned projection, filtering only unbound variables.
     *
     * @param mapping completed solution, no longer modified by its evaluator
     * @param visibleNames immutable projection shared across the result's rows
     * @throws NullPointerException if the mapping or projection is null
     */
    CoreseBindingSet(Mapping mapping, Set<String> visibleNames) {
        this.mapping = Objects.requireNonNull(mapping, "mapping");
        this.visibleNames = filterVisible(mapping, visibleNames);
    }

    @SuppressWarnings("java:S9391") // Avoid stream and intermediate allocation per emitted solution row.
    private static Set<String> filterVisible(Mapping mapping, Set<String> visibleNames) {
        boolean allPresent = true;
        for (String name : visibleNames) {
            if (mapping.getValue(name) == null) {
                allPresent = false;
                break;
            }
        }
        if (allPresent) {
            return visibleNames;
        }
        Set<String> present = LinkedHashSet.newLinkedHashSet(visibleNames.size());
        for (String name : visibleNames) {
            if (mapping.getValue(name) != null) {
                present.add(name);
            }
        }
        return Collections.unmodifiableSet(present);
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
