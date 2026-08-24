package fr.inria.corese.core.next.data.impl.io.serializer.jsonld;

import com.apicatalog.jsonld.json.JsonLdComparison;
import fr.inria.corese.core.next.data.api.term.*;
import fr.inria.corese.core.next.data.api.model.*;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.io.serializer.RDFSerializer;
import fr.inria.corese.core.next.storage.impl.model.StorageModel;
import fr.inria.corese.core.next.data.impl.io.jsonld.JSONLDOptions;
import fr.inria.corese.core.next.data.impl.adapter.CoreseValueFactory;
import fr.inria.corese.core.next.storage.api.plugin.StoragePluginManager;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JSONLDSerializerTest {

    private Model model;
    private final ValueFactory factory = new CoreseValueFactory();
    private final IRI iriNode = factory.createIRI("http://example.org/iri1");
    private final IRI iriPred = factory.createIRI("http://example.org/pred1");
    private final Literal basicLiteral = factory.createLiteral("literal1");
    private final Literal typedLiteral = factory.createLiteral("literal2", factory.createIRI("http://example.org/datatype1"));
    private final Literal langLiteral = factory.createLiteral("literal3", "en");
    private final BNode blankNode = factory.createBNode("blank1");
    private final IRI graph1 = factory.createIRI("http://example.org/graph1");
    private final IRI graph2 = factory.createIRI("http://example.org/graph2");

    @BeforeEach
    void setUp() {
        StorageConfig config = StorageConfig.builder()
                .property("type", "memory")
                .build();

        model = StorageModel.builder()
                .storage(StoragePluginManager.create(config))
                .valueFactory(new CoreseValueFactory())
                .build();
    }

    /**
     * Check that the serializer can handle a small model with all types of literals
     */
    @Test
    void smallModelTest() {
        // IRI IRI IRI
        this.model.add(iriNode, iriPred, iriNode);
        // IRI IRI Literal
        this.model.add(iriNode, iriPred, basicLiteral);
        // IRI IRI TypedLiteral
        this.model.add(iriNode, iriPred, typedLiteral);
        // IRI IRI LangLiteral
        this.model.add(iriNode, iriPred, langLiteral);

        RDFSerializer serializer = new JSONLDSerializer(this.model, (new JSONLDOptions.Builder()).ordered(true).build());

        StringWriter writer = new StringWriter();

        serializer.write(writer);
        String result = writer.toString();

        String expectedResult = """
            [
                {
                    "@id": "http://example.org/iri1",
                    "http://example.org/pred1": [
                        {
                            "@id": "http://example.org/iri1"
                        },
                        {
                            "@value": "literal1"
                        },
                        {
                            "@value": "literal2",
                            "@type": "http://example.org/datatype1"
                        },
                        {
                            "@language": "en",
                            "@value": "literal3"
                        }
                    ]
                }
            ]
            """.replace('\n', ' ').replaceAll("\\s+", "");

        Reader resultReader = new StringReader(result);
        Reader expectedResultReader = new StringReader(expectedResult);

        JsonReaderFactory jsonReaderFactory = Json.createReaderFactory(null);
        JsonReader resultJsonReader = jsonReaderFactory.createReader(resultReader);
        JsonReader expectedResultJsonReader = jsonReaderFactory.createReader(expectedResultReader);
        assertTrue(JsonLdComparison.equals(resultJsonReader.readValue(), expectedResultJsonReader.readValue()), "The result should be the expected JSON object");
    }

    /**
     * Test the serialization of a model with blank nodes.
     */
    @Test
    void modelWithBlankNodesTest() {
        // IRI IRI BlankNode
        this.model.add(iriNode, iriPred, blankNode);
        // BlankNode IRI IRI
        this.model.add(blankNode, iriPred, iriNode);
        // BlankNode IRI Literal
        this.model.add(blankNode, iriPred, basicLiteral);
        // BlankNode IRI BlankNode
        this.model.add(blankNode, iriPred, blankNode);

        RDFSerializer serializer = new JSONLDSerializer(this.model);

        StringWriter writer = new StringWriter();
        serializer.write(writer);
        String result = writer.toString();

        String expectedResult = """
[
    {
        "@id":"http://example.org/iri1",
        "http://example.org/pred1":[
            {"@id":"_:blank1"}
        ]
    },
    {
        "@id":"_:blank1",
        "http://example.org/pred1":[
            {
                "@id":"http://example.org/iri1"
            },
            {
                "@id":"_:blank1"
            },
            {
                "@value":"literal1"
            }
        ]
    }
]
            """.replace('\n', ' ').replaceAll("\\s+", "");

        Reader resultReader = new StringReader(result);
        Reader expectedResultReader = new StringReader(expectedResult);

        JsonReaderFactory jsonReaderFactory = Json.createReaderFactory(null);
        JsonReader resultJsonReader = jsonReaderFactory.createReader(resultReader);
        JsonReader expectedResultJsonReader = jsonReaderFactory.createReader(expectedResultReader);
        assertTrue(JsonLdComparison.equals(resultJsonReader.readValue(), expectedResultJsonReader.readValue()), "The result should be " + expectedResult);
    }

    /**
     * Test the serialization of a model with named graphs.
     */
    @Test
    void modelWithNamedGraphsTest() {
        // IRI IRI IRI
        this.model.add(iriNode, iriPred, iriNode);
        // IRI IRI Literal
        this.model.add(iriNode, iriPred, basicLiteral, graph1);
        // IRI IRI TypedLiteral
        this.model.add(iriNode, iriPred, typedLiteral, graph2);
        // IRI IRI LangLiteral
        this.model.add(iriNode, iriPred, langLiteral, graph1, graph2);

        RDFSerializer serializer = new JSONLDSerializer(this.model);

        StringWriter writer = new StringWriter();
        serializer.write(writer);
        String result = writer.toString();

        String expectedResult = """
[
    {
        "@id":"http://example.org/iri1",
        "http://example.org/pred1":[
            {
                "@id":"http://example.org/iri1"
            }
        ]
    },
    {
        "@id":"http://example.org/graph1",
        "@graph":[
            {
                "@id":"http://example.org/iri1",
                "http://example.org/pred1":[
                    {
                        "@value":"literal1"
                    },
                    {
                        "@language":"en",
                        "@value":"literal3"
                    }
                ]
            }
        ]
    },
    {
        "@id":"http://example.org/graph2",
        "@graph":[
            {
                "@id":"http://example.org/iri1",
                "http://example.org/pred1":[
                    {
                        "@value":"literal2",
                        "@type":"http://example.org/datatype1"
                    },
                    {
                        "@language":"en",
                        "@value":"literal3"
                    }
                ]
            }
        ]
    }
]
        """.replace('\n', ' ').replaceAll("\\s+", "");
        Reader resultReader = new StringReader(result);
        Reader expectedResultReader = new StringReader(expectedResult);

        JsonReaderFactory jsonReaderFactory = Json.createReaderFactory(null);
        JsonReader resultJsonReader = jsonReaderFactory.createReader(resultReader);
        JsonReader expectedResultJsonReader = jsonReaderFactory.createReader(expectedResultReader);
        assertTrue(JsonLdComparison.equals(resultJsonReader.readValue(), expectedResultJsonReader.readValue()), "The result should be the " + expectedResult);
    }
}
