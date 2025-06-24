package fr.inria.corese.core.next.impl.io.parser.jsonld;

import fr.inria.corese.core.next.api.BNode;
import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Literal;
import fr.inria.corese.core.next.api.Statement;
import fr.inria.corese.core.next.api.base.io.RdfFormat;
import fr.inria.corese.core.next.api.io.parser.RDFParser;
import fr.inria.corese.core.next.impl.io.parser.ParserFactory;
import fr.inria.corese.core.next.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.impl.temp.CoreseModel;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JSONLDParserTest {

    private final ParserFactory factory = new ParserFactory();

    @Test
    void testGetRDFFormat() {
        RDFParser parser = factory.createRDFParser(RdfFormat.JSONLD, new CoreseModel(), new CoreseAdaptedValueFactory());
        assertEquals(RdfFormat.JSONLD, parser.getRDFFormat());
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
        CoreseModel model = new CoreseModel();
        RDFParser parser = factory.createRDFParser(RdfFormat.JSONLD, model, new CoreseAdaptedValueFactory());
        parser.parse(new ByteArrayInputStream(sampleJsonLD.getBytes()));

        assertEquals(3, model.size());
        IRI subject = new CoreseAdaptedValueFactory().createIRI("http://me.markus-lanthaler.com/");
        IRI namePredicate = new CoreseAdaptedValueFactory().createIRI("http://xmlns.com/foaf/0.1/name");
        IRI knowsPredicate = new CoreseAdaptedValueFactory().createIRI("http://xmlns.com/foaf/0.1/knows");
        Literal nameMarkusObject = new CoreseAdaptedValueFactory().createLiteral("Markus Lanthaler");
        Literal nameDaveObject = new CoreseAdaptedValueFactory().createLiteral("Dave Longley");
        Statement daveNameStatement = new CoreseAdaptedValueFactory().createStatement(subject, namePredicate, nameMarkusObject);

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
        CoreseModel model = new CoreseModel();
        RDFParser parser = factory.createRDFParser(RdfFormat.JSONLD, model, new CoreseAdaptedValueFactory());
        parser.parse(new ByteArrayInputStream(sampleJsonLD.getBytes()), "http://me.markus-lanthaler.com/");

        assertEquals(3, model.size());
        IRI subject = new CoreseAdaptedValueFactory().createIRI("http://me.markus-lanthaler.com/");
        IRI namePredicate = new CoreseAdaptedValueFactory().createIRI("http://xmlns.com/foaf/0.1/name");
        IRI knowsPredicate = new CoreseAdaptedValueFactory().createIRI("http://xmlns.com/foaf/0.1/knows");
        Literal nameMarkusObject = new CoreseAdaptedValueFactory().createLiteral("Markus Lanthaler");
        Literal nameDaveObject = new CoreseAdaptedValueFactory().createLiteral("Dave Longley");
        Statement daveNameStatement = new CoreseAdaptedValueFactory().createStatement(subject, namePredicate, nameMarkusObject);

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
        CoreseModel model = new CoreseModel();
        RDFParser parser = factory.createRDFParser(RdfFormat.JSONLD, model, new CoreseAdaptedValueFactory());
        parser.parse(new StringReader(sampleJsonLD));

        assertEquals(3, model.size());
        IRI subject = new CoreseAdaptedValueFactory().createIRI("http://me.markus-lanthaler.com/");
        IRI namePredicate = new CoreseAdaptedValueFactory().createIRI("http://xmlns.com/foaf/0.1/name");
        IRI knowsPredicate = new CoreseAdaptedValueFactory().createIRI("http://xmlns.com/foaf/0.1/knows");
        Literal nameMarkusObject = new CoreseAdaptedValueFactory().createLiteral("Markus Lanthaler");
        Literal nameDaveObject = new CoreseAdaptedValueFactory().createLiteral("Dave Longley");
        Statement daveNameStatement = new CoreseAdaptedValueFactory().createStatement(subject, namePredicate, nameMarkusObject);

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
        CoreseModel model = new CoreseModel();
        RDFParser parser = factory.createRDFParser(RdfFormat.JSONLD, model, new CoreseAdaptedValueFactory());
        parser.parse(new StringReader(sampleJsonLD), "http://me.markus-lanthaler.com/");

        assertEquals(3, model.size());
        IRI subject = new CoreseAdaptedValueFactory().createIRI("http://me.markus-lanthaler.com/");
        IRI namePredicate = new CoreseAdaptedValueFactory().createIRI("http://xmlns.com/foaf/0.1/name");
        IRI knowsPredicate = new CoreseAdaptedValueFactory().createIRI("http://xmlns.com/foaf/0.1/knows");
        Literal nameMarkusObject = new CoreseAdaptedValueFactory().createLiteral("Markus Lanthaler");
        Literal nameDaveObject = new CoreseAdaptedValueFactory().createLiteral("Dave Longley");
        Statement daveNameStatement = new CoreseAdaptedValueFactory().createStatement(subject, namePredicate, nameMarkusObject);

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
        CoreseModel model = new CoreseModel();
        RDFParser parser = factory.createRDFParser(RdfFormat.JSONLD, model, new CoreseAdaptedValueFactory());
        parser.parse(new StringReader(sampleJsonLD));

        assertEquals(2, model.size());
        BNode b0 = new CoreseAdaptedValueFactory().createBNode("b0");
        BNode b1 = new CoreseAdaptedValueFactory().createBNode("b1");
        IRI knowsPredicate = new CoreseAdaptedValueFactory().createIRI("http://xmlns.com/foaf/0.1/knows");
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
                    "Person": "http://xmlns.com/foaf/0.1/Person",
                    "name": "http://xmlns.com/foaf/0.1/name",
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

        CoreseModel model = new CoreseModel();
        RDFParser parser = factory.createRDFParser(RdfFormat.JSONLD, model, new CoreseAdaptedValueFactory());
        parser.parse(new StringReader(sampleJsonLD));

        assertEquals(7, model.size());
        IRI graphIRI = new CoreseAdaptedValueFactory().createIRI("http://example.org/foaf-graph");
        IRI generatedAt = new CoreseAdaptedValueFactory().createIRI("http://www.w3.org/ns/prov#generatedAtTime");
        IRI datetimeDatatype = new CoreseAdaptedValueFactory().createIRI("http://www.w3.org/2001/XMLSchema#dateTime");
        Literal generatedAtValue = new CoreseAdaptedValueFactory().createLiteral("2012-04-09T00:00:00", datetimeDatatype) ;
        IRI manuIRI = new CoreseAdaptedValueFactory().createIRI("http://manu.sporny.org/about#manu");
        Literal manuName = new CoreseAdaptedValueFactory().createLiteral("Manu Sporny");
        IRI greggIRI = new CoreseAdaptedValueFactory().createIRI("https://greggkellogg.net/foaf#me");
        Literal greggName = new CoreseAdaptedValueFactory().createLiteral("Gregg Kellogg");
        IRI typeIRI = new CoreseAdaptedValueFactory().createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        IRI knowsPredicate = new CoreseAdaptedValueFactory().createIRI("http://xmlns.com/foaf/0.1/knows");
        IRI personType = new CoreseAdaptedValueFactory().createIRI("http://xmlns.com/foaf/0.1/Person");
        IRI namePredicate = new CoreseAdaptedValueFactory().createIRI("http://xmlns.com/foaf/0.1/name");


        //<http://example.org/foaf-graph> {
        //  <http://manu.sporny.org/about#manu> a foaf:Person;
        assertTrue(model.contains(manuIRI, typeIRI, personType, graphIRI));
        //     foaf:name "Manu Sporny";
        assertTrue(model.contains(manuIRI, namePredicate, manuName, graphIRI));
        //     foaf:knows <https://greggkellogg.net/foaf#me> .
        assertTrue(model.contains(manuIRI, knowsPredicate, greggIRI, graphIRI));
        //
        //  <https://greggkellogg.net/foaf#me> a foaf:Person;
        assertTrue(model.contains(greggIRI, typeIRI, personType, graphIRI));
        //     foaf:name "Gregg Kellogg";
        assertTrue(model.contains(greggIRI, namePredicate, greggName, graphIRI));
        //     foaf:knows <http://manu.sporny.org/about#manu> .
        assertTrue(model.contains(greggIRI, knowsPredicate, manuIRI, graphIRI));
        //}
        //<http://example.org/foaf-graph> prov:generatedAtTime "2012-04-09T00:00:00"^^xsd:dateTime .
        assertTrue(model.contains(graphIRI, generatedAt, generatedAtValue));
    }
}
