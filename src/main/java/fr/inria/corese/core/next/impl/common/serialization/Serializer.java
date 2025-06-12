package fr.inria.corese.core.next.impl.common.serialization;

import fr.inria.corese.core.next.api.FormatSerializer;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.impl.exception.SerializationException;

import java.io.Writer;
import java.util.Objects;

public class Serializer {

    private final Model model;
    private final FormatConfig config;

    public Serializer(Model model) {
        this(model, new FormatConfig.Builder().build());
    }

    public Serializer(Model model, FormatConfig config) {
        this.model = Objects.requireNonNull(model, "Model cannot be null for serialization");
        this.config = Objects.requireNonNull(config, "FormatConfig cannot be null for serialization");
    }

    /**
     * Serializes the RDF model to the given writer in the specified {@link RdfFormat}.
     *
     * @param writer the Writer to write the serialized data to.
     * @param format the {@link RdfFormat} describing the desired serialization format.
     * @throws SerializationException if an error occurs during serialization or if the format is not currently supported by an implementation.
     */
    public void serialize(Writer writer, RdfFormat format) throws SerializationException {
        Objects.requireNonNull(writer, "Writer cannot be null");
        Objects.requireNonNull(format, "RdfFormat cannot be null");

        FormatSerializer formatSerializer;


        if (format.equals(RdfFormats.NTRIPLES)) {
            formatSerializer = new NTriplesFormat(model, config);
        } else if (format.equals(RdfFormats.NQUADS)) {
            formatSerializer = new NQuadsFormat(model, config);
        } else if (format.equals(RdfFormats.TURTLE)) {

            throw new UnsupportedOperationException("Serialization to " + format.getName() + " format is not yet implemented.");
        } else if (format.equals(RdfFormats.JSONLD)) {
            throw new UnsupportedOperationException("Serialization to " + format.getName() + " format is not yet implemented.");
        } else if (format.equals(RdfFormats.RDFXML)) {
            throw new UnsupportedOperationException("Serialization to " + format.getName() + " format is not yet implemented.");
        } else {
            throw new IllegalArgumentException("Unknown or unsupported RdfFormat: " + format.getName());
        }

        formatSerializer.write(writer);
    }
}