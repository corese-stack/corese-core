package fr.inria.corese.core.next.query.impl.sparql.io.serializer;

import fr.inria.corese.core.next.query.api.io.ResultFormat;
import fr.inria.corese.core.next.query.api.io.serializer.BooleanResultSerializer;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializer;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultResultSerializerFactoryTest {

    private final DefaultResultSerializerFactory factory = new DefaultResultSerializerFactory();
    private final TupleQueryResult dummyResults = new TupleQueryResult() {
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
    void createsTupleSerializerForEveryStandardFormat() {
        ResultFormat.all().forEach(resultFormat -> {
            ResultSerializer serializer = factory.createTupleSerializer(resultFormat, dummyResults);
            assertEquals(resultFormat, serializer.getFormat());
        });
    }

    @Test
    void createsBooleanSerializerWithTheRequestedFormat() {
        ResultFormat.all().forEach(resultFormat -> {
            BooleanResultSerializer serializer = factory.createBooleanSerializer(resultFormat, true);
            assertEquals(resultFormat, serializer.getFormat());
        });
    }

    @Test
    void rejectsUnknownTupleResultFormat() {
        ResultFormat custom = new ResultFormat("CUSTOM", List.of("custom"), List.of("application/x-custom"));
        assertThrows(
                IllegalArgumentException.class,
                () -> factory.createTupleSerializer(custom, dummyResults));
    }
}
