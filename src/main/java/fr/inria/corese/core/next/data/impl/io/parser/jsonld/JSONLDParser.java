package fr.inria.corese.core.next.data.impl.io.parser.jsonld;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.processor.ToRdfProcessor;
import com.apicatalog.rdf.api.RdfQuadConsumer;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.support.io.parser.AbstractRDFParser;
import fr.inria.corese.core.next.data.api.io.option.RDFParsingOptions;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.impl.namespace.PrefixHandler;
import fr.inria.corese.core.next.data.api.support.term.IRIUtils;
import fr.inria.corese.core.next.data.api.exception.ParsingException;
import fr.inria.corese.core.next.data.impl.io.jsonld.JSONLDOptions;
import fr.inria.corese.core.next.data.impl.io.parser.DefaultRDFParserFactory;

import java.io.InputStream;
import java.io.Reader;
import java.net.URI;

/**
 * Parser for JSON-LD RDF files. This parser is based on the Titanium JSON-LD library.
 *
 * @see DefaultRDFParserFactory
 * @see <a href="https://github.com/filip26/titanium-json-ld">Titanium JSON-LD</a>
 */
public class JSONLDParser extends AbstractRDFParser {

    private static final String JSONLD_JAVA_DEFAULT_GRAPH = "@default";

    /**
     * Prefix handler for managing namespace prefixes.
     */
    private final PrefixHandler prefixHandler;

    /**
     * Constructor for JSONLDParser that initializes the model and value factory.
     *
     * @param model   the model to be populated by the parser
     * @param factory the value factory used to create RDF values
     */
    public JSONLDParser(Model model, ValueFactory factory) {
        this(model, factory, new JSONLDOptions.Builder().build());
    }

    /**
     * Constructor for JSONLDParser that initializes the model, value factory, and configuration options.
     *
     * @param model   the model to be populated by the parser
     * @param factory the value factory used to create RDF values
     * @param config  optional configuration options for the parser
     */
    public JSONLDParser(Model model, ValueFactory factory, RDFParsingOptions config) {
        super(model, factory, config);
        this.prefixHandler = new PrefixHandler(true);
    }

    @Override
    public RDFFormat getRDFFormat() {
        return RDFFormat.JSONLD;
    }

    /**
     * Returns the prefix handler containing namespace prefixes discovered during parsing.
     *
     * @return the PrefixHandler instance
     */
    public PrefixHandler getPrefixHandler() {
        return prefixHandler;
    }

    /**
     * Parse the given input stream as JSON-LD.
     * If baseURI is null, the base URI defined in the option for this parser will be used.
     * @param in      The InputStream to read RDF data from.
     * @param baseURI The base URI for resolving relative URIs in the RDF data.
     */
    @Override
    public void parse(InputStream in, String baseURI) {
            try {
                parseJSONLDDocument(JsonDocument.of(in), baseURI);
            } catch (JsonLdError e) {
                throw new ParsingException(e);
            }
    }

    /**
     * Parse the given reader as JSON-LD.
     * If baseURI is null, the base URI defined in the option for this parser will be used.
     * @param reader  The Reader to read RDF data from.
     * @param baseURI The base URI for resolving relative URIs in the RDF data.
     */
    @Override
    public void parse(Reader reader, String baseURI) {
        try {
            parseJSONLDDocument(JsonDocument.of(reader), baseURI);
        } catch (JsonLdError e) {
            throw new ParsingException(e);
        }
    }

    private void parseJSONLDDocument(Document document, String baseURI) {
        try {
            JsonLdOptions options = getConfig() instanceof JSONLDOptions jsonldOptions
                    ? jsonldOptions.getJsonLdOptions()
                    : new JsonLdOptions();
            if(baseURI != null && !baseURI.isEmpty()) {
                options.setBase(URI.create(baseURI));
            }
            RdfQuadConsumer consumer = getConsumer();

            ToRdfProcessor.toRdf(consumer, document, options);
        } catch (JsonLdError e) {
            throw new ParsingException(e);
        }
    }

    /**
     * Returns a consumer that will handle the RDF quads parsed from the JSON-LD document.
     * This consumer will create statements in the model using the value factory.
     *
     * @return a RdfQuadConsumer that processes RDF quads
     */
    private RdfQuadConsumer getConsumer() {
        return new RdfQuadConsumer() {
            @Override
            public RdfQuadConsumer quad(String subject, String predicate, String object, String datatype, String language, String direction, String graph) {
                Resource subjResource = createSubject(subject);
                IRI predicateIRI = getValueFactory().createIRI(predicate);
                Value objValue = createObject(object, datatype, language, direction);
                Resource graphResource = createGraph(graph);
                Statement statement = graphResource == null
                        ? getValueFactory().createStatement(subjResource, predicateIRI, objValue)
                        : getValueFactory().createStatement(subjResource, predicateIRI, objValue, graphResource);
                getModel().add(statement);

                return this;
            }
        };
    }

    private Resource createSubject(String subject) {
        return RdfQuadConsumer.isBlank(subject)
                ? getValueFactory().createBNode(subject)
                : getValueFactory().createIRI(subject);
    }

    private Value createObject(String object, String datatype, String language, String direction) {
        if (RdfQuadConsumer.isBlank(object)) {
            return getValueFactory().createBNode(object);
        }
        if (RdfQuadConsumer.isLiteral(datatype, language, direction)) {
            return createLiteral(object, datatype, language, direction);
        }
        if (IRIUtils.isStandardIRI(object)) {
            return getValueFactory().createIRI(object);
        }
        throw new ParsingException("Invalid object: " + object);
    }

    private Value createLiteral(String value, String datatype, String language, String direction) {
        if (RdfQuadConsumer.isLangString(datatype, language, direction)) {
            return getValueFactory().createLiteral(value, language);
        }
        if (!XSDDatatype.STRING.toString().equals(datatype)) {
            return getValueFactory().createLiteral(value, getValueFactory().createIRI(datatype));
        }
        return getValueFactory().createLiteral(value);
    }

    private Resource createGraph(String graph) {
        if (graph == null) {
            return null;
        }
        if (RdfQuadConsumer.isBlank(graph)) {
            return getValueFactory().createBNode(graph);
        }
        if (!JSONLD_JAVA_DEFAULT_GRAPH.equals(graph) && IRIUtils.isStandardIRI(graph)) {
            return getValueFactory().createIRI(graph);
        }
        throw new ParsingException("Invalid graph: " + graph);
    }
}
