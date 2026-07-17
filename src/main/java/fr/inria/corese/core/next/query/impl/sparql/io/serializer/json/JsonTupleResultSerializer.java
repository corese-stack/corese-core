package fr.inria.corese.core.next.query.impl.sparql.io.serializer.json;

import fr.inria.corese.core.next.data.api.BNode;
import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.Literal;
import fr.inria.corese.core.next.data.api.Value;
import fr.inria.corese.core.next.data.api.base.io.FileFormat;
import fr.inria.corese.core.next.data.api.io.IOOptions;
import fr.inria.corese.core.next.data.impl.common.literal.RDF;
import fr.inria.corese.core.next.data.impl.common.literal.XSD;
import fr.inria.corese.core.next.data.impl.exception.SerializationException;
import fr.inria.corese.core.next.query.api.io.ResultFormat;
import fr.inria.corese.core.next.query.api.io.serializer.LinksOptions;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializer;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import jakarta.json.*;

import java.io.Writer;

/**
 * Serializer for SPARQL results in JSON as described in <a href="https://www.w3.org/TR/2013/REC-sparql11-results-json-20130321/">the W3C recommendation<a/>
 */
public class JsonTupleResultSerializer  implements ResultSerializer {

    private final TupleQueryResult results;
    private final IOOptions config;

    public JsonTupleResultSerializer(TupleQueryResult results, IOOptions options) {
        this.config = options;
        this.results = results;
    }

    public JsonTupleResultSerializer(TupleQueryResult results) {
        this(results, new JsonResultSerializerOptions.Builder().build());
    }

    @Override
    public void write(Writer writer) throws SerializationException {
        JsonObjectBuilder resultBuilder = Json.createObjectBuilder();

        // header
        JsonObjectBuilder headerbuilder = Json.createObjectBuilder()
                .add(JsonResultConstants.VARS, Json.createArrayBuilder(this.results.getBindingNames()));
        if(this.config instanceof LinksOptions linksOptions && ! linksOptions.links().isEmpty() ) {
            headerbuilder.add(JsonResultConstants.LINK, Json.createArrayBuilder(linksOptions.links()));
        }
        resultBuilder.add(JsonResultConstants.HEAD, headerbuilder.build());

        // Result bindings
        resultBuilder.add(JsonResultConstants.RESULTS, Json.createObjectBuilder()
                .add(JsonResultConstants.BINDINGS, Json.createArrayBuilder(this.results.stream().map(this::bindingSetToJson).toList()))
        );

        JsonWriter jsonWriter = Json.createWriter(writer);
        jsonWriter.writeObject( resultBuilder.build());
        jsonWriter.close();
    }

    @Override
    public FileFormat getFormat() {
        return ResultFormat.JSON;
    }

    private JsonObject bindingSetToJson(BindingSet bindings) {
        JsonObjectBuilder bindingSetJsonBuilder = Json.createObjectBuilder();

        this.results.getBindingNames().forEach(bindingName -> {
            if(bindings.hasBinding(bindingName)) {
                bindingSetJsonBuilder.add(bindingName, valueToJson(bindings.getValue(bindingName)));
            }
        });

        return bindingSetJsonBuilder.build();
    }

    private JsonObject valueToJson(Value value) {
        if(value instanceof IRI iriValue) {
            return Json.createObjectBuilder()
                    .add(JsonResultConstants.TYPE, JsonResultConstants.URI)
                    .add(JsonResultConstants.VALUE, iriValue.stringValue())
                .build();
        } else if (value instanceof BNode bnodeValue) {
            return Json.createObjectBuilder()
                    .add(JsonResultConstants.TYPE, JsonResultConstants.BNODE)
                    .add(JsonResultConstants.VALUE, bnodeValue.getID())
                    .build();
        } else if (value instanceof Literal literalValue) {
            JsonObjectBuilder literalBuilder = Json.createObjectBuilder();
            if (literalValue.getLanguage().isEmpty()
                    && literalValue.getDatatype() != null
                    && literalValue.getDatatype() != XSD.STRING.getIRI()
                    && literalValue.getDatatype() != RDF.LANGSTRING.getIRI()){
                literalBuilder.add(JsonResultConstants.DATATYPE, literalValue.getDatatype().stringValue());
            }
            String literalStringValue = literalValue.stringValue();
            literalBuilder.add(JsonResultConstants.TYPE, JsonResultConstants.LITERAL)
                    .add(JsonResultConstants.VALUE, literalStringValue);
            if(literalValue.getLanguage().isPresent()) {
                literalBuilder.add(JsonResultConstants.LANG, literalValue.getLanguage().get());
            }
            return literalBuilder.build();
        }
        throw new SerializationException("Could not serialize Value " + value.stringValue(), this.getFormat());
    }
}
