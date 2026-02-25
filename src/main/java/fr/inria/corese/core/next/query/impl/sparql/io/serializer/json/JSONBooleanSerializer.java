package fr.inria.corese.core.next.query.impl.sparql.io.serializer.json;

import fr.inria.corese.core.next.data.api.base.io.FileFormat;
import fr.inria.corese.core.next.data.api.io.IOOptions;
import fr.inria.corese.core.next.data.impl.exception.SerializationException;
import fr.inria.corese.core.next.query.api.base.io.ResultFormat;
import fr.inria.corese.core.next.query.api.io.serializer.BooleanResultSerializer;
import fr.inria.corese.core.next.query.api.io.serializer.LinksOptions;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonWriter;

import java.io.Writer;

/**
 * Serializer class for boolean results (ASK results) for JSON
 */
public class JSONBooleanSerializer implements BooleanResultSerializer {

    private final boolean result;
    private final IOOptions config;

    public JSONBooleanSerializer(boolean result) {
        this(result, new JSONSerializerOptions.Builder().build());
    }

    public JSONBooleanSerializer(boolean result, IOOptions options) {
        this.config = options;
        this.result = result;
    }

    @Override
    public void write(Writer writer) throws SerializationException {
        JsonObjectBuilder resultBuilder = Json.createObjectBuilder();

        if(this.config instanceof LinksOptions linksOptions && ! linksOptions.links().isEmpty() ) {
            JsonObjectBuilder headerBuilder = Json.createObjectBuilder();
            headerBuilder.add(JSONSerializerConstants.LINK, Json.createArrayBuilder(linksOptions.links()));
            resultBuilder.add(JSONSerializerConstants.HEAD, headerBuilder.build());
        }

        resultBuilder.add(JSONSerializerConstants.BOOLEAN, String.valueOf(this.result));

        JsonWriter jsonWriter = Json.createWriter(writer);
        jsonWriter.writeObject( resultBuilder.build());
        jsonWriter.close();
    }

    @Override
    public FileFormat getFormat() {
        return ResultFormat.JSON;
    }
}
