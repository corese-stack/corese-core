package fr.inria.corese.core.next.impl.io.serialization.jsonld;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.io.serialization.FormatSerializer;
import fr.inria.corese.core.next.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.impl.temp.CoreseModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

        FormatSerializer serializer = new JSONLDSerializer(this.model);

        StringWriter writer = new StringWriter();
        serializer.write(writer);
        String result = writer.toString();

        String expectedResult= "[\n" +
                "  {\n" +
                "    \"@id\": \"http://example.org/iri1\",\n" +
                "    \"http://example.org/pred1\": [\n" +
                "      {\n" +
                "        \"@id\": \"http://example.org/iri1\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"@type\": \"http://example.org/datatype1\",\n" +
                "        \"@value\": \"literal2\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"@value\": \"literal1\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"@language\": \"en\",\n" +
                "        \"@value\": \"literal3\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "]";
        assertEquals(expectedResult.replace('\n', ' ').replaceAll("\\s+", ""), result, "The result should be a JSON object");
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

        FormatSerializer serializer = new JSONLDSerializer(this.model);

        StringWriter writer = new StringWriter();
        serializer.write(writer);
        String result = writer.toString();
        assertEquals("{}", result, "The result should be an empty JSON object");
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

        FormatSerializer serializer = new JSONLDSerializer(this.model);

        StringWriter writer = new StringWriter();
        serializer.write(writer);
        String result = writer.toString();
        assertEquals("{}", result, "The result should be an empty JSON object");
    }
}