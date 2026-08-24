package fr.inria.corese.core.next.query.impl.query;

import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.query.api.Operation;
import fr.inria.corese.core.next.query.api.QueryLanguage;
import fr.inria.corese.core.next.query.api.dataset.Dataset;
import fr.inria.corese.core.next.query.api.result.Binding;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.impl.result.CoreseBinding;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Base class for prepared SPARQL operations.
 *
 * <p>Manages initial bindings, dataset override, inference flag, and timeout.
 * Subclasses implement query-type-specific evaluation.</p>
 */
abstract class AbstractCoreseOperation implements Operation {

    private final String queryString;
    private final QueryLanguage language;
    private final LinkedHashMap<String, Value> bindings = new LinkedHashMap<>();
    private Dataset dataset;
    private boolean includeInferred = true;
    private int maxExecutionTime = 0;

    protected AbstractCoreseOperation(String queryString, QueryLanguage language) {
        this.queryString = queryString;
        this.language = language;
    }

    public String getQueryString() {
        return queryString;
    }

    public QueryLanguage getLanguage() {
        return language;
    }

    @Override
    public Operation setBinding(String name, Value value) {
        bindings.put(name, value);
        return this;
    }

    @Override
    public Operation removeBinding(String name) {
        bindings.remove(name);
        return this;
    }

    @Override
    public Operation clearBindings() {
        bindings.clear();
        return this;
    }

    @Override
    public BindingSet getBindings() {
        return new MapBackedBindingSet(Map.copyOf(bindings));
    }

    @Override
    public Operation setDataset(Dataset dataset) {
        this.dataset = dataset;
        return this;
    }

    @Override
    public Dataset getDataset() {
        return dataset;
    }

    @Override
    public Operation setIncludeInferred(boolean includeInferred) {
        this.includeInferred = includeInferred;
        return this;
    }

    @Override
    public boolean isIncludeInferred() {
        return includeInferred;
    }

    @Override
    public Operation setMaxExecutionTime(int maxExecutionTimeSeconds) {
        this.maxExecutionTime = maxExecutionTimeSeconds;
        return this;
    }

    @Override
    public int getMaxExecutionTime() {
        return maxExecutionTime;
    }

    private record MapBackedBindingSet(Map<String, Value> map) implements BindingSet {

        @Override
            public Set<String> getBindingNames() {
                return Collections.unmodifiableSet(map.keySet());
            }

            @Override
            public boolean hasBinding(String name) {
                return map.containsKey(name);
            }

            @Override
            public Value getValue(String name) {
                return map.get(name);
            }

            @Override
            @SuppressWarnings("NullableProblems")
            public Iterator<Binding> iterator() {
                return map.entrySet().stream()
                        .<Binding>map(e -> new CoreseBinding(e.getKey(), e.getValue()))
                        .iterator();
            }
        }
}
