package fr.inria.corese.core.next.impl.io.serialization.jsonld;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.api.FromRdfApi;
import com.apicatalog.jsonld.document.RdfDocument;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.io.serialization.RDFSerializer;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.impl.exception.SerializationException;
import fr.inria.corese.core.next.impl.io.serialization.option.TitaniumJSONLDProcessorOption;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

/**
 * Serializer for JSON-LD format.
 */
public class JSONLDSerializer implements RDFSerializer {

    private Model model;
    private IOOptions config;

    /**
     * Constructor.
     * @param model
     * @param config
     */
    public JSONLDSerializer(Model model, IOOptions config) {
        this.model = Objects.requireNonNull(model);
        this.config = Objects.requireNonNull(config);
    }

    public JSONLDSerializer(Model model) {
        this(model, new TitaniumJSONLDProcessorOption.Builder().build());
    }

    @Override
    public void write(Writer writer) throws SerializationException {
        TitaniumRDFDatasetSerializationAdapter adapter = new TitaniumRDFDatasetSerializationAdapter(model);

        try {
            FromRdfApi fromRdfApi = JsonLd.fromRdf(RdfDocument.of(adapter));
            if(this.config instanceof TitaniumJSONLDProcessorOption options) {
                fromRdfApi.options(options.getJsonLdOptions());
            }

            jakarta.json.JsonArray jsonArray = fromRdfApi.get();
            writer.write(jsonArray.toString());
        } catch (JsonLdError | IOException e) {
            throw new SerializationException("Error during serialization", "JSONLD", e);
        }
    }
}
