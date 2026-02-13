package fr.inria.corese.core.next.query.impl.sparql.io.serializer;

import fr.inria.corese.core.next.query.api.base.io.ResultFormat;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializer;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultSerializerFactoryTest {

    private ResultSerializerFactory factory = new ResultSerializerFactory();
    private TupleQueryResult dummyResults = new TupleQueryResult() {
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

        }
    };

    @Test
    public void createSerializer() {
        ResultFormat.all().forEach(resultFormat -> {
            ResultSerializer serializer = factory.createSerializer(resultFormat, dummyResults);
            assertEquals(resultFormat, serializer.getFormat());
        });
    }
}