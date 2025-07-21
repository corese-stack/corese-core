package fr.inria.corese.core.next.impl.io.parser.jsonld;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.processor.ToRdfProcessor;
import com.apicatalog.rdf.api.RdfConsumerException;
import com.apicatalog.rdf.api.RdfQuadConsumer;
import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.base.io.parser.AbstractRDFParser;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.impl.common.util.IRIUtils;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;
import fr.inria.corese.core.next.impl.io.option.TitaniumJSONLDProcessorOption;

import java.io.InputStream;
import java.io.Reader;
import java.net.URI;

/**
 * Parser for JSON-LD RDF files. This parser is based on the Titanium JSON-LD library.
 *
 * @see fr.inria.corese.core.next.impl.io.parser.ParserFactory
 * @see <a href="https://github.com/filip26/titanium-json-ld">Titanium JSON-LD</a>
 */
public class JSONLDParser extends AbstractRDFParser {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JSONLDParser.class);

    private static final String JSONLD_JAVA_DEFAULT_GRAPH = "@default";

    public JSONLDParser(Model model, ValueFactory factory) {
        super(model, factory, new TitaniumJSONLDProcessorOption.Builder().build());
    }

    public JSONLDParser(Model model, ValueFactory factory, IOOptions config) {
        super(model, factory, config);
    }

    @Override
    public RDFFormat getRDFFormat() {
        return RDFFormat.JSONLD;
    }

    /**
     * Parse the given input stream as JSON-LD.
     * If baseURI is null, the base URI defined in the option for this parser will be used.
     * @param in      The InputStream to read RDF data from.
     * @param baseURI The base URI for resolving relative URIs in the RDF data.
     * @throws ParsingErrorException
     */
    @Override
    public void parse(InputStream in, String baseURI) {
            try {
                parseJSONLDDocument(JsonDocument.of(in), baseURI);
            } catch (JsonLdError e) {
                throw new ParsingErrorException(e);
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
            throw new ParsingErrorException(e);
        }
    }

    private void parseJSONLDDocument(Document document, String baseURI) {
        try {
            JsonLdOptions options = new JsonLdOptions();
            if(this.getConfig() instanceof TitaniumJSONLDProcessorOption) {
                options = ((TitaniumJSONLDProcessorOption) this.getConfig()).getJsonLdOptions();
            }
            if(baseURI != null && !baseURI.isEmpty()) {
                options.setBase(URI.create(baseURI));
            }
            RdfQuadConsumer consumer = getConsumer();

            ToRdfProcessor.toRdf(consumer, document, options);
        } catch (JsonLdError e) {
            throw new ParsingErrorException(e);
        }
    }

    private RdfQuadConsumer getConsumer() {
        return new RdfQuadConsumer() {
            @Override
            public RdfQuadConsumer quad(String subject, String predicate, String object, String datatype, String language, String direction, String graph) throws RdfConsumerException {
                // Subject
                Resource subjResource = null;
                if (RdfQuadConsumer.isBlank(subject)) {
                    subjResource = getValueFactory().createBNode(subject);
                } else {
                    subjResource = getValueFactory().createIRI(subject);
                }

                IRI predicateIRI = getValueFactory().createIRI(predicate);

                // Object
                Value objValue = null;
                // Object is a BN
                if (RdfQuadConsumer.isBlank(object)) {
                    objValue = getValueFactory().createBNode(object);
                    // Object is a Literal
                } else if (RdfQuadConsumer.isLiteral(datatype, language, direction)) {
                    if (RdfQuadConsumer.isLangString(datatype, language, direction)) {
                        objValue = getValueFactory().createLiteral(object, language);
                    } else if( ! datatype.equals(XSD.STRING.toString()) ){
                        objValue = getValueFactory().createLiteral(object, getValueFactory().createIRI(datatype));
                    } else {
                        objValue = getValueFactory().createLiteral(object);
                    }
                    // Object is a IRI
                } else if(IRIUtils.isStandardIRI(object)) {
                    objValue = getValueFactory().createIRI(object);
                } else {
                    throw new RdfConsumerException("Invalid object: " + object);
                }

                // Graph
                Resource graphResource = null;
                if(graph != null) {
                    if (RdfQuadConsumer.isBlank(graph)) {
                        graphResource = getValueFactory().createBNode(graph);
                    } else if (!graph.equals(JSONLD_JAVA_DEFAULT_GRAPH) && IRIUtils.isStandardIRI(graph)) {
                        graphResource = getValueFactory().createIRI(graph);
                    } else {
                        throw new ParsingErrorException("Invalid graph: " + graph);
                    }
                }

                Statement statement = null;
                if(graphResource == null) {
                    statement = getValueFactory().createStatement(subjResource, predicateIRI, objValue);
                } else {
                    statement = getValueFactory().createStatement(subjResource, predicateIRI, objValue, graphResource);
                }
                getModel().add(statement);

                return this;
            }
        };
    }
}
