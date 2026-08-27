package fr.inria.corese.core.next.data.impl.io.parser.rdfa;

import fr.inria.corese.core.next.data.api.term.*;
import fr.inria.corese.core.next.data.api.model.*;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.io.parser.RDFParser;
import fr.inria.corese.core.next.data.api.io.serializer.RDFSerializer;
import fr.inria.corese.core.next.data.api.vocabulary.RDF;
import fr.inria.corese.core.next.data.api.vocabulary.XSD;
import fr.inria.corese.core.next.data.impl.io.parser.DefaultRDFParserFactory;
import fr.inria.corese.core.next.data.impl.io.serializer.DefaultRDFSerializerFactory;
import fr.inria.corese.core.next.data.impl.io.parser.support.ParserTestBase;
import fr.inria.corese.core.next.data.impl.adapter.CoreseValueFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RDFaParserTest extends ParserTestBase {

    private static final Logger logger = LoggerFactory.getLogger(RDFaParserTest.class);

    private final DefaultRDFParserFactory parserFactory = new DefaultRDFParserFactory();
    private final ValueFactory valueFactory = new CoreseValueFactory();
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
        Model model = createTestModel();
        ValueFactory factory = new CoreseValueFactory();
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

        Model parsedModel = createTestModel();
        Model resultModel = createTestModel();
        ValueFactory factory = new CoreseValueFactory();
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
        String currentSubjectNTriples = defaultTurtlePrefixes + """
                <> dc:creator "Jo" .
                """;

        Model parsedModel = createTestModel();
        Model resultModel = createTestModel();
        ValueFactory factory = new CoreseValueFactory();
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
    void basicBaseTest() {
        String testDataString = """
<?xml version="1.0" encoding="UTF-8"?>
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

        Model testModel = createTestModel();
        Model referenceModel = createTestModel();

        IRI subject = valueFactory.createIRI("http://www.w3.org/2006/07/SWD/RDFa/testsuite/xhtml1-testcases/photo1.jpg");
        IRI predicate = valueFactory.createIRI("http://purl.org/dc/elements/1.1/creator");
        Literal object = valueFactory.createLiteral("Mark Birbeck");

        referenceModel.add(subject, predicate, object);

        RDFParser parser = new DefaultRDFParserFactory().createRDFParser(RDFFormat.RDFA, testModel, valueFactory);

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
    void baseElementIsAppliedInXmlHost() {
        String xml = """
                <root>
                  <head><base href="http://example.org/"/></head>
                  <body>
                    <p about="#me" property="http://example.org/name">Ivan Herman</p>
                  </body>
                </root>
                """;

        Model model = createTestModel();
        RDFParser parser = parserFactory.createRDFParser(RDFFormat.RDFA, model, valueFactory);
        parser.parse(new ByteArrayInputStream(xml.getBytes()), "http://example.test/document.xml");

        IRI subject = valueFactory.createIRI("http://example.org/#me");
        IRI predicate = valueFactory.createIRI("http://example.org/name");
        Literal object = valueFactory.createLiteral("Ivan Herman");

        assertEquals(1, model.size());
        assertTrue(model.contains(subject, predicate, object));
    }

    @Test
    void baseElementDoesNotChangeXmlDocumentSubject() {
        String xml = """
                <root>
                  <head><base href="http://example.org/"/></head>
                  <body>
                    <p property="http://example.org/name">Ivan Herman</p>
                  </body>
                </root>
                """;

        Model model = createTestModel();
        RDFParser parser = parserFactory.createRDFParser(RDFFormat.RDFA, model, valueFactory);
        parser.parse(new ByteArrayInputStream(xml.getBytes()), "http://example.test/document.xml");

        IRI subject = valueFactory.createIRI("http://example.test/document.xml");
        IRI predicate = valueFactory.createIRI("http://example.org/name");
        Literal object = valueFactory.createLiteral("Ivan Herman");

        assertEquals(1, model.size());
        assertTrue(model.contains(subject, predicate, object));
    }

    @Test
    void serializesXmlLiteralDescendantsWithNamespaces() {
        String xhtml = """
                <html xmlns="http://www.w3.org/1999/xhtml"
                      prefix="foaf: http://xmlns.com/foaf/0.1/ rdf: http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                  <body about="http://example.org/mark">
                    <h2 property="foaf:name" datatype="rdf:XMLLiteral"><span property="foaf:firstName">Mark</span></h2>
                  </body>
                </html>
                """;

        Model model = createTestModel();
        RDFParser parser = parserFactory.createRDFParser(RDFFormat.RDFA, model, valueFactory);
        parser.parse(new ByteArrayInputStream(xhtml.getBytes()), "http://example.org/document");

        IRI subject = valueFactory.createIRI("http://example.org/mark");
        IRI predicate = valueFactory.createIRI("http://xmlns.com/foaf/0.1/name");
        Literal object = null;
        for (Statement statement : model) {
            if (statement.getSubject().equals(subject) && statement.getPredicate().equals(predicate)) {
                object = (Literal) statement.getObject();
                break;
            }
        }

        assertNotNull(object);
        assertTrue(model.contains(subject, predicate, object));
        assertEquals(RDF.XMLLiteral.getIRI(), object.getDatatype());
        assertEquals("<span property=\"foaf:firstName\" xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:foaf=\"http://xmlns.com/foaf/0.1/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">Mark</span>", object.getLabel());
    }

    @Test
    void incompleteRelationsDoNotLeakToFollowingSiblings() {
        String xhtml = """
                <html prefix="foaf: http://xmlns.com/foaf/0.1/">
                  <body>
                    <div about="http://example.org/alice" rel="foaf:knows">
                      <span about="http://example.org/bob" property="foaf:name">Bob</span>
                    </div>
                    <div about="http://example.org/carol" property="foaf:name">Carol</div>
                  </body>
                </html>
                """;

        Model model = createTestModel();
        RDFParser parser = parserFactory.createRDFParser(RDFFormat.RDFA, model, valueFactory);
        parser.parse(new ByteArrayInputStream(xhtml.getBytes()), "http://example.org/document");

        IRI alice = valueFactory.createIRI("http://example.org/alice");
        IRI bob = valueFactory.createIRI("http://example.org/bob");
        IRI carol = valueFactory.createIRI("http://example.org/carol");
        IRI knows = valueFactory.createIRI("http://xmlns.com/foaf/0.1/knows");
        IRI name = valueFactory.createIRI("http://xmlns.com/foaf/0.1/name");

        assertEquals(3, model.size());
        assertTrue(model.contains(alice, knows, bob));
        assertTrue(model.contains(bob, name, valueFactory.createLiteral("Bob")));
        assertTrue(model.contains(carol, name, valueFactory.createLiteral("Carol")));
    }

    @Test
    void xmlHostUsesOnlyRdfaCoreTerms() {
        String xml = """
                <root>
                  <a rel="alternate" href="http://example.org/alternate"/>
                  <a rel="license" href="http://example.org/license"/>
                </root>
                """;

        Model model = createTestModel();
        RDFParser parser = parserFactory.createRDFParser(RDFFormat.RDFA, model, valueFactory);
        parser.parse(new ByteArrayInputStream(xml.getBytes()), "http://example.org/document");

        IRI subject = valueFactory.createIRI("http://example.org/document");
        IRI alternate = valueFactory.createIRI("http://www.w3.org/1999/xhtml/vocab#alternate");
        IRI license = valueFactory.createIRI("http://www.w3.org/1999/xhtml/vocab#license");

        assertEquals(1, model.size());
        assertFalse(model.contains(subject, alternate, null));
        assertTrue(model.contains(subject, license, valueFactory.createIRI("http://example.org/license")));
    }

    @Test
    void htmlTimeUsesDatetimeValueAndInferredDatatype() {
        String xhtml = """
                <html prefix="ex: http://example.org/">
                  <body about="http://example.org/event">
                    <time property="ex:date" datetime="2012-03-18Z">18 March 2012</time>
                    <time property="ex:label" lang="en" datetime="D-Day">ignored</time>
                  </body>
                </html>
                """;

        Model model = createTestModel();
        RDFParser parser = parserFactory.createRDFParser(RDFFormat.RDFA, model, valueFactory);
        parser.parse(new ByteArrayInputStream(xhtml.getBytes()), "http://example.org/document");

        IRI subject = valueFactory.createIRI("http://example.org/event");
        assertEquals(2, model.size());
        assertTrue(model.contains(subject, valueFactory.createIRI("http://example.org/date"),
                valueFactory.createLiteral("2012-03-18Z", XSD.xsdDate.getIRI())));
        assertTrue(model.contains(subject, valueFactory.createIRI("http://example.org/label"),
                valueFactory.createLiteral("D-Day", "en")));
    }

    @Test
    void xhtmlOneIgnoresXmlBaseAndNormalizesAbsoluteIris() {
        String xhtml = """
                <html version="XHTML+RDFa 1.1" prefix="ex: http://example.org/">
                  <body xml:base="http://example.org/invalid/">
                    <span about="" property="ex:title">Title</span>
                    <a about="" rel="ex:target" href="http://example.org/foo/.."/>
                  </body>
                </html>
                """;

        Model model = createTestModel();
        RDFParser parser = parserFactory.createRDFParser(RDFFormat.RDFA, model, valueFactory);
        parser.parse(new ByteArrayInputStream(xhtml.getBytes()), "http://example.org/document");

        IRI subject = valueFactory.createIRI("http://example.org/document");
        assertEquals(2, model.size());
        assertTrue(model.contains(subject, valueFactory.createIRI("http://example.org/title"),
                valueFactory.createLiteral("Title")));
        assertTrue(model.contains(subject, valueFactory.createIRI("http://example.org/target"),
                valueFactory.createIRI("http://example.org/")));
    }

    @Test
    void aboutTest() {
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

        Model testModel = createTestModel();

        RDFParser parser = new DefaultRDFParserFactory().createRDFParser(RDFFormat.RDFA, testModel, valueFactory);

        parser.parse(new ByteArrayInputStream(testDataString.getBytes()), "http://not.the.right.base.uri");

        IRI subject = valueFactory.createIRI("http://w3id.org/people/pierre-maillot");
        IRI object = valueFactory.createIRI("http://xmlns.com/foaf/0.1/Person");

        logModelContent(testModel);

        assertEquals(1, testModel.size());
        assertTrue(testModel.contains(subject, RDF.type.getIRI(), object));
    }

    @Test
    void basicIRItoIRITest() {
        String testDataString = """
                <html xmlns:dbp="http://dbpedia.org/property/" xmlns:foaf="http://xmlns.com/foaf/0.1/" xmlns:xsd="http://www.w3.org/2001/XMLSchema#">
                  <head>
                  </head>
                  <body>
                    <div about="http://dbpedia.org/resource/Albert_Einstein">
                      <div rel="dbp:birthPlace" resource="http://dbpedia.org/resource/Germany">
                      </div>
                    </div>
                  </body>
                </html>
                """;

        Model testModel = createTestModel();
        Model referenceModel = createTestModel();

        RDFParser parser = new DefaultRDFParserFactory().createRDFParser(RDFFormat.RDFA, testModel, valueFactory);

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
    void basicIRItoStringTest() {
        String testDataString = """
                <html xmlns:dbp="http://dbpedia.org/property/" xmlns:foaf="http://xmlns.com/foaf/0.1/" xmlns:xsd="http://www.w3.org/2001/XMLSchema#">
                  <head>
                  </head>
                  <body>
                <div about="http://dbpedia.org/resource/Albert_Einstein">
                  <span property="foaf:name">Albert Einstein</span>
                </div>
                  </body>
                </html>
                """;

        Model testModel = createTestModel();
        Model referenceModel = createTestModel();

        RDFParser parser = new DefaultRDFParserFactory().createRDFParser(RDFFormat.RDFA, testModel, valueFactory);

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
    void basicIRItoTypedLiteralTest() {
        String testDataString = """
                <html xmlns:dbp="http://dbpedia.org/property/" xmlns:foaf="http://xmlns.com/foaf/0.1/" xmlns:xsd="http://www.w3.org/2001/XMLSchema#">
                  <head>
                  </head>
                  <body>
                    <div about="http://dbpedia.org/resource/Albert_Einstein">
                      <span property="dbp:dateOfBirth" datatype="xsd:date">1879-03-14</span>
                    </div>
                  </body>
                </html>
                """;

        Model testModel = createTestModel();
        Model referenceModel = createTestModel();

        RDFParser parser = new DefaultRDFParserFactory().createRDFParser(RDFFormat.RDFA, testModel, valueFactory);

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
    void basicChainTest() {
        String testDataString = """
                <html xmlns:dbp="http://dbpedia.org/property/" xmlns:foaf="http://xmlns.com/foaf/0.1/" xmlns:xsd="http://www.w3.org/2001/XMLSchema#">
                  <head>
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

        Model testModel = createTestModel();
        Model referenceModel = createTestModel();

        RDFParser parser = new DefaultRDFParserFactory().createRDFParser(RDFFormat.RDFA, testModel, valueFactory);

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
    void inheritSubjectTest() {
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

        Model testModel = createTestModel();
        Model referenceModel = createTestModel();

        RDFParser parser = new DefaultRDFParserFactory().createRDFParser(RDFFormat.RDFA, testModel, valueFactory);

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

    @Test
    void multiplePrefixDeclaration() {
        String testDataString = """
                <!DOCTYPE html>
                <html prefix="dc: http://purl.org/dc/elements/1.1/ p2: https://schema.org/">
                  <head>
                	<title>Test 0020</title>
                  </head>
                  <body>
                    <div about="photo1.jpg">
                      <span class="attribution-line">this photo was taken by
                        <span property="dc:creator">Mark Birbeck</span>
                        and <span property="p2:creator">John Doe</span>
                      </span>
                    </div>
                  </body>
                </html>
                """;

        Model testModel = createTestModel();
        Model referenceModel = createTestModel();

        RDFParser parser = new DefaultRDFParserFactory().createRDFParser(RDFFormat.RDFA, testModel, valueFactory);

        parser.parse(new ByteArrayInputStream(testDataString.getBytes()), "http://inria.fr/");

        IRI photo1 = valueFactory.createIRI("http://inria.fr/photo1.jpg");
        IRI creator1 = valueFactory.createIRI("http://purl.org/dc/elements/1.1/creator");
        Literal name1 = valueFactory.createLiteral("Mark Birbeck");
        IRI creator2 = valueFactory.createIRI("https://schema.org/creator");
        Literal name2 = valueFactory.createLiteral("John Doe");

        Statement stat1 = valueFactory.createStatement(photo1, creator1, name1);
        Statement stat2 = valueFactory.createStatement(photo1, creator2, name2);

        referenceModel.add(stat1);
        referenceModel.add(stat2);

        logModelContent(referenceModel);
        logModelContent(testModel);

        assertEquals(2, testModel.size());
        assertEquals(referenceModel, testModel);
        assertTrue(referenceModel.containsAll(testModel));
    }

    @Test
    @DisplayName("The XHTML-RDFa DTD has been shown to throw 429 error code if the parser is not configured correctly, as by default.")
    void test429ErrorOnDTD() {
        String testDataString = """
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML+RDFa 1.0//EN" "http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd">
                <html>
                  <head>
                  </head>
                  <body>
                    <p xmlns:foaf="http://xmlns.com/foaf/0.1/" about="http://w3id.org/people/pierre-maillot" typeof="foaf:Person">
                      Hello, I'm Pierre.
                    </p>
                  </body>
                </html>""";

        Model testModel = createTestModel();

        RDFaParserOptions.Builder builder = new RDFaParserOptions.Builder();
        RDFaParserOptions options = builder.build();
        RDFaParser parser = (RDFaParser) new DefaultRDFParserFactory().createRDFParser(RDFFormat.RDFA, testModel, valueFactory, options);

        parser.parse(new ByteArrayInputStream(testDataString.getBytes()), "http://not.the.right.base.uri");

        IRI subject = valueFactory.createIRI("http://w3id.org/people/pierre-maillot");
        IRI object = valueFactory.createIRI("http://xmlns.com/foaf/0.1/Person");

        logModelContent(testModel);

        assertEquals(1, testModel.size());
        assertTrue(testModel.contains(subject, RDF.type.getIRI(), object));
    }

    @Test
    void parseXmlnsWithRelativeUri() {
        String xhtml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML+RDFa 1.0//EN" "http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd">
                <html xmlns="http://www.w3.org/1999/xhtml"
                      xmlns:ex="vocab#"
                      version="XHTML+RDFa 1.1">
                  <body>
                    <p about="#me" typeof="ex:Person" property="ex:name">Alice</p>
                  </body>
                </html>""";

        Model model = createTestModel();
        RDFParser parser = parserFactory.createRDFParser(RDFFormat.RDFA, model, valueFactory);
        parser.parse(new ByteArrayInputStream(xhtml.getBytes()), "http://example.org/test/");

        IRI subject = valueFactory.createIRI("http://example.org/test/#me");
        IRI type = RDF.type.getIRI();
        IRI personClass = valueFactory.createIRI("http://example.org/test/vocab#Person");
        IRI namePred = valueFactory.createIRI("http://example.org/test/vocab#name");

        assertTrue(model.contains(subject, type, personClass));
        assertTrue(model.contains(subject, namePred, valueFactory.createLiteral("Alice")));
    }

    private static void logModelContent(Model model) {
        StringWriter outWriter = new StringWriter();
        RDFSerializer serializer = (new DefaultRDFSerializerFactory()).createSerializer(RDFFormat.TURTLE, model);
        serializer.write(outWriter);
        logger.info("{}", outWriter);
    }
}
