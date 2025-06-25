package fr.inria.corese.core.next.impl.io.serialization.jsonld;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.serialization.RdfToJsonld;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.io.serialization.FormatSerializer;
import fr.inria.corese.core.next.impl.exception.SerializationException;

import java.io.IOException;
import java.io.Writer;

public class JSONLDSerializer implements FormatSerializer {

    private Model model;
    private JSONLDSerializerConfig config;

    public JSONLDSerializer(Model model, JSONLDSerializerConfig config) {
        this.model = model;
        this.config = config;
    }

    public JSONLDSerializer(Model model) {
        this(model, null);
    }

    @Override
    public void write(Writer writer) throws SerializationException {
        TitaniumRDFDatasetSerializationAdapter adapter = new TitaniumRDFDatasetSerializationAdapter(model);
        try {
            jakarta.json.JsonArray jsonArray = RdfToJsonld.with(adapter).build();
            writer.write(jsonArray.toString());
        } catch (JsonLdError | IOException e) {
            throw new SerializationException("Error during serialization", "JSONLD", e);
        }
    }
}
