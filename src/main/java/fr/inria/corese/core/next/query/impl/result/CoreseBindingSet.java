package fr.inria.corese.core.next.query.impl.result;

import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.impl.adapter.CoreseValueConverter;
import fr.inria.corese.core.next.query.api.result.Binding;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.impl.kgram.core.Mapping;

import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

/**
 * Wrapper around Mapping
 */
public final class CoreseBindingSet implements BindingSet {

    private final Mapping mapping;
    private static final CoreseValueConverter CONVERTER = new CoreseValueConverter();

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
        if(this.hasBinding(name)) {
            return CONVERTER.fromCoreseNode(this.mapping.getValue(name));
        }
        return null;
    }

    @Override
    public Iterator<Binding> iterator() {
        return this.mapping.getMap().entrySet().stream()
                .<Binding>map(entry -> new CoreseBinding(
                        entry.getKey(),
                        CONVERTER.fromCoreseNode(entry.getValue())))
                .iterator();
    }
}
