package fr.inria.corese.core.next.query.impl.sparql.io.serializer.json;

import fr.inria.corese.core.next.data.api.exception.SerializationException;
import fr.inria.corese.core.next.data.api.io.format.FileFormat;
import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.data.api.literal.RDFDatatype;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.term.BNode;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.query.api.io.ResultFormat;
import fr.inria.corese.core.next.query.api.io.serializer.LinksOptions;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializer;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.stream.JsonGenerator;

import java.io.Writer;
import java.util.Objects;

/** Streaming serializer for the SPARQL 1.1 Query Results JSON Format. */
public class JsonTupleResultSerializer implements ResultSerializer {

    private final TupleQueryResult results;
    private final IOOptions options;

    public JsonTupleResultSerializer(TupleQueryResult results, IOOptions options) {
        this.results = Objects.requireNonNull(results, "results");
        this.options = Objects.requireNonNull(options, "options");
    }

    public JsonTupleResultSerializer(TupleQueryResult results) {
        this(results, new JsonResultSerializerOptions.Builder().build());
    }

    @Override
    @SuppressWarnings("java:S2095") // JsonGenerator.close() would close the caller-owned Writer
    public void write(Writer writer) throws SerializationException {
        Objects.requireNonNull(writer, "writer");
        try {
            JsonGenerator json = Json.createGenerator(writer);
            json.writeStartObject();
            writeHead(json);
            json.writeStartObject(JsonResultConstants.RESULTS);
            json.writeStartArray(JsonResultConstants.BINDINGS);
            while (results.hasNext()) {
                writeBindingSet(json, results.next());
            }
            json.writeEnd();
            json.writeEnd();
            json.writeEnd();
            json.flush();
        } catch (JsonException | IllegalStateException e) {
            throw new SerializationException("Could not serialize SPARQL tuple results", getFormat(), e);
        }
    }

    private void writeHead(JsonGenerator json) {
        json.writeStartObject(JsonResultConstants.HEAD);
        json.writeStartArray(JsonResultConstants.VARS);
        results.getBindingNames().forEach(json::write);
        json.writeEnd();
        if (options instanceof LinksOptions linksOptions && !linksOptions.links().isEmpty()) {
            json.writeStartArray(JsonResultConstants.LINK);
            linksOptions.links().forEach(json::write);
            json.writeEnd();
        }
        json.writeEnd();
    }

    private void writeBindingSet(JsonGenerator json, BindingSet bindings) {
        json.writeStartObject();
        for (String bindingName : results.getBindingNames()) {
            if (bindings.hasBinding(bindingName)) {
                writeValue(json, bindingName, bindings.getValue(bindingName));
            }
        }
        json.writeEnd();
    }

    private void writeValue(JsonGenerator json, String bindingName, Value value) {
        json.writeStartObject(bindingName);
        switch (value) {
            case IRI iri -> json.write(JsonResultConstants.TYPE, JsonResultConstants.URI)
                    .write(JsonResultConstants.VALUE, iri.stringValue());
            case BNode bNode -> json.write(JsonResultConstants.TYPE, JsonResultConstants.BNODE)
                    .write(JsonResultConstants.VALUE, bNode.getID());
            case Literal literal -> writeLiteral(json, literal);
            default -> throw new SerializationException(
                    "Could not serialize value " + value.stringValue(), getFormat());
        }
        json.writeEnd();
    }

    private static void writeLiteral(JsonGenerator json, Literal literal) {
        if (literal.getLanguage().isEmpty()
                && literal.getDatatype() != null
                && !literal.getDatatype().equals(XSDDatatype.STRING.getIRI())
                && !literal.getDatatype().equals(RDFDatatype.LANGSTRING.getIRI())) {
            json.write(JsonResultConstants.DATATYPE, literal.getDatatype().stringValue());
        }
        json.write(JsonResultConstants.TYPE, JsonResultConstants.LITERAL)
                .write(JsonResultConstants.VALUE, literal.stringValue());
        literal.getLanguage().ifPresent(language -> json.write(JsonResultConstants.LANG, language));
    }

    @Override
    public FileFormat getFormat() {
        return ResultFormat.JSON;
    }
}
