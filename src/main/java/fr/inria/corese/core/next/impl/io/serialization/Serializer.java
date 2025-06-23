package fr.inria.corese.core.next.impl.io.serialization;

import fr.inria.corese.core.next.api.io.serialization.FormatSerializer;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.base.io.RdfFormat;
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
     * Serializes the RDF model to the given writer in the specified {@link fr.inria.corese.core.next.api.base.io.RdfFormat}.
     *
     * @param writer the Writer to write the serialized data to.
     * @param format the {@link fr.inria.corese.core.next.api.base.io.RdfFormat} describing the desired serialization format.
     * @throws SerializationException if an error occurs during serialization or if the format is not currently supported by an implementation.
     */
    public void serialize(Writer writer, fr.inria.corese.core.next.api.base.io.RdfFormat format) throws SerializationException {
        Objects.requireNonNull(writer, "Writer cannot be null");
        Objects.requireNonNull(format, "RdfFormat cannot be null");

        FormatSerializer formatSerializer;


        if (format.equals(fr.inria.corese.core.next.api.base.io.RdfFormat.NTRIPLES)) {
            formatSerializer = new NTriplesFormat(model, config);
        } else if (format.equals( fr.inria.corese.core.next.api.base.io.RdfFormat.NQUADS)) {
            formatSerializer = new NQuadsFormat(model, config);
        } else if (format.equals( fr.inria.corese.core.next.api.base.io.RdfFormat.TURTLE)) {

            throw new UnsupportedOperationException("Serialization to " + format.getName() + " format is not yet implemented.");
        } else if (format.equals( fr.inria.corese.core.next.api.base.io.RdfFormat.JSONLD)) {
            throw new UnsupportedOperationException("Serialization to " + format.getName() + " format is not yet implemented.");
        } else if (format.equals( RdfFormat.RDFXML)) {
            throw new UnsupportedOperationException("Serialization to " + format.getName() + " format is not yet implemented.");
        } else {
            throw new IllegalArgumentException("Unknown or unsupported RdfFormat: " + format.getName());
        }

        formatSerializer.write(writer);
    }
}