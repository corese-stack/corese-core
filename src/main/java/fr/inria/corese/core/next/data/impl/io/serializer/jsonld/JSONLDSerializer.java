package fr.inria.corese.core.next.data.impl.io.serializer.jsonld;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.api.FromRdfApi;
import com.apicatalog.jsonld.document.RdfDocument;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.io.serializer.RDFSerializer;
import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.data.api.exception.SerializationException;
import fr.inria.corese.core.next.data.api.io.JSONLDOptions;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

/**
 * Serializer for JSON-LD format. This serializer uses the <a href="https://github.com/filip26/titanium">Titanium</a> library.
 * @see <a href="https://github.com/filip26/titanium">Titanium JSON-LD</a>
 */
public class JSONLDSerializer implements RDFSerializer {

    private final Model model;
    private final IOOptions config;

    /**
     * Constructor.
     * @param model the model to serialize
     * @param config the options for the serialization
     */
    public JSONLDSerializer(Model model, IOOptions config) {
        this.model = Objects.requireNonNull(model);
        this.config = Objects.requireNonNull(config);
    }

    /**
     * Constructor for a JSON-LD with default options.
     */
    public JSONLDSerializer(Model model) {
        this(model, new JSONLDOptions.Builder().build());
    }

    @Override
    public void write(Writer writer) throws SerializationException {
        TitaniumRDFDatasetSerializationAdapter adapter = new TitaniumRDFDatasetSerializationAdapter(model);

        try {
            FromRdfApi fromRdfApi = JsonLd.fromRdf(RdfDocument.of(adapter));
            if(this.config instanceof JSONLDOptions options) {
                fromRdfApi.options(options.getJsonLdOptions());
            }

            jakarta.json.JsonArray jsonArray = fromRdfApi.get();
            writer.write(jsonArray.toString());
        } catch (JsonLdError | IOException e) {
            throw new SerializationException("Error during serialization", "JSONLD", e);
        }
    }

    @Override
    public RDFFormat getFormat() {
        return RDFFormat.JSONLD;
    }
}
