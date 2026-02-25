package fr.inria.corese.core.next.query.impl.sparql.io.serializer.common;

import fr.inria.corese.core.next.data.api.base.io.FileFormat;
import fr.inria.corese.core.next.data.api.io.IOOptions;
import fr.inria.corese.core.next.data.impl.exception.SerializationException;
import fr.inria.corese.core.next.query.api.io.serializer.BooleanResultSerializer;

import java.io.IOException;
import java.io.Writer;

/**
 * Serializer class for boolean results (ASK results) for textual formats such as CSV and TSV.
 */
public class BooleanStringSerializer implements BooleanResultSerializer {

    private final boolean result;

    public BooleanStringSerializer(boolean result) {
        this.result = result;
    }

    BooleanStringSerializer(boolean result, IOOptions options) {
        this.result = result;
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
    public FileFormat getFormat() {
        return FileFormat.PLAIN_TEXT;
    }
}
