package fr.inria.corese.core.next.impl.io.parser.rdfa;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.io.parser.RDFParser;
import fr.inria.corese.core.next.api.io.serializer.RDFSerializer;
import fr.inria.corese.core.next.impl.io.serialization.SerializerFactory;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.common.vocabulary.XSD;
import fr.inria.corese.core.next.impl.io.parser.ParserFactory;
import fr.inria.corese.core.next.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.impl.temp.CoreseModel;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

class RDFaParserTest {

    private static final Logger logger = LoggerFactory.getLogger(RDFaParserTest.class);

    private ParserFactory parserFactory = new ParserFactory();
    private ValueFactory valueFactory = new CoreseAdaptedValueFactory();
    private final String defaultTurtlePrefixes = """
            @prefix bibo: 	<http://purl.org/ontology/bibo/> .
            @prefix cc: 	<http://creativecommons.org/ns#> .
            @prefix dbp: 	<http://dbpedia.org/property/> .
            @prefix dbp-owl: 	<http://dbpedia.org/ontology/> .
            @prefix dbr: 	<http://dbpedia.org/resource/> .
            @prefix dc: 	<http://purl.org/dc/terms/> .
            @prefix ex: 	<http://example.org/> .
            @prefix foaf: 	<http://xmlns.com/foaf/0.1/> .
            @prefix owl: 	<http://www.w3.org/2002/07/owl#> .
            @prefix rdf: 	<http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix rdfa: 	<http://www.w3.org/ns/rdfa#> .
            @prefix rdfs: 	<http://www.w3.org/2000/01/rdf-schema#> .
            @prefix xhv: 	<http://www.w3.org/1999/xhtml/vocab#> .
            @prefix xsd: 	<http://www.w3.org/2001/XMLSchema#> .
            """;

    @Test
    void getRDFFormat() {
        Model model = new CoreseModel();
        ValueFactory factory = new CoreseAdaptedValueFactory();
        RDFParser parser = new RDFaParser(model, factory);
        assertEquals(RDFFormat.RDFA, parser.getRDFFormat());
    }

    @Test
    void parseCurrentSubjectCreatorHead() {
        String currentSubjectXHTML = """
                <html xmlns="http://www.w3.org/1999/xhtml">
                  <head>
                    <title>Jo's Friends and Family Blog</title>
                    <link rel="foaf:primaryTopic" href="#bbq" />
                    <meta property="dc:creator" content="Jo" />
                  </head>
                  <body>
                    ...
                  </body>
                </html>
                """;
        String currentSubjectNTriples = defaultTurtlePrefixes + """
                <http://example.org/> foaf:primaryTopic <http://example.org/#bbq> .
                <http://example.org/> dc:creator "Jo" .
                """;

        Model parsedModel = new CoreseModel();
        Model resultModel = new CoreseModel();
        ValueFactory factory = new CoreseAdaptedValueFactory();
        RDFParser testedParser = new RDFaParser(parsedModel, factory);
        RDFParser resultParser = parserFactory.createRDFParser(RDFFormat.TURTLE, resultModel, valueFactory);

        assertEquals(RDFFormat.RDFA, testedParser.getRDFFormat());

        resultParser.parse(new ByteArrayInputStream(currentSubjectNTriples.getBytes()), "http://example.org/");
        testedParser.parse(new ByteArrayInputStream(currentSubjectXHTML.getBytes()), "http://example.org/");

        logModelContent(parsedModel);
        assertEquals(resultModel.size(), parsedModel.size());
        Iterator<Statement> itStatementRef = resultModel.iterator();
        Iterator<Statement> itStatementTest = parsedModel.iterator();
        while(itStatementRef.hasNext() && itStatementTest.hasNext()) {
            Statement statementRef = itStatementRef.next();
            Statement statementTest = itStatementTest.next();
            assertEquals(statementRef.getSubject(), statementTest.getSubject());
            assertEquals(statementRef.getPredicate(), statementTest.getPredicate());
            assertEquals(statementRef.getObject(), statementTest.getObject());
            assertEquals(statementRef.getContext(), statementTest.getContext());
        }
    }

    @Test
    void parseCurrentSubjectCreatorMiddle() {
        String currentSubjectXHTML = """
                <html xmlns="http://www.w3.org/1999/xhtml">
                  <head>
                    <title>Jo's Blog</title>
                  </head>
                  <body>
                    <h1><span property="dc:creator">Jo</span>'s blog</h1>
                    <p>
                      Welcome to my blog.
                    </p>
                  </body>
                </html>
                """;
        String currentSubjectNTriples = """
                <> dc:creator "Jo" .
                """;
    }

    @Test
    public void basicBaseTest() {
        String testDataString = """
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML+RDFa 1.0//EN" "http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:dc="http://purl.org/dc/elements/1.1/">
    <head>
        <base href="http://www.w3.org/2006/07/SWD/RDFa/testsuite/xhtml1-testcases/" />
        <title>Test 0001</title>
    </head>
    <body>
        <p>This photo was taken by <span class="author" about="photo1.jpg" property="dc:creator">Mark Birbeck</span>.</p>
    </body>
</html>""";

        Model testModel = new CoreseModel();
        Model referenceModel = new CoreseModel();

        IRI subject = valueFactory.createIRI("http://www.w3.org/2006/07/SWD/RDFa/testsuite/xhtml1-testcases/photo1.jpg");
        IRI predicate = valueFactory.createIRI("http://purl.org/dc/elements/1.1/creator");
        Literal object = valueFactory.createLiteral("Mark Birbeck");

        referenceModel.add(subject, predicate, object);

        RDFParser parser = new ParserFactory().createRDFParser(RDFFormat.RDFA, testModel, valueFactory);

        parser.parse(new ByteArrayInputStream(testDataString.getBytes()), "http://www.w3.org/2006/07/SWD/RDFa/testsuite/xhtml1-testcases/");

        assertEquals(RDFFormat.RDFA, parser.getRDFFormat());
        logModelContent(referenceModel);
        logModelContent(testModel);
        assertEquals(referenceModel.size(), testModel.size());
        Iterator<Statement> itStatementRef = referenceModel.iterator();
        Iterator<Statement> itStatementTest = testModel.iterator();
        while(itStatementRef.hasNext() && itStatementTest.hasNext()) {
            Statement statementRef = itStatementRef.next();
            Statement statementTest = itStatementTest.next();
            assertEquals(statementRef.getSubject(), statementTest.getSubject());
            assertEquals(statementRef.getPredicate(), statementTest.getPredicate());
            assertEquals(statementRef.getObject(), statementTest.getObject());
            assertEquals(statementRef.getContext(), statementTest.getContext());
        }
        assertTrue(testModel.contains(subject, predicate, object));
    }

    @Test
    public void aboutTest() {
        String testDataString = """
                <html>
                  <head>
                  </head>
                  <body>
                    <p xmlns:foaf="http://xmlns.com/foaf/0.1/" about="http://w3id.org/people/pierre-maillot" typeof="foaf:Person">
                      Hello, I'm Pierre.
                    </p>
                  </body>
                </html>""";

        Model testModel = new CoreseModel();

        RDFParser parser = new ParserFactory().createRDFParser(RDFFormat.RDFA, testModel, valueFactory);

        parser.parse(new ByteArrayInputStream(testDataString.getBytes()), "http://not.the.right.base.uri");

        IRI subject = valueFactory.createIRI("http://w3id.org/people/pierre-maillot");
        IRI object = valueFactory.createIRI("http://xmlns.com/foaf/0.1/Person");

        logModelContent(testModel);

        assertEquals(1, testModel.size());
        assertTrue(testModel.contains(subject, RDF.type.getIRI(), object));
    }

    @Test
    public void basicIRItoIRITest() {
        String testDataString = """
                <html>
                  <head>
                    <meta xmlns:dbp="http://dbpedia.org/property/" xmlns:foaf="http://xmlns.com/foaf/0.1/" xmlns:xsd="http://www.w3.org/2001/XMLSchema#"></meta>
                  </head>
                  <body>
                    <div about="http://dbpedia.org/resource/Albert_Einstein">
                      <div rel="dbp:birthPlace" resource="http://dbpedia.org/resource/Germany">
                      </div>
                    </div>
                  </body>
                </html>
                """;

        Model testModel = new CoreseModel();
        Model referenceModel = new CoreseModel();

        RDFParser parser = new ParserFactory().createRDFParser(RDFFormat.RDFA, testModel, valueFactory);

        parser.parse(new ByteArrayInputStream(testDataString.getBytes()), "http://not.the.right.base.uri");

        IRI albertEinstein = valueFactory.createIRI("http://dbpedia.org/resource/Albert_Einstein");
        IRI birthPlace = valueFactory.createIRI("http://dbpedia.org/property/birthPlace");
        IRI germany = valueFactory.createIRI("http://dbpedia.org/resource/Germany");

        Statement aeBirthPlaceStatement = valueFactory.createStatement(albertEinstein, birthPlace, germany);

        referenceModel.add(aeBirthPlaceStatement);

        logModelContent(referenceModel);
        logModelContent(testModel);

        assertEquals(1, testModel.size());
        assertEquals(referenceModel, testModel);
        assertTrue(referenceModel.containsAll(testModel));
    }

    @Test
    public void basicIRItoStringTest() {
        String testDataString = """
                <html>
                  <head>
                  <meta xmlns:dbp="http://dbpedia.org/property/" xmlns:foaf="http://xmlns.com/foaf/0.1/" xmlns:xsd="http://www.w3.org/2001/XMLSchema#"></meta>
                  </head>
                  <body>
                <div about="http://dbpedia.org/resource/Albert_Einstein">
                  <span property="foaf:name">Albert Einstein</span>
                </div>
                  </body>
                </html>
                """;

        Model testModel = new CoreseModel();
        Model referenceModel = new CoreseModel();

        RDFParser parser = new ParserFactory().createRDFParser(RDFFormat.RDFA, testModel, valueFactory);

        parser.parse(new ByteArrayInputStream(testDataString.getBytes()), "http://not.the.right.base.uri");

        IRI albertEinstein = valueFactory.createIRI("http://dbpedia.org/resource/Albert_Einstein");
        IRI foafName = valueFactory.createIRI("http://xmlns.com/foaf/0.1/name");
        Literal aeName = valueFactory.createLiteral("Albert Einstein");

        Statement aeNameStatement = valueFactory.createStatement(albertEinstein, foafName, aeName);

        referenceModel.add(aeNameStatement);

        assertEquals(1, testModel.size());
        assertEquals(referenceModel, testModel);
        assertTrue(referenceModel.containsAll(testModel));

    }

    @Test
    public void basicIRItoTypedLiteralTest() {
        String testDataString = """
                <html>
                  <head>
                  <meta xmlns:dbp="http://dbpedia.org/property/" xmlns:foaf="http://xmlns.com/foaf/0.1/" xmlns:xsd="http://www.w3.org/2001/XMLSchema#"></meta>
                  </head>
                  <body>
                    <div about="http://dbpedia.org/resource/Albert_Einstein">
                      <span property="dbp:dateOfBirth" datatype="xsd:date">1879-03-14</span>
                    </div>
                  </body>
                </html>
                """;

        Model testModel = new CoreseModel();
        Model referenceModel = new CoreseModel();

        RDFParser parser = new ParserFactory().createRDFParser(RDFFormat.RDFA, testModel, valueFactory);

        parser.parse(new ByteArrayInputStream(testDataString.getBytes()), "http://not.the.right.base.uri");

        IRI albertEinstein = valueFactory.createIRI("http://dbpedia.org/resource/Albert_Einstein");
        IRI dateOfBirth = valueFactory.createIRI("http://dbpedia.org/property/dateOfBirth");
        Literal aeDateOfBirth = valueFactory.createLiteral("1879-03-14", XSD.xsdDate.getIRI());

        Statement aeDateOfBirthStatement = valueFactory.createStatement(albertEinstein, dateOfBirth, aeDateOfBirth);

        referenceModel.add(aeDateOfBirthStatement);

        assertEquals(1, testModel.size());
        assertEquals(referenceModel.size(), testModel.size());
        Iterator<Statement> itStatementRef = referenceModel.iterator();
        Iterator<Statement> itStatementTest = testModel.iterator();
        while(itStatementRef.hasNext() && itStatementTest.hasNext()) {
            Statement statementRef = itStatementRef.next();
            Statement statementTest = itStatementTest.next();
            assertEquals(statementRef.getSubject(), statementTest.getSubject());
            assertEquals(statementRef.getPredicate(), statementTest.getPredicate());
            assertEquals(statementRef.getObject().isLiteral(), statementTest.getObject().isLiteral());
            if(statementRef.getObject().isLiteral()) {
                assertEquals(((Literal) statementRef.getObject()).getDatatype(), ((Literal) statementTest.getObject()).getDatatype());
            }
            assertEquals(statementRef.getObject(), statementTest.getObject());
            assertEquals(statementRef.getContext(), statementTest.getContext());
        }
        assertTrue(referenceModel.containsAll(testModel));
    }

    @Test
    public void basicChainTest() {
        String testDataString = """
                <html>
                  <head>
                  <meta xmlns:dbp="http://dbpedia.org/property/" xmlns:foaf="http://xmlns.com/foaf/0.1/" xmlns:xsd="http://www.w3.org/2001/XMLSchema#"></meta>
                  </head>
                  <body>
                <div about="http://dbpedia.org/resource/Albert_Einstein">
                  <div rel="dbp:birthPlace" resource="http://dbpedia.org/resource/Germany" />
                  <span about="http://dbpedia.org/resource/Germany"
                    property="dbp:conventionalLongName">Federal Republic of Germany</span>
                </div>
                  </body>
                </html>
                """;

        Model testModel = new CoreseModel();
        Model referenceModel = new CoreseModel();

        RDFParser parser = new ParserFactory().createRDFParser(RDFFormat.RDFA, testModel, valueFactory);

        parser.parse(new ByteArrayInputStream(testDataString.getBytes()), "http://not.the.right.base.uri");

        IRI albertEinstein = valueFactory.createIRI("http://dbpedia.org/resource/Albert_Einstein");
        IRI birthPlace = valueFactory.createIRI("http://dbpedia.org/property/birthPlace");
        IRI germany = valueFactory.createIRI("http://dbpedia.org/resource/Germany");
        IRI conventionalLongName = valueFactory.createIRI("http://dbpedia.org/property/conventionalLongName");
        Literal gerLongName = valueFactory.createLiteral("Federal Republic of Germany");

        Statement aeBirthPlaceStatement = valueFactory.createStatement(albertEinstein, birthPlace, germany);
        Statement germanyNameStatement = valueFactory.createStatement(germany, conventionalLongName, gerLongName);

        referenceModel.add(aeBirthPlaceStatement);
        referenceModel.add(germanyNameStatement);

        assertEquals(2, testModel.size());
        assertEquals(referenceModel, testModel);
        assertTrue(referenceModel.containsAll(testModel));

    }

    @Test
    public void inheritSubjectTest() {
        String testDataString = """
                <!DOCTYPE html>
                <html prefix="dc: http://purl.org/dc/elements/1.1/">
                  <head>
                	<title>Test 0020</title>
                  </head>
                  <body>
                    <div about="photo1.jpg">
                      <span class="attribution-line">this photo was taken by
                        <span property="dc:creator">Mark Birbeck</span>
                      </span>
                    </div>
                  </body>
                </html>
                """;

        Model testModel = new CoreseModel();
        Model referenceModel = new CoreseModel();

        RDFParser parser = new ParserFactory().createRDFParser(RDFFormat.RDFA, testModel, valueFactory);

        parser.parse(new ByteArrayInputStream(testDataString.getBytes()), "http://inria.fr/");

        IRI photo1 = valueFactory.createIRI("http://inria.fr/photo1.jpg");
        IRI creator = valueFactory.createIRI("http://purl.org/dc/elements/1.1/creator");
        Literal name = valueFactory.createLiteral("Mark Birbeck");

        Statement aeDateOfBirthStatement = valueFactory.createStatement(photo1, creator, name);

        referenceModel.add(aeDateOfBirthStatement);

        logModelContent(referenceModel);
        logModelContent(testModel);

        assertEquals(1, testModel.size());
        assertEquals(referenceModel.size(), testModel.size());
        Iterator<Statement> itStatementRef = referenceModel.iterator();
        Iterator<Statement> itStatementTest = testModel.iterator();
        while(itStatementRef.hasNext() && itStatementTest.hasNext()) {
            Statement statementRef = itStatementRef.next();
            Statement statementTest = itStatementTest.next();
            assertEquals(statementRef.getSubject(), statementTest.getSubject());
            assertEquals(statementRef.getPredicate(), statementTest.getPredicate());
            assertEquals(statementRef.getObject(), statementTest.getObject());
            assertEquals(statementRef.getContext(), statementTest.getContext());
        }
        assertTrue(referenceModel.containsAll(testModel));
    }

    private static void logModelContent(Model model) {
        StringWriter outWriter = new StringWriter();
        RDFSerializer serializer = (new SerializerFactory()).createSerializer(RDFFormat.TURTLE, model);
        serializer.write(outWriter);
        logger.info("{}", outWriter.toString());
    }
}