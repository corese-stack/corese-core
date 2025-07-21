package fr.inria.corese.core.next.impl.io.parser.jsonld;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.Reader;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import fr.inria.corese.core.next.api.BNode;
import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Literal;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Statement;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.io.parser.RDFParser;
import fr.inria.corese.core.next.impl.io.parser.ParserFactory;
import fr.inria.corese.core.next.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.impl.temp.CoreseModel;

public class JSONLDParserTest {

    private final ParserFactory factory = new ParserFactory();
    private final ValueFactory valueFactory = new CoreseAdaptedValueFactory();

    @Test
    void constructorNullModelThrowsTest() {
        assertThrows(NullPointerException.class, () -> new JSONLDParser(null, valueFactory));
    }

    @Test
    void constructorNullValueFactoryThrowsTest() {
        assertThrows(NullPointerException.class, () -> new JSONLDParser(new CoreseModel(), null));
    }

    @Test
    void constructorNullModelAndValueFactoryThrowsTest() {
        assertThrows(NullPointerException.class, () -> new JSONLDParser(null, null));
    }

    @Test
    void constructorConfigNoThrowsTest() {
        assertDoesNotThrow(() -> new JSONLDParser(new CoreseModel(), valueFactory, null));
    }

    @Test
    void getRDFFormatTest() {
        RDFParser parser = factory.createRDFParser(RDFFormat.JSONLD, new CoreseModel(), valueFactory);
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
        Model model = new CoreseModel();
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
        Model model = new CoreseModel();
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
        Model model = new CoreseModel();
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
        Model model = new CoreseModel();
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
        Model model = new CoreseModel();
        RDFParser parser = factory.createRDFParser(RDFFormat.JSONLD, model, valueFactory);
        Reader reader = new StringReader(sampleJsonLD);
        parser.parse(reader);

        assertEquals(2, model.size());
        BNode b0 = valueFactory.createBNode("b0");
        BNode b1 = valueFactory.createBNode("b1");
        IRI knowsPredicate = valueFactory.createIRI("http://xmlns.com/foaf/0.1/knows");
        assertTrue(model.contains(b0, knowsPredicate, b1));
        assertTrue(model.contains(b1, knowsPredicate, b0));
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

        Model model = new CoreseModel();
        RDFParser parser = factory.createRDFParser(RDFFormat.JSONLD, model, valueFactory);
        parser.parse(new StringReader(sampleJsonLD));

        assertEquals(7, model.size());
        Resource graphIRI = valueFactory.createIRI("http://example.org/foaf-graph");
        IRI generatedAt = valueFactory.createIRI("http://www.w3.org/ns/prov#generatedAtTime");
        IRI datetimeDatatype = valueFactory.createIRI("http://www.w3.org/2001/XMLSchema#dateTime");
        Literal generatedAtValue = valueFactory.createLiteral("2012-04-09T00:00:00", datetimeDatatype) ;
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
