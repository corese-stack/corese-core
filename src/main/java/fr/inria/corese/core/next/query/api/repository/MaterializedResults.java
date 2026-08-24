package fr.inria.corese.core.next.query.api.repository;

import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.query.api.result.Binding;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.api.result.GraphQueryResult;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Package-private materialized result implementations used by repository shortcuts. */
final class MaterializedResults {

    private MaterializedResults() {
    }

    static TupleQueryResult tuple(List<String> bindingNames, List<BindingSet> rows) {
        List<BindingSet> snapshots = rows.stream()
                .map(MaterializedResults::snapshot)
                .toList();
        return new TupleResult(List.copyOf(bindingNames), snapshots.iterator());
    }

    static GraphQueryResult graph(List<Statement> statements) {
        return new GraphResult(List.copyOf(statements).iterator());
    }

    private static BindingSet snapshot(BindingSet source) {
        Map<String, Value> values = new LinkedHashMap<>();
        for (String name : source.getBindingNames()) {
            Value value = source.getValue(name);
            if (value != null) {
                values.put(name, value);
            }
        }
        return new ImmutableBindingSet(values);
    }

    private static final class TupleResult implements TupleQueryResult {
        private final List<String> bindingNames;
        private final Iterator<BindingSet> rows;
        private boolean closed;

        private TupleResult(List<String> bindingNames, Iterator<BindingSet> rows) {
            this.bindingNames = bindingNames;
            this.rows = rows;
        }

        @Override
        public List<String> getBindingNames() {
            return bindingNames;
        }

        @Override
        public boolean hasNext() {
            checkOpen();
            return rows.hasNext();
        }

        @Override
        public BindingSet next() {
            checkOpen();
            return rows.next();
        }

        @Override
        public void close() {
            closed = true;
        }

        private void checkOpen() {
            if (closed) {
                throw new IllegalStateException("This tuple query result is closed.");
            }
        }
    }

    private static final class GraphResult implements GraphQueryResult {
        private final Iterator<Statement> statements;
        private boolean closed;

        private GraphResult(Iterator<Statement> statements) {
            this.statements = statements;
        }

        @Override
        public boolean hasNext() {
            checkOpen();
            return statements.hasNext();
        }

        @Override
        public Statement next() {
            checkOpen();
            return statements.next();
        }

        @Override
        public void close() {
            closed = true;
        }

        private void checkOpen() {
            if (closed) {
                throw new IllegalStateException("This graph query result is closed.");
            }
        }
    }

    private record ImmutableBindingSet(Map<String, Value> values) implements BindingSet {

        private ImmutableBindingSet {
            values = Map.copyOf(values);
        }

        @Override
        public Set<String> getBindingNames() {
            return values.keySet();
        }

        @Override
        public boolean hasBinding(String name) {
            return values.containsKey(name);
        }

        @Override
        public Value getValue(String name) {
            return values.get(name);
        }

        @Override
        public Iterator<Binding> iterator() {
            return values.entrySet().stream()
                    .<Binding>map(entry -> new ImmutableBinding(entry.getKey(), entry.getValue()))
                    .iterator();
        }
    }

    private record ImmutableBinding(String name, Value value) implements Binding {
    }
}
