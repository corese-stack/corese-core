package fr.inria.corese.core.next.query.impl.sparql.io.serializer.json;

import fr.inria.corese.core.next.query.api.io.serializer.BooleanResultSerializer;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.common.AbstractBooleanResultSerializerTest;

public class JSONBooleanSerializerTest extends AbstractBooleanResultSerializerTest {
    @Override
    protected BooleanResultSerializer getSerializer(boolean result) {
        return new JSONBooleanSerializer(result);
    }

    @Override
    protected String getTrueResultString() {
        return "{\"boolean\":\"true\"}";
    }

    @Override
    protected String getFalseResultString() {
        return "{\"boolean\":\"false\"}";
    }
}
