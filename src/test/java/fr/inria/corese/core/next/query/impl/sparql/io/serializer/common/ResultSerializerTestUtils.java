package fr.inria.corese.core.next.query.impl.sparql.io.serializer.common;

import fr.inria.corese.core.next.data.api.Value;
import fr.inria.corese.core.next.query.api.result.Binding;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ResultSerializerTestUtils {

    public static final class MockQueryResults implements TupleQueryResult {
        private List<MockBindingSet> innerData;
        private Iterator<MockBindingSet> innerIterator;
        private List<String> bindingsNames;

        public MockQueryResults(List<String> bindingsNames, List<Map<String, Value>> bindings) {
            this.innerData = bindings.stream().map(MockBindingSet::new).toList();

            this.innerIterator = this.innerData.iterator();
            this.bindingsNames = bindingsNames;
        }

        @Override
        public List<String> getBindingNames() {
            return this.bindingsNames;
        }

        @Override
        public boolean hasNext() {
            return this.innerIterator.hasNext();
        }

        @Override
        public BindingSet next() {
            return this.innerIterator.next();
        }

        @Override
        public void close() {

        }

    }

    protected record MockBindingSet(Map<String, Value> values) implements BindingSet {

        @Override
        public Set<String> getBindingNames() {
            return this.values.keySet();
        }

        @Override
        public boolean hasBinding(String name) {
            return this.values.containsKey(name);
        }

        @Override
        public Value getValue(String name) {
            return this.values.get(name);
        }

        @Override
        public Iterator<Binding> iterator() {
            return this.values.entrySet().stream().map(entry -> (Binding) new MockBinding(entry.getKey(), entry.getValue())).iterator();
        }
    }

    protected record MockBinding(String name, Value value) implements Binding {
    }
}
