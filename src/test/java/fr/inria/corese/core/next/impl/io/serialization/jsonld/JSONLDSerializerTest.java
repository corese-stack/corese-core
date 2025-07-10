package fr.inria.corese.core.next.impl.io.serialization.jsonld;

import com.apicatalog.jsonld.json.JsonLdComparison;
import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.io.serialization.RDFSerializer;
import fr.inria.corese.core.next.impl.io.option.TitaniumJSONLDProcessorOption;
import fr.inria.corese.core.next.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.impl.temp.CoreseModel;
import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

class JSONLDSerializerTest {

    private Model model;
    private ValueFactory factory = new CoreseAdaptedValueFactory();
    private IRI iriNode = factory.createIRI("http://example.org/iri1");
    private IRI iriPred = factory.createIRI("http://example.org/pred1");
    private Literal basicLiteral = factory.createLiteral("literal1");
    private Literal typedLiteral = factory.createLiteral("literal2", factory.createIRI("http://example.org/datatype1"));
    private Literal langLiteral = factory.createLiteral("literal3", "en");
    private BNode blankNode = factory.createBNode("blank1");
    private IRI graph1 = factory.createIRI("http://example.org/graph1");
    private IRI graph2 = factory.createIRI("http://example.org/graph2");

    @BeforeEach
    public void setUp() {
        model = new CoreseModel();
    }

    @Test
    public void smallModelTest() {
        // IRI IRI IRI
        this.model.add(iriNode, iriPred, iriNode);
        // IRI IRI Literal
        this.model.add(iriNode, iriPred, basicLiteral);
        // IRI IRI TypedLiteral
        this.model.add(iriNode, iriPred, typedLiteral);
        // IRI IRI LangLiteral
        this.model.add(iriNode, iriPred, langLiteral);

        RDFSerializer serializer = new JSONLDSerializer(this.model, (new TitaniumJSONLDProcessorOption.Builder()).ordered(true).build());

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

        JsonReaderFactory factory = Json.createReaderFactory(null);
        JsonReader resultJsonReader = factory.createReader(resultReader);
        JsonReader expectedResultJsonReader = factory.createReader(expectedResultReader);
        assertTrue(JsonLdComparison.equals(resultJsonReader.readValue(), expectedResultJsonReader.readValue()), "The result should be the expected JSON object");
    }

    @Test
    public void modelWithBlankNodesTest() {
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
            {"@id":"blank1"}
        ]
    },
    {
        "@id":"blank1",
        "http://example.org/pred1":[
            {
                "@id":"http://example.org/iri1"
            },
            {
                "@id":"blank1"
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

        JsonReaderFactory factory = Json.createReaderFactory(null);
        JsonReader resultJsonReader = factory.createReader(resultReader);
        JsonReader expectedResultJsonReader = factory.createReader(expectedResultReader);
        assertTrue(JsonLdComparison.equals(resultJsonReader.readValue(), expectedResultJsonReader.readValue()), "The result should be " + expectedResult);
    }

    @Test
    public void modelWithNamedGraphsTest() {
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
            },
            {
                "@value":"literal1"
            },
            {
                "@value":"literal2",
                "@type":"http://example.org/datatype1"
            },
            {
               "@language":"en",
               "@value":"literal3"
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