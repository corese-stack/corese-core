package fr.inria.corese.core.next.query.impl.sparql.io.serializer.support;

import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.query.api.io.ResultFormat;
import fr.inria.corese.core.next.query.api.io.serializer.BooleanResultSerializer;

class BooleanStringSerializerTest extends AbstractBooleanResultSerializerTest {
    @Override
    protected BooleanResultSerializer getSerializer(boolean result) {
        return new BooleanStringSerializer(result, ResultFormat.CSV);
    }

    @Override
    protected BooleanResultSerializer getSerializer(boolean result, IOOptions options) {
        return new BooleanStringSerializer(result, ResultFormat.CSV);
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
