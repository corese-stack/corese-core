package fr.inria.corese.core.next.io;

import fr.inria.corese.core.next.data.Models;
import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.io.option.BaseIRIOptions;
import fr.inria.corese.core.next.data.api.io.option.RDFParsingOptions;
import fr.inria.corese.core.next.data.api.io.option.RDFSerializationOptions;
import fr.inria.corese.core.next.data.api.io.parser.RDFParser;
import fr.inria.corese.core.next.data.api.io.parser.RDFParserFactory;
import fr.inria.corese.core.next.data.api.io.serializer.RDFSerializer;
import fr.inria.corese.core.next.data.api.io.serializer.RDFSerializerFactory;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.impl.io.parser.DefaultRDFParserFactory;
import fr.inria.corese.core.next.data.impl.io.serializer.DefaultRDFSerializerFactory;
import fr.inria.corese.core.next.query.api.io.ResultFormat;
import fr.inria.corese.core.next.query.api.io.serializer.BooleanResultSerializer;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializer;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializerFactory;
import fr.inria.corese.core.next.query.api.io.serializer.ResultIOOptions;
import fr.inria.corese.core.next.query.api.repository.Repository;
import fr.inria.corese.core.next.query.api.repository.RepositoryConnection;
import fr.inria.corese.core.next.query.api.result.GraphQueryResult;
import fr.inria.corese.core.next.query.api.result.StatementResult;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.impl.sparql.io.serializer.DefaultResultSerializerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Public entry point for RDF and SPARQL result I/O.
 *
 * <p>The short {@code read}, {@code write}, and {@code writeToString} methods
 * cover the common path. Factory accessors and {@code serializer} methods keep
 * the complete configurable API available without exposing implementation
 * packages.</p>
 *
 * <p>Caller-owned readers, input streams, writers, and query results are never
 * closed. Serialization consumes query results once. JSON, XML, CSV, TSV,
 * N-Triples, and N-Quads are written progressively. Graph-oriented RDF formats
 * may materialize a one-shot statement source because they require global graph
 * analysis.</p>
 */
public final class CoreseIO {

    private static final ValueFactory VALUE_FACTORY = Values.factory();
    private static final RDFParserFactory RDF_PARSERS = new DefaultRDFParserFactory();
    private static final RDFSerializerFactory RDF_SERIALIZERS = new DefaultRDFSerializerFactory();
    private static final ResultSerializerFactory RESULT_SERIALIZERS = new DefaultResultSerializerFactory();

    private CoreseIO() {
    }

    /** Returns the advanced RDF parser factory. */
    public static RDFParserFactory rdfParserFactory() {
        return RDF_PARSERS;
    }

    /** Returns the advanced RDF serializer factory. */
    public static RDFSerializerFactory rdfSerializerFactory() {
        return RDF_SERIALIZERS;
    }

    /** Returns the advanced SPARQL result serializer factory. */
    public static ResultSerializerFactory resultSerializerFactory() {
        return RESULT_SERIALIZERS;
    }

    /** Reads RDF into a new in-memory model. */
    public static Model read(Reader source, RDFFormat format) {
        Model target = Models.create();
        return read(source, format, target, VALUE_FACTORY);
    }

    /** Reads configured RDF into a new in-memory model. */
    public static Model read(Reader source, RDFFormat format, RDFParsingOptions options) {
        Model target = Models.create();
        return read(source, format, target, VALUE_FACTORY, options);
    }

    /** Reads RDF into a new in-memory model. */
    public static Model read(InputStream source, RDFFormat format) {
        Model target = Models.create();
        return read(source, format, target, VALUE_FACTORY);
    }

    /** Reads configured RDF into a new in-memory model. */
    public static Model read(InputStream source, RDFFormat format, RDFParsingOptions options) {
        Model target = Models.create();
        return read(source, format, target, VALUE_FACTORY, options);
    }

    /**
     * Reads UTF-8 RDF from a file into a new in-memory model, using the file
     * URI as the base IRI.
     */
    public static Model read(Path source, RDFFormat format) {
        Model target = Models.create();
        RDF_PARSERS.createRDFParser(format, target, VALUE_FACTORY).parse(source);
        return target;
    }

    /**
     * Reads configured UTF-8 RDF from a file into a new in-memory model. An
     * explicit {@link BaseIRIOptions} value takes precedence over the file URI.
     */
    public static Model read(Path source, RDFFormat format, RDFParsingOptions options) {
        Model target = Models.create();
        RDFParser parser = RDF_PARSERS.createRDFParser(format, target, VALUE_FACTORY, options);
        if (options instanceof BaseIRIOptions baseOptions && baseOptions.getBaseIRI() != null) {
            parser.parse(source, baseOptions.getBaseIRI());
        } else {
            parser.parse(source);
        }
        return target;
    }

    private static final String PARAM_SOURCE = "source";

    /** Reads RDF into an existing model and returns that model. */
    public static Model read(
            Reader source,
            RDFFormat format,
            Model target,
            ValueFactory valueFactory) {
        Objects.requireNonNull(source, PARAM_SOURCE);
        RDF_PARSERS.createRDFParser(format, target, valueFactory).parse(source);
        return target;
    }

    /** Reads configured RDF into an existing model and returns that model. */
    public static Model read(
            Reader source,
            RDFFormat format,
            Model target,
            ValueFactory valueFactory,
            RDFParsingOptions options) {
        Objects.requireNonNull(source, PARAM_SOURCE);
        RDF_PARSERS.createRDFParser(format, target, valueFactory, options).parse(source);
        return target;
    }

    /** Reads RDF into an existing model and returns that model. */
    public static Model read(
            InputStream source,
            RDFFormat format,
            Model target,
            ValueFactory valueFactory) {
        Objects.requireNonNull(source, PARAM_SOURCE);
        RDF_PARSERS.createRDFParser(format, target, valueFactory).parse(source);
        return target;
    }

    /** Reads configured RDF into an existing model and returns that model. */
    public static Model read(
            InputStream source,
            RDFFormat format,
            Model target,
            ValueFactory valueFactory,
            RDFParsingOptions options) {
        Objects.requireNonNull(source, PARAM_SOURCE);
        RDF_PARSERS.createRDFParser(format, target, valueFactory, options).parse(source);
        return target;
    }

    public static RDFSerializer serializer(Model source, RDFFormat format) {
        return RDF_SERIALIZERS.createSerializer(format, source);
    }

    public static RDFSerializer serializer(
            Model source,
            RDFFormat format,
            RDFSerializationOptions options) {
        return RDF_SERIALIZERS.createSerializer(format, source, options);
    }

    public static RDFSerializer serializer(Iterable<Statement> source, RDFFormat format) {
        return RDF_SERIALIZERS.createSerializer(format, source);
    }

    public static RDFSerializer serializer(StatementResult source, RDFFormat format) {
        return RDF_SERIALIZERS.createSerializer(format, source);
    }

    public static RDFSerializer serializer(
            Iterable<Statement> source,
            RDFFormat format,
            RDFSerializationOptions options) {
        return RDF_SERIALIZERS.createSerializer(format, source, options);
    }

    public static RDFSerializer serializer(
            StatementResult source,
            RDFFormat format,
            RDFSerializationOptions options) {
        return RDF_SERIALIZERS.createSerializer(format, source, options);
    }

    public static ResultSerializer serializer(TupleQueryResult source, ResultFormat format) {
        return RESULT_SERIALIZERS.createTupleSerializer(format, source);
    }

    public static ResultSerializer serializer(
            TupleQueryResult source,
            ResultFormat format,
            ResultIOOptions options) {
        return RESULT_SERIALIZERS.createTupleSerializer(format, source, options);
    }

    public static BooleanResultSerializer serializer(boolean source, ResultFormat format) {
        return RESULT_SERIALIZERS.createBooleanSerializer(format, source);
    }

    public static BooleanResultSerializer serializer(
            boolean source,
            ResultFormat format,
            ResultIOOptions options) {
        return RESULT_SERIALIZERS.createBooleanSerializer(format, source, options);
    }

    public static void write(Model source, RDFFormat format, Writer destination) {
        serializer(source, format).write(destination);
    }

    public static void write(
            Model source,
            RDFFormat format,
            RDFSerializationOptions options,
            Writer destination) {
        serializer(source, format, options).write(destination);
    }

    public static void write(StatementResult source, RDFFormat format, Writer destination) {
        serializer(source, format).write(destination);
    }

    public static void write(
            StatementResult source,
            RDFFormat format,
            RDFSerializationOptions options,
            Writer destination) {
        serializer(source, format, options).write(destination);
    }

    public static void write(TupleQueryResult source, ResultFormat format, Writer destination) {
        serializer(source, format).write(destination);
    }

    public static void write(
            TupleQueryResult source,
            ResultFormat format,
            ResultIOOptions options,
            Writer destination) {
        serializer(source, format, options).write(destination);
    }

    public static void write(boolean source, ResultFormat format, Writer destination) {
        serializer(source, format).write(destination);
    }

    public static void write(Model source, RDFFormat format, OutputStream destination) {
        serializer(source, format).write(destination);
    }

    public static void write(StatementResult source, RDFFormat format, OutputStream destination) {
        serializer(source, format).write(destination);
    }

    public static void write(TupleQueryResult source, ResultFormat format, OutputStream destination) {
        serializer(source, format).write(destination);
    }

    public static void write(boolean source, ResultFormat format, OutputStream destination) {
        serializer(source, format).write(destination);
    }

    public static void write(Model source, RDFFormat format, Path destination) {
        serializer(source, format).write(destination);
    }

    public static void write(StatementResult source, RDFFormat format, Path destination) {
        serializer(source, format).write(destination);
    }

    public static void write(TupleQueryResult source, ResultFormat format, Path destination) {
        serializer(source, format).write(destination);
    }

    public static void write(boolean source, ResultFormat format, Path destination) {
        serializer(source, format).write(destination);
    }

    public static void write(
            boolean source,
            ResultFormat format,
            ResultIOOptions options,
            Writer destination) {
        serializer(source, format, options).write(destination);
    }

    public static String writeToString(Model source, RDFFormat format) {
        return serializer(source, format).writeToString();
    }

    public static String writeToString(
            Model source,
            RDFFormat format,
            RDFSerializationOptions options) {
        return serializer(source, format, options).writeToString();
    }

    public static String writeToString(StatementResult source, RDFFormat format) {
        return serializer(source, format).writeToString();
    }

    public static String writeToString(
            StatementResult source,
            RDFFormat format,
            RDFSerializationOptions options) {
        return serializer(source, format, options).writeToString();
    }

    public static String writeToString(TupleQueryResult source, ResultFormat format) {
        return serializer(source, format).writeToString();
    }

    public static String writeToString(
            TupleQueryResult source,
            ResultFormat format,
            ResultIOOptions options) {
        return serializer(source, format, options).writeToString();
    }

    public static String writeToString(boolean source, ResultFormat format) {
        return serializer(source, format).writeToString();
    }

    public static String writeToString(
            boolean source,
            ResultFormat format,
            ResultIOOptions options) {
        return serializer(source, format, options).writeToString();
    }

    /**
     * Evaluates and progressively writes a SELECT query while owning the
     * connection and result it creates. The destination remains caller-owned.
     */
    public static void writeSelect(
            Repository repository,
            String sparql,
            ResultFormat format,
            Writer destination) {
        try (RepositoryConnection connection = repository.getConnection();
             TupleQueryResult result = connection
                     .prepareTupleQuery(sparql)
                     .evaluate()) {
            write(result, format, destination);
        }
    }

    /** Evaluates and progressively writes a configured SELECT query. */
    public static void writeSelect(
            Repository repository,
            String sparql,
            ResultFormat format,
            ResultIOOptions options,
            Writer destination) {
        try (RepositoryConnection connection = repository.getConnection();
             TupleQueryResult result = connection
                     .prepareTupleQuery(sparql)
                     .evaluate()) {
            write(result, format, options, destination);
        }
    }

    /** Evaluates and writes an ASK query while keeping the destination open. */
    public static void writeAsk(
            Repository repository,
            String sparql,
            ResultFormat format,
            Writer destination) {
        try (RepositoryConnection connection = repository.getConnection()) {
            boolean result = connection
                    .prepareBooleanQuery(sparql)
                    .evaluate();
            write(result, format, destination);
        }
    }

    /** Evaluates and writes a configured ASK query. */
    public static void writeAsk(
            Repository repository,
            String sparql,
            ResultFormat format,
            ResultIOOptions options,
            Writer destination) {
        try (RepositoryConnection connection = repository.getConnection()) {
            boolean result = connection
                    .prepareBooleanQuery(sparql)
                    .evaluate();
            write(result, format, options, destination);
        }
    }

    /** Evaluates and progressively writes a CONSTRUCT or DESCRIBE query. */
    public static void writeGraph(
            Repository repository,
            String sparql,
            RDFFormat format,
            Writer destination) {
        try (RepositoryConnection connection = repository.getConnection();
             GraphQueryResult result = connection
                     .prepareGraphQuery(sparql)
                     .evaluate()) {
            write(result, format, destination);
        }
    }

    /** Evaluates and progressively writes a configured graph query. */
    public static void writeGraph(
            Repository repository,
            String sparql,
            RDFFormat format,
            RDFSerializationOptions options,
            Writer destination) {
        try (RepositoryConnection connection = repository.getConnection();
             GraphQueryResult result = connection
                     .prepareGraphQuery(sparql)
                     .evaluate()) {
            write(result, format, options, destination);
        }
    }

    /** Creates an advanced parser configured for a caller-owned target model. */
    public static RDFParser parser(RDFFormat format, Model target, ValueFactory valueFactory) {
        return RDF_PARSERS.createRDFParser(format, target, valueFactory);
    }

    /** Creates an advanced configured parser for a caller-owned target model. */
    public static RDFParser parser(
            RDFFormat format,
            Model target,
            ValueFactory valueFactory,
            RDFParsingOptions options) {
        return RDF_PARSERS.createRDFParser(format, target, valueFactory, options);
    }
}
