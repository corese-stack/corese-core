package fr.inria.corese.core.next.impl.io.parser.jsonld;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.rdf.api.RdfConsumerException;
import com.apicatalog.rdf.api.RdfQuadConsumer;
import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.base.io.parser.AbstractParser;
import fr.inria.corese.core.next.api.base.io.RdfFormat;
import fr.inria.corese.core.next.api.io.IOConfig;
import fr.inria.corese.core.next.impl.common.util.IRIUtils;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;

import java.io.InputStream;
import java.io.Reader;

public class JSONLDParser extends AbstractParser {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JSONLDParser.class);

    private static final String JSONLD_JAVA_DEFAULT_GRAPH = "@default";

    public JSONLDParser(Model model, ValueFactory factory) {
        super(model, factory);
    }

    public JSONLDParser(Model model, ValueFactory factory, IOConfig config) {
        super(model, factory);
        if(config instanceof JSONLDParserConfig) {
            setConfig(config);
        }
    }

    @Override
    public RdfFormat getRDFFormat() {
        return RdfFormat.JSONLD;
    }

    @Override
    public void parse(InputStream in, String baseURI) throws ParsingErrorException {
            try {
                parseJSONLDDocument(JsonDocument.of(in), baseURI);
            } catch (JsonLdError e) {
                throw new ParsingErrorException(e);
            }
    }

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
            JsonLd.toRdf(document)
                    .base(baseURI)
                    .provide(new RdfQuadConsumer() {
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
                                    } else if( datatype != null ){
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
                    });
        } catch (JsonLdError e) {
            throw new ParsingErrorException(e);
        }
    }
}
