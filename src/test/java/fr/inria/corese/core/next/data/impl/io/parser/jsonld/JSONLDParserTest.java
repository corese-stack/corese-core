package fr.inria.corese.core.next.data.impl.io.parser.jsonld;

import fr.inria.corese.core.next.data.api.*;
import fr.inria.corese.core.next.data.api.base.io.RDFFormat;
import fr.inria.corese.core.next.data.api.io.parser.RDFParser;
import fr.inria.corese.core.next.data.impl.io.common.JSONLDOptions;
import fr.inria.corese.core.next.data.impl.io.parser.ParserFactory;
import fr.inria.corese.core.next.data.impl.io.util.ParserTestBase;
import fr.inria.corese.core.next.data.impl.adapter.CoreseValueFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class JSONLDParserTest extends ParserTestBase {

    private final ParserFactory factory = new ParserFactory();
    private final ValueFactory valueFactory = new CoreseValueFactory();



    @Test
    void constructorNullModelThrowsTest() {
        assertThrows(NullPointerException.class, () -> new JSONLDParser(null, valueFactory));
    }

    @Test
    void constructorNullValueFactoryThrowsTest() {
        assertThrows(NullPointerException.class, () -> new JSONLDParser(createTestModel(), null));
    }

    @Test
    void constructorNullModelAndValueFactoryThrowsTest() {
        assertThrows(NullPointerException.class, () -> new JSONLDParser(null, null));
    }

    @Test
    void constructorConfigNoThrowsTest() {
        assertDoesNotThrow(() -> new JSONLDParser(createTestModel(), valueFactory, new JSONLDOptions.Builder().build()));
    }

    @Test
    void getRDFFormatTest() {
        RDFParser parser = factory.createRDFParser(RDFFormat.JSONLD, createTestModel(), valueFactory);
        assertEquals(RDFFormat.JSONLD, parser.getRDFFormat());
    }

    /**
     * Test method for {@link JSONLDParser#parse(java.io.InputStream)}. No relative IRIs in this test.
     */
    @Test
    public void testParseInputStream() {
        // taken from https://www.w3.org/TR/json-ld-api/#object-to-rdf-conversion
        String sampleJsonLD = """
                {
                  "@context": {
                    "name": "http://xmlns.com/foaf/0.1/name",
                    "knows": "http://xmlns.com/foaf/0.1/knows"
                  },
                  "@id": "http://me.markus-lanthaler.com/",
                  "name": "Markus Lanthaler",
                  "knows": [
                    {
                      "name": "Dave Longley"
                    }
                  ]
                }
                """;
        Model model = createTestModel();
        RDFParser parser = factory.createRDFParser(RDFFormat.JSONLD, model, valueFactory);
        parser.parse(new ByteArrayInputStream(sampleJsonLD.getBytes()));

        assertEquals(3, model.size());
        IRI subject = valueFactory.createIRI("http://me.markus-lanthaler.com/");
        IRI namePredicate = valueFactory.createIRI("http://xmlns.com/foaf/0.1/name");
        IRI knowsPredicate = valueFactory.createIRI("http://xmlns.com/foaf/0.1/knows");
        Literal nameMarkusObject = valueFactory.createLiteral("Markus Lanthaler");
        Literal nameDaveObject = valueFactory.createLiteral("Dave Longley");
        Statement daveNameStatement = valueFactory.createStatement(subject, namePredicate, nameMarkusObject);

        assertTrue(model.contains(daveNameStatement));
        assertTrue(model.contains(subject, knowsPredicate, null));
        assertTrue(model.contains(null, namePredicate, nameDaveObject));
    }

    /**
     * Test method for {@link JSONLDParser#parse(java.io.InputStream, java.lang.String)}. A relative IRI is used in this test.
     */
    @Test
    public void testParseInputStreamString() {
        // taken from https://www.w3.org/TR/json-ld-api/#object-to-rdf-conversion
        String sampleJsonLD = """
                {
                  "@context": {
                    "name": "http://xmlns.com/foaf/0.1/name",
                    "knows": "http://xmlns.com/foaf/0.1/knows"
                  },
                  "@id": "",
                  "name": "Markus Lanthaler",
                  "knows": [
                    {
                      "name": "Dave Longley"
                    }
                  ]
                }
                """;
        Model model = createTestModel();
        RDFParser parser = factory.createRDFParser(RDFFormat.JSONLD, model, valueFactory);
        parser.parse(new ByteArrayInputStream(sampleJsonLD.getBytes()), "http://me.markus-lanthaler.com/");

        assertEquals(3, model.size());
        IRI subject = valueFactory.createIRI("http://me.markus-lanthaler.com/");
        IRI namePredicate = valueFactory.createIRI("http://xmlns.com/foaf/0.1/name");
        IRI knowsPredicate = valueFactory.createIRI("http://xmlns.com/foaf/0.1/knows");
        Literal nameMarkusObject = valueFactory.createLiteral("Markus Lanthaler");
        Literal nameDaveObject = valueFactory.createLiteral("Dave Longley");
        Statement daveNameStatement = valueFactory.createStatement(subject, namePredicate, nameMarkusObject);

        assertTrue(model.contains(daveNameStatement));
        assertTrue(model.contains(subject, knowsPredicate, null));
        assertTrue(model.contains(null, namePredicate, nameDaveObject));
    }

    /**
     * Test of {@link JSONLDParser#parse(java.io.Reader, java.lang.String)}, of class JSONLDParser. No relative IRIs are used in this test.
     */
    @Test
    public void testParseReader() {
        // taken from https://www.w3.org/TR/json-ld-api/#object-to-rdf-conversion
        String sampleJsonLD = """
                {
                  "@context": {
                    "name": "http://xmlns.com/foaf/0.1/name",
                    "knows": "http://xmlns.com/foaf/0.1/knows"
                  },
                  "@id": "http://me.markus-lanthaler.com/",
                  "name": "Markus Lanthaler",
                  "knows": [
                    {
                      "name": "Dave Longley"
                    }
                  ]
                }
                """;
        Model model = createTestModel();
        RDFParser parser = factory.createRDFParser(RDFFormat.JSONLD, model, valueFactory);
        parser.parse(new StringReader(sampleJsonLD));

        assertEquals(3, model.size());
        IRI subject = valueFactory.createIRI("http://me.markus-lanthaler.com/");
        IRI namePredicate = valueFactory.createIRI("http://xmlns.com/foaf/0.1/name");
        IRI knowsPredicate = valueFactory.createIRI("http://xmlns.com/foaf/0.1/knows");
        Literal nameMarkusObject = valueFactory.createLiteral("Markus Lanthaler");
        Literal nameDaveObject = valueFactory.createLiteral("Dave Longley");
        Statement daveNameStatement = valueFactory.createStatement(subject, namePredicate, nameMarkusObject);

        assertTrue(model.contains(daveNameStatement));
        assertTrue(model.contains(subject, knowsPredicate, null));
        assertTrue(model.contains(null, namePredicate, nameDaveObject));
    }

    /**
     * Test of {@link JSONLDParser#parse(java.io.Reader, java.lang.String)}, of class JSONLDParser. A relative IRI is used in this test.
     */
    @Test
    public void testParseReaderString() {
        // taken from https://www.w3.org/TR/json-ld-api/#object-to-rdf-conversion
        String sampleJsonLD = """
                {
                  "@context": {
                    "name": "http://xmlns.com/foaf/0.1/name",
                    "knows": "http://xmlns.com/foaf/0.1/knows"
                  },
                  "@id": "",
                  "name": "Markus Lanthaler",
                  "knows": [
                    {
                      "name": "Dave Longley"
                    }
                  ]
                }
                """;
        Model model = createTestModel();
        RDFParser parser = factory.createRDFParser(RDFFormat.JSONLD, model, valueFactory);
        parser.parse(new StringReader(sampleJsonLD), "http://me.markus-lanthaler.com/");

        assertEquals(3, model.size());
        IRI subject = valueFactory.createIRI("http://me.markus-lanthaler.com/");
        IRI namePredicate = valueFactory.createIRI("http://xmlns.com/foaf/0.1/name");
        IRI knowsPredicate = valueFactory.createIRI("http://xmlns.com/foaf/0.1/knows");
        Literal nameMarkusObject = valueFactory.createLiteral("Markus Lanthaler");
        Literal nameDaveObject = valueFactory.createLiteral("Dave Longley");
        Statement daveNameStatement = valueFactory.createStatement(subject, namePredicate, nameMarkusObject);

        assertTrue(model.contains(daveNameStatement));
        assertTrue(model.contains(subject, knowsPredicate, null));
        assertTrue(model.contains(null, namePredicate, nameDaveObject));
    }

    /**
     * Test parsing JSON-LD with blank nodes.
     */
    @Test
    public void testParseJsonLDWithBlankNodes() {
        String sampleJsonLD = """
                    {
                    "@context": {
                      "foaf": "http://xmlns.com/foaf/0.1/"
                    },
            
                    "@graph":
                    [
                      {
                        "@id": "_:b0",
                        "foaf:knows": {"@id": "_:b1"}
                      },
            
                      {
                        "@id": "_:b1",
                        "foaf:knows": {"@id": "_:b0"}
                      }
                    ]
                    }
            
            """;
        Model model = createTestModel();
        RDFParser parser = factory.createRDFParser(RDFFormat.JSONLD, model, valueFactory);
        Reader reader = new StringReader(sampleJsonLD);
        parser.parse(reader);

        // Verify structure instead of specific blank node IDs
        assertEquals(2, model.size(), "Should have 2 statements");

        IRI knowsPredicate = valueFactory.createIRI("http://xmlns.com/foaf/0.1/knows");

        List<Statement> knowsStatements = new ArrayList<>();
        for (Statement stmt : model.getStatements(null, knowsPredicate, null)) {
            knowsStatements.add(stmt);
        }

        assertEquals(2, knowsStatements.size(), "Should have 2 'knows' relationships");

        // Verify both statements have blank nodes as subject and object
        for (Statement stmt : knowsStatements) {
            assertTrue(stmt.getSubject().isBNode(),
                    "Subject should be a blank node");
            assertInstanceOf(BNode.class, stmt.getObject(), "Object should be a blank node");
            assertEquals(knowsPredicate, stmt.getPredicate(),
                    "Predicate should be foaf:knows");
        }

        // Verify the circular relationship structure
        Set<Resource> subjects = knowsStatements.stream()
                .map(Statement::getSubject)
                .collect(Collectors.toSet());

        Set<Value> objects = knowsStatements.stream()
                .map(Statement::getObject)
                .collect(Collectors.toSet());

        assertEquals(2, subjects.size(), "Should have 2 distinct blank node subjects");
        assertEquals(2, objects.size(), "Should have 2 distinct blank node objects");
    }

    @Test
    public void testParseJSONLDWithGraphs() {
        // Taken from https://www.w3.org/TR/json-ld11/#named-graphs
        String sampleJsonLD = """
                {
                  "@context": {
                    "generatedAt": {
                      "@id": "http://www.w3.org/ns/prov#generatedAtTime"
                    },
                    "Person": {
                      "@id": "http://xmlns.com/foaf/0.1/Person"
                    },
                    "name": {
                      "@id": "http://xmlns.com/foaf/0.1/name"
                    },
                    "knows": {"@id": "http://xmlns.com/foaf/0.1/knows", "@type": "@id"}
                  },
                  "@id": "http://example.org/foaf-graph",
                  "generatedAt": {
                    "@value": "2012-04-09T00:00:00",
                    "@type": "http://www.w3.org/2001/XMLSchema#dateTime"
                  },
                  "@graph": [
                    {
                      "@id": "http://manu.sporny.org/about#manu",
                      "@type": "Person",
                      "name": "Manu Sporny",
                      "knows": "https://greggkellogg.net/foaf#me"
                    }, {
                      "@id": "https://greggkellogg.net/foaf#me",
                      "@type": "Person",
                      "name": "Gregg Kellogg",
                      "knows": "http://manu.sporny.org/about#manu"
                    }
                  ]
                }
                """;

        Model model = createTestModel();
        RDFParser parser = factory.createRDFParser(RDFFormat.JSONLD, model, valueFactory);
        parser.parse(new StringReader(sampleJsonLD));

        assertEquals(7, model.size());
        Resource graphIRI = valueFactory.createIRI("http://example.org/foaf-graph");
        IRI generatedAt = valueFactory.createIRI("http://www.w3.org/ns/prov#generatedAtTime");
        IRI datetimeDatatype = valueFactory.createIRI("http://www.w3.org/2001/XMLSchema#dateTime");
        Literal generatedAtValue = valueFactory.createLiteral("2012-04-09T00:00:00", datetimeDatatype);
        IRI manuIRI = valueFactory.createIRI("http://manu.sporny.org/about#manu");
        Literal manuName = valueFactory.createLiteral("Manu Sporny");
        IRI greggIRI = valueFactory.createIRI("https://greggkellogg.net/foaf#me");
        Literal greggName = valueFactory.createLiteral("Gregg Kellogg");
        IRI typeIRI = valueFactory.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        IRI knowsPredicate = valueFactory.createIRI("http://xmlns.com/foaf/0.1/knows");
        IRI personType = valueFactory.createIRI("http://xmlns.com/foaf/0.1/Person");
        IRI namePredicate = valueFactory.createIRI("http://xmlns.com/foaf/0.1/name");

        //<http://manu.sporny.org/about#manu> <http://xmlns.com/foaf/0.1/knows> <https://greggkellogg.net/foaf#me> <http://example.org/foaf-graph> .
        assertTrue(model.contains(manuIRI, knowsPredicate, greggIRI, graphIRI));
        //<https://greggkellogg.net/foaf#me> <http://xmlns.com/foaf/0.1/name> "Gregg Kellogg" <http://example.org/foaf-graph> .
        assertTrue(model.contains(greggIRI, namePredicate, greggName, graphIRI));
        //<https://greggkellogg.net/foaf#me> <http://xmlns.com/foaf/0.1/knows> <http://manu.sporny.org/about#manu> <http://example.org/foaf-graph> .
        assertTrue(model.contains(greggIRI, knowsPredicate, manuIRI, graphIRI));
        //<http://manu.sporny.org/about#manu> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> <http://example.org/foaf-graph> .
        assertTrue(model.contains(manuIRI, typeIRI, personType, graphIRI));
        //<http://example.org/foaf-graph> <http://www.w3.org/ns/prov#generatedAtTime> "2012-04-09T00:00:00"^^<http://www.w3.org/2001/XMLSchema#dateTime> .
        assertTrue(model.contains(graphIRI, generatedAt, generatedAtValue));
        //<http://manu.sporny.org/about#manu> <http://xmlns.com/foaf/0.1/name> "Manu Sporny" <http://example.org/foaf-graph> .
        assertTrue(model.contains(manuIRI, namePredicate, manuName, graphIRI));
        //<https://greggkellogg.net/foaf#me> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> <http://example.org/foaf-graph> .
        assertTrue(model.contains(greggIRI, typeIRI, personType, graphIRI));
    }
}