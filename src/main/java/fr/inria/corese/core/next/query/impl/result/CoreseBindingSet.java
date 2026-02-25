package fr.inria.corese.core.next.query.impl.result;

import fr.inria.corese.core.next.data.api.Value;
import fr.inria.corese.core.next.data.impl.temp.CoreseValueConverter;
import fr.inria.corese.core.next.query.api.result.Binding;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.kgram.core.Mapping;

import java.util.Iterator;
import java.util.Set;

/**
 * Wrapper around Mapping
 */
public class CoreseBindingSet implements BindingSet {

    private final Mapping mapping;
    private static final CoreseValueConverter converter = new CoreseValueConverter();

    public CoreseBindingSet(Mapping mapping) {
        this.mapping = mapping;
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
            return converter.toRdf4jValue(this.mapping.getValue(name));
        }
        return null;
    }

    @Override
    public Iterator<Binding> iterator() {
        return this.mapping.getMap().entrySet().stream().map((entry) -> (Binding) new CoreseBinding(entry.getKey(), converter.toRdf4jValue(entry.getValue()))).iterator();
    }
}
