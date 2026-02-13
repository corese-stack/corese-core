package fr.inria.corese.core.next.query.impl.sparql.io.serializer.json;

import fr.inria.corese.core.next.data.api.BNode;
import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.Literal;
import fr.inria.corese.core.next.data.api.Value;
import fr.inria.corese.core.next.data.api.base.io.AbstractIOOptions;
import fr.inria.corese.core.next.data.api.base.io.FileFormat;
import fr.inria.corese.core.next.data.api.io.IOOptions;
import fr.inria.corese.core.next.data.impl.common.literal.RDF;
import fr.inria.corese.core.next.data.impl.common.literal.XSD;
import fr.inria.corese.core.next.data.impl.exception.SerializationException;
import fr.inria.corese.core.next.data.impl.io.serialization.util.SerializationConstants;
import fr.inria.corese.core.next.query.api.base.io.ResultFormat;
import fr.inria.corese.core.next.query.api.base.io.SerializerUtils;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializer;
import fr.inria.corese.core.next.query.api.result.Binding;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import jakarta.json.*;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JSONSerializer  implements ResultSerializer {

    private final TupleQueryResult results;
    private final IOOptions config;

    public JSONSerializer(TupleQueryResult results, IOOptions options) {
        this.config = options;
        this.results = results;
    }

    public JSONSerializer(TupleQueryResult results) {
        this.config = new JSONSerializerOptions.Builder().build();
        this.results = results;
    }

    @Override
    public void write(Writer writer) throws SerializationException {
        JsonObjectBuilder resultBuilder = Json.createObjectBuilder();

        // header
        JsonObjectBuilder headerbuilder = Json.createObjectBuilder()
                .add(JSONSerializerConstants.VARS, Json.createArrayBuilder(this.results.getBindingNames()));
        if(this.config instanceof JSONSerializerOptions jsonSerializerOptions && ! jsonSerializerOptions.links().isEmpty() ) {
            headerbuilder.add(JSONSerializerConstants.LINK, Json.createArrayBuilder(jsonSerializerOptions.links()));
        }
        resultBuilder.add(JSONSerializerConstants.HEAD, headerbuilder.build());

        // Result bindings
        resultBuilder.add(JSONSerializerConstants.RESULTS, Json.createObjectBuilder()
                .add(JSONSerializerConstants.BINDINGS, Json.createArrayBuilder(this.results.stream().map(this::bindingSetToJson).toList()))
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
                    .add(JSONSerializerConstants.TYPE, JSONSerializerConstants.URI)
                    .add(JSONSerializerConstants.VALUE, iriValue.stringValue())
                .build();
        } else if (value instanceof BNode bnodeValue) {
            return Json.createObjectBuilder()
                    .add(JSONSerializerConstants.TYPE, JSONSerializerConstants.BNODE)
                    .add(JSONSerializerConstants.VALUE, bnodeValue.getID())
                    .build();
        } else if (value instanceof Literal literalValue) {
            JsonObjectBuilder literalBuilder = Json.createObjectBuilder();
            if (literalValue.getLanguage().isEmpty()
                    && literalValue.getDatatype() != null
                    && literalValue.getDatatype() != XSD.STRING.getIRI()
                    && literalValue.getDatatype() != RDF.LANGSTRING.getIRI()){
                literalBuilder.add(JSONSerializerConstants.DATATYPE, literalValue.getDatatype().stringValue());
            }
            String literalStringValue = literalValue.stringValue();
            literalBuilder.add(JSONSerializerConstants.TYPE, JSONSerializerConstants.LITERAL)
                    .add(JSONSerializerConstants.VALUE, literalStringValue);
            if(literalValue.getLanguage().isPresent()) {
                literalBuilder.add(JSONSerializerConstants.LANG, literalValue.getLanguage().get());
            }
            return literalBuilder.build();
        }
        throw new SerializationException("Could not serialize Value " + value.stringValue(), this.getFormat());
    }
}
