package fr.inria.corese.core.next.query.impl.sparql.io.serializer.support;

import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.query.api.io.serializer.BooleanResultSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractBooleanResultSerializerTest {

    protected abstract BooleanResultSerializer getSerializer(boolean result);
    protected abstract BooleanResultSerializer getSerializer(boolean result, IOOptions options);

    protected abstract String getTrueResultString();

    @Test
    @DisplayName("Tests the serialization of a query returning True")
    public void trueResultTest() {
        BooleanResultSerializer serializer = getSerializer(true);
        StringWriter resultWriter = new StringWriter();
        serializer.write(resultWriter);
        assertEquals(getTrueResultString(), resultWriter.toString());
    }

    protected abstract String getFalseResultString();

    @Test
    @DisplayName("Tests the serialization of a query returning False")
    public void falseResultTest() {
        BooleanResultSerializer serializer = getSerializer(false);
        StringWriter resultWriter = new StringWriter();
        serializer.write(resultWriter);
        assertEquals(getFalseResultString(), resultWriter.toString());
    }

}
