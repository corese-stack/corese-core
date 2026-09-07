package fr.inria.corese.core.next.query.impl.sparql.execution;

import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.query.api.result.Binding;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.impl.kgram.core.Mapping;
import fr.inria.corese.core.next.query.impl.result.CoreseBinding;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Adapts a KGRAM {@link Mapping} to the {@link BindingSet} API.
 */
public final class CoreseBindingSet implements BindingSet {

    private final Mapping mapping;

    public CoreseBindingSet(Mapping mapping) {
        this.mapping = Objects.requireNonNull(mapping, "mapping");
    }

    @Override
    public Set<String> getBindingNames() {
        return this.mapping.getVariableNames();
    }

    @Override
    public boolean hasBinding(String name) {
        return this.mapping.getValue(name) != null;
    }

    @Override
    public Value getValue(String name) {
        return this.mapping.getValue(name) instanceof Value value ? value : null;
    }

    @Override
    public Iterator<Binding> iterator() {
        return this.mapping.getMap().entrySet().stream()
                .map(CoreseBindingSet::toBinding)
                .iterator();
    }

    private static Binding toBinding(
            Map.Entry<String, DatatypeValue> entry) {
        if (entry.getValue() instanceof Value value) {
            return new CoreseBinding(entry.getKey(), value);
        }
        throw new IllegalStateException(
                "Query binding is not backed by an RDF value: " + entry.getKey());
    }
}
