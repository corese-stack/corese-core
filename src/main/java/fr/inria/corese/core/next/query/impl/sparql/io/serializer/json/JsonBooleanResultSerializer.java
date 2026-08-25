package fr.inria.corese.core.next.query.impl.sparql.io.serializer.json;

import fr.inria.corese.core.next.data.api.exception.SerializationException;
import fr.inria.corese.core.next.data.api.io.format.FileFormat;
import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.query.api.io.ResultFormat;
import fr.inria.corese.core.next.query.api.io.serializer.BooleanResultSerializer;
import fr.inria.corese.core.next.query.api.io.serializer.LinksOptions;
import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.stream.JsonGenerator;

import java.io.Writer;
import java.util.Objects;

/** Streaming serializer for SPARQL boolean results in JSON. */
public class JsonBooleanResultSerializer implements BooleanResultSerializer {

    private final boolean result;
    private final IOOptions options;

    public JsonBooleanResultSerializer(boolean result) {
        this(result, new JsonResultSerializerOptions.Builder().build());
    }

    public JsonBooleanResultSerializer(boolean result, IOOptions options) {
        this.options = Objects.requireNonNull(options, "options");
        this.result = result;
    }

    @Override
    @SuppressWarnings("java:S2095") // JsonGenerator.close() would close the caller-owned Writer
    public void write(Writer writer) throws SerializationException {
        Objects.requireNonNull(writer, "writer");
        try {
            JsonGenerator json = Json.createGenerator(writer);
            json.writeStartObject();
            json.writeStartObject(JsonResultConstants.HEAD);
            if (options instanceof LinksOptions linksOptions && !linksOptions.links().isEmpty()) {
                json.writeStartArray(JsonResultConstants.LINK);
                linksOptions.links().forEach(json::write);
                json.writeEnd();
            }
            json.writeEnd();
            json.write(JsonResultConstants.BOOLEAN, result);
            json.writeEnd();
            json.flush();
        } catch (JsonException | IllegalStateException e) {
            throw new SerializationException("Could not serialize SPARQL boolean result", getFormat(), e);
        }
    }

    @Override
    public FileFormat getFormat() {
        return ResultFormat.JSON;
    }
}
