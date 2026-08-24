package fr.inria.corese.core.next.query.api.result;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import fr.inria.corese.core.next.data.api.model.Statement;

class QueryResultIteratorTest {

    @Test
    void tupleIteratorRejectsIterationBeyondTheEnd() {
        try (TupleQueryResult result = new EmptyTupleQueryResult()) {
            Iterator<BindingSet> iterator = result.iterator();

            assertThrows(NoSuchElementException.class, iterator::next);
        }
    }

    @Test
    void graphIteratorRejectsIterationBeyondTheEnd() {
        try (GraphQueryResult result = new EmptyGraphQueryResult()) {
            Iterator<Statement> iterator = result.iterator();

            assertThrows(NoSuchElementException.class, iterator::next);
        }
    }

    private static final class EmptyTupleQueryResult implements TupleQueryResult {

        @Override
        public List<String> getBindingNames() {
            return List.of();
        }

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public BindingSet next() {
            return null;
        }

        @Override
        public void close() {
            // This in-memory test double owns no resources.
        }
    }

    private static final class EmptyGraphQueryResult implements GraphQueryResult {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Statement next() {
            return null;
        }

        @Override
        public void close() {
            // This in-memory test double owns no resources.
        }
    }
}
