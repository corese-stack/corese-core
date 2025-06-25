package fr.inria.corese.core.next.impl.io.serialization.jsonld;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.serialization.RdfToJsonld;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.io.serialization.FormatSerializer;
import fr.inria.corese.core.next.impl.exception.SerializationException;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

/**
 * Serializer for JSON-LD format.
 */
public class JSONLDSerializer implements FormatSerializer {

    private Model model;
    private JSONLDSerializerConfig config;

    public JSONLDSerializer(Model model, JSONLDSerializerConfig config) {
        this.model = Objects.requireNonNull(model);
        this.config = Objects.requireNonNull(config);
    }

    public JSONLDSerializer(Model model) {
        this(model, new JSONLDSerializerConfig());
    }

    @Override
    public void write(Writer writer) throws SerializationException {
        TitaniumRDFDatasetSerializationAdapter adapter = new TitaniumRDFDatasetSerializationAdapter(model);
        try {
            RdfToJsonld builder = RdfToJsonld.with(adapter)
                .ordered(this.config.isOrdered())
                .processingMode(this.config.getVersion())
                .useNativeTypes(this.config.usesNativeTypes())
                .useRdfType(this.config.usesRdfType());
            jakarta.json.JsonArray jsonArray = builder.build();
            writer.write(jsonArray.toString());
        } catch (JsonLdError | IOException e) {
            throw new SerializationException("Error during serialization", "JSONLD", e);
        }
    }
}
