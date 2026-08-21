package fr.inria.corese.core.next.query.impl.sparql.io.serializer.support;

import fr.inria.corese.core.next.data.api.exception.SerializationException;
import fr.inria.corese.core.next.query.api.io.ResultFormat;
import fr.inria.corese.core.next.query.api.io.serializer.BooleanResultSerializer;

import java.io.IOException;
import java.io.Writer;

/**
 * Serializer class for boolean results (ASK results) for textual formats such as CSV and TSV.
 */
public class BooleanStringSerializer implements BooleanResultSerializer {

    private final boolean result;
    private final ResultFormat format;

    public BooleanStringSerializer(boolean result, ResultFormat format) {
        this.result = result;
        this.format = format;
    }

    @Override
    public void write(Writer writer) throws SerializationException {
        try {
            writer.write(String.valueOf(result));
        } catch (IOException e) {
            throw new SerializationException("Error during boolean result serialization", this.getFormat());
        }
    }

    @Override
    public ResultFormat getFormat() {
        return format;
    }
}
