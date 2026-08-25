package fr.inria.corese.core.next.query.impl.query;

import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.query.api.Query;
import fr.inria.corese.core.next.query.api.dataset.Dataset;
import fr.inria.corese.core.next.query.api.result.Binding;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.impl.result.CoreseBinding;

import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Base class for prepared SPARQL queries.
 *
 * <p>Manages initial bindings, dataset override, and timeout.
 * Subclasses implement query-type-specific evaluation.</p>
 */
abstract class AbstractCoreseQuery<T> implements Query<T> {

    private final String queryString;
    private final Runnable executionGuard;
    private final LinkedHashMap<String, Value> bindings = new LinkedHashMap<>();
    private Dataset dataset;
    private Duration timeout = Duration.ZERO;

    protected AbstractCoreseQuery(String queryString, Runnable executionGuard) {
        this.queryString = Objects.requireNonNull(queryString, "queryString");
        this.executionGuard = Objects.requireNonNull(executionGuard, "executionGuard");
    }

    @Override
    public String getQueryString() {
        return queryString;
    }

    @Override
    public Query<T> setBinding(String name, Value value) {
        bindings.put(requireVariableName(name), Objects.requireNonNull(value, "value"));
        return this;
    }

    @Override
    public Query<T> removeBinding(String name) {
        bindings.remove(requireVariableName(name));
        return this;
    }

    @Override
    public Query<T> clearBindings() {
        bindings.clear();
        return this;
    }

    @Override
    public BindingSet getBindings() {
        return new MapBackedBindingSet(Map.copyOf(bindings));
    }

    @Override
    public Query<T> setDataset(Dataset dataset) {
        this.dataset = dataset;
        return this;
    }

    @Override
    public Dataset getDataset() {
        return dataset;
    }

    @Override
    public Query<T> setTimeout(Duration timeout) {
        Duration checkedTimeout = Objects.requireNonNull(timeout, "timeout");
        if (checkedTimeout.isNegative()) {
            throw new IllegalArgumentException("timeout must not be negative");
        }
        this.timeout = checkedTimeout;
        return this;
    }

    @Override
    public Duration getTimeout() {
        return timeout;
    }

    protected long timeoutMillis() {
        if (timeout.isZero()) {
            return 0L;
        }
        long millis;
        try {
            millis = timeout.toMillis();
        } catch (ArithmeticException e) {
            return Long.MAX_VALUE;
        }
        return Math.max(1L, millis);
    }

    protected void checkExecutable() {
        executionGuard.run();
    }

    private String requireVariableName(String name) {
        String checkedName = Objects.requireNonNull(name, "name");
        if (checkedName.isBlank()) {
            throw new IllegalArgumentException("Variable name must not be blank");
        }
        if (checkedName.startsWith("?") || checkedName.startsWith("$")) {
            throw new IllegalArgumentException(
                    "Variable name must not include a leading '?' or '$': " + checkedName);
        }
        return checkedName;
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
