package fr.inria.corese.core.next.query.impl.sparql.io.serializer.common;

import fr.inria.corese.core.next.query.api.io.serializer.BooleanResultSerializer;

public class BooleanStringSerializerTest extends AbstractBooleanResultSerializerTest {
    @Override
    protected BooleanResultSerializer getSerializer(boolean result) {
        return new BooleanStringSerializer(result);
    }

    @Override
    protected String getTrueResultString() {
        return "true";
    }

    @Override
    protected String getFalseResultString() {
        return "false";
    }
}
